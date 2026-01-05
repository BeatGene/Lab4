package org.ztglab.workspace;

import org.ztglab.workspace.editor.operations.XmlOperation;
import org.ztglab.command.CommandBus;
import org.ztglab.spellcheck.ISpellChecker;
import org.ztglab.spellcheck.LanguageToolAdapter;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XML编辑器 - 基于树形结构的XML文件编辑器
 * 
 * 架构设计:
 * - 物理视图: Document (List<String> lines) - 文件在内存中的实际存储
 * - 逻辑视图: XmlNode树 - XML的DOM树结构
 * - 同步策略: 每次操作后立即将DOM树序列化回Document
 */
public class XmlEditor implements IEditor {

    private final Map<String, EditorCommand> commandMap = new HashMap<>();
    private final ISpellChecker checker = new LanguageToolAdapter();

    public XmlEditor() {
        initCommands();
    }

    private void initCommands() {
        commandMap.put("insert-before", this::handleInsertBefore);
        commandMap.put("append-child", this::handleAppendChild);
        commandMap.put("edit-id", this::handleEditId);
        commandMap.put("edit-text", this::handleEditText);
        commandMap.put("delete", this::handleDelete);
        commandMap.put("xml-tree", this::handleXmlTree);
        commandMap.put("spellcheck", this::handleSpellCheck);
    }

    @Override
    public void registerCommands(CommandBus bus, Workspace workspace) {
        // XML编辑器不需要注册额外的全局命令
        // 所有命令通过 EditorCommandRequest 分发
    }

    @Override
    public void initDocument(Document doc) {
        doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        doc.append("<root id=\"root\">\n");
        doc.append("</root>");
    }

    @Override
    public EditorCommand resolveCommand(String name) {
        return commandMap.get(name.toLowerCase());
    }

    // ==================== 命令处理器 ====================

    /**
     * 处理 insert-before 命令
     * 格式: insert-before <tagName> <newId> <targetId> ["text"]
     */
    private void handleInsertBefore(Document doc, String args) throws Exception {
        String[] parts = args.trim().split("\\s+", 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException("用法: insert-before <tagName> <newId> <targetId> [\"text\"]");
        }

        String tagName = parts[0];
        String newId = parts[1];
        String targetId = parts[2];
        String text = parts.length == 4 ? extractQuotedText(parts[3]) : "";

        insertBefore(doc, tagName, newId, targetId, text);
    }

    /**
     * 处理 append-child 命令
     * 格式: append-child <tagName> <newId> <parentId> ["text"]
     */
    private void handleAppendChild(Document doc, String args) throws Exception {
        String[] parts = args.trim().split("\\s+", 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException("用法: append-child <tagName> <newId> <parentId> [\"text\"]");
        }

        String tagName = parts[0];
        String newId = parts[1];
        String parentId = parts[2];
        String text = parts.length == 4 ? extractQuotedText(parts[3]) : "";

        appendChild(doc, tagName, newId, parentId, text);
    }

    /**
     * 处理 edit-id 命令
     * 格式: edit-id <oldId> <newId>
     */
    private void handleEditId(Document doc, String args) throws Exception {
        String[] parts = args.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("用法: edit-id <oldId> <newId>");
        }

        String oldId = parts[0];
        String newId = parts[1];

        editId(doc, oldId, newId);
    }

    /**
     * 处理 edit-text 命令
     * 格式: edit-text <elementId> ["text"]
     */
    private void handleEditText(Document doc, String args) throws Exception {
        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length < 1) {
            throw new IllegalArgumentException("用法: edit-text <elementId> [\"text\"]");
        }

        String elementId = parts[0];
        String text = parts.length == 2 ? extractQuotedText(parts[1]) : "";

        editText(doc, elementId, text);
    }

    /**
     * 处理 delete 命令
     * 格式: delete <elementId>
     */
    private void handleDelete(Document doc, String args) throws Exception {
        String elementId = args.trim();
        if (elementId.isEmpty()) {
            throw new IllegalArgumentException("用法: delete <elementId>");
        }

        deleteElement(doc, elementId);
    }

    /**
     * 处理 xml-tree 命令
     * 格式: xml-tree [file]
     */
    private void handleXmlTree(Document doc, String args) throws Exception {
        // xml-tree 命令显示XML树形结构
        String treeStr = showXmlTree(doc);
        System.out.println(treeStr);
    }
    // ==================== XML 操作方法 ====================

    /**
     * 在目标元素前插入新元素
     */
    private void insertBefore(Document doc, String tagName, String newId, String targetId, String text) throws Exception {
        XmlNode root = parseDocument(doc);
        
        // 查找目标节点
        XmlNode target = findNodeById(root, targetId);
        if (target == null) {
            throw new IllegalArgumentException("目标元素不存在: " + targetId);
        }

        // 检查根元素
        if (target.getParent() == null) {
            throw new IllegalArgumentException("不能在根元素前插入元素");
        }

        // 检查ID唯一性
        if (findNodeById(root, newId) != null) {
            throw new IllegalArgumentException("元素ID已存在: " + newId);
        }

        // 创建新节点
        XmlNode newNode = new XmlNode(tagName, newId);
        if (text != null && !text.isEmpty()) {
            newNode.setTextContent(text);
        }

        // 插入到目标节点前
        XmlNode parent = target.getParent();
        List<XmlNode> children = parent.getChildren();
        int index = children.indexOf(target);
        children.add(index, newNode);
        newNode.setParent(parent);

        // 同步回Document
        syncToDocument(doc, root, "insert-before " + tagName + " " + newId);
    }

    /**
     * 在父元素下添加子元素
     */
    private void appendChild(Document doc, String tagName, String newId, String parentId, String text) throws Exception {
        XmlNode root = parseDocument(doc);
        
        // 查找父节点
        XmlNode parent = findNodeById(root, parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父元素不存在: " + parentId);
        }

        // 检查ID唯一性
        if (findNodeById(root, newId) != null) {
            throw new IllegalArgumentException("元素ID已存在: " + newId);
        }

        // 创建新节点
        XmlNode newNode = new XmlNode(tagName, newId);
        if (text != null && !text.isEmpty()) {
            newNode.setTextContent(text);
        }

        // 添加子节点
        parent.addChild(newNode);

        // 同步回Document
        syncToDocument(doc, root, "append-child " + tagName + " " + newId);
    }

    /**
     * 修改元素ID
     */
    private void editId(Document doc, String oldId, String newId) throws Exception {
        XmlNode root = parseDocument(doc);
        
        // 查找目标节点
        XmlNode target = findNodeById(root, oldId);
        if (target == null) {
            throw new IllegalArgumentException("目标元素不存在: " + oldId);
        }

        // 检查是否是根元素
        if (target.getParent() == null) {
            throw new IllegalArgumentException("不建议修改根元素ID");
        }

        // 检查新ID是否已存在
        if (findNodeById(root, newId) != null) {
            throw new IllegalArgumentException("元素ID已存在: " + newId);
        }

        // 修改ID
        target.setId(newId);

        // 同步回Document
        syncToDocument(doc, root, "edit-id " + oldId + " -> " + newId);
    }

    /**
     * 修改元素文本内容
     */
    private void editText(Document doc, String elementId, String text) throws Exception {
        XmlNode root = parseDocument(doc);
        
        // 查找目标节点
        XmlNode target = findNodeById(root, elementId);
        if (target == null) {
            throw new IllegalArgumentException("目标元素不存在: " + elementId);
        }

        // 修改文本内容
        target.setTextContent(text == null ? "" : text);

        // 同步回Document
        syncToDocument(doc, root, "edit-text " + elementId);
    }

    /**
     * 删除元素
     */
    private void deleteElement(Document doc, String elementId) throws Exception {
        XmlNode root = parseDocument(doc);
        
        // 查找目标节点
        XmlNode target = findNodeById(root, elementId);
        if (target == null) {
            throw new IllegalArgumentException("目标元素不存在: " + elementId);
        }

        // 检查是否是根元素
        if (target.getParent() == null) {
            throw new IllegalArgumentException("不能删除根元素");
        }

        // 删除节点
        XmlNode parent = target.getParent();
        parent.removeChild(target);

        // 同步回Document
        syncToDocument(doc, root, "delete " + elementId);
    }

    /**
     * 显示XML树形结构
     */
    private String showXmlTree(Document doc) throws Exception {
        XmlNode root = parseDocument(doc);
        StringBuilder sb = new StringBuilder();
        buildTreeString(root, "", true, sb);
        return sb.toString();
    }

    /**
     * 递归构建树形字符串
     */
    private void buildTreeString(XmlNode node, String prefix, boolean isLast, StringBuilder sb) {
        // 构建当前节点的显示
        sb.append(prefix);
        if (!prefix.isEmpty()) {
            sb.append(isLast ? "└── " : "├── ");
        }
        
        // 显示标签名和属性
        sb.append(node.getTagName()).append(" [id=\"").append(node.getId()).append("\"");
        for (Map.Entry<String, String> entry : node.getAttributes().entrySet()) {
            if (!entry.getKey().equals("id")) {
                sb.append(", ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }
        sb.append("]");
        
        // 显示文本内容
        if (!node.getTextContent().isEmpty()) {
            sb.append("\n").append(prefix);
            if (!prefix.isEmpty()) {
                sb.append(isLast ? "    " : "│   ");
            }
            sb.append("└── \"").append(node.getTextContent()).append("\"");
        }
        
        sb.append("\n");
        
        // 递归处理子节点
        List<XmlNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            String newPrefix = prefix;
            if (!prefix.isEmpty()) {
                newPrefix += (isLast ? "    " : "│   ");
            }
            buildTreeString(children.get(i), newPrefix, i == children.size() - 1, sb);
        }
    }

    // ==================== IEditor 接口实现 ====================

    @Override
    public boolean undo(Document doc) {
        boolean success = doc.getHistory().undo();
        if (success) {
            doc.setModified(true);
        }
        return success;
    }

    @Override
    public boolean redo(Document doc) {
        boolean success = doc.getHistory().redo();
        if (success) {
            doc.setModified(true);
        }
        return success;
    }

    @Override
    public String show(Document doc, int start, int end) {
        // XML编辑器使用标准的行显示
        int totalLines = doc.getLineCount();
        
        if (totalLines == 0) {
            return "";
        }
        
        if (start <= 0 || end <= 0) {
            start = 1;
            end = totalLines;
        }
        
        if (start < 1) {
            start = 1;
        }
        
        if (end > totalLines) {
            end = totalLines;
        }
        
        if (start > end) {
            return "";
        }
        
        String content = doc.getContent();
        String[] lines = content.split("\n", -1);
        
        StringBuilder result = new StringBuilder();
        for (int i = start - 1; i < end; i++) {
            if (i >= 0 && i < lines.length) {
                result.append(i + 1).append(": ").append(lines[i]);
                if (i < end - 1) {
                    result.append("\n");
                }
            }
        }
        
        return result.toString();
    }

    @Override
    public String show(Document doc) {
        return show(doc, 0, 0);
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析Document为XML树
     */
    private XmlNode parseDocument(Document doc) throws Exception {
        List<String> lines = new ArrayList<>();
        String content = doc.getContent();
        if (content.isEmpty()) {
            throw new Exception("文档为空");
        }
        
        String[] lineArray = content.split("\n", -1);
        for (String line : lineArray) {
            lines.add(line);
        }
        
        return listToXmlTree(lines);
    }

    /**
     * 将XML树同步回Document
     */
    private void syncToDocument(Document doc, XmlNode root, String description) {
        List<String> lines = xmlTreeToList(root);
        String newContent = String.join("\n", lines);
        
        // 创建操作并通过History执行（支持undo/redo）
        XmlOperation operation = new XmlOperation(doc, newContent, description);
        doc.getHistory().execute(operation);
        doc.setModified(true);
    }

    /**
     * 根据ID查找节点
     */
    private XmlNode findNodeById(XmlNode root, String id) {
        if (root.getId().equals(id)) {
            return root;
        }
        
        for (XmlNode child : root.getChildren()) {
            XmlNode found = findNodeById(child, id);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }

    /**
     * 提取引号内的文本
     */
    private String extractQuotedText(String input) {
        input = input.trim();
        int firstQuote = input.indexOf('"');
        int lastQuote = input.lastIndexOf('"');
        if (firstQuote == -1 || lastQuote == -1 || firstQuote == lastQuote) {
            return "";
        }
        String content = input.substring(firstQuote + 1, lastQuote);
        return content.replace("\\n", "\n");
    }

    // ==================== XML 节点类和解析方法 ====================

    public static class XmlNode {
        private String tagName;        // 标签名
        private String id;             // 元素ID（必须唯一）
        private String textContent;    // 文本内容
        private Map<String, String> attributes; // 属性映射
        private List<XmlNode> children; // 子节点列表
        private XmlNode parent;        // 父节点
        
        /**
         * 构造函数
         * @param tagName 标签名
         * @param id 元素ID
         */
        public XmlNode(String tagName, String id) {
            this.tagName = tagName;
            this.id = id;
            this.textContent = "";
            this.attributes = new HashMap<>();
            this.children = new ArrayList<>();
            this.parent = null;
        }
        
        /**
         * 添加子节点
         * @param child 子节点
         */
        public void addChild(XmlNode child) {
            children.add(child);
            child.parent = this;
        }
        
        /**
         * 移除子节点
         * @param child 子节点
         */
        public void removeChild(XmlNode child) {
            children.remove(child);
            if (child.parent == this) {
                child.parent = null;
            }
        }
        
        // Getters and setters
        public String getTagName() {
            return tagName;
        }
        
        public void setTagName(String tagName) {
            this.tagName = tagName;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getTextContent() {
            return textContent;
        }
        
        public void setTextContent(String textContent) {
            this.textContent = textContent;
        }
        
        public Map<String, String> getAttributes() {
            return attributes;
        }
        
        public void setAttribute(String name, String value) {
            attributes.put(name, value);
        }
        
        public List<XmlNode> getChildren() {
            return children;
        }
        
        public XmlNode getParent() {
            return parent;
        }
        
        public void setParent(XmlNode parent) {
            this.parent = parent;
        }
    }
    /**
     * 将XML文本行列表转换为XML树
     * 将Document的物理视图（List<String>）转换为逻辑视图（XmlNode树）
     * @param lines XML文本行列表，代表文件的物理状态
     * @return XML树的根节点，代表文件的逻辑结构
     * @throws Exception 如果解析失败
     */
    public static XmlNode listToXmlTree(List<String> lines) throws Exception {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("XML lines cannot be null or empty");
        }
        
        Stack<XmlNode> nodeStack = new Stack<>();
        XmlNode root = null;
        
        // 正则表达式模式
        Pattern startTagPattern = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)[^>]*id=\"([^\"]*)\"[^>]*>");
        Pattern endTagPattern = Pattern.compile("</([a-zA-Z][a-zA-Z0-9]*)>");
        Pattern attributePattern = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*)=\"([^\"]*)\"");
        
        // 处理XML声明
        String firstLine = lines.get(0).trim();
        int startLine = 0;
        if (firstLine.startsWith("<?xml")) {
            startLine = 1;
        }
        
        // 遍历所有行
        for (int i = startLine; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // 跳过空行和注释
            }
            
            // 处理开始标签
            Matcher startTagMatcher = startTagPattern.matcher(line);
            if (startTagMatcher.find()) {
                String tagName = startTagMatcher.group(1);
                String id = startTagMatcher.group(2);
                
                // 创建新节点
                XmlNode currentNode = new XmlNode(tagName, id);
                
                // 解析其他属性
                Matcher attributeMatcher = attributePattern.matcher(line);
                while (attributeMatcher.find()) {
                    String attrName = attributeMatcher.group(1);
                    String attrValue = attributeMatcher.group(2);
                    if (!attrName.equals("id")) { // id已经处理过了
                        currentNode.setAttribute(attrName, attrValue);
                    }
                }
                
                // 处理嵌套关系
                if (nodeStack.isEmpty()) {
                    root = currentNode;
                } else {
                    XmlNode parent = nodeStack.peek();
                    parent.addChild(currentNode);
                }
                nodeStack.push(currentNode);
                
                // 检查是否是自闭合标签（简化处理，实际应该更严格）
                if (line.endsWith("/>") || line.matches(".*<[^>]*></[^>]*>")) {
                    nodeStack.pop();
                }
            }
            
            // 处理文本内容
            if (!nodeStack.isEmpty()) {
                // 提取标签之间的文本
                Pattern textPattern = Pattern.compile(">(.*?)(?:<|$)");
                Matcher textMatcher = textPattern.matcher(line);
                if (textMatcher.find()) {
                    String text = textMatcher.group(1).trim();
                    if (!text.isEmpty()) {
                        XmlNode currentNode = nodeStack.peek();
                        currentNode.setTextContent(text);
                    }
                }
            }
            
            // 处理结束标签
            Matcher endTagMatcher = endTagPattern.matcher(line);
            if (endTagMatcher.find()) {
                if (!nodeStack.isEmpty()) {
                    nodeStack.pop();
                }
            }
        }
        
        if (root == null) {
            throw new Exception("Invalid XML: No root element found");
        }
        
        return root;
    }
    
    /**
     * 将XML树转换为文本行列表
     * 将逻辑视图（XmlNode树）序列化为物理视图（List<String>）
     * @param root XML树的根节点，代表文件的逻辑结构
     * @return XML文本行列表，代表文件的物理状态
     */
    public static List<String> xmlTreeToList(XmlNode root) {
        List<String> lines = new ArrayList<>();
        
        // 添加XML声明
        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        
        // 递归转换XML树
        xmlNodeToLines(root, lines, 0);
        
        return lines;
    }
    
    /**
     * 递归将XML节点转换为文本行
     * @param node 当前节点
     * @param lines 行列表
     * @param indent 缩进级别
     */
    private static void xmlNodeToLines(XmlNode node, List<String> lines, int indent) {
        StringBuilder sb = new StringBuilder();
        
        // 添加缩进
        for (int i = 0; i < indent; i++) {
            sb.append("    "); // 使用4个空格作为缩进
        }
        
        // 开始标签
        sb.append("<").append(node.getTagName());
        
        // 添加id属性（必须的）
        sb.append(" id=\"").append(node.getId()).append("\"");
        
        // 添加其他属性
        for (Map.Entry<String, String> entry : node.getAttributes().entrySet()) {
            if (!entry.getKey().equals("id")) { // id已经添加过了
                sb.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }
        
        List<XmlNode> children = node.getChildren();
        String textContent = node.getTextContent();
        
        if (children.isEmpty() && textContent.isEmpty()) {
            // 自闭合标签
            sb.append(" />");
            lines.add(sb.toString());
        } else {
            // 完整标签
            sb.append(">");
            
            if (children.isEmpty()) {
                // 只有文本内容
                sb.append(textContent);
                sb.append("</").append(node.getTagName()).append(">");
                lines.add(sb.toString());
            } else {
                // 有子节点
                lines.add(sb.toString());
                
                // 添加文本内容（如果有）
                if (!textContent.isEmpty()) {
                    StringBuilder textSb = new StringBuilder();
                    for (int i = 0; i < indent + 1; i++) {
                        textSb.append("    ");
                    }
                    textSb.append(textContent);
                    lines.add(textSb.toString());
                }
                
                // 递归处理子节点
                for (XmlNode child : children) {
                    xmlNodeToLines(child, lines, indent + 1);
                }
                
                // 结束标签
                StringBuilder endSb = new StringBuilder();
                for (int i = 0; i < indent; i++) {
                    endSb.append("    ");
                }
                endSb.append("</").append(node.getTagName()).append(">");
                lines.add(endSb.toString());
            }
        }
    }
    
    private void handleSpellCheck(Document doc, String args) throws Exception {
        String xmlText = doc.getContent();

        // 解析 DOM
        javax.xml.parsers.DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document dom =
                builder.parse(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));

        List<String> results = new ArrayList<>();

        // 遍历所有文本节点
        traverse(dom.getDocumentElement(), results);

        System.out.println("拼写检查结果:");
        if (results.isEmpty()) {
            System.out.println("无拼写错误");
        } else {
            for (String s : results) {
                System.out.println(s);
            }
        }
    }

    private void traverse(Node node, List<String> results) throws Exception {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String content = node.getNodeValue().trim();
            if (!content.isEmpty()) {

                // 用 LanguageTool 检查
                List<String> issues = checker.check(content);

                // 找到父元素名，用于输出格式
                Node parent = node.getParentNode();
                String tagName = parent != null ? parent.getNodeName() : "unknown";

                for (String issue : issues) {
                    results.add(String.format("元素 %s: %s", tagName, issue));
                }
            }
        }

        // 递归子节点
        Node child = node.getFirstChild();
        while (child != null) {
            traverse(child, results);
            child = child.getNextSibling();
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 创建测试XML树
            XmlNode root = new XmlNode("bookstore", "root");
            
            XmlNode book1 = new XmlNode("book", "book1");
            book1.setAttribute("category", "COOKING");
            root.addChild(book1);
            
            XmlNode title1 = new XmlNode("title", "title1");
            title1.setAttribute("lang", "en");
            title1.setTextContent("Everyday Italian");
            book1.addChild(title1);
            
            XmlNode author1 = new XmlNode("author", "author1");
            author1.setTextContent("Giada De Laurentiis");
            book1.addChild(author1);
            
            // 转换为文本行
            List<String> lines = xmlTreeToList(root);
            System.out.println("XML Tree to List:");
            for (String line : lines) {
                System.out.println(line);
            }
            
            // 转换回XML树
            XmlNode parsedRoot = listToXmlTree(lines);
            System.out.println("\nList to XML Tree:");
            List<String> parsedLines = xmlTreeToList(parsedRoot);
            for (String line : parsedLines) {
                System.out.println(line);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

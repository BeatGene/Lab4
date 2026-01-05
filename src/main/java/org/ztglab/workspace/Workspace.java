package org.ztglab.workspace;

import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.event.EventBus;
import org.ztglab.event.events.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Workspace - 工作区实体 (聚合根)
 * 职责：
 * 1. 管理所有打开的文档 (Document)
 * 2. 维护当前活动文档 (Active Document)
 * 3. 管理编辑器注册表 (Editor Registry)
 * 4. 提供文档生命周期管理 (Open, Close, Switch)
 * 5. 发布工作区相关事件 (Event Publishing)
 */
public class Workspace {
    // ==================== 核心数据 ====================
    
    // 文件路径-文档实例的映射
    private Map<String, Document> documents = new LinkedHashMap<>();
    // 当前活动文档
    private Document activeDocument;

    // ==================== 编辑器配置 ====================

    // 编辑器注册表 (后缀 -> 编辑器)
    private Map<String, IEditor> editorRegistry = new HashMap<>();
    // 默认编辑器 (TextEditor)
    private IEditor defaultEditor;

    /**
     * 初始化工作区
     * 注册默认支持的编辑器类型
     */
    public Workspace() {
        // 初始化默认编辑器
        this.defaultEditor = new TextEditor();
        // 注册编辑器
        registerEditor("txt", defaultEditor);

        // 注册 XML 编辑器
        XmlEditor xmlEditor = new XmlEditor();
        registerEditor("xml", xmlEditor);
    }

    // ==================== 编辑器管理 ====================

    /**
     * 注册编辑器实现
     * 
     * @param extension 文件后缀 (不带点)，如 "txt", "xml"
     * @param editor    编辑器实例
     */
    public void registerEditor(String extension, IEditor editor) {
        editorRegistry.put(extension.toLowerCase(), editor);
    }

    /**
     * 获取所有已注册的编辑器
     */
    public Collection<IEditor> getRegisteredEditors() {
        return new HashSet<>(editorRegistry.values());
    }

    // ==================== 文档查询 ====================

    public Map<String, Document> getDocuments() {
        return documents;
    }

    public Document getActiveDocument() {
        return activeDocument;
    }

    /**
     * 根据文件名或路径查找文档
     * @param file 文件名或路径
     * @return 匹配的文档对象
     * @throws Exception 如果未找到或找到多个匹配项
     */
    public Document getDocument(String file) throws Exception {
        List<String> matched = documents.keySet().stream()
                .filter(p -> p.endsWith(file))
                .collect(Collectors.toList());

        if (matched.isEmpty())
            throw new Exception("文件未打开: " + file);
        if (matched.size() > 1)
            throw new Exception("存在多个同名文件，请输入完整路径");

        return documents.get(matched.get(0));
    }

    /**
     * 获取所有已修改的文档
     */
    public List<Document> getModifiedDocuments() {
        return documents.values().stream()
                .filter(Document::isModified)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前活动文档对应的编辑器服务
     */
    public IEditor getEditorService() {
        if (activeDocument == null) {
            return defaultEditor;
        }
        return getEditorService(activeDocument);
    }

    /**
     * 获取指定文档对应的编辑器服务
     */
    public IEditor getEditorService(Document doc) {
        String path = doc.getFilePath();
        String ext = getExtension(path);
        return editorRegistry.getOrDefault(ext, defaultEditor);
    }

    private String getExtension(String path) {
        if (path == null)
            return "";

        // 处理临时文件路径 (如 <unsaved-xml-...>)
        if (path.startsWith("<unsaved-")) {
            // 格式: <unsaved-TYPE-TIMESTAMP>
            String[] parts = path.split("-");
            if (parts.length >= 2) {
                String type = parts[1];
                // 保持与文件后缀一致，text -> txt
                if ("text".equals(type))
                    return "txt";
                return type;
            }
        }

        int i = path.lastIndexOf('.');
        if (i > 0) {
            return path.substring(i + 1).toLowerCase();
        }
        return "";
    }

    // ==================== 文档生命周期管理 ====================

    /**
     * 设置当前活动文档
     * 并触发 ActiveDocumentChangedEvent 事件
     */
    public void setActiveDocument(Document activeDocument) {
        // 保存旧的活动文档，用于事件发布
        Document oldDocument = this.activeDocument;

        // 更新活动文档
        this.activeDocument = activeDocument;

        // 发布活动文档切换事件（用于统计模块等）
        try {
            EventBus eventBus = ApplicationContext.getInstance().getEventBus();
            if (eventBus != null) {
                eventBus.publish(new ActiveDocumentChangedEvent(oldDocument, activeDocument));
            }
        } catch (Exception e) {
            // 事件发布失败不应影响主流程，仅打印警告
            System.err.println("[Workspace] 发布活动文档切换事件失败: " + e.getMessage());
        }
    }

    /**
     * 打开文档 (纯内存操作)
     * 如果文档已存在，则切换到该文档；否则创建新文档对象并加入管理。
     * 触发 DocumentOpenedEvent 事件。
     * 
     * @param absPath 文件绝对路径
     * @param content 文件内容 (可为null，表示空文件)
     */
    public void openDocument(String absPath, String content) throws Exception {
        if (documents.containsKey(absPath)) {
            System.out.println("文件已经加载: " + absPath);
            setActiveDocument(documents.get(absPath));
            return;
        }

        Document doc = (content == null) ? new Document() : new Document(content);
        doc.setFilePath(absPath);
        documents.put(absPath, doc);

        // 发布文档打开事件
        try {
            EventBus eventBus = ApplicationContext.getInstance().getEventBus();
            if (eventBus != null) {
                eventBus.publish(new DocumentOpenedEvent(doc));
            }
        } catch (Exception e) {
            System.err.println("[Workspace] 发布文档打开事件失败: " + e.getMessage());
        }

        setActiveDocument(doc);

        System.out.println("文件已加载: " + absPath);
    }

    /**
     * 通知工作区文件已保存
     * 更新文档的修改状态为 false
     * 
     * @param path 文件路径
     */
    public void notifySaved(String path) throws Exception {
        if (!documents.containsKey(path))
            throw new Exception("文件未在工作区中打开: " + path);
        Document doc = documents.get(path);
        doc.setModified(false);
        System.out.println("已保存: " + path);
    }

    /**
     * 通知工作区文件已另存为
     * 更新文档路径、修改状态，并更新映射关系。
     * 触发 DocumentPathUpdatedEvent 事件。
     * 
     * @param oldPath 旧路径
     * @param newPath 新路径
     */
    public void notifySavedAs(String oldPath, String newPath) throws Exception {
        if (!documents.containsKey(oldPath))
            throw new Exception("文件未在工作区中打开: " + oldPath);
        Document doc = documents.get(oldPath);

        // 更新documents map
        documents.remove(oldPath);
        documents.put(newPath, doc);

        // 更新document属性
        doc.setFilePath(newPath);
        doc.setModified(false);

        // 发布路径更新事件
        try {
            EventBus eventBus = ApplicationContext.getInstance().getEventBus();
            if (eventBus != null) {
                eventBus.publish(new DocumentPathUpdatedEvent(oldPath, newPath));
            }
        } catch (Exception e) {
            System.err.println("发布路径更新事件失败: " + e.getMessage());
        }

        System.out.println("已保存: " + newPath);
    }

    /**
     * 初始化新文档
     * 支持两种形式：
     * 1) init <text|xml> -> 创建未命名缓冲区 (如 <unsaved-text-timestamp>)
     * 2) init <filepath> -> 以指定路径创建缓冲区
     * 
     * @param fileTypeOrPath 文件类型或文件路径
     */
    public void init(String fileTypeOrPath) throws Exception {
        boolean isPath = fileTypeOrPath.contains(File.separator) || fileTypeOrPath.contains(":")
                || fileTypeOrPath.contains(".");

        String targetPath;
        if (isPath) {
            targetPath = new File(fileTypeOrPath).getAbsolutePath();
        } else {
            String kind = fileTypeOrPath.toLowerCase();
            if ("txt".equals(kind))
                kind = "text"; // 规范化为 text
            targetPath = "<unsaved-" + kind + "-" + System.currentTimeMillis() + ">";
        }

        Document doc = new Document();
        doc.setFilePath(targetPath);
        doc.setModified(true);

        // 初始化内容 - 委托给对应的编辑器
        IEditor editor = getEditorService(doc);
        if (editor != null) {
            editor.initDocument(doc);
        }

        documents.put(targetPath, doc);
        setActiveDocument(doc);

        System.out.println("新文件创建成功: " + targetPath);
    }

    /**
     * 关闭当前活动文档
     */
    public void close() throws Exception {
        if (activeDocument == null)
            throw new Exception("没有活动文件");
        close(activeDocument.getFilePath());
    }

    /**
     * 关闭指定文档
     * 移除文档管理，并自动切换到下一个可用文档（如果有）。
     * 触发 DocumentClosedEvent 事件。
     * 
     * @param file 文件名或路径
     */
    public void close(String file) throws Exception {
        Document doc = getDocument(file);
        String abs = doc.getFilePath();

        documents.remove(abs);

        // 发布文档关闭事件
        try {
            EventBus eventBus = ApplicationContext.getInstance().getEventBus();
            if (eventBus != null) {
                eventBus.publish(new DocumentClosedEvent(abs));
            }
        } catch (Exception e) {
            System.err.println("[Workspace] 发布文档关闭事件失败: " + e.getMessage());
        }

        if (!documents.isEmpty()) {
            String last = documents.keySet().stream().reduce((a, b) -> b).get();
            setActiveDocument(documents.get(last));
            System.out.println("切换到文件: " + last);
        } else {
            setActiveDocument(null);
            System.out.println("没有打开的文件了");
        }
        System.out.println("已关闭: " + abs);
    }

    /**
     * 切换编辑文档
     * 
     * @param file 文件名或路径
     */
    public void edit(String file) throws Exception {
        List<String> matched = documents.keySet().stream()
                .filter(p -> p.endsWith(file))
                .collect(Collectors.toList());

        if (matched.isEmpty())
            throw new Exception("文件未打开: " + file);
        if (matched.size() > 1)
            throw new Exception("存在多个同名文件，请输入完整路径");

        String abs = matched.get(0);
        setActiveDocument(documents.get(abs));
        System.out.println("切换到文件: " + abs);
    }

    // ==================== 系统操作 ====================

    /**
     * 退出工作区
     * 触发 WorkspaceClosingEvent 事件。
     */
    public void exit() {
        // 发布工作区关闭事件
        try {
            EventBus eventBus = ApplicationContext.getInstance().getEventBus();
            if (eventBus != null) {
                eventBus.publish(new WorkspaceClosingEvent());
            }
        } catch (Exception e) {
            System.err.println("[Workspace] 发布工作区关闭事件失败: " + e.getMessage());
        }

        System.out.println("已退出编辑器");
    }
}

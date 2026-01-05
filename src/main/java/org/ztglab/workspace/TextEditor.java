package org.ztglab.workspace;

import org.ztglab.workspace.editor.EditOperation;
import org.ztglab.workspace.editor.operations.*;
import org.ztglab.command.CommandBus;
import org.ztglab.spellcheck.ISpellChecker;
import org.ztglab.spellcheck.LanguageToolAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本编辑器服务 - 无状态
 */
public class TextEditor implements IEditor {

    private final Map<String, EditorCommand> commandMap = new HashMap<>();
    private final ISpellChecker checker = new LanguageToolAdapter();

    public TextEditor() {
        initCommands();
    }

    private void initCommands() {
        commandMap.put("append", this::handleAppend);
        commandMap.put("insert", this::handleInsert);
        commandMap.put("delete", this::handleDelete);
        commandMap.put("replace", this::handleReplace);
        commandMap.put("spellcheck", this::handleSpellCheck);
        // show 命令已提升为通用命令，但如果需要特定实现也可以在这里覆盖
    }
    @Override
    public void registerCommands(CommandBus bus, Workspace workspace) {
        // 注册通用分发器 (只需注册一次，通常由 Workspace 注册，但为了保险起见或特定覆盖)
        // 在新架构下，TextEditor 不再直接注册 AppendTextCommand 等具体 Handler
        // 而是依赖 WorkspaceManager 注册 EditorCommandRequest.Handler
    }

    @Override
    public void initDocument(Document doc) {
        // 文本文件初始化为空，无需特殊操作
    }

    @Override
    public EditorCommand resolveCommand(String name) {
        return commandMap.get(name.toLowerCase());
    }

    // ==================== 内部命令实现 (负责参数解析) ====================

    private void handleAppend(Document doc, String args) throws Exception {
        String text = extractQuotedText(args);
        if (text == null) {
            throw new IllegalArgumentException("用法: append \"text\"");
        }
        append(doc, text);
    }

    private void handleInsert(Document doc, String args) throws Exception {
        // 解析 line:col "text"
        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("用法: insert <line:col> \"text\"");
        }
        
        String[] position = parts[0].split(":");
        if (position.length != 2) {
            throw new IllegalArgumentException("位置格式错误，应为 line:col");
        }
        
        int line = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);
        String text = extractQuotedText(parts[1]);
        
        if (text == null) {
            throw new IllegalArgumentException("文本必须用双引号包裹");
        }
        
        insert(doc, line, col, text);
    }

    private void handleDelete(Document doc, String args) throws Exception {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("用法: delete <line:col> <len>");
        }
        
        String[] position = parts[0].split(":");
        if (position.length != 2) {
            throw new IllegalArgumentException("位置格式错误，应为 line:col");
        }
        
        int line = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);
        int len = Integer.parseInt(parts[1]);
        
        delete(doc, line, col, len);
    }

    private void handleReplace(Document doc, String args) throws Exception {
        String[] parts = args.trim().split("\\s+", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("用法: replace <line:col> <len> \"text\"");
        }
        
        String[] position = parts[0].split(":");
        if (position.length != 2) {
            throw new IllegalArgumentException("位置格式错误，应为 line:col");
        }
        
        int line = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);
        int len = Integer.parseInt(parts[1]);
        String text = extractQuotedText(parts[2]);
        
        if (text == null) {
            throw new IllegalArgumentException("文本必须用双引号包裹");
        }
        
        replace(doc, line, col, len, text);
    }

    private void handleSpellCheck(Document doc, String args) throws Exception {
        String text = doc.getContent();
        List<String> issues = checker.check(text);

        System.out.println("拼写检查结果:");
        if (issues.isEmpty()) {
            System.out.println("无拼写错误");
        } else {
            for (String s : issues) {
                System.out.println(s);
            }
        }
    }

    /**
     * 辅助方法：提取双引号内的文本
     */
    private String extractQuotedText(String input) {
        input = input.trim();
        int firstQuote = input.indexOf('"');
        int lastQuote = input.lastIndexOf('"');
        if (firstQuote == -1 || lastQuote == -1 || firstQuote == lastQuote) {
            return null;
        }
        String content = input.substring(firstQuote + 1, lastQuote);
        // 处理转义字符 \n -> 换行符
        return content.replace("\\n", "\n");
    }

    /**
     * 执行编辑操作，注意modified状态
     */
    public void executeOperation(Document doc, EditOperation operation) {
        // 通过history执行操作（会自动压入undoStack）
        doc.getHistory().execute(operation);
        // 只有可撤销的操作才标记为已修改
        if (operation.isUndoable()) {
            doc.setModified(true);
        }
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
    public boolean undo(Document doc) {
        boolean success = doc.getHistory().undo();
        if (success) {
            doc.setModified(true);
        }
        return success;
    }

    @Override
    public String show(Document doc, int start, int end) {
        // 获取总行数
        int totalLines = doc.getLineCount();
        
        // 边界处理：空文件或没有内容
        if (totalLines == 0) {
            return "";
        }
        
        // 边界处理：如果 start <= 0 或 end <= 0，显示全部
        if (start <= 0 || end <= 0) {
            start = 1;
            end = totalLines;
        }
        
        // 边界处理：start 不能小于 1（确保从1开始）
        if (start < 1) {
            start = 1;
        }
        
        // 边界处理：end 不能超过总行数
        if (end > totalLines) {
            end = totalLines;
        }
        
        // 边界处理：start 不能大于 end（无效范围）
        if (start > end) {
            return ""; // 无效范围，返回空字符串
        }
        
        // 从 buffer 获取内容并分割成行
        String content = doc.getContent();
        String[] lines = content.split("\n", -1); // -1 保留末尾空行
        
        // 构建格式化输出
        StringBuilder result = new StringBuilder();
        for (int i = start - 1; i < end; i++) { // 转换为0-based索引
            if (i >= 0 && i < lines.length) {
                result.append(i + 1).append(": ").append(lines[i]);
                // 如果不是最后一行，添加换行符
                if (i < end - 1) {
                    result.append("\n");
                }
            }
        }
        
        return result.toString();
    }

    @Override
    public String show(Document doc) {
        return show(doc, 0, 0); // 0 表示显示全部
    }

    // === TextEditor 特有的行级编辑方法 ===
    
    public void append(Document doc, String text) {
        EditOperation operation = new AppendOperation(doc, text);
        executeOperation(doc, operation);
    }

    public void insert(Document doc, int line, int col, String text) {
        EditOperation operation = new InsertOperation(doc, line, col, text);
        executeOperation(doc, operation);
    }

    public void delete(Document doc, int line, int col, int len) {
        EditOperation operation = new DeleteOperation(doc, line, col, len);
        executeOperation(doc, operation);
    }

    public void replace(Document doc, int line, int col, int len, String text) {
        EditOperation operation = new ReplaceOperation(doc, line, col, len, text);
        executeOperation(doc, operation);
    }
}

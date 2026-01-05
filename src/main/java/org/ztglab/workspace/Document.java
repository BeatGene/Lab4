package org.ztglab.workspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ztglab.workspace.editor.OperationHistory;

/**
 * Document - 文件的内存映射对象 (充血模型)
 * 职责： 
 * 1. 存储文件内容（按行）
 * 2. 存储文件元数据（路径、修改状态）
 * 3. 管理操作历史（Undo/Redo）
 * 4. 提供核心内容操作方法
 */
public class Document {

    // 文本内容（按行存储）
    private List<String> lines;
    
    // 元数据
    private String filePath;
    private boolean modified;
    
    // 操作历史
    private OperationHistory history;

    /**
     * 创建空 Document
     */
    Document() {
        this.lines = new ArrayList<>();
        this.history = new OperationHistory();
        this.modified = false;
    }

    /**
     * 从已有内容创建 Document
     */
    Document(String content) {
        this.lines = new ArrayList<>();
        if (content != null && !content.isEmpty()) {
            String[] lineList = content.split("\n", -1);
            this.lines.addAll(Arrays.asList(lineList));
        }
        this.history = new OperationHistory();
        this.modified = false;
    }

    // ==================== 元数据管理 ====================
    
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public OperationHistory getHistory() {
        return history;
    }
    
    public void setHistory(OperationHistory history) {
        this.history = history;
    }

    // ==================== 增删改查操作 ====================
    /**
     * 追加文本
     */
    public void append(String text) {
        if (text == null) {
            return;
        }

        // 处理换行符
        if (text.contains("\n")) {
            String[] parts = text.split("\n", -1);
            lines.add(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                lines.add(parts[i]);
            }
        } else {
            lines.add(text);
        }
        this.modified = true;
    }

    /**
     * 插入文本
     * 在指定位置插入文本。
     * 异常处理:
     *  - 行号或列号越界：提示"行号或列号越界"
     *  - 空文件插入非1:1位置：提示"空文件只能在1:1位置插入"
     */
    public void insert(int line, int col, String text) {
        // 空文件特殊处理：只有一行且为空行
        if (getLineCount() == 1 && lines.get(0).isEmpty()) {
            if (line != 1 || col != 1) {
                throw new IllegalArgumentException("空文件只能在1:1位置插入");
            }
        }

        // 检查行号范围 [1, 总行数+1]
        if (line < 1 || line > getLineCount() + 1) {
            throw new IllegalArgumentException("行号或列号越界");
        }

        // 检查列号
        if (line <= getLineCount()) {
            // 现有行：列号范围 [1, 当前行长度+1]
            String currentLine = lines.get(line - 1);
            if (col < 1 || col > currentLine.length() + 1) {
                throw new IllegalArgumentException("行号或列号越界");
            }
        } else {
            // 新行（line = 总行数+1）：只能在1:1位置插入
            if (col != 1) {
                throw new IllegalArgumentException("行号或列号越界");
            }
        }

        if (text == null || text.isEmpty()) {
            return;
        }

        if (line <= getLineCount()) {
            // 在现有行插入，newLines是在指定行列插入后新的行
            String currentLine = lines.get(line - 1);
            String part1 = currentLine.substring(0, col - 1);
            String part2 = currentLine.substring(col - 1);
            String combined = part1 + text + part2;
            String[] newLines = combined.split("\n", -1);

            // 替换当前行并插入新行
            lines.remove(line - 1);
            for (int i = 0; i < newLines.length; i++) {
                lines.add(line - 1 + i, newLines[i]);
            }
        } else {
            // 在新行末尾插入
            String[] newLines = text.split("\n", -1);
            lines.addAll(Arrays.asList(newLines));
        }
        this.modified = true;
    }

    /**
     * 删除字符
     * 异常处理:
     *  - 删除长度超出该行剩余字符：提示"删除长度超出行尾"
     *  - 行号或列号越界：提示相应的范围错误
     */
    public void delete(int line, int col, int len) {
        // 检查行号
        if (line < 1 || line > getLineCount()) {
            throw new IllegalArgumentException("行号越界，范围应为1到" + getLineCount());
        }

        String currentLine = lines.get(line - 1);
        int lineLen = currentLine.length();

        // 检查列号
        if (col < 1 || col > lineLen) {
            throw new IllegalArgumentException("列号越界，范围应为1到" + lineLen);
        }

        // 检查删除长度
        if (len < 0) {
            throw new IllegalArgumentException("删除长度不能为负数");
        }
        if (len == 0) {
            return; // 无需操作
        }
        int availableChars = lineLen - (col - 1);
        if (len > availableChars) {
            throw new IllegalArgumentException("删除长度超出行尾");
        }

        // 执行删除
        String part1 = currentLine.substring(0, col - 1);
        String part2 = currentLine.substring(col - 1 + len);
        lines.set(line - 1, part1 + part2);
        this.modified = true;
    }

    /**
     * 替换文本
     */
    public void replace(int line, int col, int len, String text) {
        // 检查行号
        if (line < 1 || line > getLineCount()) {
            throw new IllegalArgumentException("行号越界，范围应为1到" + getLineCount());
        }

        String currentLine = lines.get(line - 1);
        int lineLen = currentLine.length();

        // 检查列号
        if (col < 1 || col > lineLen) {
            throw new IllegalArgumentException("列号越界，范围应为1到" + lineLen);
        }

        // 检查替换长度
        if (len < 0) {
            throw new IllegalArgumentException("替换长度不能为负数");
        }
        int availableChars = lineLen - (col - 1);
        if (len > availableChars) {
            throw new IllegalArgumentException("替换长度超出行尾");
        }

        // 处理空替换文本
        if (text == null) {
            text = "";
        }

        // 构建新内容
        String part1 = currentLine.substring(0, col - 1);
        String part2 = currentLine.substring(col - 1 + len);
        String combined = part1 + text + part2;
        String[] newLines = combined.split("\n", -1);

        // 替换原行
        lines.remove(line - 1);
        for (int i = 0; i < newLines.length; i++) {
            lines.add(line - 1 + i, newLines[i]);
        }
        this.modified = true;
    }

    /**
     * 获取指定位置的文本
     */
    public String getText(int line, int col, int len) {
        // 检查行号
        if (line < 1 || line > getLineCount()) {
            throw new IllegalArgumentException("行号越界");
        }

        String currentLine = lines.get(line - 1);
        int lineLen = currentLine.length();

        // 检查列号
        if (col < 1 || col > lineLen + 1) { // +1 是为了允许读取行尾之后（虽然通常没意义，但保持一致性）
             throw new IllegalArgumentException("列号越界");
        }

        // 检查长度
        if (len < 0) {
            throw new IllegalArgumentException("长度不能为负数");
        }
        
        int availableChars = lineLen - (col - 1);
        if (len > availableChars) {
             throw new IllegalArgumentException("长度超出行尾");
        }

        return currentLine.substring(col - 1, col - 1 + len);
    }

    /**
     * 删除指定行
     */
    public void deleteLine(int line) {
        if (line < 1 || line > getLineCount()) {
            throw new IllegalArgumentException("行号越界");
        }
        lines.remove(line - 1);
        this.modified = true;
    }

    /**
     * 显示全部内容
     */
    public String getContent() {
        return String.join("\n", lines);
    }

    /**
     * 获取总行数
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * 获取指定行的文本
     * 异常处理:
     *  - 行号或越界：提示"行号越界"
     */
    public String getLine(int i) throws Exception{
        if(i < 1 || i > getLineCount()) {
            throw new Exception("行号越界");
        }
        return lines.get(i - 1);
    }
}

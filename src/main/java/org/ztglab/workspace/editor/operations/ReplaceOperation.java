package org.ztglab.workspace.editor.operations;

import org.ztglab.workspace.Document;
import org.ztglab.workspace.editor.EditOperation;

/**
 * 替换文本操作
 */
public class ReplaceOperation implements EditOperation {

    private final Document document;
    private final int line;
    private final int col;
    private final int length;
    private final String newText;
    private String oldText; // 保存被替换的内容，用于撤销

    public ReplaceOperation(Document document, int line, int col, int length, String newText) {
        this.document = document;
        this.line = line;
        this.col = col;
        this.length = length;
        this.newText = newText;
    }

    @Override
    public void execute() {
        // 保存被替换的文本用于撤销
        this.oldText = document.getText(line, col, length);
        document.replace(line, col, length, newText);
    }

    @Override
    public void undo() {
        if (oldText != null) {
            document.replace(line, col, newText.length(), oldText);
        }
    }

    @Override
    public String getDescription() {
        return String.format("替换文本: 位置[%d:%d] 长度[%d] 新文本[%s]", line, col, length, newText);
    }
}

package org.ztglab.workspace.editor.operations;

import org.ztglab.workspace.Document;
import org.ztglab.workspace.editor.EditOperation;

/**
 * 删除文本操作
 */
public class DeleteOperation implements EditOperation {

    private final Document document;
    private final int line;
    private final int col;
    private final int length;
    private String deletedText; // 保存删除的内容，用于撤销

    public DeleteOperation(Document document, int line, int col, int length) {
        this.document = document;
        this.line = line;
        this.col = col;
        this.length = length;
    }

    @Override
    public void execute() {
        // 保存被删除的文本用于撤销
        this.deletedText = document.getText(line, col, length);
        document.delete(line, col, length);
    }

    @Override
    public void undo() {
        if (deletedText != null) {
            document.insert(line, col, deletedText);
        }
    }

    @Override
    public String getDescription() {
        return String.format("删除文本: 位置[%d:%d] 长度[%d]", line, col, length);
    }
}

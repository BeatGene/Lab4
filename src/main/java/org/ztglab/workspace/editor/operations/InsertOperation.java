package org.ztglab.workspace.editor.operations;

import org.ztglab.workspace.Document;
import org.ztglab.workspace.editor.EditOperation;

/**
 * 插入文本操作
 */
public class InsertOperation implements EditOperation {

    private final Document document;
    private final int line;
    private final int col;
    private final String text;

    public InsertOperation(Document document, int line, int col, String text) {
        this.document = document;
        this.line = line;
        this.col = col;
        this.text = text;
    }

    @Override
    public void execute() {
        document.insert(line, col, text);
    }

    @Override
    public void undo() {
        document.delete(line, col, text.length());
    }

    @Override
    public String getDescription() {
        return String.format("插入文本: 位置[%d:%d] 内容[%s]", line, col, text);
    }
}

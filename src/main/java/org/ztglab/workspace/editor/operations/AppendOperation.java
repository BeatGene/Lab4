package org.ztglab.workspace.editor.operations;

import org.ztglab.workspace.Document;
import org.ztglab.workspace.editor.EditOperation;

/**
 * 追加文本操作
 */
public class AppendOperation implements EditOperation {

    private final Document document;
    private final String text;
    private int savedLineCount; // 保存操作前的行数，用于撤销

    public AppendOperation(Document document, String text) {
        this.document = document;
        this.text = text;
    }

    @Override
    public void execute() {
        savedLineCount = document.getLineCount();
        document.append(text);
    }

    @Override
    public void undo() {
        int currentLines = document.getLineCount();
        int linesToRemove = currentLines - savedLineCount;
        
        for (int i = 0; i < linesToRemove; i++) {
            try {
                // 移除最后一行
                document.deleteLine(document.getLineCount());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getDescription() {
        return "追加文本: " + text;
    }
}

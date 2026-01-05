package org.ztglab.workspace.editor.operations;

import org.ztglab.workspace.Document;
import org.ztglab.workspace.editor.EditOperation;

/**
 * XML编辑操作
 * 保存整个文档的前后状态以支持undo/redo
 */
public class XmlOperation implements EditOperation {
    
    private final Document doc;
    private final String oldContent;
    private final String newContent;
    private final String description;
    
    public XmlOperation(Document doc, String newContent, String description) {
        this.doc = doc;
        this.oldContent = doc.getContent();
        this.newContent = newContent;
        this.description = description;
    }
    
    @Override
    public void execute() {
        // 清空文档并写入新内容
        while (doc.getLineCount() > 0) {
            doc.deleteLine(doc.getLineCount());
        }
        doc.append(newContent);
    }
    
    @Override
    public void undo() {
        // 恢复到旧内容
        while (doc.getLineCount() > 0) {
            doc.deleteLine(doc.getLineCount());
        }
        doc.append(oldContent);
    }
    
    @Override
    public boolean isUndoable() {
        return true;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
}

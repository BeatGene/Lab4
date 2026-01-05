package org.ztglab.event.events;

import org.ztglab.workspace.Document;
import org.ztglab.event.Event;

/**
 * 活动文档切换事件 - 当WorkspaceManager切换活动文档时发布
 * 
 * 用于通知统计模块等监听者文档切换事件，以便进行时长统计等功能
 */
public class ActiveDocumentChangedEvent extends Event {
    
    private final Document oldDocument;
    private final Document newDocument;

    /**
     * 构造函数
     * 
     * @param oldDocument 旧的活动文档（可为null，表示首次加载）
     * @param newDocument 新的活动文档（可为null，表示关闭所有文件）
     */
    public ActiveDocumentChangedEvent(Document oldDocument, Document newDocument) {
        super();
        this.oldDocument = oldDocument;
        this.newDocument = newDocument;
    }

    /**
     * 获取旧的活动文档
     * 
     * @return 旧的文档，如果为首次加载则返回null
     */
    public Document getOldDocument() {
        return oldDocument;
    }

    /**
     * 获取新的活动文档
     * 
     * @return 新的文档，如果关闭所有文件则返回null
     */
    public Document getNewDocument() {
        return newDocument;
    }

    @Override
    public String getDescription() {
        String oldPath = oldDocument != null ? oldDocument.getFilePath() : "null";
        String newPath = newDocument != null ? newDocument.getFilePath() : "null";
        return String.format("活动文档切换: %s -> %s", oldPath, newPath);
    }
}


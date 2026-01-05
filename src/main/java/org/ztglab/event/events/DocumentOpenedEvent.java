package org.ztglab.event.events;

import org.ztglab.event.Event;
import org.ztglab.workspace.Document;

/**
 * 文档打开事件
 */
public class DocumentOpenedEvent extends Event {
    private final Document document;

    public DocumentOpenedEvent(Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

    @Override
    public String getDescription() {
        return "文档已打开: " + document.getFilePath();
    }
}

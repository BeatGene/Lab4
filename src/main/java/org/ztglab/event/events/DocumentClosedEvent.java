package org.ztglab.event.events;

import org.ztglab.event.Event;

/**
 * 文档关闭事件
 */
public class DocumentClosedEvent extends Event {
    private final String filePath;

    public DocumentClosedEvent(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getDescription() {
        return "文档已关闭: " + filePath;
    }
}

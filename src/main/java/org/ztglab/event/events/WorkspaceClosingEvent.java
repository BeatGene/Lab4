package org.ztglab.event.events;

import org.ztglab.event.Event;

/**
 * 工作区关闭事件
 */
public class WorkspaceClosingEvent extends Event {
    public WorkspaceClosingEvent() {
    }

    @Override
    public String getDescription() {
        return "工作区即将关闭";
    }
}

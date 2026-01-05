package org.ztglab.event.events;

import org.ztglab.event.Event;

public class DocumentPathUpdatedEvent extends Event {
    private final String oldPath;
    private final String newPath;

    public DocumentPathUpdatedEvent(String oldPath, String newPath) {
        super();
        this.oldPath = oldPath;
        this.newPath = newPath;
    }

    public String getOldPath() {
        return oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    @Override
    public String getDescription() {
        return "Document path updated from " + oldPath + " to " + newPath;
    }
}

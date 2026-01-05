package org.ztglab.event.events;

import org.ztglab.command.AbstractCommand;
import org.ztglab.event.Event;

/**
 * 命令失败事件 - 当命令执行失败时发布
 */
public class CommandFailedEvent extends Event {
    
    private final AbstractCommand command;
    private final Exception exception;

    public CommandFailedEvent(AbstractCommand command, Exception exception) {
        super();
        this.command = command;
        this.exception = exception;
    }

    public AbstractCommand getCommand() {
        return command;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String getDescription() {
        return "命令失败: " + command.getDescription() + 
               " (原因: " + exception.getMessage() + ")";
    }
}

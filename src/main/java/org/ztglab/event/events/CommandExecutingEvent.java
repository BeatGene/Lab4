package org.ztglab.event.events;

import org.ztglab.command.AbstractCommand;
import org.ztglab.event.Event;

/**
 * 命令执行中事件 - 当命令状态变为EXECUTING时发布
 */
public class CommandExecutingEvent extends Event {
    
    private final AbstractCommand command;

    public CommandExecutingEvent(AbstractCommand command) {
        super();
        this.command = command;
    }

    public AbstractCommand getCommand() {
        return command;
    }

    @Override
    public String getDescription() {
        return "命令执行中: " + command.getDescription();
    }
}

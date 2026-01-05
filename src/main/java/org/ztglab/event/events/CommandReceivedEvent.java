package org.ztglab.event.events;

import org.ztglab.command.AbstractCommand;
import org.ztglab.event.Event;

/**
 * 命令接收事件 - 当CommandBus收到命令时发布
 */
public class CommandReceivedEvent extends Event {
    
    private final AbstractCommand command;

    public CommandReceivedEvent(AbstractCommand command) {
        super();
        this.command = command;
    }

    public AbstractCommand getCommand() {
        return command;
    }

    @Override
    public String getDescription() {
        return "命令接收: " + command.getDescription();
    }
}

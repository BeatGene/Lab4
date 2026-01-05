package org.ztglab.event.events;

import org.ztglab.command.AbstractCommand;
import org.ztglab.event.Event;

/**
 * 命令完成事件 - 当命令成功执行完毕时发布
 */
public class CommandCompletedEvent extends Event {
    
    private final AbstractCommand command;

    public CommandCompletedEvent(AbstractCommand command) {
        super();
        this.command = command;
    }

    public AbstractCommand getCommand() {
        return command;
    }

    @Override
    public String getDescription() {
        return "命令完成: " + command.getDescription();
    }
}

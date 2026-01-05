package org.ztglab.command;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ztglab.event.EventBus;
import org.ztglab.event.events.*;

/**
 * 命令总线 - 负责命令的分发和路由
 * 
 * 核心职责：
 * 1. 维护命令处理器注册表
 * 2. 接收命令并分发到对应的处理器
 * 3. 管理命令执行生命周期（状态更新、时间记录）
 * 4. 发布命令相关事件到事件总线
 * 5. 提供命令执行历史记录
 */
public class CommandBus {

    // 命令处理器注册表：命令类型 -> 处理器列表
    private final Map<Class<? extends AbstractCommand>, List<ICommandHandler<? extends AbstractCommand>>> handlers;

    // 命令执行历史（可选，用于调试和日志）
    private final List<AbstractCommand> commandHistory;

    // 事件总线（用于发布命令相关事件）
    private final EventBus eventBus;

    // 是否记录历史
    private boolean recordHistory = true;

    /**
     * 构造函数
     * 
     * @param eventBus 事件总线实例
     */
    public CommandBus(EventBus eventBus) {
        this.handlers = new ConcurrentHashMap<>();
        this.commandHistory = new ArrayList<>();
        this.eventBus = eventBus;
    }

    /**
     * 注册命令处理器
     * 
     * @param commandClass 命令类型
     * @param handler 处理器实例
     */
    public <T extends AbstractCommand> void registerHandler(Class<T> commandClass, ICommandHandler<T> handler) {
        handlers.computeIfAbsent(commandClass, k -> new ArrayList<>()).add(handler);
    }

    /**
     * 取消注册命令处理器
     * 
     * @param commandClass 命令类型
     * @param handler 处理器实例
     */
    public <T extends AbstractCommand> void unregisterHandler(Class<T> commandClass, ICommandHandler<T> handler) {
        List<ICommandHandler<? extends AbstractCommand>> handlerList = handlers.get(commandClass);
        if (handlerList != null) {
            handlerList.remove(handler);
            if (handlerList.isEmpty()) {
                handlers.remove(commandClass);
            }
        }
    }

    /**
     * 分发命令到对应的处理器
     * 
     * @param command 要执行的命令
     * @throws Exception 命令执行失败时抛出异常
     */
    @SuppressWarnings("unchecked")
    public void dispatch(AbstractCommand command) throws Exception {
        if (command == null) {
            throw new IllegalArgumentException("命令不能为null");
        }

        // 1. 发布命令接收事件
        eventBus.publish(new CommandReceivedEvent(command));

        // 2. 更新命令状态为执行中，并发布事件
        command.setStatus(AbstractCommand.CommandStatus.EXECUTING);
        eventBus.publish(new CommandExecutingEvent(command));

        try {
            // 查找对应的处理器
            List<ICommandHandler<? extends AbstractCommand>> handlerList = handlers.get(command.getClass());

            if (handlerList == null || handlerList.isEmpty()) {
                throw new IllegalStateException("未找到命令处理器: " + command.getCommandType());
            }

            // 执行所有注册的处理器（支持多个处理器处理同一命令）
            for (ICommandHandler<? extends AbstractCommand> handler : handlerList) {
                ((ICommandHandler<AbstractCommand>) handler).handle(command);
            }

            // 3. 更新命令状态为完成，并发布事件
            command.setStatus(AbstractCommand.CommandStatus.COMPLETED);
            command.setCompletedTime(LocalDateTime.now());
            eventBus.publish(new CommandCompletedEvent(command));

            // 记录历史
            if (recordHistory) {
                commandHistory.add(command);
            }

        } catch (Exception e) {
            // 4. 更新命令状态为失败，并发布事件
            command.setStatus(AbstractCommand.CommandStatus.FAILED);
            command.setCompletedTime(LocalDateTime.now());
            eventBus.publish(new CommandFailedEvent(command, e));
            
            throw e;
        }
    }

    /**
     * 获取命令执行历史
     * 
     * @return 命令历史列表
     */
    public List<AbstractCommand> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }

    /**
     * 清空命令历史
     */
    public void clearHistory() {
        commandHistory.clear();
    }

    /**
     * 设置是否记录历史
     * 
     * @param recordHistory 是否记录
     */
    public void setRecordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    /**
     * 获取已注册的处理器数量
     * 
     * @return 处理器数量
     */
    public int getHandlerCount() {
        return handlers.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 检查是否有命令处理器注册
     * 
     * @param commandClass 命令类型
     * @return 如果已注册返回true
     */
    public boolean hasHandler(Class<? extends AbstractCommand> commandClass) {
        List<ICommandHandler<? extends AbstractCommand>> handlerList = handlers.get(commandClass);
        return handlerList != null && !handlerList.isEmpty();
    }
}

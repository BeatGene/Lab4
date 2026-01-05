package org.ztglab.command;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 命令抽象基类 - 所有命令的唯一基类
 * 
 * 命令对象是不可变的数据载体（参数包），只包含：
 * 1. 时间戳（发出时间、完成时间）
 * 2. 命令标识（ID、类型）
 * 3. 执行状态
 * 
 * 所有业务逻辑由命令处理器负责，命令本身不包含任何处理逻辑
 */
public abstract class AbstractCommand {

    private final String commandId;
    private final String commandType;
    private final LocalDateTime issuedTime;
    private LocalDateTime completedTime;
    private CommandStatus status;

    private String originalName;
    private String originalArgs;

    /**
     * 构造函数 - 自动初始化基础属性
     */
    protected AbstractCommand() {
        this.commandId = UUID.randomUUID().toString();
        this.commandType = this.getClass().getSimpleName();
        this.issuedTime = LocalDateTime.now();
        this.status = CommandStatus.PENDING;
    }

    /**
     * 获取命令唯一标识符
     */
    public final String getCommandId() {
        return commandId;
    }

    /**
     * 获取命令类型
     */
    public final String getCommandType() {
        return commandType;
    }

    /**
     * 获取命令发出时间
     */
    public final LocalDateTime getIssuedTime() {
        return issuedTime;
    }

    /**
     * 获取命令完成时间
     */
    public final LocalDateTime getCompletedTime() {
        return completedTime;
    }

    /**
     * 设置命令完成时间（仅供CommandBus调用）
     */
    public final void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    /**
     * 获取命令执行状态
     */
    public final CommandStatus getStatus() {
        return status;
    }

    /**
     * 设置命令执行状态（仅供CommandBus调用）
     */
    public final void setStatus(CommandStatus status) {
        this.status = status;
    }

    /**
     * 获取命令描述（用于日志）
     */
    public abstract String getDescription();

    public void setOriginalCommand(String name, String args) {
        this.originalName = name;
        this.originalArgs = args;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getOriginalArgs() {
        return originalArgs;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, issued=%s, status=%s]",
                commandType, commandId.substring(0, 8), issuedTime, status);
    }

    /**
     * 命令执行状态枚举
     */
    public enum CommandStatus {
        PENDING,    // 待执行
        EXECUTING,  // 执行中
        COMPLETED,  // 已完成
        FAILED      // 执行失败
    }
}

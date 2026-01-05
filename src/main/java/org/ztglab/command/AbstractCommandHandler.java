package org.ztglab.command;

/**
 * 命令处理器抽象基类
 * 
 * 提供canHandle方法的默认实现，简化具体处理器的开发
 * 
 * @param <T> 命令类型
 */
public abstract class AbstractCommandHandler<T extends AbstractCommand> implements ICommandHandler<T> {

    private final Class<T> commandType;

    /**
     * 构造函数
     * 
     * @param commandType 处理器支持的命令类型
     */
    protected AbstractCommandHandler(Class<T> commandType) {
        this.commandType = commandType;
    }

    @Override
    public boolean canHandle(Class<? extends AbstractCommand> commandClass) {
        return commandType.isAssignableFrom(commandClass);
    }

    @Override
    public Class<T> getCommandType() {
        return commandType;
    }
}

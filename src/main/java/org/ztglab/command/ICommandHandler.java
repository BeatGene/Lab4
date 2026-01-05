package org.ztglab.command;

/**
 * 命令处理器接口
 * 
 * 各个模块实现此接口来定义自己的命令处理逻辑
 * 
 * @param <T> 命令类型，必须继承AbstractCommand
 */
public interface ICommandHandler<T extends AbstractCommand> {

    /**
     * 处理命令
     * 
     * @param command 要处理的命令
     * @throws Exception 命令执行过程中可能抛出的异常
     */
    void handle(T command) throws Exception;

    /**
     * 判断是否可以处理指定的命令类型
     * 
     * @param commandClass 命令类型
     * @return 如果可以处理返回true，否则返回false
     */
    boolean canHandle(Class<? extends AbstractCommand> commandClass);

    /**
     * 获取处理器支持的命令类型
     * 
     * @return 命令类型
     */
    Class<T> getCommandType();
}

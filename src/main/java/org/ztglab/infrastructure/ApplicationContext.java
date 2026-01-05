package org.ztglab.infrastructure;

import org.ztglab.command.CommandBus;
import org.ztglab.event.EventBus;
import org.ztglab.event.events.*;

/**
 * 应用上下文 - 管理全局基础设施组件
 * 
 * 职责：
 * 1. 初始化和管理事件总线
 * 2. 初始化和管理命令总线
 * 3. 初始化和管理日志服务
 * 4. 初始化和管理统计服务
 * 5. 配置事件监听器
 * 
 * 采用单例模式，确保全局只有一个实例
 */
public class ApplicationContext {
    
    private static ApplicationContext instance;
    
    private final EventBus eventBus;
    private final CommandBus commandBus;
    private final LoggingService loggingService;
    private final StatisticsService statisticsService;
    
    /**
     * 私有构造函数，初始化所有基础设施组件
     */
    private ApplicationContext() {
        // 1. 初始化事件总线
        this.eventBus = new EventBus();
        
        // 2. 初始化命令总线（注入事件总线）
        this.commandBus = new CommandBus(eventBus);
        
        // 3. 初始化日志服务
        this.loggingService = new LoggingService();

        // 4. 初始化统计服务
        this.statisticsService = new StatisticsService();
        
        // 5. 配置事件监听器
        configureEventListeners();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }
    
    /**
     * 配置事件监听器
     * 注册日志服务到事件总线，监听所有命令相关事件
     * 注册统计服务到事件总线，监听活动文档切换事件
     */
    private void configureEventListeners() {
        // 注册日志服务
        eventBus.subscribe(CommandReceivedEvent.class, loggingService::onEvent);
        eventBus.subscribe(CommandExecutingEvent.class, loggingService::onEvent);
        eventBus.subscribe(CommandCompletedEvent.class, loggingService::onEvent);
        eventBus.subscribe(CommandFailedEvent.class, loggingService::onEvent);
        eventBus.subscribe(ActiveDocumentChangedEvent.class, loggingService::onEvent);
        eventBus.subscribe(DocumentPathUpdatedEvent.class, loggingService::onEvent);
        
        // 注册统计服务
        eventBus.subscribe(ActiveDocumentChangedEvent.class, statisticsService::onEvent);
        eventBus.subscribe(DocumentOpenedEvent.class, statisticsService::onEvent);
        eventBus.subscribe(DocumentClosedEvent.class, statisticsService::onEvent);
        eventBus.subscribe(WorkspaceClosingEvent.class, statisticsService::onEvent);
    }
    
    /**
     * 获取事件总线
     */
    public EventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * 获取命令总线
     */
    public CommandBus getCommandBus() {
        return commandBus;
    }
    
    /**
     * 获取日志服务
     */
    public LoggingService getLoggingService() {
        return loggingService;
    }

    /**
     * 获取统计服务
     */
    public StatisticsService getStatisticsService() {
        return statisticsService;
    }
    
    /**
     * 重置单例（用于测试）
     */
    public static synchronized void reset() {
        instance = null;
    }
}

package org.ztglab.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 事件基类 - 所有事件的抽象基类
 * 
 * 职责：
 * 1. 提供事件的基础属性（ID、时间戳、类型）
 * 2. 事件是不可变的数据对象
 */
public abstract class Event {
    
    private final String eventId;
    private final String eventType;
    private final LocalDateTime timestamp;

    /**
     * 构造函数
     */
    protected Event() {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 获取事件唯一标识符
     */
    public final String getEventId() {
        return eventId;
    }

    /**
     * 获取事件类型
     */
    public final String getEventType() {
        return eventType;
    }

    /**
     * 获取事件时间戳
     */
    public final LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 获取事件描述（用于日志）
     */
    public abstract String getDescription();

    @Override
    public String toString() {
        return String.format("%s[id=%s, time=%s]",
                eventType, eventId.substring(0, 8), timestamp);
    }
}

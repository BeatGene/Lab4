package org.ztglab.event;

/**
 * 事件监听器接口
 * 
 * 各个模块实现此接口来监听感兴趣的事件
 * 
 * @param <T> 事件类型，必须继承Event
 */
@FunctionalInterface
public interface IEventListener<T extends Event> {

    /**
     * 处理事件
     * 
     * @param event 要处理的事件
     */
    void onEvent(T event);
}

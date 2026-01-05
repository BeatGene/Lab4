package org.ztglab.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件总线 - 负责事件的发布和订阅
 * 
 * 核心职责：
 * 1. 维护事件监听器注册表
 * 2. 接收事件并分发到对应的监听器
 * 3. 支持同步和异步事件发布
 * 4. 提供事件历史记录（可选）
 */
public class EventBus {

    // 事件监听器注册表：事件类型 -> 监听器列表（线程安全）
    private final Map<Class<? extends Event>, List<IEventListener<? extends Event>>> listeners;

    // 事件发布历史（可选，用于调试）
    private final List<Event> eventHistory;

    // 是否记录历史
    private boolean recordHistory = false;

    /**
     * 构造函数
     */
    public EventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.eventHistory = new ArrayList<>();
    }

    /**
     * 注册事件监听器
     * 
     * @param eventClass 事件类型
     * @param listener 监听器实例
     */
    public <T extends Event> void subscribe(Class<T> eventClass, IEventListener<? super T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                .add((IEventListener<? extends Event>) listener);
    }

    /**
     * 取消注册事件监听器
     * 
     * @param eventClass 事件类型
     * @param listener 监听器实例
     */
    public <T extends Event> void unsubscribe(Class<T> eventClass, IEventListener<? super T> listener) {
        List<IEventListener<? extends Event>> listenerList = listeners.get(eventClass);
        if (listenerList != null) {
            listenerList.remove(listener);
            if (listenerList.isEmpty()) {
                listeners.remove(eventClass);
            }
        }
    }

    /**
     * 发布事件（同步）
     * 
     * @param event 要发布的事件
     */
    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        if (event == null) {
            return;
        }

        // 记录历史
        if (recordHistory) {
            eventHistory.add(event);
        }

        // 查找对应的监听器
        List<IEventListener<? extends Event>> listenerList = listeners.get(event.getClass());

        if (listenerList == null || listenerList.isEmpty()) {
            return; // 没有监听器，静默返回
        }

        // 通知所有监听器
        for (IEventListener<? extends Event> listener : listenerList) {
            try {
                ((IEventListener<Event>) listener).onEvent(event);
            } catch (Exception e) {
                // 监听器异常不应影响事件发布流程
                System.err.println("[EventBus] 监听器处理事件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 发布事件（异步）
     * 
     * @param event 要发布的事件
     */
    public void publishAsync(Event event) {
        new Thread(() -> publish(event)).start();
    }

    /**
     * 获取事件发布历史
     * 
     * @return 事件历史列表
     */
    public List<Event> getEventHistory() {
        return new ArrayList<>(eventHistory);
    }

    /**
     * 清空事件历史
     */
    public void clearHistory() {
        eventHistory.clear();
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
     * 获取已注册的监听器数量
     * 
     * @return 监听器数量
     */
    public int getListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 检查是否有事件监听器注册
     * 
     * @param eventClass 事件类型
     * @return 如果已注册返回true
     */
    public boolean hasListener(Class<? extends Event> eventClass) {
        List<IEventListener<? extends Event>> listenerList = listeners.get(eventClass);
        return listenerList != null && !listenerList.isEmpty();
    }
}

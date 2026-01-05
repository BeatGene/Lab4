package org.ztglab.infrastructure;

import org.ztglab.workspace.Document;
import org.ztglab.event.events.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计服务 - 记录每个文件在当前会话中的编辑时长
 * 
 * 功能：
 * 1. 监听活动文档切换事件，自动开始/停止计时
 * 2. 累计每个文件的编辑时长
 * 3. 提供可读格式的时长查询接口
 */
public class StatisticsService {

    // 文件路径 -> 累计时长（毫秒）
    private final Map<String, Long> fileDurations = new HashMap<>();
    
    // 上一次切换的时间戳（毫秒）
    private long lastSwitchTime = 0;
    
    // 当前活动文件路径（用于退出时统计最后一次）
    private String currentActiveFilePath = null;

    /**
     * 构造函数
     */
    public StatisticsService() {
        // 初始化时记录当前时间
        this.lastSwitchTime = System.currentTimeMillis();
    }

    /**
     * 监听事件
     */
    /**
     * 处理文档打开事件
     */
    public void onEvent(DocumentOpenedEvent event) {
        resetDuration(event.getDocument().getFilePath());
    }

    /**
     * 处理文档关闭事件
     */
    public void onEvent(DocumentClosedEvent event) {
        resetDuration(event.getFilePath());
    }

    /**
     * 处理工作区关闭事件
     */
    public void onEvent(WorkspaceClosingEvent event) {
        finalizeSession();
    }

    /**
     * 处理活动文档切换事件
     */
    public void onEvent(ActiveDocumentChangedEvent event) {
        long now = System.currentTimeMillis();
        
        // 1. 结算上一个文件的时长
        if (currentActiveFilePath != null) {
            long duration = now - lastSwitchTime;
            if (duration > 0) {
                fileDurations.put(currentActiveFilePath, 
                    fileDurations.getOrDefault(currentActiveFilePath, 0L) + duration);
            }
        }
        
        // 2. 更新当前活动文件
        Document newDoc = event.getNewDocument();
        if (newDoc != null) {
            currentActiveFilePath = newDoc.getFilePath();
        } else {
            currentActiveFilePath = null;
        }
        
        // 3. 重置计时器
        lastSwitchTime = now;
    }

    /**
     * 获取指定文件的编辑时长（可读格式）
     * 
     * @param filePath 文件路径
     * @return 格式化的时长字符串，如 "25分钟"、"2小时15分钟" 等
     */
    public String getDuration(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        // 获取已累计的时长
        long milliseconds = fileDurations.getOrDefault(filePath, 0L);
        
        // 如果该文件是当前活动文件，需要加上从上次切换到现在的时间
        // 使用规范化路径比较，避免路径格式不一致的问题
        if (isCurrentActiveFile(filePath) && lastSwitchTime > 0) {
            long now = System.currentTimeMillis();
            long currentDuration = now - lastSwitchTime;
            if (currentDuration > 0) {
                milliseconds += currentDuration;
            }
        }
        
        return formatDuration(milliseconds);
    }
    
    /**
     * 判断指定路径是否是当前活动文件
     * 使用规范化路径比较，处理路径格式不一致的情况
     */
    private boolean isCurrentActiveFile(String filePath) {
        if (filePath == null || currentActiveFilePath == null) {
            return false;
        }
        // 规范化路径：转换为绝对路径并标准化
        try {
            java.io.File file1 = new java.io.File(filePath);
            java.io.File file2 = new java.io.File(currentActiveFilePath);
            return file1.getAbsolutePath().equals(file2.getAbsolutePath());
        } catch (Exception e) {
            // 如果路径比较失败，使用字符串比较
            return filePath.equals(currentActiveFilePath);
        }
    }

    /**
     * 格式化时长为可读字符串
     * 
     * @param milliseconds 毫秒数
     * @return 格式化的时长字符串
     */
    public String formatDuration(long milliseconds) {
        if (milliseconds < 0) {
            return "0秒";
        }
        
        long totalSeconds = milliseconds / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long days = totalHours / 24;
        
        // 构建结果字符串
        StringBuilder result = new StringBuilder();
        
        // ≥ 24小时：X天Y小时Z分钟T秒
        if (days > 0) {
            result.append(days).append("天");
            if (hours > 0) {
                result.append(hours).append("小时");
            }
            if (minutes > 0) {
                result.append(minutes).append("分钟");
            }
            if (seconds > 0) {
                result.append(seconds).append("秒");
            }
            return result.toString();
        }
        
        // 1-23小时：X小时Y分钟Z秒
        if (totalHours > 0) {
            result.append(hours).append("小时");
            if (minutes > 0) {
                result.append(minutes).append("分钟");
            }
            if (seconds > 0) {
                result.append(seconds).append("秒");
            }
            return result.toString();
        }
        
        // 1-59分钟：X分钟Y秒
        if (totalMinutes > 0) {
            result.append(minutes).append("分钟");
            if (seconds > 0) {
                result.append(seconds).append("秒");
            }
            return result.toString();
        }
        
        // < 1分钟：X秒
        return seconds + "秒";
    }

    /**
     * 重置指定文件的编辑时长
     * 当文件关闭后再次打开时调用
     * 
     * @param filePath 文件路径
     */
    public void resetDuration(String filePath) {
        if (filePath != null) {
            fileDurations.put(filePath, 0L);
        }
    }

    /**
     * 完成会话统计（处理程序退出时的最后一次统计）
     */
    public void finalizeSession() {
        try {
            if (currentActiveFilePath != null) {
                long now = System.currentTimeMillis();
                long duration = now - lastSwitchTime;
                if (duration > 0) {
                    fileDurations.put(currentActiveFilePath,
                        fileDurations.getOrDefault(currentActiveFilePath, 0L) + duration);
                    
                    // 输出统计信息
                    String fileName = new java.io.File(currentActiveFilePath).getName();
                    String totalDuration = getDuration(currentActiveFilePath);
                    System.out.println("[统计] 完成最后一次时长统计: " + fileName + " (总时长: " + totalDuration + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("[StatisticsService] 完成会话统计失败: " + e.getMessage());
        }
    }
}

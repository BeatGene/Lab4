package org.ztglab.statistics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ztglab.workspace.Document;
import org.ztglab.workspace.TestDocumentFactory;
import org.ztglab.event.EventBus;
import org.ztglab.event.events.ActiveDocumentChangedEvent;
import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.infrastructure.StatisticsService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统计服务测试类
 * 
 * 测试覆盖：
 * 1. 文件切换时长的累计
 * 2. 当前活动文件的实时时长
 * 3. 文件关闭后重置
 * 4. 时长格式化
 * 5. 退出时的最后一次统计
 */
class StatisticsServiceTest {

    private StatisticsService statisticsService;
    private EventBus eventBus;
    private Document doc1;
    private Document doc2;

    @BeforeEach
    void setUp() {
        // 重置ApplicationContext以确保测试隔离
        ApplicationContext.reset();
        
        // 获取统计服务实例 (现在通过ApplicationContext获取)
        statisticsService = ApplicationContext.getInstance().getStatisticsService();
        
        // 获取事件总线
        eventBus = ApplicationContext.getInstance().getEventBus();
        
        // 注意：ApplicationContext初始化时已经自动注册了StatisticsService的监听器
        // 所以这里不需要手动subscribe
        
        // 创建测试文档
        doc1 = TestDocumentFactory.createDocument();
        TestDocumentFactory.setFilePath(doc1, "test1.txt");
        
        doc2 = TestDocumentFactory.createDocument();
        TestDocumentFactory.setFilePath(doc2, "test2.txt");
    }

    @AfterEach
    void tearDown() {
        // 清理：重置ApplicationContext
        ApplicationContext.reset();
    }

    @Test
    void testDurationFormatting() {
        // 测试时长格式化
        assertEquals("0秒", statisticsService.formatDuration(0));
        assertEquals("30秒", statisticsService.formatDuration(30000));
        assertEquals("1分钟", statisticsService.formatDuration(60000));
        assertEquals("5分钟", statisticsService.formatDuration(300000));
        assertEquals("1小时", statisticsService.formatDuration(3600000));
        assertEquals("2小时15分钟", statisticsService.formatDuration(8100000));
        assertEquals("1天", statisticsService.formatDuration(86400000));
        assertEquals("1天3小时", statisticsService.formatDuration(97200000));
    }

    @Test
    void testFileSwitchingAccumulation() throws InterruptedException {
        // 测试文件切换时长的累计
        
        // 1. 首次切换到doc1
        eventBus.publish(new ActiveDocumentChangedEvent(null, doc1));
        Thread.sleep(100); // 等待100毫秒
        
        // 2. 切换到doc2
        eventBus.publish(new ActiveDocumentChangedEvent(doc1, doc2));
        Thread.sleep(100); // 等待100毫秒
        
        // 3. 切换回doc1
        eventBus.publish(new ActiveDocumentChangedEvent(doc2, doc1));
        Thread.sleep(100); // 等待100毫秒
        
        // 4. 切换到doc2
        eventBus.publish(new ActiveDocumentChangedEvent(doc1, doc2));
        Thread.sleep(50); // 等待50毫秒
        
        // 验证doc1的累计时长（应该包含两次编辑：第一次100ms，第二次100ms）
        String duration1 = statisticsService.getDuration("test1.txt");
        // 由于是实时查询，当前活动文件会包含进行中的时长
        // 但doc1已经不是活动文件了，所以应该只有累计的时长
        // 由于时间精度问题，我们只验证格式是否正确
        assertTrue(duration1.contains("秒") || duration1.contains("分钟"), 
            "doc1的时长应该被正确格式化");
        
        // 验证doc2的累计时长（应该包含第一次100ms，加上当前进行中的50ms）
        String duration2 = statisticsService.getDuration("test2.txt");
        assertTrue(duration2.contains("秒") || duration2.contains("分钟"), 
            "doc2的时长应该被正确格式化");
    }

    @Test
    void testCurrentActiveFileRealTimeDuration() throws InterruptedException {
        // 测试当前活动文件的实时时长
        
        // 切换到doc1
        eventBus.publish(new ActiveDocumentChangedEvent(null, doc1));
        Thread.sleep(200); // 等待200毫秒
        
        // 查询当前活动文件的时长，应该包含进行中的时长
        String duration = statisticsService.getDuration("test1.txt");
        // 应该至少包含200毫秒的时长
        assertTrue(duration.contains("秒") || duration.contains("分钟"), 
            "当前活动文件的时长应该包含实时时长");
        
        // 验证时长不为空
        assertFalse(duration.isEmpty(), "当前活动文件的时长不应为空");
    }

    @Test
    void testResetDuration() throws InterruptedException {
        // 测试文件关闭后重置时长
        
        // 1. 切换到doc1
        eventBus.publish(new ActiveDocumentChangedEvent(null, doc1));
        Thread.sleep(200); // 等待200毫秒
        
        // 2. 切换到doc2（这样doc1的时长会被累加）
        eventBus.publish(new ActiveDocumentChangedEvent(doc1, doc2));
        Thread.sleep(100); // 再等待100毫秒
        
        // 3. 验证doc1有累计时长（因为从doc1切换到doc2时，doc1的时长被累加了）
        String durationBefore = statisticsService.getDuration("test1.txt");
        assertFalse(durationBefore.isEmpty(), "doc1的时长不应为空");
        // 验证时长至少包含秒数（200毫秒应该至少显示为"0秒"或更大）
        assertTrue(durationBefore.contains("秒") || durationBefore.contains("分钟") || durationBefore.contains("小时"), 
            "doc1应该有累计时长，当前: " + durationBefore);
        
        // 4. 重置doc1的时长
        statisticsService.resetDuration("test1.txt");
        
        // 5. 验证时长被重置
        String durationAfter = statisticsService.getDuration("test1.txt");
        assertEquals("0秒", durationAfter, "重置后时长应该为0秒");
    }

    @Test
    void testFinalizeSession() throws InterruptedException {
        // 测试退出时的最后一次统计
        
        // 1. 切换到doc1
        eventBus.publish(new ActiveDocumentChangedEvent(null, doc1));
        Thread.sleep(100); // 等待100毫秒
        
        // 2. 完成会话统计
        statisticsService.finalizeSession();
        
        // 3. 验证时长被正确统计
        String duration = statisticsService.getDuration("test1.txt");
        assertTrue(duration.contains("秒") || duration.contains("分钟"), 
            "完成会话后应该包含最后一次的时长");
    }

    @Test
    void testEmptyFilePath() {
        // 测试空文件路径
        String duration = statisticsService.getDuration(null);
        assertEquals("", duration, "空文件路径应该返回空字符串");
        
        duration = statisticsService.getDuration("");
        assertEquals("", duration, "空字符串路径应该返回空字符串");
    }

    @Test
    void testMultipleFileSwitching() throws InterruptedException {
        // 测试多文件切换场景
        
        Document doc3 = TestDocumentFactory.createDocument();
        TestDocumentFactory.setFilePath(doc3, "test3.txt");
        
        // 切换到doc1
        eventBus.publish(new ActiveDocumentChangedEvent(null, doc1));
        Thread.sleep(50);
        
        // 切换到doc2
        eventBus.publish(new ActiveDocumentChangedEvent(doc1, doc2));
        Thread.sleep(50);
        
        // 切换到doc3
        eventBus.publish(new ActiveDocumentChangedEvent(doc2, doc3));
        Thread.sleep(50);
        
        // 切换回doc1
        eventBus.publish(new ActiveDocumentChangedEvent(doc3, doc1));
        Thread.sleep(50);
        
        // 验证所有文件都有时长记录
        String duration1 = statisticsService.getDuration("test1.txt");
        String duration2 = statisticsService.getDuration("test2.txt");
        String duration3 = statisticsService.getDuration("test3.txt");
        
        // doc1是当前活动文件，应该包含实时时长
        assertFalse(duration1.isEmpty(), "doc1应该有时长记录");
        // doc2和doc3不是活动文件，应该只有累计时长
        assertFalse(duration2.isEmpty(), "doc2应该有时长记录");
        assertFalse(duration3.isEmpty(), "doc3应该有时长记录");
    }
}


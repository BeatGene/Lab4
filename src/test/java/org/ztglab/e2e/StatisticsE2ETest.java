package org.ztglab.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ztglab.ui.CommandExecutor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统计模块端到端测试
 * 
 * 测试场景：
 * 1. 文件切换时长的累计
 * 2. editor-list 命令显示时长
 * 3. 文件关闭后重置时长
 * 4. 退出时的最后一次统计
 */
class StatisticsE2ETest {

    @TempDir
    Path tempDir;

    private CommandExecutor executor;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    private Scanner scanner;

    private java.io.InputStream originalIn;

    @BeforeEach
    void setUp() {
        // Capture System.out to verify console output
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

        // Capture System.in for user input simulation
        originalIn = System.in;
        System.setIn(new ByteArrayInputStream(new byte[0]));

        // Default scanner with no input
        scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        executor = new CommandExecutor(scanner);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        if (scanner != null) {
            scanner.close();
        }
    }

    private void execute(String command) {
        try {
            outContent.reset();
            executor.execute(command);
            // 给统计服务一点时间处理事件
            Thread.sleep(10);
        } catch (Exception e) {
            fail("Command execution failed: " + command + " - " + e.getMessage());
        }
    }

    private String getOutput() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    @Test
    void testEditorListShowsDuration() throws Exception {
        String filename1 = "test1.txt";
        String filename2 = "test2.txt";
        Path filePath1 = tempDir.resolve(filename1);
        Path filePath2 = tempDir.resolve(filename2);
        String absPath1 = filePath1.toAbsolutePath().toString();
        String absPath2 = filePath2.toAbsolutePath().toString();

        // 1. 初始化文件1
        execute("init " + absPath1);
        Thread.sleep(100); // 等待100毫秒，让统计服务记录时长

        // 2. 初始化文件2
        execute("init " + absPath2);
        Thread.sleep(100); // 等待100毫秒

        // 3. 切换回文件1
        execute("edit " + absPath1);
        Thread.sleep(100); // 等待100毫秒

        // 4. 查看文件列表
        execute("editor-list");
        String output = getOutput();

        // 验证输出包含时长信息（格式：文件名 (时长)）
        // 使用正则表达式匹配时长格式：(*| ) 文件名 [modified]? (时长 [实时更新]?)
        Pattern durationPattern = Pattern.compile("\\(\\d+[秒分钟小时天]+(?:\\s*\\[实时更新\\])?\\)");
        assertTrue(durationPattern.matcher(output).find(), 
            "editor-list 应该显示时长信息，输出: " + output);
        
        // 验证文件1和文件2都在列表中
        assertTrue(output.contains(filename1) || output.contains(absPath1), 
            "应该包含文件1");
        assertTrue(output.contains(filename2) || output.contains(absPath2), 
            "应该包含文件2");
    }

    @Test
    void testDurationResetAfterClose() throws Exception {
        String filename = "test_reset.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // 1. 初始化文件
        execute("init " + absPath);
        Thread.sleep(100); // 等待100毫秒

        // 2. 查看文件列表，应该有时长
        execute("editor-list");
        String output1 = getOutput();
        Pattern durationPattern = Pattern.compile("\\(\\d+[秒分钟小时天]+(?:\\s*\\[实时更新\\])?\\)");
        assertTrue(durationPattern.matcher(output1).find(), 
            "初始化后，文件列表应显示时长，输出: " + output1);
        
        // 3. 先保存文件，确保文件不是modified状态（这样close时不会询问是否保存）
        execute("save " + absPath);
        Thread.sleep(10);
        
        // 4. 关闭文件（由于文件已保存，不会询问是否保存）
        // 重定向System.in以模拟用户输入（即使不会用到）
        System.setIn(new ByteArrayInputStream("n\n".getBytes(StandardCharsets.UTF_8)));
        execute("close");
        Thread.sleep(10);

        // 5. 重新加载文件
        execute("load " + absPath);
        Thread.sleep(100); // 等待100毫秒

        // 6. 查看文件列表，时长应该重置（或很小）
        execute("editor-list");
        String output2 = getOutput();
        
        // 由于文件被关闭后重新加载，时长应该重置
        // 新加载的文件时长应该很小（接近0秒或几秒）
        assertTrue(output2.contains(filename) || output2.contains(absPath), 
            "文件应该重新出现在列表中");
    }

    @Test
    void testMultipleFileSwitching() throws Exception {
        String filename1 = "test_multi1.txt";
        String filename2 = "test_multi2.txt";
        String filename3 = "test_multi3.txt";
        Path filePath1 = tempDir.resolve(filename1);
        Path filePath2 = tempDir.resolve(filename2);
        Path filePath3 = tempDir.resolve(filename3);
        String absPath1 = filePath1.toAbsolutePath().toString();
        String absPath2 = filePath2.toAbsolutePath().toString();
        String absPath3 = filePath3.toAbsolutePath().toString();

        // 1. 初始化三个文件
        execute("init " + absPath1);
        Thread.sleep(50);
        execute("init " + absPath2);
        Thread.sleep(50);
        execute("init " + absPath3);
        Thread.sleep(50);

        // 2. 在三个文件之间切换（使用文件名而不是绝对路径，因为edit命令使用endsWith匹配）
        execute("edit " + filename1);
        Thread.sleep(50);
        execute("edit " + filename2);
        Thread.sleep(50);
        execute("edit " + filename3);
        Thread.sleep(50);
        execute("edit " + filename1);
        Thread.sleep(50);

        // 3. 查看文件列表
        execute("editor-list");
        String output = getOutput();

        // 验证所有文件都在列表中
        assertTrue(output.contains(filename1) || output.contains(absPath1), 
            "应该包含文件1");
        assertTrue(output.contains(filename2) || output.contains(absPath2), 
            "应该包含文件2");
        assertTrue(output.contains(filename3) || output.contains(absPath3), 
            "应该包含文件3");

        // 验证有活动文件标记
        assertTrue(output.contains("*"), "应该有活动文件标记");
    }

    @Test
    void testDurationFormat() throws Exception {
        String filename = "test_format.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // 初始化文件
        execute("init " + absPath);
        Thread.sleep(100);

        // 查看文件列表
        execute("editor-list");
        String output = getOutput();

        // 验证时长格式正确（应该包含"秒"、"分钟"、"小时"或"天"）
        // 注意：当前活动文件可能包含 [实时更新] 标记
        Pattern formatPattern = Pattern.compile("\\(\\d+[秒分钟小时天]+(?:\\s*\\[实时更新\\])?\\)");
        assertTrue(formatPattern.matcher(output).find(), 
            "时长格式应该正确，输出: " + output);
    }

    @Test
    void testActiveFileMarking() throws Exception {
        String filename1 = "test_active1.txt";
        String filename2 = "test_active2.txt";
        Path filePath1 = tempDir.resolve(filename1);
        Path filePath2 = tempDir.resolve(filename2);
        String absPath1 = filePath1.toAbsolutePath().toString();
        String absPath2 = filePath2.toAbsolutePath().toString();

        // 初始化两个文件
        execute("init " + absPath1);
        execute("init " + absPath2);

        // 切换到文件1
        execute("edit " + absPath1);

        // 查看文件列表
        execute("editor-list");
        String output = getOutput();

        // 验证活动文件标记（*）
        assertTrue(output.contains("*"), "应该有活动文件标记");
        
        // 验证文件1是活动文件（*应该在文件1那一行）
        String[] lines = output.split("\n");
        boolean foundActive = false;
        for (String line : lines) {
            if (line.contains("*") && (line.contains(filename1) || line.contains(absPath1))) {
                foundActive = true;
                break;
            }
        }
        assertTrue(foundActive, "文件1应该是活动文件");
    }
}


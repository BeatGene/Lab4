package org.ztglab.e2e;

/**
 * 端到端测试 (E2E)
 * 
 * 测试覆盖的指令流顺序：
 * 1. 文件生命周期 (testBasicFileLifecycle):
 *    init -> insert -> save -> close
 * 
 * 2. 编辑与撤销 (testEditingAndUndo):
 *    init -> insert -> append -> undo -> redo
 * 
 * 3. 插入与删除 (testInsertAndDelete):
 *    init -> append -> insert -> delete
 * 
 * 4. 替换操作 (testReplace):
 *    init -> insert -> replace
 * 
 * 5. 日志命令 (testLoggingCommands):
 *    init ... with-log -> append -> save -> log-show
 * 
 * 6. 自动加载日志 (testLoadExistingFileWithLogHeader):
 *    load (带 # log 头的文件) -> 验证日志自动启用
 * 
 * 7. 多文件切换 (testMultipleFilesSwitching):
 *    init (file1) -> init (file2) -> edit (file1) -> editor-list
 * 
 * 8. 显示命令 (testShowCommand):
 *    init -> insert -> append -> show -> show start:end
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ztglab.ui.CommandExecutor;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class TextEditorE2ETest {

    @TempDir
    Path tempDir;

    private CommandExecutor executor;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        // Capture System.out to verify console output
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

        // Default scanner with no input
        scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        executor = new CommandExecutor(scanner);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        if (scanner != null) {
            scanner.close();
        }
    }

    private void execute(String command) {
        try {
            // Clear previous output before executing new command
            outContent.reset();
            executor.execute(command);
        } catch (Exception e) {
            // Print exception to original out for debugging if needed
            // originalOut.println("Command failed: " + command);
            // e.printStackTrace(originalOut);
            fail("Command execution failed: " + command + " - " + e.getMessage());
        }
    }

    private String getOutput() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    @Test
    void testBasicFileLifecycle() throws Exception {
        String filename = "test_lifecycle.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // 1. Init file
        execute("init " + absPath);
        assertTrue(getOutput().contains("新文件创建成功"), "Should confirm file creation");
        
        Workspace wm = executor.getWorkspace();
        assertNotNull(wm.getActiveDocument(), "Should have active document");
        assertEquals(absPath, wm.getActiveDocument().getFilePath());

        // 2. Insert text at start (to avoid empty first line issue with append)
        execute("insert 1:1 \"Hello World\"");
        
        // 3. Save
        execute("save");
        assertTrue(getOutput().contains("已保存"), "Should confirm save");
        assertTrue(Files.exists(filePath), "File should exist on disk");
        assertEquals("Hello World", Files.readString(filePath).trim());

        // 4. Close
        execute("close");
        assertTrue(getOutput().contains("已关闭"), "Should confirm close");
        assertNull(wm.getActiveDocument(), "Active document should be null after closing last file");
    }

    @Test
    void testEditingAndUndo() throws Exception {
        String filename = "test_undo.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // Init
        execute("init " + absPath);

        // Insert initial line
        execute("insert 1:1 \"Line 1\"");
        Document doc = executor.getWorkspace().getActiveDocument();
        assertTrue(doc.getContent().contains("Line 1"));

        // Append (adds new line)
        execute("append \"Line 2\"");
        assertTrue(doc.getContent().contains("Line 2"));

        // Undo
        execute("undo");
        String contentAfterUndo = doc.getContent();
        assertTrue(contentAfterUndo.contains("Line 1"));
        assertFalse(contentAfterUndo.contains("Line 2"), "Line 2 should be removed after undo");

        // Redo
        execute("redo");
        String contentAfterRedo = doc.getContent();
        assertTrue(contentAfterRedo.contains("Line 2"), "Line 2 should be restored after redo");
    }

    @Test
    void testInsertAndDelete() throws Exception {
        String filename = "test_edit.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        execute("init " + absPath);
        
        // Insert initial text
        execute("insert 1:1 \"Hello\"");
        
        // Insert " World" at line 1, col 6
        execute("insert 1:6 \" World\"");
        Document doc = executor.getWorkspace().getActiveDocument();
        assertEquals("Hello World", doc.getLine(1));

        // Delete " World" (length 6) starting at col 6
        execute("delete 1:6 6");
        assertEquals("Hello", doc.getLine(1));
    }

    @Test
    void testReplace() throws Exception {
        String filename = "test_replace.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        execute("init " + absPath);
        execute("insert 1:1 \"Hello World\"");

        // Replace "World" with "Java"
        // "Hello World" -> "World" starts at index 7 (1-based)
        execute("replace 1:7 5 \"Java\"");
        
        Document doc = executor.getWorkspace().getActiveDocument();
        assertEquals("Hello Java", doc.getLine(1));
    }

    @Test
    void testLoggingCommands() throws Exception {
        String filename = "test_log_" + System.currentTimeMillis() + ".txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // Init with log
        execute("init " + absPath + " with-log");
        
        execute("append \"Log Test\"");
        execute("save");

        // Check log file existence
        execute("log-show");
        String output = getOutput();
        
        assertTrue(output.contains("=== 日志内容"), "Should show log header");
        assertTrue(output.contains("append \"Log Test\""), "Should contain logged command");
    }

    @Test
    void testLoadExistingFileWithLogHeader() throws Exception {
        String filename = "lab_test.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // Create file with # log header
        Files.writeString(filePath, "# log\nHello");

        // Load file
        execute("load " + absPath);
        
        assertTrue(getOutput().contains("已启用日志"), "Should auto-enable logging");
        
        Document doc = executor.getWorkspace().getActiveDocument();
        assertNotNull(doc);
        assertEquals("# log", doc.getLine(1).trim());
    }

    @Test
    void testMultipleFilesSwitching() throws Exception {
        String file1 = tempDir.resolve("file1.txt").toAbsolutePath().toString();
        String file2 = tempDir.resolve("file2.txt").toAbsolutePath().toString();

        execute("init " + file1);
        execute("init " + file2);

        Workspace wm = executor.getWorkspace();
        assertEquals(file2, wm.getActiveDocument().getFilePath(), "Should be active on file2");

        execute("edit " + file1);
        assertEquals(file1, wm.getActiveDocument().getFilePath(), "Should switch to file1");

        execute("editor-list");
        String listOutput = getOutput();
        assertTrue(listOutput.contains("file1.txt"), "List should contain file1");
        assertTrue(listOutput.contains("file2.txt"), "List should contain file2");
    }
    
    @Test
    void testShowCommand() throws Exception {
        String filename = "test_show.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();
        
        execute("init " + absPath);
        execute("insert 1:1 \"Line 1\"");
        execute("append \"Line 2\"");
        
        execute("show");
        String output = getOutput();
        assertTrue(output.contains("1: Line 1"));
        assertTrue(output.contains("2: Line 2"));
        
        execute("show 2:2");
        output = getOutput();
        assertFalse(output.contains("1: Line 1"));
        assertTrue(output.contains("2: Line 2"));
    }

    @Test
    void testWorkspacePersistence() throws Exception {
        String filename = "test_persist.txt";
        Path filePath = tempDir.resolve(filename);
        String absPath = filePath.toAbsolutePath().toString();

        // 1. Start session 1
        // Prepare input for exit command: "y" to save changes
        String input = "y" + System.lineSeparator();
        Scanner session1Scanner = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        CommandExecutor executor1 = new CommandExecutor(session1Scanner);

        // Init file and modify it
        executor1.execute("init " + absPath);
        executor1.execute("append \"Persisted Content\"");
        
        // Exit (should trigger saveWorkspaceState and prompt for save)
        try {
            executor1.execute("exit");
        } catch (org.ztglab.ui.ConsoleUI.ExitRequestException e) {
            // Expected exception
        }

        // Verify file saved to disk
        assertTrue(Files.exists(filePath), "File should be saved on exit");
        String content = Files.readString(filePath);
        assertTrue(content.contains("Persisted Content"));

        // Verify workspace state file exists
        Path statePath = Path.of("workspace.state");
        assertTrue(Files.exists(statePath), "Workspace state file should exist");

        // 2. Start session 2 (Simulate restart)
        // No input needed as state loading is automatic
        Scanner session2Scanner = new Scanner(new ByteArrayInputStream(new byte[0]));
        CommandExecutor executor2 = new CommandExecutor(session2Scanner);
        
        // Verify state restored
        Workspace wm2 = executor2.getWorkspace();
        assertNotNull(wm2.getActiveDocument(), "Active document should be restored");
        assertEquals(absPath, wm2.getActiveDocument().getFilePath());
        
        // Verify content loaded
        Document doc = wm2.getActiveDocument();
        String restoredContent = doc.getContent();
        assertTrue(restoredContent.contains("Persisted Content"));

        // Cleanup state file
        Files.deleteIfExists(statePath);
    }
}

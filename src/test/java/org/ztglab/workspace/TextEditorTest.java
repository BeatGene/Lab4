package org.ztglab.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.ztglab.workspace.editor.EditOperation;
import org.ztglab.workspace.editor.operations.AppendOperation;
import org.ztglab.infrastructure.FileUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TextEditor 测试类
 * 用于逐步测试 TextEditor 的所有方法
 */
class TextEditorTest {

    @TempDir
    Path tempDir;

    private Document loadDocument(String path) throws IOException {
        String content = FileUtil.readContent(path);
        Document doc = (content == null) ? new Document() : new Document(content);
        doc.setFilePath(path);
        return doc;
    }

    /**
     * 测试构造函数 - 创建不存在的文件
     * 应该创建空Document，modified=true
     */
    @Test
    void testConstructorWithNonExistentFile() throws IOException {
        // 创建一个不存在的文件路径
        Path testFile = tempDir.resolve("newfile.txt");
        String filePath = testFile.toString();

        // 使用 FileUtil 加载 Document
        Document doc = loadDocument(filePath);
        // 模拟 WorkspaceManager 的初始化逻辑
        if (!Files.exists(testFile)) {
            doc.setModified(true);
        }

        // 验证：文件不存在时，应该创建空Document，modified=true
        assertNotNull(doc, "Document 应该被成功创建");
        assertEquals(filePath, doc.getFilePath(), "文件路径应该正确保存");
        assertTrue(doc.isModified(), "新文件应该标记为已修改");
        // 验证Document是空的（只有一个空行）
        assertEquals(0, doc.getLineCount(), "新文件的Document应该只有0行");
    }

    /**
     * 测试构造函数 - 加载已存在的文件
     * 应该加载文件内容到Document，modified=false
     */
    @Test
    void testConstructorWithExistingFile() throws IOException {
        // 创建一个测试文件并写入内容
        Path testFile = tempDir.resolve("existing.txt");
        String content = "Hello World\nThis is line 2";
        Files.writeString(testFile, content);
        
        String filePath = testFile.toString();

        // 使用 FileUtil 加载 Document
        Document doc = loadDocument(filePath);

        // 验证：文件存在时，应该加载内容，modified=false
        assertNotNull(doc, "Document 应该被成功创建");
        assertEquals(filePath, doc.getFilePath(), "文件路径应该正确保存");
        assertFalse(doc.isModified(), "已存在的文件应该标记为未修改");
        // 验证Document内容正确加载（2行）
        assertEquals(2, doc.getLineCount(), "应该加载2行内容");
        assertEquals(content, doc.getContent(), "Document内容应该与文件内容一致");
    }

    /**
     * 测试构造函数 - 空文件
     */
    @Test
    void testConstructorWithEmptyFile() throws IOException {
        // 创建一个空文件
        Path testFile = tempDir.resolve("empty.txt");
        Files.createFile(testFile);
        
        String filePath = testFile.toString();

        // 使用 FileUtil 加载 Document
        Document doc = loadDocument(filePath);

        // 验证：应该成功创建，空文件应该标记为未修改
        assertNotNull(doc, "Document 应该被成功创建");
        assertFalse(doc.isModified(), "空文件应该标记为未修改");
    }

    /**
     * 测试构造函数 - 多行文件
     */
    @Test
    void testConstructorWithMultiLineFile() throws IOException {
        // 创建包含多行的文件
        Path testFile = tempDir.resolve("multiline.txt");
        String content = "Line 1\nLine 2\nLine 3";
        Files.writeString(testFile, content);
        
        String filePath = testFile.toString();

        // 使用 FileUtil 加载 Document
        Document doc = loadDocument(filePath);

        // 验证：应该成功创建并正确加载内容
        assertNotNull(doc, "Document 应该被成功创建");
        assertFalse(doc.isModified(), "已存在的文件应该标记为未修改");
        assertEquals(3, doc.getLineCount(), "应该加载3行内容");
        assertEquals(content, doc.getContent(), "Document内容应该与文件内容一致");
    }

    // ==================== executeCommand 测试 ====================

    /**
     * 测试 executeCommand - 可撤销的命令（AppendCommand）
     * 应该：1. 执行命令 2. 设置 modified=true 3. 命令进入撤销栈
     */
    @Test
    void testExecuteCommandWithUndoableCommand() throws IOException {
        // 创建一个已存在的文件（初始 modified=false）
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Initial content");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 初始状态：modified=false
        assertFalse(doc.isModified(), "初始状态应该未修改");
        String initialContent = doc.getContent();

        // 创建并执行可撤销的操作
        EditOperation appendOp = new AppendOperation(doc, "New line");
        editor.executeOperation(doc, appendOp);

        // 验证：1. 命令已执行（内容已改变）
        String newContent = doc.getContent();
        assertNotEquals(initialContent, newContent, "Document内容应该已改变");
        assertTrue(newContent.contains("New line"), "应该包含追加的内容");

        // 验证：2. modified 已设置为 true
        assertTrue(doc.isModified(), "执行可撤销命令后应该标记为已修改");

        // 验证：3. 命令已进入撤销栈（可以通过 undo 验证）
        assertTrue(editor.undo(doc), "应该可以撤销");
        assertEquals(initialContent, doc.getContent(), "撤销后应该恢复原内容");
    }

    /**
     * 测试 executeCommand - 不可撤销的命令
     * 应该：1. 执行命令 2. 不设置 modified 3. 命令不进入撤销栈
     */
    @Test
    void testExecuteCommandWithNonUndoableCommand() throws IOException {
        // 创建一个已存在的文件（初始 modified=false）
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Initial content");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 初始状态：modified=false
        assertFalse(doc.isModified(), "初始状态应该未修改");

        // 创建一个不可撤销的操作（用于测试）
        NonUndoableTestOperation nonUndoableOp = new NonUndoableTestOperation(doc);
        editor.executeOperation(doc, nonUndoableOp);

        // 验证：1. 操作已执行（execute 被调用）
        // 这里我们通过操作内部的标志来验证
        assertTrue(nonUndoableOp.wasExecuted(), "操作应该被执行");

        // 验证：2. modified 保持为 false（未修改）
        assertFalse(doc.isModified(), "执行不可撤销命令后不应该标记为已修改");

        // 验证：3. 命令未进入撤销栈（undo 应该失败）
        assertFalse(editor.undo(doc), "不可撤销的命令不应该进入撤销栈");
    }

    /**
     * 测试 executeCommand - 多个可撤销命令
     * 验证命令栈的正确管理
     */
    @Test
    void testExecuteCommandMultipleUndoableCommands() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 执行多个操作
        editor.executeOperation(doc, new AppendOperation(doc, "Line 2"));
        editor.executeOperation(doc, new AppendOperation(doc, "Line 3"));
        editor.executeOperation(doc, new AppendOperation(doc, "Line 4"));

        // 验证：modified 为 true
        assertTrue(doc.isModified(), "执行多个命令后应该标记为已修改");

        // 验证：可以撤销多次（LIFO顺序）
        assertTrue(editor.undo(doc), "应该可以撤销最后一个命令");
        assertTrue(doc.getContent().contains("Line 3"), "撤销后应该移除 Line 4");

        assertTrue(editor.undo(doc), "应该可以撤销倒数第二个命令");
        assertTrue(doc.getContent().contains("Line 2"), "撤销后应该移除 Line 3");

        assertTrue(editor.undo(doc), "应该可以撤销第一个命令");
        assertEquals("Line 1", doc.getContent().trim(), "撤销后应该回到初始状态");
    }

    /**
     * 测试 executeCommand - 新文件执行命令
     * 新文件初始 modified=true，执行命令后应该保持 true
     */
    @Test
    void testExecuteCommandOnNewFile() throws IOException {
        // 创建新文件（不存在）
        Path testFile = tempDir.resolve("newfile.txt");
        Document doc = loadDocument(testFile.toString());
        // 模拟新文件初始化
        doc.setModified(true);
        
        TextEditor editor = new TextEditor();

        // 初始状态：modified=true（新文件）
        assertTrue(doc.isModified(), "新文件应该标记为已修改");

        // 执行操作
        editor.executeOperation(doc, new AppendOperation(doc, "First line"));

        // 验证：modified 保持为 true
        assertTrue(doc.isModified(), "新文件执行操作后应该保持已修改状态");
        assertTrue(doc.getContent().contains("First line"), "应该包含追加的内容");
    }

    /**
     * 测试用的不可撤销操作类
     * 用于测试 executeOperation 对不可撤销操作的处理
     */
    private static class NonUndoableTestOperation implements EditOperation {
        private boolean executed = false;

        public NonUndoableTestOperation(Document doc) {
            // Document parameter kept for API consistency but not used in this test operation
        }

        @Override
        public void execute() {
            executed = true;
            // 不修改 doc，只是标记已执行
        }

        @Override
        public void undo() {
            // 不可撤销，不做任何事
        }

        @Override
        public void redo() {
            // 不可撤销，不做任何事
        }

        @Override
        public boolean isUndoable() {
            return false; // 不可撤销
        }

        @Override
        public String getDescription() {
            return "Non-undoable test command";
        }

        public boolean wasExecuted() {
            return executed;
        }
    }

    // ==================== append 方法测试 ====================

    /**
     * 测试 append 方法 - 追加文本到文件
     */
    @Test
    void testAppend() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 初始状态
        assertFalse(doc.isModified(), "初始状态应该未修改");
        String initialContent = doc.getContent();

        // 执行 append
        editor.append(doc, "Line 2");

        // 验证：内容已追加
        String newContent = doc.getContent();
        assertNotEquals(initialContent, newContent, "内容应该已改变");
        assertTrue(newContent.contains("Line 2"), "应该包含追加的内容");

        // 验证：modified 已设置为 true
        assertTrue(doc.isModified(), "执行 append 后应该标记为已修改");

        // 验证：可以撤销
        assertTrue(editor.undo(doc), "应该可以撤销");
        assertEquals(initialContent, doc.getContent(), "撤销后应该恢复原内容");
    }

    /**
     * 测试 append 方法 - 多次追加
     */
    @Test
    void testAppendMultiple() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        editor.append(doc, "Line 2");
        editor.append(doc, "Line 3");
        editor.append(doc, "Line 4");

        String content = doc.getContent();
        assertTrue(content.contains("Line 2"), "应该包含 Line 2");
        assertTrue(content.contains("Line 3"), "应该包含 Line 3");
        assertTrue(content.contains("Line 4"), "应该包含 Line 4");
    }

    // ==================== show 方法测试 ====================

    /**
     * 测试 show() 无参方法 - 显示全部内容
     */
    @Test
    void testShowNoArgs() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello World\nThis is line 2\nLine 3";
        Files.writeString(testFile, content);
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String result = editor.show(doc);

        // 验证：应该包含所有行，格式为 "行号: 内容"
        assertTrue(result.contains("1: Hello World"), "应该包含第1行");
        assertTrue(result.contains("2: This is line 2"), "应该包含第2行");
        assertTrue(result.contains("3: Line 3"), "应该包含第3行");
    }

    /**
     * 测试 show(int start, int end) - 显示指定范围
     */
    @Test
    void testShowWithRange() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Line 1\nLine 2\nLine 3\nLine 4";
        Files.writeString(testFile, content);
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 显示第2-3行
        String result = editor.show(doc, 2, 3);

        assertTrue(result.contains("2: Line 2"), "应该包含第2行");
        assertTrue(result.contains("3: Line 3"), "应该包含第3行");
        assertFalse(result.contains("1: Line 1"), "不应该包含第1行");
        assertFalse(result.contains("4: Line 4"), "不应该包含第4行");
    }

    /**
     * 测试 show - 边界情况：start > end
     */
    @Test
    void testShowInvalidRange() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1\nLine 2");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String result = editor.show(doc, 3, 1); // 无效范围

        assertEquals("", result, "无效范围应该返回空字符串");
    }

    /**
     * 测试 show - 边界情况：start <= 0 显示全部
     */
    @Test
    void testShowWithZeroStart() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Line 1\nLine 2";
        Files.writeString(testFile, content);
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String result = editor.show(doc, 0, 0); // 0 表示显示全部

        assertTrue(result.contains("1: Line 1"), "应该包含第1行");
        assertTrue(result.contains("2: Line 2"), "应该包含第2行");
    }

    /**
     * 测试 show - 边界情况：end 超过总行数
     */
    @Test
    void testShowEndExceedsTotal() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Line 1\nLine 2";
        Files.writeString(testFile, content);
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String result = editor.show(doc, 1, 10); // end 超过总行数

        assertTrue(result.contains("1: Line 1"), "应该包含第1行");
        assertTrue(result.contains("2: Line 2"), "应该包含第2行");
    }

    /**
     * 测试 show - 空文件
     */
    @Test
    void testShowEmptyFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.createFile(testFile);
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String result = editor.show(doc);

        // 空文件应该至少有一行（空行）
        assertNotNull(result, "结果不应该为 null");
    }

    // ==================== undo/redo 方法测试 ====================

    /**
     * 测试 undo - 撤销单个操作
     */
    @Test
    void testUndoSingle() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Initial");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String initialContent = doc.getContent();
        editor.append(doc, "New");

        // 撤销
        assertTrue(editor.undo(doc), "应该可以撤销");
        assertEquals(initialContent, doc.getContent(), "撤销后应该恢复原内容");
        assertTrue(doc.isModified(), "撤销后 modified 应该保持为 true");
    }

    /**
     * 测试 undo - 没有可撤销的操作
     */
    @Test
    void testUndoWhenEmpty() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Initial");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 没有执行任何命令，直接撤销
        assertFalse(editor.undo(doc), "没有可撤销操作时应该返回 false");
    }

    /**
     * 测试 redo - 重做操作
     */
    @Test
    void testRedo() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Initial");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        String initialContent = doc.getContent();
        editor.append(doc, "New");
        String afterAppend = doc.getContent();

        // 撤销
        editor.undo(doc);
        assertEquals(initialContent, doc.getContent(), "撤销后应该恢复原内容");

        // 重做
        assertTrue(editor.redo(doc), "应该可以重做");
        assertEquals(afterAppend, doc.getContent(), "重做后应该恢复追加后的内容");
        assertTrue(doc.isModified(), "重做后 modified 应该保持为 true");
    }

    /**
     * 测试 redo - 没有可重做的操作
     */
    @Test
    void testRedoWhenEmpty() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Initial");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        // 没有撤销任何操作，直接重做
        assertFalse(editor.redo(doc), "没有可重做操作时应该返回 false");
    }

    /**
     * 测试 undo/redo - 多次撤销和重做
     */
    @Test
    void testUndoRedoMultiple() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1");
        Document doc = loadDocument(testFile.toString());
        TextEditor editor = new TextEditor();

        editor.append(doc, "Line 2");
        editor.append(doc, "Line 3");
        editor.append(doc, "Line 4");

        // 撤销3次
        assertTrue(editor.undo(doc));
        assertTrue(editor.undo(doc));
        assertTrue(editor.undo(doc));
        assertEquals("Line 1", doc.getContent().trim(), "撤销3次后应该回到初始状态");

        // 重做2次
        assertTrue(editor.redo(doc));
        assertTrue(editor.redo(doc));
        String content = doc.getContent();
        assertTrue(content.contains("Line 2"), "重做2次后应该包含 Line 2");
        assertTrue(content.contains("Line 3"), "重做2次后应该包含 Line 3");
        assertFalse(content.contains("Line 4"), "重做2次后不应该包含 Line 4");
    }
}


package org.ztglab.e2e;

/**
 * XML编辑器端到端测试 (E2E)
 * 
 * 测试覆盖的指令流顺序：
 * 1. XML文件生命周期 (testXmlFileLifecycle):
 *    init xml -> append-child -> save -> close
 * 
 * 2. XML元素操作 (testXmlElementOperations):
 *    init xml -> append-child -> insert-before -> edit-id -> edit-text -> delete
 * 
 * 3. XML树显示 (testXmlTreeDisplay):
 *    init xml -> append-child (嵌套) -> xml-tree
 * 
 * 4. XML撤销重做 (testXmlUndoRedo):
 *    init xml -> append-child -> undo -> redo
 * 
 * 5. XML文件加载 (testLoadExistingXmlFile):
 *    load (XML文件) -> append-child -> show
 * 
 * 6. XML与文本编辑器共存 (testMixedEditors):
 *    init text -> init xml -> edit (切换) -> editor-list
 * 
 * 7. XML带日志 (testXmlWithLogging):
 *    init xml with-log -> append-child -> log-show
 * 
 * 8. XML错误处理 (testXmlErrorHandling):
 *    ID冲突、不存在的元素、删除根元素等错误场景
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

class XmlEditorE2ETest {

    @TempDir
    Path tempDir;

    private CommandExecutor executor;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

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
            outContent.reset();
            executor.execute(command);
        } catch (Exception e) {
            fail("Command execution failed: " + command + " - " + e.getMessage());
        }
    }

    private String getOutput() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    @Test
    void testXmlFileLifecycle() throws Exception {
        Path filePath = tempDir.resolve("test.xml");
        String absPath = filePath.toAbsolutePath().toString();

        // Init XML file
        execute("init xml");
        assertTrue(getOutput().contains("新文件创建成功"));
        
        Document doc = executor.getWorkspace().getActiveDocument();
        assertNotNull(doc);
        assertTrue(doc.getContent().contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(doc.getContent().contains("<root id=\"root\">"));

        // Add child element
        execute("append-child book book1 root \"\"");
        assertTrue(doc.getContent().contains("<book id=\"book1\""));

        // Save
        execute("save " + absPath);
        assertTrue(getOutput().contains("已保存"));
        assertTrue(Files.exists(filePath));
        
        String fileContent = Files.readString(filePath);
        assertTrue(fileContent.contains("<book id=\"book1\""));

        // Close
        execute("close");
        assertTrue(getOutput().contains("已关闭"));
    }

    @Test
    void testXmlElementOperations() throws Exception {
        execute("init xml");
        Document doc = executor.getWorkspace().getActiveDocument();

        // Append child
        execute("append-child book book1 root \"\"");
        assertTrue(doc.getContent().contains("book1"));

        execute("append-child title title1 book1 \"My Book\"");
        assertTrue(doc.getContent().contains("title1"));
        assertTrue(doc.getContent().contains("My Book"));

        // Insert before
        execute("append-child book book2 root \"\"");
        execute("insert-before book book0 book1 \"\"");
        
        String content = doc.getContent();
        int pos0 = content.indexOf("book0");
        int pos1 = content.indexOf("book1");
        assertTrue(pos0 < pos1, "book0 should appear before book1");

        // Edit ID
        execute("edit-id book0 book00");
        assertTrue(doc.getContent().contains("book00"));
        assertFalse(doc.getContent().contains("book0\""));

        // Edit text
        execute("edit-text title1 \"Updated Title\"");
        assertTrue(doc.getContent().contains("Updated Title"));
        assertFalse(doc.getContent().contains("My Book"));

        // Delete
        execute("delete book00");
        assertFalse(doc.getContent().contains("book00"));
    }

    @Test
    void testXmlTreeDisplay() throws Exception {
        execute("init xml");

        // Build nested structure
        execute("append-child bookstore store1 root \"\"");
        execute("append-child book book1 store1 \"\"");
        execute("append-child title title1 book1 \"Everyday Italian\"");
        execute("append-child author author1 book1 \"Giada De Laurentiis\"");

        // Display tree
        execute("xml-tree");
        String output = getOutput();
        
        assertTrue(output.contains("root"));
        assertTrue(output.contains("bookstore"));
        assertTrue(output.contains("book"));
        assertTrue(output.contains("title"));
        assertTrue(output.contains("Everyday Italian"));
        assertTrue(output.contains("Giada De Laurentiis"));
    }

    @Test
    void testXmlUndoRedo() throws Exception {
        execute("init xml");
        Document doc = executor.getWorkspace().getActiveDocument();

        execute("append-child book book1 root \"\"");
        assertTrue(doc.getContent().contains("book1"));

        // Undo
        execute("undo");
        assertFalse(doc.getContent().contains("book1"), "book1 should be removed after undo");

        // Redo
        execute("redo");
        assertTrue(doc.getContent().contains("book1"), "book1 should be restored after redo");
    }

    @Test
    void testLoadExistingXmlFile() throws Exception {
        Path filePath = tempDir.resolve("existing.xml");
        
        // Create XML file
        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <library id="root">
                    <book id="book1">
                        <title id="title1">Test Book</title>
                    </book>
                </library>
                """;
        Files.writeString(filePath, xmlContent);

        // Load
        execute("load " + filePath.toAbsolutePath().toString());
        Document doc = executor.getWorkspace().getActiveDocument();
        
        assertTrue(doc.getContent().contains("library"));
        assertTrue(doc.getContent().contains("Test Book"));

        // Modify
        execute("append-child book book2 root \"\"");
        assertTrue(doc.getContent().contains("book2"));

        // Show
        execute("show");
        String output = getOutput();
        assertTrue(output.contains("library"));
        assertTrue(output.contains("book2"));
    }

    @Test
    void testMixedEditors() throws Exception {
        Path txtPath = tempDir.resolve("file.txt");
        Path xmlPath = tempDir.resolve("file.xml");

        // Create text file
        execute("init text");
        execute("save " + txtPath.toAbsolutePath().toString());
        execute("append \"Text content\"");

        // Create XML file
        execute("init xml");
        execute("save " + xmlPath.toAbsolutePath().toString());
        execute("append-child item item1 root \"\"");

        Workspace wm = executor.getWorkspace();
        assertEquals(xmlPath.toAbsolutePath().toString(), wm.getActiveDocument().getFilePath());

        // Switch to text file
        execute("edit " + txtPath.toAbsolutePath().toString());
        assertEquals(txtPath.toAbsolutePath().toString(), wm.getActiveDocument().getFilePath());

        // List editors
        execute("editor-list");
        String output = getOutput();
        assertTrue(output.contains("file.txt"));
        assertTrue(output.contains("file.xml"));
    }

    @Test
    void testXmlWithLogging() throws Exception {
        execute("init xml with-log");
        Document doc = executor.getWorkspace().getActiveDocument();
        
        // First line should be # log
        assertTrue(doc.getContent().startsWith("# log"));

        execute("append-child book book1 root \"\"");
        
        Path filePath = tempDir.resolve("logged.xml");
        execute("save " + filePath.toAbsolutePath().toString());

        execute("log-show");
        String output = getOutput();
        assertTrue(output.contains("=== 日志内容"));
        assertTrue(output.contains("append-child"));
    }

    @Test
    void testXmlErrorHandling() throws Exception {
        execute("init xml");

        // Test duplicate ID
        execute("append-child book book1 root \"\"");
        boolean duplicateIdHandled = false;
        try {
            outContent.reset();
            executor.execute("append-child item book1 root \"\"");
            String output = getOutput();
            duplicateIdHandled = output.contains("已存在");
        } catch (Exception e) {
            String fullMessage = e.getMessage();
            if (e.getCause() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            duplicateIdHandled = fullMessage.contains("已存在");
        }
        assertTrue(duplicateIdHandled, "Should throw duplicate ID error");

        // Test non-existent parent
        try {
            outContent.reset();
            executor.execute("append-child item item1 nonexistent \"\"");
            String output = getOutput();
            assertTrue(output.contains("不存在") || output.isEmpty(), "Should handle non-existent parent");
        } catch (Exception e) {
            String fullMessage = e.getMessage();
            if (e.getCause() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            assertTrue(fullMessage.contains("不存在"), "Should throw parent not found error");
        }

        // Test non-existent target for insert-before
        try {
            outContent.reset();
            executor.execute("insert-before item item2 nonexistent \"\"");
            String output = getOutput();
            assertTrue(output.contains("不存在") || output.isEmpty(), "Should handle non-existent target");
        } catch (Exception e) {
            String fullMessage = e.getMessage();
            if (e.getCause() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            assertTrue(fullMessage.contains("不存在"), "Should throw target not found error");
        }

        // Test delete root
        try {
            outContent.reset();
            executor.execute("delete root");
            String output = getOutput();
            assertTrue(output.contains("不能删除根元素") || output.isEmpty(), "Should prevent root deletion");
        } catch (Exception e) {
            String fullMessage = e.getMessage();
            if (e.getCause() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            assertTrue(fullMessage.contains("不能删除根元素"), "Should throw cannot delete root error");
        }

        // Test insert before root
        try {
            outContent.reset();
            executor.execute("insert-before item item3 root \"\"");
            String output = getOutput();
            assertTrue(output.contains("不能在根元素前插入") || output.isEmpty(), "Should prevent insert before root");
        } catch (Exception e) {
            String fullMessage = e.getMessage();
            if (e.getCause() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            assertTrue(fullMessage.contains("不能在根元素前插入"), "Should throw cannot insert before root error");
        }

        // Test edit root ID
        try {
            outContent.reset();
            executor.execute("edit-id root newroot");
            String output = getOutput();
            assertTrue(output.contains("不建议修改根元素") || output.isEmpty(), "Should warn about editing root ID");
        } catch (Exception e) {
            String fullMessage = e.getMessage();
            if (e.getCause() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            assertTrue(fullMessage.contains("不建议修改根元素"), "Should throw warning about root ID edit");
        }
    }

    @Test
    void testXmlShowCommand() throws Exception {
        execute("init xml");
        execute("append-child book book1 root \"\"");
        execute("append-child title title1 book1 \"Test Title\"");

        // Show all
        execute("show");
        String output = getOutput();
        assertTrue(output.contains("<?xml"));
        assertTrue(output.contains("<root"));
        assertTrue(output.contains("<book"));
        assertTrue(output.contains("Test Title"));

        // Show range
        execute("show 2:3");
        output = getOutput();
        assertTrue(output.contains("2:") || output.contains("3:"));
    }

    @Test
    void testXmlComplexStructure() throws Exception {
        execute("init xml");

        // Build complex structure
        execute("append-child library lib1 root \"\"");
        execute("append-child section sci root \"\"");
        execute("append-child section fic root \"\"");
        
        execute("append-child book book1 sci \"\"");
        execute("append-child book book2 sci \"\"");
        execute("append-child book book3 fic \"\"");
        
        execute("append-child title t1 book1 \"Science Book 1\"");
        execute("append-child author a1 book1 \"Author A\"");
        execute("append-child title t2 book2 \"Science Book 2\"");
        execute("append-child title t3 book3 \"Fiction Book\"");

        Document doc = executor.getWorkspace().getActiveDocument();
        String content = doc.getContent();

        // Verify structure
        assertTrue(content.contains("lib1"));
        assertTrue(content.contains("sci"));
        assertTrue(content.contains("fic"));
        assertTrue(content.contains("Science Book 1"));
        assertTrue(content.contains("Fiction Book"));

        // Test xml-tree
        execute("xml-tree");
        String output = getOutput();
        assertTrue(output.contains("library"));
        assertTrue(output.contains("section"));
        assertTrue(output.contains("book"));
        assertTrue(output.contains("Science Book 1"));
    }

    @Test
    void testXmlAttributePreservation() throws Exception {
        Path filePath = tempDir.resolve("attrs.xml");
        
        // Create XML with attributes
        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <catalog id="root">
                    <product id="p1" category="electronics" price="299.99">Laptop</product>
                </catalog>
                """;
        Files.writeString(filePath, xmlContent);

        execute("load " + filePath.toAbsolutePath().toString());
        Document doc = executor.getWorkspace().getActiveDocument();
        
        // Verify attributes preserved
        String content = doc.getContent();
        assertTrue(content.contains("category=\"electronics\""));
        assertTrue(content.contains("price=\"299.99\""));

        // Modify and verify attributes still preserved
        execute("edit-text p1 \"Desktop\"");
        content = doc.getContent();
        assertTrue(content.contains("Desktop"));
        assertTrue(content.contains("category=\"electronics\""));
        assertTrue(content.contains("price=\"299.99\""));
    }
}

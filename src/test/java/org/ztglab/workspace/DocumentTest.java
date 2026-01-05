package org.ztglab.workspace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author YangZhang
 * @createTime 2025/11/19/ 20:56
 */
public class DocumentTest {
    @Test
    @DisplayName("append 普通追加与多行追加")
    void testAppend() {
        Document doc = new Document("first");
        doc.append(" second");
        doc.append("third\nfourth");

        assertEquals(4, doc.getLineCount());
        assertEquals("first\n second\nthird\nfourth", doc.getContent());
    }

    @Test
    @DisplayName("insert - 空文件只能 1:1 插入")
    void testInsertEmptyFile() {
        Document doc = new Document();
        assertThrows(Exception.class, () -> doc.insert(1, 2, "a"));
        assertThrows(Exception.class, () -> doc.insert(2, 1, "a"));
        assertDoesNotThrow(() -> doc.insert(1, 1, "hello"));
        assertEquals("hello", doc.getContent());
    }

    @Test
    @DisplayName("insert - 普通插入（行中、行首、行尾）")
    void testInsertNormal() throws Exception {
        Document doc = new Document("abc\ndefg\nhi");
        doc.insert(1, 2, "123");        // a123bc
        doc.insert(2, 1, ">>");          // >>defg
        doc.insert(3, 3, "\nINSERTED");  // hi + \nINSERTED → 两行

        assertEquals("a123bc\n>>defg\nhi\nINSERTED", doc.getContent());
    }

    @Test
    @DisplayName("insert - 插入多行文本")
    void testInsertMultiLine() throws Exception {
        Document doc = new Document("line1\nline2");
        doc.insert(2, 3, "X\nY\nZ");

        assertEquals(4, doc.getLineCount());
        assertEquals("line1\nliX\nY\nZne2", doc.getContent());
    }

    @Test
    @DisplayName("insert - 越界检查")
    void testInsertOutOfBounds() {
        Document doc = new Document("abc");
        assertAll(
                () -> assertThrows(Exception.class, () -> doc.insert(0, 1, "x")),
                () -> assertThrows(Exception.class, () -> doc.insert(3, 1, "x")),
                () -> assertThrows(Exception.class, () -> doc.insert(1, 5, "x")),
                () -> assertThrows(Exception.class, () -> doc.insert(2, 2, "x")) // 新行只能 col=1
        );
    }

    @Test
    @DisplayName("delete - 正常删除")
    void testDelete() throws Exception {
        Document doc = new Document("abcdefghij");
        doc.delete(1, 3, 5); // 删除 cdefg → ab hij
        assertEquals("abhij", doc.getContent());
    }

    @Test
    @DisplayName("delete - 删除到行尾 & 越界")
    void testDeleteEdge() throws Exception {
        Document doc = new Document("abc");
        doc.delete(1, 2, 2); // 删除 bc
        assertEquals("a", doc.getContent());

        assertAll(
                () -> assertThrows(Exception.class, () -> doc.delete(1, 1, 5)), // 超出
                () -> assertThrows(Exception.class, () -> doc.delete(2, 1, 1)), // 行号越界
                () -> assertThrows(Exception.class, () -> doc.delete(1, 3, 1))  // 列号越界
        );
    }

    @Test
    @DisplayName("replace - 普通替换")
    void testReplace() throws Exception {
        Document doc = new Document("hello world");
        doc.replace(1, 7, 5, "ztglab"); // world → ztglab
        assertEquals("hello ztglab", doc.getContent());
    }

    @Test
    @DisplayName("replace - 替换为多行")
    void testReplaceMultiLine() throws Exception {
        Document doc = new Document("aaaabcccc");
        doc.replace(1, 5, 5, "\nBBB\nCCC"); // bcccc → \nBBB\nCCC
        assertEquals("aaaa\nBBB\nCCC", doc.getContent());
    }

    @Test
    @DisplayName("replace - 替换长度为0（相当于插入）")
    void testReplaceZeroLength() throws Exception {
        Document doc = new Document("abc");
        doc.replace(1, 2, 0, "XYZ");
        assertEquals("aXYZbc", doc.getContent());
    }

    @Test
    @DisplayName("getLine & getLineCount")
    void testGetLine() throws Exception {
        Document doc = new Document("line1\nline2\nline3");
        assertEquals(3, doc.getLineCount());
        assertEquals("line2", doc.getLine(2));
        assertThrows(Exception.class, () -> doc.getLine(0));
        assertThrows(Exception.class, () -> doc.getLine(4));
    }

    @Test
    @DisplayName("getContent 包含结尾换行")
    void testGetContentTrailingNewline() {
        Document doc = new Document("a\nb\n");
        assertEquals("a\nb\n", doc.getContent());
    }
}

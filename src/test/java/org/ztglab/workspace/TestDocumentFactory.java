package org.ztglab.workspace;

public class TestDocumentFactory {
    public static Document createDocument() {
        return new Document();
    }
    public static Document createDocument(String content) {
        return new Document(content);
    }
    public static void setFilePath(Document doc, String path) {
        doc.setFilePath(path);
    }
}

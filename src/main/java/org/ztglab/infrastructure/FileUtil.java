package org.ztglab.infrastructure;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.ztglab.workspace.Document;

/**
 * 文件工具类 - 负责文件的读写操作
 */
public class FileUtil {

    /**
     * 读取文件内容
     * @return 文件内容，如果文件不存在返回 null
     */
    public static String readContent(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 保存Document到文件
     */
    public static void saveFile(String filepath, Document document) throws IOException {
        Path path = Paths.get(filepath);

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 写入文件（UTF-8 编码）
        String content = document.getContent();
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * 获取文件的绝对路径
     *
     * @param filepath 文件路径
     * @return 绝对路径字符串
     */
    public static String getAbsolutePath(String filepath) {
        return Paths.get(filepath).toAbsolutePath().toString();
    }
    public static boolean fileExists(String path) {
        return new File(path).exists();
    }
}

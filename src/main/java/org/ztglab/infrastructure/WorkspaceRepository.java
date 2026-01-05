package org.ztglab.infrastructure;

import org.ztglab.workspace.Document;
import org.ztglab.workspace.Workspace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作区状态持久化仓储
 * 负责将工作区的状态（打开的文件列表、活动文件等）保存到磁盘，并从磁盘恢复。
 */
public class WorkspaceRepository {
    
    private static final String STATE_FILE = "workspace.state";

    public void save(Workspace workspace) {
        try {
            List<String> lines = new ArrayList<>();
            Document activeDocument = workspace.getActiveDocument();
            if (activeDocument != null) {
                lines.add("Active: " + activeDocument.getFilePath());
            }
            for (Map.Entry<String, Document> entry : workspace.getDocuments().entrySet()) {
                String path = entry.getKey();
                Document doc = entry.getValue();
                boolean isModified = doc.isModified();
                lines.add("File: " + path + " | Modified: " + isModified);
            }
            Path configPath = Path.of(STATE_FILE);
            Files.write(configPath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("工作区状态已保存到 " + STATE_FILE);
        } catch (Exception e) {
            System.err.println("保存工作区状态失败: " + e.getMessage());
        }
    }

    public void restore(Workspace workspace) {
        Path configPath = Path.of(STATE_FILE);
        if (!Files.exists(configPath)) return;
        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            String activePath = null;
            for (String line : lines) {
                if (line.startsWith("Active: ")) {
                    activePath = line.substring("Active: ".length()).trim();
                } else if (line.startsWith("File: ")) {
                    try {
                        String[] parts = line.split("\\|");
                        String pathPart = parts[0].trim();
                        String modPart = parts[1].trim();
                        String path = pathPart.substring("File: ".length()).trim();
                        boolean modified = Boolean.parseBoolean(modPart.substring("Modified: ".length()).trim());

                        if (Files.exists(Paths.get(path))) {
                            // 恢复时需要读取文件内容
                            String content = FileUtil.readContent(path);
                            workspace.openDocument(path, content);
                        } else {
                            workspace.init(path);
                        }
                        Document doc = workspace.getDocument(path);
                        if (doc != null) {
                            doc.setModified(modified);
                        }
                    } catch (Exception e) {
                        System.err.println("恢复文件失败: " + line + " - " + e.getMessage());
                    }
                }
            }
            if (activePath != null) {
                try {
                    Document doc = workspace.getDocument(activePath);
                    workspace.setActiveDocument(doc);
                    System.out.println("恢复活动文件: " + activePath);
                } catch (Exception e) {
                    // ignore if not found
                }
            }
        } catch (Exception e) {
            System.err.println("加载工作区状态失败: " + e.getMessage());
        }
    }
}

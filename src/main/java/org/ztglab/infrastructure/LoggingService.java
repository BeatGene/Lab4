package org.ztglab.infrastructure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ztglab.event.events.*;
import org.ztglab.command.AbstractCommand;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;

/**
 * 日志服务：监听命令事件并记录日志
 * 
 * 实现IEventListener<Event>接口，监听所有命令相关事件：
 * 1. CommandReceivedEvent - 命令接收
 * 2. CommandExecutingEvent - 命令执行中
 * 3. CommandCompletedEvent - 命令完成
 * 4. CommandFailedEvent - 命令失败
 * 
 * 功能点：
 * 1. enableLogging(file) / disableLogging(file)
 * 2. 自动监听事件并记录日志
 * 3. showLog(file)
 * 4. 自动写入 session start at 行（每程序启动 + 每文件第一次启用时）
 *
 * 日志文件命名：项目根目录log文件夹下 "." + 原文件名 + ".log"，例如：lab.txt -> .lab.txt.log
 *
 * 失败策略：捕获并吞掉异常，仅打印警告，不影响主流程。
 */
public class LoggingService {

    // 文件是否开启日志记录
    private final Map<String, Boolean> enabledMap = new HashMap<>();
    // 本次运行中已写入 session start 的文件
    private final Set<String> sessionStarted = new HashSet<>();
    // 时间格式化
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    
    // 当前活动文件路径（用于判断是否需要记录日志）
    private String activeFilePath;
    
    // 每个文件的排除命令列表（从文件头解析）
    private final Map<String, Set<String>> excludedCommandsMap = new HashMap<>();

    private Workspace workspace;

    public LoggingService() {
    }

    /**
     * 启用指定文件的日志记录
     */
    public void enable(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        enabledMap.put(filePath, true);
        // 重新启用日志时，清除一次缓存，下次执行命令时会重新解析文件头
        excludedCommandsMap.remove(filePath);
        // 如果本文件还没有写入 session start，则追加一行
        if (!sessionStarted.contains(filePath)) {
            appendLine(filePath, "session start at " + now());
            sessionStarted.add(filePath);
        }
    }

    /**
     * 关闭指定文件的日志记录
     */
    public void disable(String filePath) {
        if (filePath == null) return;
        enabledMap.put(filePath, false);
    }

    /**
     * 是否启用
     */
    public boolean isEnabled(String filePath) {
        return enabledMap.getOrDefault(filePath, false);
    }

    /**
     * 记录命令成功
     */
    public void logCommand(String filePath, String commandName, String rawArgs) {
        if (!isEnabled(filePath)) return;
        StringBuilder line = new StringBuilder();
        line.append(now()).append(' ').append(commandName);
        if (rawArgs != null && !rawArgs.trim().isEmpty()) {
            line.append(' ').append(rawArgs.trim());
        }
        appendLine(filePath, line.toString());
    }

    /**
     * 记录命令失败
     */
    public void logFailure(String filePath, String commandName, String rawArgs, String errorMessage) {
        if (!isEnabled(filePath)) return;
        StringBuilder line = new StringBuilder();
        line.append(now()).append(' ').append(commandName);
        if (rawArgs != null && !rawArgs.trim().isEmpty()) {
            line.append(' ').append(rawArgs.trim());
        }
        line.append(" FAILED");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            line.append(' ').append(errorMessage.replaceAll("\n", " ")); // 简单压平换行
        }
        appendLine(filePath, line.toString());
    }

    /**
     * 显示日志内容（返回字符串；若不存在返回空字符串）
     */
    public String readLog(String filePath) {
        Path logPath = getLogFilePath(filePath);
        if (logPath == null || !Files.exists(logPath)) {
            return ""; // 不存在直接返回空
        }
        try {
            return Files.readString(logPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[log warn] 读取日志失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 迁移日志：将旧文件路径对应的日志内容迁移到新文件路径对应的日志文件。
     * 不删除旧日志，仅做追加，确保历史不丢失。
     */
    public void migrateLogs(String fromFilePath, String toFilePath) {
        if (fromFilePath == null || toFilePath == null) return;
        String content = readLog(fromFilePath);
        if (content == null || content.isEmpty()) {
            return;
        }
        // 确保目标已启用
        enable(toFilePath);
        // 逐行追加，避免一次性写入丢失换行控制
        String[] lines = content.split("\n", -1);
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                appendLine(toFilePath, line);
            }
        }
    }

    /**
     * 获取对应的日志文件路径
     */
    private Path getLogFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        // 日志集中到项目根目录 log 文件夹
        String projectRoot = System.getProperty("user.dir");
        String name = new File(filePath).getName();
        // Windows 文件名不允许的字符替换，处理临时路径如 <unsaved-xml-...>
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        String logDir = projectRoot + File.separator + "log";
        return Paths.get(logDir, "." + name + ".log");
    }

    /**
     * 追加一行到日志文件
     */
    private void appendLine(String filePath, String line) {
        Path logPath = getLogFilePath(filePath);
        if (logPath == null) return;
        try {
            Files.createDirectories(logPath.getParent());
            // 使用 NIO API 追加 UTF-8 写入
            try (BufferedWriter bw = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[log warn] 写入日志失败: " + e.getMessage());
        }
    }

    /**
     * 当前时间字符串
     */
    private String now() {
        return formatter.format(new Date());
    }

    /**
     * 设置当前活动文件路径
     */
    public void setActiveFilePath(String filePath) {
        this.activeFilePath = filePath;
    }

    /**
     * 获取当前活动文件路径
     */
    public String getActiveFilePath() {
        return activeFilePath;
    }

    // ==================== 事件监听器实现 ====================

    /**
     * 处理活动文档变更事件
     */
    public void onEvent(ActiveDocumentChangedEvent event) {
        Document doc = event.getNewDocument();
        if (doc != null) {
            this.activeFilePath = doc.getFilePath();
            // Check for # log
            if (doc.getLineCount() > 0) {
                try {
                    String firstLine = doc.getLine(1);
                    if (firstLine != null && firstLine.trim().startsWith("# log")) {
                        enable(doc.getFilePath());
                        System.out.println("已启用日志: " + doc.getFilePath());
                    }
                } catch (Exception ex) {
                    System.err.println("Error checking log header: " + ex.getMessage());
                }
            }
        } else {
            this.activeFilePath = null;
        }
    }

    /**
     * 处理文档路径更新事件
     */
    public void onEvent(DocumentPathUpdatedEvent event) {
        try {
            migrateLogs(event.getOldPath(), event.getNewPath());
        } catch (Exception ex) {
            System.err.println("Failed to migrate logs: " + ex.getMessage());
        }
    }

    /**
     * 处理命令接收事件
     */
    public void onEvent(CommandReceivedEvent event) {
        if (activeFilePath == null || !isEnabled(activeFilePath)) {
            return;
        }
        // AbstractCommand command = event.getCommand();
        // appendLine(activeFilePath, now() + " [RECEIVED] " + command.getDescription());
    }

    /**
     * 处理命令执行中事件
     * 在此阶段解析文件头，提取排除命令列表
     */
    public void onEvent(CommandExecutingEvent event) {
        if (activeFilePath == null || !isEnabled(activeFilePath)) {
            return;
        }
        
        // 解析文件头，提取排除命令列表
        parseFileHeader(activeFilePath);
    }

    /**
     * 处理命令完成事件
     */
    public void onEvent(CommandCompletedEvent event) {
        if (activeFilePath == null || !isEnabled(activeFilePath)) {
            return;
        }
        AbstractCommand command = event.getCommand();
        String name = command.getOriginalName();
        String args = command.getOriginalArgs();
        
        if (name == null) name = command.getCommandType(); // Fallback
        
        // 检查命令是否在排除列表中
        if (isCommandExcluded(activeFilePath, name)) {
            return; // 跳过记录
        }
        
        StringBuilder line = new StringBuilder();
        line.append(now()).append(' ').append(name);
        if (args != null && !args.isEmpty()) {
            line.append(' ').append(args);
        }
        
        appendLine(activeFilePath, line.toString());
    }

    /**
     * 处理命令失败事件
     */
    public void onEvent(CommandFailedEvent event) {
        if (activeFilePath == null || !isEnabled(activeFilePath)) {
            return;
        }
        AbstractCommand command = event.getCommand();
        String name = command.getOriginalName();
        if (name == null) name = command.getCommandType(); // Fallback
        
        // 检查命令是否在排除列表中
        if (isCommandExcluded(activeFilePath, name)) {
            return; // 跳过记录
        }
        
        String errorMsg = event.getException().getMessage();
        appendLine(activeFilePath, now() + " [FAILED] " + command.getDescription() + 
                   " (原因: " + errorMsg + ")");
    }

    // ==================== 日志过滤功能 ====================
    
    /**
     * 解析文件头，提取排除命令列表
     * 语法：`# log -e cmd1 -e cmd2 ...` 或 `# log`
     * 
     * @param filePath 文件路径
     */
    private void parseFileHeader(String filePath) {
        if (filePath == null || workspace == null) {
            return;
        }
        
        // 如果已经解析过，直接返回
        if (excludedCommandsMap.containsKey(filePath)) {
            return;
        }
        
        try {
            // 获取文档
            Document doc = workspace.getActiveDocument();
            if (doc == null || !doc.getFilePath().equals(filePath)) {
                // 如果当前活动文档不是目标文件，尝试从 documents 中查找
                Map<String, Document> documents = workspace.getDocuments();
                doc = documents.get(filePath);
            }
            
            if (doc == null || doc.getLineCount() == 0) {
                // 文件不存在或为空，初始化排除列表为空
                excludedCommandsMap.put(filePath, new HashSet<>());
                return;
            }
            
            // 读取第一行
            String firstLine = doc.getLine(1);
            if (firstLine == null) {
                excludedCommandsMap.put(filePath, new HashSet<>());
                return;
            }
            

            // 检查是否以 "# log" 开头
            if (!firstLine.startsWith("# log")) {
                excludedCommandsMap.put(filePath, new HashSet<>());
                return;
            }
            
            // 解析排除命令列表
            // 语法：`# log -e <cmd> [-e <cmd> ...]`
            // 每个 -e 后面跟一个命令名
            Set<String> excludedCommands = new HashSet<>();
            
            // 检查是否有 -e 参数
            if (firstLine.contains("-e")) {
                String[] parts = firstLine.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("-e") && i + 1 < parts.length) {
                        // 找到 -e，下一个部分就是命令名
                        String cmdName = parts[i + 1].trim();
                        if (!cmdName.isEmpty() && !cmdName.equals("-e")) {
                            excludedCommands.add(cmdName);
                        }
                        i++; // 跳过已处理的命令名
                    }
                }
            }
            
            // 缓存解析结果
            excludedCommandsMap.put(filePath, excludedCommands);
            
        } catch (Exception e) {
            // 解析失败，使用空排除列表
            System.err.println("[log warn] 解析文件头失败: " + e.getMessage());
            excludedCommandsMap.put(filePath, new HashSet<>());
        }
    }
    
    /**
     * 检查命令是否在排除列表中
     * 
     * @param filePath 文件路径
     * @param commandName 命令名称
     * @return true 如果命令被排除，false 否则
     */
    private boolean isCommandExcluded(String filePath, String commandName) {
        if (filePath == null || commandName == null) {
            return false;
        }
        
        Set<String> excludedCommands = excludedCommandsMap.get(filePath);
        if (excludedCommands == null) {
            // 如果还没有解析过，先解析
            parseFileHeader(filePath);
            excludedCommands = excludedCommandsMap.get(filePath);
        }
        
        if (excludedCommands == null) {
            return false;
        }
        
        return excludedCommands.contains(commandName);
    }
}

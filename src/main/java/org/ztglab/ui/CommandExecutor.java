package org.ztglab.ui;

import org.ztglab.command.CommandBus;
import org.ztglab.workspace.Workspace;
import org.ztglab.command.commands.*;
import org.ztglab.ui.ConsoleUI.ExitRequestException;

import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.infrastructure.WorkspaceRepository;
import org.ztglab.infrastructure.FileUtil;
import org.ztglab.workspace.IEditor;


import java.util.Scanner;

/**
 * 命令执行器 - 负责解析命令并通过命令总线分发
 * 
 * 设计思路：
 * - 只负责命令解析（命令名 + 参数提取）
 * - 创建命令实例并通过命令总线分发
 * - 由命令总线将命令路由到相应的处理器
 */
public class CommandExecutor {

    private final Workspace workspace;
    private final CommandBus commandBus;
    private final Scanner scanner;
    private final WorkspaceRepository workspaceRepository;

    public CommandExecutor(Scanner scanner) {
        this.scanner = scanner;
        this.workspace = new Workspace();
        this.workspaceRepository = new WorkspaceRepository();
        
        ApplicationContext context = ApplicationContext.getInstance();
        this.commandBus = context.getCommandBus();
        
        registerHandlers();
        
        // 恢复工作区状态
        this.workspaceRepository.restore(this.workspace);
    }

    private void registerHandlers() {
        // 工作区命令
        commandBus.registerHandler(LoadFileCommand.class, new LoadFileCommand.Handler(workspace));
        commandBus.registerHandler(SaveFileCommand.class, new SaveFileCommand.Handler(workspace));
        commandBus.registerHandler(InitFileCommand.class, new InitFileCommand.Handler(workspace));
        commandBus.registerHandler(CloseFileCommand.class, new CloseFileCommand.Handler(workspace, scanner));
        commandBus.registerHandler(EditFileCommand.class, new EditFileCommand.Handler(workspace));
        commandBus.registerHandler(ShowEditorListCommand.class, new ShowEditorListCommand.Handler(workspace));
        commandBus.registerHandler(ShowDirTreeCommand.class, new ShowDirTreeCommand.Handler(workspace));
        
        // 通用显示命令
        commandBus.registerHandler(ShowCommand.class, new ShowCommand.Handler(workspace));
        
        // 编辑器通用请求分发器 (核心)
        commandBus.registerHandler(EditorCommandRequest.class, new EditorCommandRequest.Handler(workspace));
        
        // 撤销/重做 (属于工作区通用命令，但委托给当前编辑器处理)
        commandBus.registerHandler(UndoCommand.class, new UndoCommand.Handler(workspace));
        commandBus.registerHandler(RedoCommand.class, new RedoCommand.Handler(workspace));
        
        // 日志命令
        commandBus.registerHandler(LogOnCommand.class, new LogOnCommand.Handler(workspace));
        commandBus.registerHandler(LogOffCommand.class, new LogOffCommand.Handler(workspace));
        commandBus.registerHandler(LogShowCommand.class, new LogShowCommand.Handler(workspace));
        
        // 注册编辑器命令
        for (IEditor editor : workspace.getRegisteredEditors()) {
            editor.registerCommands(commandBus, workspace);
        }
    }

    /**
     * 执行命令
     * @param input 完整的用户输入
     */
    public void execute(String input) throws Exception {
        // 解析命令和参数
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        org.ztglab.command.AbstractCommand cmd = null;

        // 根据命令名分发
        switch (commandName) {
            // ==================== 工作区命令 ====================
            case "load" -> cmd = createLoad(args);
            case "save" -> cmd = createSave(args);
            case "init" -> cmd = createInit(args);
            case "close" -> cmd = createClose(args);
            case "edit" -> cmd = createEdit(args);
            case "editor-list" -> cmd = createEditorList();
            case "dir-tree" -> cmd = createDirTree(args);
            case "undo" -> cmd = createUndo();
            case "redo" -> cmd = createRedo();
            case "exit" -> executeExit(); // Exit is special
            
            // ==================== 文本编辑命令 (现在通过通用请求分发) ====================
            case "append", "insert", "delete", "replace" -> cmd = new EditorCommandRequest(commandName, args);
            
            case "show" -> cmd = createShow(args);
            
            // ==================== XML 编辑命令 ====================
            case "insert-before", "append-child", "edit-id", "edit-text", "xml-tree" -> cmd = new EditorCommandRequest(commandName, args);
            
            // ==================== 日志命令 ====================
            case "log-on" -> cmd = createLogOn(args);
            case "log-off" -> cmd = createLogOff(args);
            case "log-show" -> cmd = createLogShow(args);
            // ==================== 拼写检查 ==================== 视作(仿照)编辑命令处理
            case "spell-check" -> cmd = new EditorCommandRequest("spellcheck", args);
            // ==================== 其他命令 ====================
            case "help" -> executeHelp();
            
            default -> throw new IllegalArgumentException(
                "未知命令: " + commandName + "\n输入 help 查看帮助"
            );
        }

        if (cmd != null) {
            cmd.setOriginalCommand(commandName, args);
            commandBus.dispatch(cmd);
        }
    }

    // ==================== 工作区命令 ====================

    /**
     * 加载文件
     * 格式: load <file>
     */
    private LoadFileCommand createLoad(String args) throws Exception {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("用法: load <filepath>");
        }
        
        String filepath = args.trim();
        return new LoadFileCommand(filepath);
    }

    /**
     * 保存文件
     * 格式: save [file|all]
     */
    private SaveFileCommand createSave(String args) throws Exception {
        String option = args.trim();
        
        if (option.isEmpty()) {
            // 保存当前文件
            return new SaveFileCommand(null, false);
        } else if ("all".equalsIgnoreCase(option)) {
            // 保存所有文件
            return new SaveFileCommand(null, true);
        } else {
            // 保存指定文件
            return new SaveFileCommand(args, false);
        }
    }


    /**
     * 创建新缓冲区
     * 格式: init <text|xml> [with-log]
     */
    private InitFileCommand createInit(String args) throws Exception {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("用法: init <text|xml|filepath> [with-log]");
        }
        String[] parts = args.trim().split("\\s+", 2);
        String first = parts[0];
        boolean withLog = parts.length > 1 && "with-log".equalsIgnoreCase(parts[1]);

        // 允许 filepath 或 明确类型
        String token = first;
        return new InitFileCommand(token, withLog);
    }

    /**
     * 关闭文件
     * 格式: close [file]
     */
    private CloseFileCommand createClose(String args) throws Exception {
        String filepath = args.trim();
        
        if (filepath.isEmpty()) {
            // 关闭当前文件
            return new CloseFileCommand(null);
        } else {
            // 关闭指定文件
            return new CloseFileCommand(filepath);
        }
    }

    /**
     * 切换活动文件
     * 格式: edit <file>
     */
    private EditFileCommand createEdit(String args) throws Exception {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("用法: edit <filepath>");
        }
        
        String filepath = args.trim();
        return new EditFileCommand(filepath);
    }

    /**
     * 显示文件列表
     * 格式: editor-list
     */
    private ShowEditorListCommand createEditorList() throws Exception {
        return new ShowEditorListCommand();
    }

    /**
     * 显示目录树
     * 格式: dir-tree [path]
     */
    private ShowDirTreeCommand createDirTree(String args) throws Exception {
        String path = args.trim();
        return new ShowDirTreeCommand(path.isEmpty() ? null : path);
    }

    /**
     * 撤销操作
     * 格式: undo
     */
    private UndoCommand createUndo() throws Exception {
        return new UndoCommand();
    }

    /**
     * 重做操作
     * 格式: redo
     */
    private RedoCommand createRedo() throws Exception {
        return new RedoCommand();
    }

    /**
     * 退出程序
     * 格式: exit
     */
    private void executeExit() throws Exception {
        // 检查未保存的文件
        for (org.ztglab.workspace.Document doc : workspace.getModifiedDocuments()) {
            System.out.println("文件未保存: " + doc.getFilePath() + " 是否保存? (y/n)");
            String ans = scanner.nextLine();
            if (ans.equalsIgnoreCase("y")) {
                // 使用 FileUtil 直接保存，并通知 workspace 更新状态
                FileUtil.saveFile(doc.getFilePath(), doc);
                workspace.notifySaved(doc.getFilePath());
            }
        }
        
        workspace.exit();
        workspaceRepository.save(workspace);
        throw new ExitRequestException();
    }

    // ==================== 显示命令 ====================

    /**
     * 显示内容
     * 格式: show [start:end]
     */
    private ShowCommand createShow(String args) throws Exception {
        if (args.isEmpty()) {
            // 显示全部
            return new ShowCommand(null, null);
        } else {
            // 解析 start:end
            String[] range = args.trim().split(":");
            if (range.length != 2) {
                throw new IllegalArgumentException("格式错误，应为 start:end");
            }
            
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            
            return new ShowCommand(start, end);
        }
    }

    // ==================== 日志命令 ====================

    /**
     * 启用日志
     * 格式: log-on [file]
     */
    private LogOnCommand createLogOn(String args) throws Exception {
        String filepath = args.trim();
        return new LogOnCommand(filepath.isEmpty() ? null : filepath);
    }

    /**
     * 关闭日志
     * 格式: log-off [file]
     */
    private LogOffCommand createLogOff(String args) throws Exception {
        String filepath = args.trim();
        return new LogOffCommand(filepath.isEmpty() ? null : filepath);
    }

    /**
     * 显示日志
     * 格式: log-show [file]
     */
    private LogShowCommand createLogShow(String args) throws Exception {
        String filepath = args.trim();
        return new LogShowCommand(filepath.isEmpty() ? null : filepath);
    }

    // ==================== 其他命令 ====================

    /**
     * 显示帮助信息
     * 格式: help
     */
    private void executeHelp() {
        System.out.println(getHelpText());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取帮助文本
     */
    private String getHelpText() {
        return """
            ╔════════════════════════════════════════════════════════════╗
            ║                    命令帮助                                ║
            ╚════════════════════════════════════════════════════════════╝
            
            ==================== 工作区命令 ====================
            load <file>              - 加载文件
            save [file|all]          - 保存文件（不指定参数保存当前文件）
            init <file> [with-log]   - 创建新文件
            close [file]             - 关闭文件
            edit <file>              - 切换活动文件
            editor-list              - 显示已打开文件列表
            dir-tree [path]          - 显示目录树
            undo                     - 撤销
            redo                     - 重做
            exit                     - 退出程序
            
            ==================== 文本编辑命令 ====================
            append "text"                    - 追加文本到末尾
            insert <line:col> "text"         - 在指定位置插入文本
            delete <line:col> <len>          - 删除指定长度的字符
            replace <line:col> <len> "text"  - 替换指定长度的文本
            show [start:end]                 - 显示内容（不指定参数显示全部）
            
            ==================== 日志命令 ====================
            log-on [file]            - 启用日志记录
            log-off [file]           - 关闭日志记录
            log-show [file]          - 显示日志内容
            
            ==================== 拼写检查 ====================
            spell-check[file]        -检查文本文件、xml文件中的拼写错误
            
            ==================== 说明 ====================
            - 行号和列号从 1 开始计数
            - 带空格的文本参数必须用双引号包裹
            - 命令不区分大小写
            """;
    }

    /**
     * 获取编辑器管理器（供测试使用）
     */
    public Workspace getWorkspace() {
        return workspace;
    }
}

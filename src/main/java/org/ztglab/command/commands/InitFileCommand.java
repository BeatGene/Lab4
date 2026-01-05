package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;

import org.ztglab.infrastructure.LoggingService;
import org.ztglab.infrastructure.ApplicationContext;

/**
 * 初始化新文件命令
 * Lab2更新: 支持 init <text|xml> [with-log] 语法
 */
public class InitFileCommand extends AbstractCommand {
    
    public final String fileTypeOrPath; // "text" or "xml" or filepath
    public final boolean withLog;

    public InitFileCommand(String fileTypeOrPath, boolean withLog) {
        super();
        this.fileTypeOrPath = fileTypeOrPath;
        this.withLog = withLog;
    }

    @Override
    public String getDescription() {
        return "初始化" + fileTypeOrPath + "文件" + (withLog ? " (带日志)" : "");
    }

    public static class Handler extends AbstractCommandHandler<InitFileCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(InitFileCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(InitFileCommand command) throws Exception {
            workspace.init(command.fileTypeOrPath);
            
            if (command.withLog) {
                LoggingService loggingService = ApplicationContext.getInstance().getLoggingService();

                if(loggingService == null ) {
                    throw new Exception("日志服务不可用！");
                }
                if(workspace.getActiveDocument() == null) {
                    throw new Exception("没有活动文件！");
                }
                String path = workspace.getActiveDocument().getFilePath();
                loggingService.enable(path);
                if (workspace.getActiveDocument().getLineCount() > 0) {
                    String firstLine = workspace.getActiveDocument().getLine(1);
                    if (firstLine == null || !firstLine.startsWith("# log")) {
                        workspace.getActiveDocument().insert(1, 1, "# log");
                    }
                } else {
                    workspace.getActiveDocument().append("# log");
                }
            }
        }
    }
}

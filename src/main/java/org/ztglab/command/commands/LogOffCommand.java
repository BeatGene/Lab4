package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;
import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.infrastructure.LoggingService;
import java.io.File;

public class LogOffCommand extends AbstractCommand {
    private final String filepath;

    public LogOffCommand(String filepath) {
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String getDescription() {
        return "关闭日志记录: " + (filepath != null ? filepath : "当前文件");
    }

    public static class Handler extends AbstractCommandHandler<LogOffCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(LogOffCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(LogOffCommand command) throws Exception {
            String path = command.getFilepath();
            
            if (path == null) {
                Document doc = workspace.getActiveDocument();
                if (doc == null) throw new Exception("没有活动文件");
                path = doc.getFilePath();
            } else {
                // Try to resolve
                File f = new File(path);
                String abs = f.getAbsolutePath();
                if (workspace.getDocuments().containsKey(abs)) {
                    path = abs;
                } else {
                    for (String key : workspace.getDocuments().keySet()) {
                        if (key.endsWith(path)) {
                            path = key;
                            break;
                        }
                    }
                }
            }

            LoggingService service = ApplicationContext.getInstance().getLoggingService();
            if (service != null) {
                service.disable(path);
                System.out.println("已关闭日志: " + path);
            }
        }
    }
}

package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;
import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.infrastructure.LoggingService;
import java.io.File;

public class LogShowCommand extends AbstractCommand {
    private final String filepath;

    public LogShowCommand(String filepath) {
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String getDescription() {
        return "显示日志内容: " + (filepath != null ? filepath : "当前文件");
    }

    public static class Handler extends AbstractCommandHandler<LogShowCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(LogShowCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(LogShowCommand command) throws Exception {
            String path = command.getFilepath();
            
            if (path == null) {
                Document doc = workspace.getActiveDocument();
                if (doc == null) throw new Exception("没有活动文件");
                path = doc.getFilePath();
            } else {
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
                String content = service.readLog(path);
                if (content.isEmpty()) {
                    System.out.println("(日志为空或文件不存在)");
                } else {
                    System.out.println("=== 日志内容 [" + path + "] ===");
                    System.out.println(content);
                    System.out.println("=====================================");
                }
            }
        }
    }
}

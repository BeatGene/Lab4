package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;
import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.infrastructure.LoggingService;
import java.io.File;

public class LogOnCommand extends AbstractCommand {
    private final String filepath;

    public LogOnCommand(String filepath) {
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String getDescription() {
        return "启用日志记录: " + (filepath != null ? filepath : "当前文件");
    }

    public static class Handler extends AbstractCommandHandler<LogOnCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(LogOnCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(LogOnCommand command) throws Exception {
            String path = command.getFilepath();
            Document doc = null;

            if (path == null) {
                doc = workspace.getActiveDocument();
                if (doc == null)
                    throw new Exception("没有活动文件");
                path = doc.getFilePath();
            } else {
                File f = new File(path);
                String abs = f.getAbsolutePath();
                if (workspace.getDocuments().containsKey(abs)) {
                    doc = workspace.getDocuments().get(abs);
                    path = abs;
                } else {
                    for (String key : workspace.getDocuments().keySet()) {
                        if (key.endsWith(path)) {
                            doc = workspace.getDocuments().get(key);
                            path = key;
                            break;
                        }
                    }
                }
            }

            LoggingService service = ApplicationContext.getInstance().getLoggingService();
            if (service == null) {
                throw new Exception("日志服务不可用！");
            }
            if (doc != null) {
                if (doc.getLineCount() > 0) {
                    String firstLine = doc.getLine(1);
                    if (firstLine == null || !firstLine.trim().startsWith("# log")) {
                        doc.insert(1, 1, "# log");
                        doc.setModified(true);
                    }
                } else {
                    doc.append("# log");
                    doc.setModified(true);
                }
                service.enable(path);
                System.out.println("已启用日志: " + path);
            } else {
                File f = new File(path);
                if (f.exists()) {
                    service.enable(f.getAbsolutePath());
                    System.out.println("已启用日志: " + f.getAbsolutePath());
                } else {
                    System.out.println("文件未打开: " + path);
                }
            }

        }
    }
}

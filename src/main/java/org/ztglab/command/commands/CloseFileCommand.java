package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;
import org.ztglab.infrastructure.FileUtil;
import java.util.Scanner;

/**
 * 关闭文件命令
 */
public class CloseFileCommand extends AbstractCommand {
    
    private final String filepath; // null表示关闭当前文件

    public CloseFileCommand(String filepath) {
        super();
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String getDescription() {
        return filepath == null ? "关闭当前文件" : "关闭文件: " + filepath;
    }

    public static class Handler extends AbstractCommandHandler<CloseFileCommand> {
        private final Workspace workspace;
        private final Scanner scanner;

        public Handler(Workspace workspace, Scanner scanner) {
            super(CloseFileCommand.class);
            this.workspace = workspace;
            this.scanner = scanner;
        }

        @Override
        public void handle(CloseFileCommand command) throws Exception {
            String path = command.getFilepath();
            Document doc;
            if (path == null) {
                if (workspace.getActiveDocument() == null) throw new Exception("没有活动文件");
                doc = workspace.getActiveDocument();
            } else {
                doc = workspace.getDocument(path);
            }

            if (doc.isModified()) {
                System.out.println("文件已修改，是否保存? (y/n)");
                String ans = scanner.nextLine();
                if (ans.equalsIgnoreCase("y")) {
                    workspace.notifySaved(doc.getFilePath());
                    FileUtil.saveFile(doc.getFilePath(), doc);
                }
            }
            
            workspace.close(doc.getFilePath());
        }
    }
}

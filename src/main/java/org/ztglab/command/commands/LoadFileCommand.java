package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.infrastructure.FileUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 加载文件命令
 */
public class LoadFileCommand extends AbstractCommand {
    
    private final String filepath;

    public LoadFileCommand(String filepath) {
        super();
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String getDescription() {
        return "加载文件: " + filepath;
    }

    public static class Handler extends AbstractCommandHandler<LoadFileCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(LoadFileCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(LoadFileCommand command) throws Exception {
            String abs = new File(command.getFilepath()).getAbsolutePath();
            
            // 如果已经在工作区中，直接切换
            if (workspace.getDocuments().containsKey(abs)) {
                workspace.openDocument(abs, null); // content null 表示不重新加载
                return;
            }

            // 如果文件不存在，走初始化流程
            if (!Files.exists(Paths.get(abs))) {
                workspace.init(abs);
                System.out.println("新文件已创建并加载: " + abs);
                return;
            }

            // 读取文件内容 (I/O 操作在应用层进行)
            String content = FileUtil.readContent(abs);
            workspace.openDocument(abs, content);
        }
    }
}

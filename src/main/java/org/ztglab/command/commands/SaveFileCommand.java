package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;
import org.ztglab.infrastructure.FileUtil;
import java.io.File;

/**
 * 保存文件命令
 */
public class SaveFileCommand extends AbstractCommand {
    
    private final String filepath; // null表示保存当前文件
    private final boolean saveAll;

    public SaveFileCommand(String filepath, boolean saveAll) {
        super();
        this.filepath = filepath;
        this.saveAll = saveAll;
    }

    public String getFilepath() {
        return filepath;
    }

    public boolean isSaveAll() {
        return saveAll;
    }

    @Override
    public String getDescription() {
        if (saveAll) {
            return "保存所有文件";
        } else if (filepath == null) {
            return "保存当前文件";
        } else {
            return "保存文件: " + filepath;
        }
    }

    public static class Handler extends AbstractCommandHandler<SaveFileCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(SaveFileCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(SaveFileCommand command) throws Exception {
            if (command.isSaveAll()) {
                for (Document doc : workspace.getDocuments().values()) {
                    workspace.notifySaved(doc.getFilePath());
                    FileUtil.saveFile(doc.getFilePath(), doc);
                }
            } else if (command.getFilepath() == null) {
                // 保存当前文件
                Document activeDoc = workspace.getActiveDocument();
                if (activeDoc == null) throw new Exception("没有活动文件");
                
                workspace.notifySaved(activeDoc.getFilePath());
                FileUtil.saveFile(activeDoc.getFilePath(), activeDoc);
            } else {
                // 另存为或保存指定文件
                String targetPath = new File(command.getFilepath()).getAbsolutePath();
                Document activeDoc = workspace.getActiveDocument();
                
                // 检查是否是“另存为”场景（当前是临时文件，或者目标路径与当前不同）
                if (activeDoc != null && activeDoc.getFilePath().startsWith("<unsaved-")) {
                    // 这是一个“另存为”操作
                    workspace.notifySavedAs(activeDoc.getFilePath(), targetPath);
                    FileUtil.saveFile(targetPath, activeDoc);
                } else {
                    // 只是保存指定路径的文件（必须已打开）
                    if (!workspace.getDocuments().containsKey(targetPath)) {
                        throw new Exception("文件未在工作区中打开: " + targetPath);
                    }
                    Document doc = workspace.getDocuments().get(targetPath);
                    workspace.notifySaved(targetPath);
                    FileUtil.saveFile(targetPath, doc);
                }
            }
        }
    }
}

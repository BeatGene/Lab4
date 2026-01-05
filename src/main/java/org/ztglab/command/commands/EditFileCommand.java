package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;

/**
 * 切换活动文件命令
 */
public class EditFileCommand extends AbstractCommand {
    
    private final String filepath;

    public EditFileCommand(String filepath) {
        super();
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String getDescription() {
        return "切换到文件: " + filepath;
    }

    public static class Handler extends AbstractCommandHandler<EditFileCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(EditFileCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(EditFileCommand command) throws Exception {
            workspace.edit(command.getFilepath());
        }
    }
}

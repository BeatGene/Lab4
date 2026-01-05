package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;

/**
 * 撤销命令
 */
public class UndoCommand extends AbstractCommand {
    
    public UndoCommand() {
        super();
    }

    @Override
    public String getDescription() {
        return "撤销操作";
    }

    public static class Handler extends AbstractCommandHandler<UndoCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(UndoCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(UndoCommand command) throws Exception {
            Document doc = workspace.getActiveDocument();
            if (doc == null) {
                throw new Exception("没有活动文件");
            }
            workspace.getEditorService().undo(doc);
        }
    }
}

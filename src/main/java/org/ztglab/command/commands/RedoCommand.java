package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;

/**
 * 重做命令
 */
public class RedoCommand extends AbstractCommand {
    
    public RedoCommand() {
        super();
    }

    @Override
    public String getDescription() {
        return "重做操作";
    }

    public static class Handler extends AbstractCommandHandler<RedoCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(RedoCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(RedoCommand command) throws Exception {
            Document doc = workspace.getActiveDocument();
            if (doc == null) {
                throw new Exception("没有活动文件");
            }
            workspace.getEditorService().redo(doc);
        }
    }
}

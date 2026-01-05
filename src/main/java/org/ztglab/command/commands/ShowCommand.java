package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;

/**
 * 显示命令 (通用)
 * 
 * 无论是文本还是XML，都通过此命令触发显示。
 * 具体显示逻辑委托给 activeEditor.show()
 */
public class ShowCommand extends AbstractCommand {
    
    private final Integer start; // null表示显示全部
    private final Integer end;

    public ShowCommand(Integer start, Integer end) {
        super();
        this.start = start;
        this.end = end;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    @Override
    public String getDescription() {
        if (start == null) {
            return "显示全部内容";
        } else {
            return String.format("显示内容: [%d:%d]", start, end);
        }
    }

    public static class Handler extends AbstractCommandHandler<ShowCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(ShowCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(ShowCommand command) throws Exception {
            Document doc = workspace.getActiveDocument();
            if (doc == null) {
                throw new Exception("没有活动文件");
            }
            String content;
            if (command.getStart() == null) {
                content = workspace.getEditorService().show(doc);
            } else {
                content = workspace.getEditorService().show(doc, command.getStart(), command.getEnd());
            }
            System.out.println(content);
        }
    }
}

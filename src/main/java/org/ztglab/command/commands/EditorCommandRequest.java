package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.IEditor;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.IEditor.EditorCommand;

/**
 * 编辑器命令请求
 * 
 * 这是一个通用载体，用于将 UI 层的原始命令字符串传递给当前活动的编辑器。
 * UI 层不再负责解析特定编辑器的参数，而是将原始参数直接透传。
 */
public class EditorCommandRequest extends AbstractCommand {
    
    private final String commandName;
    private final String rawArgs;

    public EditorCommandRequest(String commandName, String rawArgs) {
        super();
        this.commandName = commandName;
        this.rawArgs = rawArgs;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getRawArgs() {
        return rawArgs;
    }

    @Override
    public String getDescription() {
        return "编辑器命令: " + commandName + " " + rawArgs;
    }

    /**
     * 核心分发器：将请求路由到当前编辑器的具体命令实现
     */
    public static class Handler extends AbstractCommandHandler<EditorCommandRequest> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(EditorCommandRequest.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(EditorCommandRequest request) throws Exception {
            IEditor editor = workspace.getEditorService();
            if (editor == null) {
                throw new IllegalStateException("没有可用的编辑器服务");
            }
            
            // 委托给编辑器解析并查找命令
            EditorCommand command = editor.resolveCommand(request.getCommandName());
            if (command == null) {
                throw new IllegalArgumentException("当前编辑器不支持命令: " + request.getCommandName());
            }
            
            // 执行命令 (参数解析在 execute 内部进行)
            command.execute(workspace.getActiveDocument(), request.getRawArgs());
        }
    }
}

package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import org.ztglab.workspace.Document;
import org.ztglab.infrastructure.ApplicationContext;
import org.ztglab.infrastructure.StatisticsService;
import java.io.File;
import java.util.Map;

/**
 * 显示编辑器列表命令
 */
public class ShowEditorListCommand extends AbstractCommand {
    
    public ShowEditorListCommand() {
        super();
    }

    @Override
    public String getDescription() {
        return "显示编辑器列表";
    }

    public static class Handler extends AbstractCommandHandler<ShowEditorListCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(ShowEditorListCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(ShowEditorListCommand command) throws Exception {
            Map<String, Document> documents = workspace.getDocuments();
            Document activeDocument = workspace.getActiveDocument();
            StatisticsService statisticsService = ApplicationContext.getInstance().getStatisticsService();

            for (Map.Entry<String, Document> entry : documents.entrySet()) {
                String path = entry.getKey();
                Document doc = entry.getValue();
                String name = new File(path).getName();
                
                // 构建显示行：文件名 + 修改标记 + 活动标记 + 时长
                StringBuilder lineBuilder = new StringBuilder();
                
                // 活动文件标记
                if (doc == activeDocument) {
                    lineBuilder.append("* ");
                } else {
                    lineBuilder.append("  ");
                }
                
                // 文件名
                lineBuilder.append(name);
                
                // 修改标记
                if (doc.isModified()) {
                    lineBuilder.append(" [modified]");
                }
                
                // 时长信息
                String duration = statisticsService.getDuration(path);
                if (!duration.isEmpty()) {
                    lineBuilder.append(" (").append(duration);
                    // 如果是当前活动文件，添加提示说明这是实时更新的
                    if (doc == activeDocument) {
                        lineBuilder.append(" [实时更新]");
                    }
                    lineBuilder.append(")");
                }
                
                System.out.println(lineBuilder.toString());
            }
        }
    }
}

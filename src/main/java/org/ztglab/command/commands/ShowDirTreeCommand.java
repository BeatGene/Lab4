package org.ztglab.command.commands;

import org.ztglab.command.AbstractCommand;
import org.ztglab.command.AbstractCommandHandler;
import org.ztglab.workspace.Workspace;
import java.io.File;

/**
 * 显示目录树命令
 */
public class ShowDirTreeCommand extends AbstractCommand {
    
    private final String path; // null表示显示当前工作目录

    public ShowDirTreeCommand(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getDescription() {
        return path == null ? "显示当前目录树" : "显示目录树: " + path;
    }

    public static class Handler extends AbstractCommandHandler<ShowDirTreeCommand> {
        private final Workspace workspace;

        public Handler(Workspace workspace) {
            super(ShowDirTreeCommand.class);
            this.workspace = workspace;
        }

        @Override
        public void handle(ShowDirTreeCommand command) throws Exception {
            String path = command.getPath();
            if (path == null) {
                if (workspace.getActiveDocument() == null) {
                    throw new Exception("没有活动文件");
                }
                path = workspace.getActiveDocument().getFilePath();
            }
            printDirectoryTree(path);
        }

        // 显示路径——支持目录或文件
        private void printDirectoryTree(String path) {
            File target = new File(path);

            if (!target.exists()) {
                System.out.println("路径不存在: " + path);
                return;
            }

            File root;

            // 如果是文件 → 找到父目录
            if (target.isFile()) {
                root = target.getParentFile();
                System.out.println("输入的是文件，将展示其父目录：" + root.getAbsolutePath());
            } else {
                root = target;
            }

            printTreeRecursive(root, "", true, target);
        }

        // 核心递归方法
        private void printTreeRecursive(File file, String prefix, boolean isLast, File target) {
            boolean isTarget = file.equals(target);

            // 标记目标文件
            String displayName = isTarget ? ("[** " + file.getName() + " **]") : file.getName();

            System.out.println(prefix + (isLast ? "└── " : "├── ") + displayName);

            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children == null || children.length == 0) {
                    return;
                }

                for (int i = 0; i < children.length; i++) {
                    boolean last = (i == children.length - 1);
                    printTreeRecursive(children[i], prefix + (isLast ? "    " : "│   "), last, target);
                }
            }
        }
    }
}

package org.ztglab.ui;

import java.util.Scanner;

/**
 * 控制台用户界面 - 简化版
 *
 * 设计思路：
 * - ConsoleUI 只负责读取用户输入并传递给 CommandExecutor
 * - 不处理具体命令逻辑
 */
public class ConsoleUI {

    private final Scanner scanner;
    private final CommandExecutor executor;
    private boolean shouldExit = false;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
        this.executor = new CommandExecutor(scanner);
    }

    /**
     * 启动交互界面
     */
    public void start() {
        printWelcome();

        while (!shouldExit) {
            try {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                executor.execute(input);
            } catch (ExitRequestException e) {
                // 正常退出信号
                shouldExit = true;
                System.out.println("再见！");
            } catch (Exception e) {
                System.err.println("错误: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * 打印欢迎信息
     */
    private void printWelcome() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   文本编辑器 v1.0                      ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("输入 help 查看帮助");
        System.out.println();
    }

    /**
     * 退出请求异常（用于优雅退出）
     */
    public static class ExitRequestException extends RuntimeException {
        public ExitRequestException() {
            super("用户请求退出");
        }
    }
}

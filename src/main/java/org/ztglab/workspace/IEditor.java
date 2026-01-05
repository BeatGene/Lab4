package org.ztglab.workspace;

/**
 * 编辑器接口 - 定义所有编辑器的通用行为 (无状态服务)
 * 
 * 统一了 TextEditor 和 XmlEditor 的通用操作接口
 * 注意：行级编辑方法(append/insert/delete/replace)已移除，由各编辑器自行实现
 */
public interface IEditor {
    
    // === 读操作 (直接返回数据) ===
    String show(Document doc, int start, int end);
    String show(Document doc);
    
    // === 元操作 (委托给 History) ===
    boolean undo(Document doc);
    boolean redo(Document doc);

    /**
     * 初始化文档内容
     * @param doc 新创建的文档对象
     */
    void initDocument(Document doc);

    /**
     * 注册编辑器特定的命令
     * @param bus 命令总线
     * @param workspace 工作区 (用于Handler构造)
     */
    void registerCommands(org.ztglab.command.CommandBus bus, Workspace workspace);

    /**
     * 解析并获取编辑器特定的命令实现
     * @param name 命令名称 (如 "insert", "append")
     * @return 对应的命令实现，如果不支持则返回 null
     */
    EditorCommand resolveCommand(String name);

    /**
     * 编辑器特定命令接口
     * 
     * 每个编辑器（TextEditor, XmlEditor）内部维护一组实现了此接口的命令。
     * 这些命令负责解析原始参数字符串，并调用编辑器的底层 API。
     */
    interface EditorCommand {
        /**
         * 执行命令
         * @param doc 目标文档
         * @param args 原始参数字符串 (需要自行解析)
         * @throws Exception 执行失败或参数错误
         */
        void execute(Document doc, String args) throws Exception;
    }
}

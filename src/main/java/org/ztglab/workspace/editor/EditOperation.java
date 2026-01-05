package org.ztglab.workspace.editor;

/**
 * 编辑操作接口 - 定义可撤销的编辑操作
 *
 * 职责： 
 * 1. 封装对 Buffer 的具体操作
 * 2. 支持撤销/重做功能
 * 3. 只用于 TextEditor 内部的文本编辑
 * 
 * 与应用层命令（AbstractCommand）的区别：
 * - EditOperation：Buffer 级别的可撤销操作，包含业务逻辑
 * - AbstractCommand：应用级别的不可变参数包，不包含业务逻辑
 */
public interface EditOperation {

    /**
     * 执行操作
     */
    void execute();

    /**
     * 撤销操作
     */
    void undo();

    /**
     * 重做操作（默认实现为重新执行）
     */
    default void redo() {
        execute();
    }

    /**
     * 判断操作是否可撤销
     */
    default boolean isUndoable() {
        return true;
    }

    /**
     * 获取操作描述（用于日志）
     */
    String getDescription();
}

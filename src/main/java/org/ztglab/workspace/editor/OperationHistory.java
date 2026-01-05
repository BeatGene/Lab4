package org.ztglab.workspace.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 操作历史管理器 - 负责管理编辑操作的撤销/重做
 *
 * 职责： 
 * 1. 执行编辑操作并记录到历史栈 
 * 2. 提供 undo/redo 功能 
 * 3. 管理操作执行的生命周期
 * 
 * 只用于 TextEditor 内部管理可撤销的编辑操作
 */
public class OperationHistory {
    // 已执行的操作栈（用于撤销）
    private final Deque<EditOperation> undoStack;
    // 已撤销的操作栈（用于重做）
    private final Deque<EditOperation> redoStack;

    public OperationHistory() {
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
    }

    /**
     * 执行操作
     * 1. 调用操作的 execute 方法
     * 2. 将操作压入 undoStack
     * 3. 清空 redoStack（执行新操作后，之前的重做历史失效）
     */
    public void execute(EditOperation operation) {
        operation.execute();
        if (operation.isUndoable()) {
            undoStack.push(operation);
            redoStack.clear();
        }
    }

    /**
     * 撤销操作
     * 1. 从 undoStack 弹出最后一个操作
     * 2. 调用该操作的 undo 方法
     * 3. 将操作压入 redoStack
     * 
     * @return 如果成功撤销返回 true，否则返回 false
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        EditOperation operation = undoStack.pop();
        operation.undo();
        redoStack.push(operation);
        return true;
    }

    /**
     * 重做操作
     * 1. 从 redoStack 弹出最后一个操作
     * 2. 调用该操作的 redo 方法（默认为重新执行）
     * 3. 将操作压入 undoStack
     * 
     * @return 如果成功重做返回 true，否则返回 false
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }

        EditOperation operation = redoStack.pop();
        operation.redo();
        undoStack.push(operation);
        return true;
    }

    /**
     * 清空所有历史记录
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * 检查是否有可撤销的操作
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * 检查是否有可重做的操作
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 获取撤销栈大小
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }

    /**
     * 获取重做栈大小
     */
    public int getRedoStackSize() {
        return redoStack.size();
    }
}

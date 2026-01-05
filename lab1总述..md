# Lab1：基于字符命令界面的文本编辑器 总述

## 实验结果概览

本项目实现了基于命令行的多文件文本编辑器，支持文件操作(load/save/init/close/edit)、查看文件(editor-list/dir-tree)、文本编辑（append/insert/delete/replace/show）、撤销/重做（undo/redo）、日志记录（log-on/log-off/log-show）以及工作区状态的持久化。

下面按三个核心模块（工作区 / 编辑器 / 日志）分别总结。

---

## 模块一：工作区模块（Workspace）

1. 模块总体作用
- 管理会话内的打开文件集合与当前活动编辑器，协调高层命令（加载/保存/创建/关闭/切换/列出/目录树/持久化工作区状态等），连接命令分发（`CommandBus`）和日志服务与单文件编辑器（`TextEditor`）模块。

1. 关联类与设计模式
- 核心类：
	- `org.ztglab.workspace.EditorManager`：处理 editors 映射、activeEditor 管理、命令处理器注册与工作区状态持久化。
	- `org.ztglab.workspace.TextEditor`：处理单文件编辑封装。
	- `org.ztglab.util.FileUtil` / `org.ztglab.util.TreePrinter`：处理文件加载/保存与目录树打印。

- 设计模式：
 	- 命令模式（Command）：`CommandExecutor` 创建 `AbstractCommand` 子类，`CommandBus` 分发，`AbstractCommandHandler` / `ICommandHandler` 执行具体逻辑（对应 `EditorManager` 的内部处理器）。
 	- 服务定位模式（Service Locator）：`ApplicationContext` 提供并缓存 `CommandBus`、`EventBus`、`LoggingService` 等共享组件，表现为单例式访问点。
 	- 观察者模式（Observer）：`CommandBus`/`EventBus` 在命令生命周期发布事件，`LoggingService` 等实现 `IEventListener` 并订阅处理。
 	- 策略模式（Strategy）：各命令的内部处理器（`LoadFileHandler`、`SaveFileHandler` 等）是不同策略的实现，`EditorManager.registerHandlers()` 将它们注册到 `CommandBus`。
 	- 工厂模式（Factory）：`CommandExecutor` 的 `createXxx(...)` 系列方法充当命令对象的工厂，构造不同命令实例。

1. 与工作区相关的命令（映射、示例与结果）
- `load <file>`
	- 映射：`CommandExecutor.createLoad` -> `LoadFileCommand` -> `CommandBus.dispatch` -> `EditorManager.LoadFileHandler.handle` -> `EditorManager.load` -> `new TextEditor(abs)`（或 `init`）。
	- 示例：`load docs/todo.txt` -> 若存在则把文件装入 `editors` 并 `setActiveEditor`，若首行包含 `# log` 则自动启用日志。
- `save [file|all]`
	- 映射：`SaveFileCommand` -> `SaveFileHandler.handle` -> `EditorManager.save` / `saveAll` -> `FileUtil.saveFile`。
	- 示例：`save` 保存当前活动编辑器到磁盘并将 `modified=false`。
- `init <file> [with-log]`
	- 映射：`InitFileCommand` -> `InitFileHandler.handle` -> `EditorManager.init`（创建空 `Buffer`、`OperationHistory`，若 `with-log` 则在首行写入 `# log OperationHistory` 并启用日志）。
	- 示例：`init draft.txt with-log` -> 新建缓冲并启用该文件的日志记录。
- `close [file]`
	- 映射：`CloseFileCommand` -> `CloseFileHandler.handle` -> `EditorManager.close`（支持尾部匹配；若未保存则提示用户）。
	- 示例：`close draft.txt` -> 若 `modified` 则提示保存；随后从 `editors` 中移除并切换活跃编辑器。
- `edit <file>`
	- 映射：`EditFileCommand` -> `EditFileHandler.handle` -> `EditorManager.edit`（按尾部匹配切换 `activeEditor`）。
	- 示例：`edit todo.txt` -> 切换活动文件为 `todo.txt`（前提为已打开）。
- `editor-list`
	- 映射：`ShowEditorListCommand` -> `ShowEditorListHandler.handle` -> `EditorManager.editList()`（返回/打印打开的文件及 `[modified]` 标记）。
	- 示例：`editor-list` 输出会话中所有已打开文件及其修改状态。
- `dir-tree [path]`
	- 映射：`ShowDirTreeCommand` -> `ShowDirTreeHandler.handle` -> `EditorManager.dirTree()` -> 使用 `TreePrinter` 打印目录树。
	- 示例：`dir-tree` 在控制台显示活动文件所在目录的树状结构。
- `exit`（由 `ConsoleUI` / `Main` 控制流程退出）
	- 示例：`exit` 终止程序，未保存的编辑器提示保存。

---

## 模块二：编辑器模块（Editor）

1. 模块总体作用
- 封装单文件的文本数据与可撤销编辑逻辑：维护 `Buffer`（文本行存储）、`OperationHistory`（undo/redo 栈）、`filePath` 与 `modified` 标志，并提供方法供 `EditorManager` 调用以执行具体编辑操作。

1. 关联核心类、函数与设计模式
- 核心类：
	- `org.ztglab.workspace.TextEditor`：对外提供 `append/insert/delete/replace/show/undo/redo` 等接口，并在内部将具体修改封装为 `EditOperation`。
	- `org.ztglab.core.Buffer`：按行管理文本内容（`append/insert/delete/replace/getContent/getLineCount/getLine` 等方法），是所有编辑操作的基础。
	- `org.ztglab.editor.EditOperation`与 `org.ztglab.editor.OperationHistory`：操作模式实现撤销与重做。
- 设计模式：
	- 命令模式（Command）：每个编辑动作为 `EditOperation`（抽象），由不同具体实现（`AppendOperation`、`InsertOperation`、`DeleteOperation`、`ReplaceOperation`）封装 `execute()`/`undo()` 行为，`OperationHistory` 管理 undo/redo 栈。
	- 策略模式（Strategy）： `EditOperation` 类实现了各自的执行/回滚策略，`TextEditor.executeOperation()` 根据传入的操作对象统一调用。
	- 备忘录模式（Memento）：`OperationHistory` 保存操作记录（history stacks）；虽未严格采用单独的 Memento 对象用于完整状态快照，但 `OperationHistory` + `EditOperation` 组合承担了状态保存/恢复的职责。
	- 工厂模式（Factory）：`TextEditor` 或 `CommandExecutor` 在构建高层命令时会创建对应的 `EditOperation` 实例（例如 `new AppendOperation(...)`），构造逻辑集中且可视为工厂调用点。
	- 适配器模式（Adapter）：`TextEditor` 为单文件编辑提供统一接口，封装 `Buffer` 的细节；`Buffer` 作为底层文本表示，对上层提供适配的方法（`insert/replace/delete` 等）。

1. 与编辑器相关的命令（映射、示例与结果）
- `append "text"`（只对文本文件，如 `.txt`）
	- 映射：`AppendTextCommand` -> `AppendTextHandler.handle` -> `activeEditor.append(text)` -> 创建 `AppendOperation` 并 `execute`。
	- 结果：在缓冲末尾添加新行；`AppendOperation` 在 `execute()` 时记录 `savedLineCount`，`undo()` 会删除新增的行以回退。
	- 示例：`append "New line"` -> 在最后一行之后新增一行 `New line`。
- `insert <line:col> "text"`
	- 映射：`InsertTextCommand` -> `InsertTextHandler.handle` -> `activeEditor.insert(line,col,text)` -> `InsertOperation.execute()` -> `Buffer.insert(...)`。
	- 结果：在指定行列插入文本；若 `text` 含换行符，会按行拆分并调整缓冲行数；`undo()` 撤销插入。
	- 示例：`insert 1:4 "XYZ"` -> 在第1行第4列前插入 `XYZ`。
- `delete <line:col> <len>`
	- 映射：`DeleteTextCommand` -> `DeleteTextHandler.handle` -> `activeEditor.delete(line,col,len)` -> `DeleteOperation.execute()` -> `Buffer.delete(...)`。
	- 结果：在指定位置删除指定长度字符；`undo()` 恢复被删字符。
	- 示例：`delete 1:7 5` -> 在第1行从第7列删除 5 个字符。
- `replace <line:col> <len> "text"`
	- 映射：`ReplaceTextCommand` -> `ReplaceTextHandler.handle` -> `activeEditor.replace(...)` -> `ReplaceOperation.execute()` -> `Buffer.replace(...)`。
	- 结果：替换指定长度的字符为新文本；等价于 delete+insert 组合；`undo()` 恢复原始被替换文本。
	- 示例：`replace 1:1 4 "slow"` -> 把第1行前4个字符替换为 `slow`。
- `show [start:end]`
	- 映射：`ShowTextCommand` -> `ShowTextHandler.handle` -> `activeEditor.show(start,end)` -> 使用 `buffer.getContent()` 生成按行编号的输出。
	- 示例：`show` 输出全文；`show 1:2` 输出第1到第2行内容（带行号）。
- `undo` / `redo`
	- 映射：`UndoCommand` / `RedoCommand` -> 对应 Handler -> `activeEditor.undo()` / `activeEditor.redo()` -> `OperationHistory` 的栈操作。
	- 结果：`undo` 将上一次可撤销的 `EditOperation` 回滚并将其移动到 redo 栈；`redo` 从 redo 栈重做并移回 undo 栈。

---

## 模块三：日志模块（Logging）

1. 模块总体作用
- 订阅命令生命周期事件（`CommandReceivedEvent`、`CommandExecutingEvent`、`CommandCompletedEvent`、`CommandFailedEvent`），并将与文件相关的命令活动以时间戳写入项目 `log` 目录下的单文件日志（文件名以 `.<原文件名>.log` 存放），支持按文件启用/禁用与查看日志内容。

2. 关联核心类、函数与设计模式
- 核心类：
	- `org.ztglab.logging.LoggingService`：实现 `IEventListener<Event>`，注册为 `EventBus` 的监听者以接收命令相关事件。
	- `org.ztglab.command.CommandBus` / `org.ztglab.event.EventBus`：事件发布者和命令生命周期管理。
- 主要函数（`LoggingService`）：
	- `enable(String filePath)` / `disable(String filePath)`：在 `enabledMap` 中切换日志开关并在首次启用时写入会话开始行。
	- `readLog(String filePath)`：读取对应日志文件并返回内容。
	- `appendLine(String filePath,String line)`：执行 NIO 追加写入（会创建 `log` 目录），用于将事件行写入日志文件。
	- `onEvent(Event event)`：接收事件并根据 `activeFilePath` 与 `enabledMap` 决定是否记录，分派到 `handleCommandReceived/Executing/Completed/Failed`。
- 设计模式：
	- 观察者模式（Observer）：`EventBus` 在命令生命周期发布事件，`LoggingService` 实现 `IEventListener` 并订阅这些事件来写入日志，从而实现记录逻辑与命令执行解耦。
	- 服务定位模式（Service Locator）：`ApplicationContext` 提供 `LoggingService`、`CommandBus`、`EventBus` 等共享实例，日志模块通过该通道取得运行时依赖。
	- 策略模式（Strategy）：`LoggingService` 根据事件类型（Received/Executing/Completed/Failed）选择不同处理函数（`handleCommandReceived`、`handleCommandCompleted` 等），表现出按事件类型分派责任的策略式组织。
	- 适配器模式（Adapter）：在写入日志时对命令信息做格式化包装（`AbstractCommand.getOriginalName()` / `getOriginalArgs()`）写成可读行，类似在原始事件上附加描述性信息再写入持久化层。

1. 与日志相关的命令（映射、示例与结果）
- `log-on [file]`
	- 映射：`LogOnCommand` -> `LogOnHandler.handle` -> `EditorManager.enableLogging(targetFile)` -> `LoggingService.enable(targetFile)`。
	- 示例：`log-on`（无参数）对当前活动文件启用日志；`log-on notes.txt` 对指定文件启用日志。
	- 结果：在 `log/.<filename>.log` 中追加会话开始与后续命令记录行（如 `RECEIVED | name args`、`COMPLETED | ...` 等）。
- `log-off [file]`
	- 映射：`LogOffCommand` -> `LogOffHandler.handle` -> `EditorManager.disableLogging` -> `LoggingService.disable`。
	- 示例：`log-off` 关闭当前活动文件的日志记录。
	- 结果：后续针对该文件的命令不再被写入日志。
- `log-show [file]`
	- 映射：`LogShowCommand` -> `LogShowHandler.handle` -> `EditorManager.showLog(filepath)` -> `LoggingService.readLog(targetFile)`。
	- 示例：`log-show notes.txt` -> 把 `log/.notes.txt.log` 内容读出并打印到控制台。

---

## 附录

### 命令速查表

#### 工作区命令
| 命令 | 功能 | 必需参数 | 可选参数 |
|------|------|---------|---------|
| `load <file>` | 加载文件 | 文件路径 | - |
| `save [file\|all]` | 保存文件 | - | file/all |
| `init <file> [with-log]` | 创建新缓冲区 | 文件 | with-log |
| `close [file]` | 关闭文件 | - | file |
| `edit <file>` | 切换活动文件 | 文件 | - |
| `editor-list` | 显示文件列表 | - | - |
| `dir-tree [path]` | 显示目录树 | - | path |
| `undo` | 撤销 | - | - |
| `redo` | 重做 | - | - |
| `exit` | 退出程序 | - | - |

#### 文本编辑命令

| 命令                              | 功能     | 适用文件 |
| --------------------------------- | -------- | -------- |
| `append "text"`                   | 追加文本 | .txt     |
| `insert <line:col> "text"`        | 插入文本 | .txt     |
| `delete <line:col> <len>`         | 删除字符 | .txt     |
| `replace <line:col> <len> "text"` | 替换文本 | .txt     |
| `show [start:end]`                | 显示内容 | .txt     |

#### 日志命令

| 命令              | 功能     |
| ----------------- | -------- |
| `log-on [file]`   | 启用日志 |
| `log-off [file]`  | 关闭日志 |
| `log-show [file]` | 显示日志 |

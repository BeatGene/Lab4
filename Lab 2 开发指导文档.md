# Lab 2 开发指导文档

本文档旨在指导开发者基于现有的“三层命令体系”架构，实现 Lab 2 的功能需求（XML 编辑器、统计模块、拼写检查、日志增强）。

## 1. 任务概览

Lab 2 的核心挑战在于引入一种全新的编辑器类型 (`XmlEditor`)，并保证其与现有的 `TextEditor` 共存且互不干扰。得益于之前的架构重构，我们只需要关注领域层的实现。

## 2. 详细开发步骤

### 步骤一：实现 XML 领域模型 (Composite Pattern)

**设计理念澄清**：
在我们的架构中，`Document` 类始终是核心的**充血模型**，它代表了文件在内存中的物理状态（即“行”的集合，`List<String>`），不区分具体文件类型。
XML 领域模型（DOM 树）仅仅是 `XmlEditor` 为了方便处理结构化数据而引入的**逻辑视图**或**辅助工具**。
*   **物理视图**：`Document` (维护 `lines`) —— 负责持久化、基础行操作。
*   **逻辑视图**：`XmlNode` / `XmlTree` —— 负责理解 XML 结构、定位元素。
*   **同步机制**：`XmlEditor` 负责在每次操作后，将 DOM 树的状态序列化回 `Document`，或者将 `Document` 的变更解析为 DOM 树。所有的 XML 操作最终都必须落地为对 `Document` 内容的修改。

**核心洞察**：
在实际应用中，XML 文件本质上也是文本文件。这意味着同一个文件既可以用 `TextEditor` 打开（把它当作纯文本进行行级编辑），也可以用 `XmlEditor` 打开（把它当作树结构进行元素级编辑）。
项目中的 `TextEditor` 和 `XmlEditor` 实际上是对同一份数据（`Document`）的两种不同的**操作策略** (Strategy)。

基于此，你可能需要使用 **组合模式** 来构建这个逻辑视图。具体的步骤如下：

1.  **创建包结构**
2.  **定义节点类**
3.  **实现核心方法**

具体实现细节，请你自行完成，参考 Lab 2 需求文档中的命令说明。
### 步骤二：实现 XmlEditor

这是 Lab 2 的重头戏。你需要创建一个新的编辑器实现。

1.  **创建类**：
    在 `src/main/java/org/ztglab/xml` 下创建 `XmlEditor`，实现 `IEditor` 接口。

2.  **实现 `resolveCommand`**：
    这是连接架构的关键。参考 `TextEditor` 的实现：
    ```java
    @Override
    public EditorCommand resolveCommand(String name) {
        return commandMap.get(name);
    }
    ```

3.  **数据同步策略 (关键)**：
    在 `XmlEditor` 中，你需要维护 `Document` (物理) 和 `XmlRoot` (逻辑) 的一致性。
    *   **初始化/加载**：在构造函数或 `attach(Document doc)` 时，读取 `doc` 的所有行，解析生成 `XmlRoot` 树。
    *   **执行命令**：
        1.  在 DOM 树上执行逻辑操作（如 `insertBefore`）。
        2.  **回写 (Sync Back)**：操作完成后，立即调用 `xmlRoot.toXmlString()` 生成新的文本行，并更新 `Document` 的内容（例如 `doc.clear(); doc.addAll(newLines);`）。
        *   *原因说明*：`WorkspaceManager` 在执行 `save` 或切换文件时，会直接读取 `Document`。如果只在保存时回写，需要修改 `WorkspaceManager` 增加通知机制，否则会保存过期数据。为了保持架构简单，推荐**实时回写**。

4.  **注册 XML 命令**：
    在 `XmlEditor` 的构造函数中，注册 Lab 2 要求的命令：
    *   `insert-before`: 解析参数 -> 调用 `xmlDocument.insertBefore(...)`
    *   `append-child`: 解析参数 -> 调用 `xmlDocument.appendChild(...)`
    *   `edit-id`, `edit-text`, `delete`, `xml-tree` 等。

    **注意**：你需要为每个操作编写对应的 `EditOperation` 实现（如 `XmlInsertOperation`），以支持 Undo/Redo。

5.  **集成到 WorkspaceManager**：
    修改 `WorkspaceManager.java` 中的 `load` 或 `init` 逻辑。
    *   如果文件后缀是 `.xml`，实例化 `XmlEditor`。
    *   如果文件后缀是 `.txt`，实例化 `TextEditor`。

### 步骤三：实现统计模块 (Statistics) - 基于事件驱动

为了实现统计功能且不修改现有的编辑器代码，我们采用 **事件驱动** 的方式。

1.  **定义事件**：
    在 `src/main/java/org/ztglab/event/events/` 下创建 `ActiveDocumentChangedEvent`。

2.  **发布事件**：
    修改 `WorkspaceManager.java` 的 `setActiveDocument` 方法，在切换活动文档时发布 `ActiveDocumentChangedEvent`。

3.  **创建统计服务**：
    创建 `org.ztglab.statistics.StatisticsService`，实现 `IEventListener`。
    *   维护 `Map<String, Long> fileDurations` (文件路径 -> 累计毫秒数)。
    *   维护 `long lastSwitchTime` (上一次切换的时间戳)。
    *   在 `onEvent` 中处理 `ActiveDocumentChangedEvent`：
        *   如果 `oldDocument` 存在，计算 `now - lastSwitchTime` 并累加到 `fileDurations`。
        *   更新 `lastSwitchTime = now`。
    *   提供 `getDuration(String filePath)` 方法供外部查询。
    *   **注意**：还需要处理程序退出时的最后一次时长统计（可以监听 `CommandReceivedEvent` 捕获 `exit` 命令，或者在 `WorkspaceManager.exit` 中手动触发一次事件）。

4.  **集成到 `editor-list`**：
    修改 `ListEditorsCommand`，调用 `StatisticsService.getInstance().getDuration(path)` 来获取并显示时长。

### 步骤四：实现拼写检查 (Adapter Pattern)

拼写检查应作为一项独立服务。

1.  **定义接口**：
    创建 `org.ztglab.spellcheck.ISpellChecker` 接口，定义 `List<String> check(String text)`。

2.  **实现适配器**：
    创建一个适配器类（如 `LanguageToolAdapter`），封装第三方库的调用。

3.  **实现 `spell-check` 命令**：
    *   这是一个编辑器通用命令，需要在 `TextEditor` 和 `XmlEditor` 中分别处理。
    *   对于 `TextEditor`，检查整个 Buffer。
    *   对于 `XmlEditor`，遍历 DOM 树，检查所有 `XmlText` 节点的内容。

### 步骤五：日志增强

1.  **修改 `LoggingService`**：
    你需要修改日志服务以支持过滤。

2.  **解析文件头**：
    在 `CommandExecutingEvent` 触发时：
    *   获取当前命令的目标文件（编辑器）。
    *   读取文件的第一行（`editor.getContent().split("\n")[0]`）。
    *   解析 `# log -e cmd` 语法。

3.  **过滤逻辑**：
    如果当前命令在排除列表中，则不记录日志。

## 3. 现有代码参考指引

*   **如何解析命令参数？**
    请参考 `TextEditor.java`
*   **如何实现 Undo/Redo？**
    请参考 `org.ztglab.editor.operations` 包下的 `InsertOperation.java`。你的 XML 操作也需要保存“逆操作”所需的数据（例如删除一个节点时，要保存它原来的位置和内容）。
*   **如何添加新命令？**
    1. 定义命令逻辑 (在 Editor 中)。
    2. 注册到 `commandMap`。
    3. 如果是全局命令（如 `init`），则在 `org.ztglab.workspace.commands` 中添加，并在 `Main` 或 `ApplicationContext` 中注册。

## 4. 常见问题 (FAQ)

*   **Q: `CommandExecutor` 还需要修改吗？**

    A: 不需要。得益于三层架构，`CommandExecutor` 现在只负责转发字符串。所有的参数解析都在你的 `XmlEditor` 内部完成。

*   **Q: XML 解析器需要自己写吗？**

    A: 题目要求“解析XML文件为树形结构”，建议使用简单的字符串解析或现有的轻量级 XML 解析库。如果为了练习，可以手写一个简单的递归下降解析器。

祝开发顺利！

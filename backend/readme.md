# 智慧图书管理系统 - AI 助手模块说明文档

本模块基于 **Spring Boot** 和 **LangChain4j** 框架构建，为智慧图书管理系统提供强大的 AI 问答、文档知识库检索（RAG）、以及自然语言转 SQL 数据分析（Text-to-SQL & 自动图表生成）功能。

## 1. 模块架构与核心组件

AI 助手模块主要由以下几个核心组件构成：

- **控制器层 (Controller)**：处理 HTTP 请求，管理会话状态及聊天记录。
- **服务层 (Service)**：封装 AI 核心业务逻辑，包括文档的解析、向量化存储以及对话前置提示词注入。
- **配置层 (Config)**：配置 LangChain4j 的上下文记忆（Memory）与内容检索器（Retriever）。
- **AI 代理 (Agent)**：通过声明式接口定义的大语言模型交互入口。
- **工具层 (Tools)**：赋予大模型外部执行能力的函数（如数据库查询工具）。

---

## 2. 详细代码实现与逻辑

### 2.1 AI 配置层 (`AiConfig.java`)
负责配置 AI 的对话记忆与向量数据库检索策略。

- **对话记忆 (ChatMemoryProvider)**: 使用 `MessageWindowChatMemory` 维护上下文，最大保留 20 条消息。借助 Redis (`redisChatMemoryStore`) 实现分布式记忆持久化。
- **RAG 内容检索 (ContentRetriever)**: 基于 `RedisEmbeddingStore` 和 `EmbeddingModel` 构建向量库检索器。设置最低相似度阈值为 `0.5`，每次最多返回 `3` 条相关性最高的文档片段，用于补充 AI 回答的背景知识。

### 2.2 控制器层 (`AiController.java`)
暴露 RESTful API 接口，负责与前端进行交互：

- **文档上传 (`/upload`)**: 接收用户上传的文件，调用服务层进行 OSS 上传与向量化处理。
- **会话管理 (`/session/**`)**: 提供新建、获取列表、重命名和删除会话功能，基于 `UserContext` 保证数据权限隔离。
- **聊天交互 (`/chat/{sessionId}`)**:
  1. 接收用户消息并保存至数据库（角色为 `user`）。
  2. 调用 AI 服务获取响应。
  3. 将 AI 响应的 JSON 数据保存至数据库（角色为 `ai`），限制长度不超过 65535 字符防止数据库溢出。
  4. 返回包含图表或文本的混合响应给前端。

### 2.3 核心服务层 (`AiServiceImpl.java`)
处理具体的业务编排逻辑：

- **知识库录入 (`uploadAndProcessDoc`)**:
  - 先将文件上传至 OSS 获取访问链接。
  - 使用 `ApachePdfBoxDocumentParser`（针对 PDF）或 `ApachePoiDocumentParser`（针对 Word/Excel 等）提取文档纯文本。
  - 在文档元数据中注入文件名 `metadata().put("file_name", filename)`，以供 RAG 引用来源。
  - 使用递归字符分割器 `DocumentSplitters.recursive(500, 50)` 将长文档切片（最大 500 字符，重叠 50 字符）。
  - 调用 `EmbeddingStoreIngestor` 完成向量化并写入 Redis 向量库。

- **对话处理 (`chat`)**:
  - **意图预判与 Prompt 注入**: 根据用户输入关键词（“折线图”、“柱状图”、“饼图”）动态拼接提示词，要求模型输出特定的 JSON 格式（如 `{"type":"chart","chartType":"line","data":[...]}`），并提示其将计数字段重命名为 `value` 以适配前端 ECharts 等图表库的渲染。
  - **响应清洗**: 移除大模型返回数据中可能包含的 Markdown 代码块标记（如 ```json），解析为 `Map<String, Object>` 供前端直接使用。

### 2.4 AI 代理接口 (`SmartAssistant.java`)
基于 LangChain4j 的 `@AiService` 注解实现的声明式 AI 接口：

- **组件绑定**: 显式绑定了语言模型 (`openAiChatModel`)、记忆提供者 (`chatMemoryProvider`)、知识库检索器 (`contentRetriever`) 以及数据分析工具 (`dataAnalysisTool`)。
- **系统提示词 (System Prompt)**: 
  通过 `@SystemMessage` 赋予 AI 人设，并严格规范了它的输出 JSON 格式规范：
  - **图表输出 (`type: chart`)**: 当调用工具获取统计数据时使用。
  - **知识库问答 (`type: doc`)**: 当基于 RAG 检索的文档片段回答时使用，需附带 `source_nodes`。
  - **常规问答 (`type: text`)**: 默认的基础文本回答。

### 2.5 数据分析工具 (`DataAnalysisTool.java`)
通过 LangChain4j 的 `@Tool` 机制，赋予大语言模型查询数据库的能力（Text-to-SQL）。

- 提示词中详细告知了数据库的表结构（`user`, `book`, `borrow_record`, `reserve_record` 等字段）。
- 当用户询问统计数据（如“借书数量前十的用户”、“热门借阅榜”）时，大模型会自主生成 MySQL 语句。
- 拦截并执行生成的 SQL（通过 `JdbcTemplate`），并将结果集序列化为 JSON 返回给大模型，由大模型进一步整合输出。

---

## 3. 核心业务数据流转 (Workflow)

### 3.1 RAG 知识库问答流程
1. **录入**: 用户上传文档 -> 解析文本 -> 文本切片 -> 文本向量化 -> 存入 Redis 向量库。
2. **检索**: 用户提问 -> 提问向量化 -> 在 Redis 中进行相似度检索 (Top 3, 阈值 0.5) -> 将检索出的文档片段连同用户提问一起发送给大模型。
3. **回答**: 大模型根据片段生成回答，并以 `{"type":"doc", "source_nodes":["来源"]}` 的 JSON 格式返回。

### 3.2 智能数据分析与图表生成流程
1. **意图识别**: 用户发送消息，如 "给我生成一个最近借阅量最高的柱状图"。
2. **提示词增强**: 服务层拦截消息，追加隐藏 Prompt（指示图表类型及 JSON 格式）。
3. **工具调用**: 大模型识别到需要查询统计数据，构造 SQL 语句调用 `DataAnalysisTool`。
4. **数据执行**: `DataAnalysisTool` 在本地数据库执行 SQL，返回数据集合。
5. **格式化输出**: 大模型结合查询结果，输出符合前端图表渲染规范的 JSON 数据。服务层清洗 JSON 标记后返回给前端。

---

## 4. 总结
该模块通过高度解耦的架构，巧妙结合了 **LangChain4j** 的 RAG 与 Tool 能力。在确保会话安全、上下文连贯的基础上，不仅能解答知识库内容，还能作为智能 BI 工具自动分析数据库并生成可视化图表。

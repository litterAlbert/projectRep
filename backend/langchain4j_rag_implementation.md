# 基于 LangChain4j 的 AI + RAG 流程实现指南

本项目展示了如何使用 LangChain4j 结合 Spring Boot 实现完整的 AI + RAG (检索增强生成) 流程。核心流程涵盖了文档加载、切分、向量化存储、多轮对话记忆、以及流式输出的整个生命周期。

通过这份文档，其他大模型只需阅读此文件即可复现整个 AI 助手的 RAG 架构。

## 1. 核心依赖配置 (pom.xml)
首先，需要引入 LangChain4j 及其相关的 Spring Boot Starter，包括 OpenAI 兼容模型、Redis 向量与记忆存储、文档解析器和响应式编程支持。

```xml
<!-- 2026-03-28 引入相关依赖 -->
<dependencies>
    <!-- Web与响应式编程 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- LangChain4j 核心与 Spring Boot 集成 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>1.0.1-beta6</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
        <version>1.0.1-beta6</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-reactor</artifactId> <!-- 流式输出支持 -->
        <version>1.0.1-beta6</version>
    </dependency>
    
    <!-- RAG 与文档解析 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-easy-rag</artifactId>
        <version>1.0.1-beta6</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-pdfbox</artifactId> <!-- 解析PDF -->
        <version>1.0.1-beta6</version>
    </dependency>

    <!-- Redis 支持 (用于记忆和向量存储) -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-community-redis-spring-boot-starter</artifactId>
        <version>1.0.1-beta6</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

## 2. 核心配置信息 (application.yml)
使用阿里云 DashScope 提供的 OpenAI 兼容接口，配置大语言模型、流式模型和向量嵌入模型。同时配置 Redis 用于存储记忆和向量。

```yaml
# 2026-03-28 配置模型与 Redis
langchain4j:
  open-ai:
    chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${API-KEY}
      model-name: qwen-plus
    streaming-chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${API-KEY}
      model-name: qwen-plus
    embedding-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${API-KEY}
      model-name: text-embedding-v4
      max-segments-per-batch: 10
  community:
    redis:
      host: localhost
      port: 6379

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 3. RAG 核心组件与记忆配置 (CommonConfig.java)
这一部分是 RAG 的核心。需要完成：
1. **文档加载与切分**：将本地 PDF 知识库加载到内存并切割成段落。
2. **向量化与存储**：使用 `EmbeddingModel` 将段落转化为向量，并存入 Redis。
3. **检索器构建**：构建 `ContentRetriever`，在对话时用于根据用户问题检索相关向量。
4. **多轮对话记忆**：基于 Redis 维护上下文记忆。

```java
package org.example.consultant.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommonConfig {

    @Autowired
    private ChatMemoryStore redisChatMemoryStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private RedisEmbeddingStore redisEmbeddingStore;

    /**
     * 2026-03-28 构建会话记忆提供者，基于 Redis 存储对话上下文，每个会话最多保留20条消息
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    /**
     * 2026-03-28 构建并初始化向量数据库存储操作，完成文档加载、切分、向量化及存储流程
     * 注意：如果数据已经持久化在 Redis 中，此方法在生产环境中不需要每次启动都执行。
     */
    public void initEmbeddingStore() {
        // 1. 加载文档（读取 resources/content 目录下的 PDF 文件）
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content", new ApachePdfBoxDocumentParser());
        
        // 2. 构建文档分割器，每个片段最大500字符，重叠100字符，保证语义连贯
        DocumentSplitter ds = DocumentSplitters.recursive(500, 100);

        // 3. 构建 Ingestor 对象，将切割后的文本片段进行向量化并存入 Redis
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(redisEmbeddingStore)
                .documentSplitter(ds)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(documents);
    }

    /**
     * 2026-03-28 构建向量数据库内容检索器，在 RAG 流程中负责查询匹配度最高的知识库片段
     */
    @Bean
    public ContentRetriever contentRetriever(){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .minScore(0.5) // 设置最低相似度阈值
                .maxResults(3) // 每次最多返回3条相关文档片段
                .build();
    }
}
```

## 4. 声明式 AI 服务接口 (ConsultantService.java)
使用 LangChain4j 的 `@AiService` 注解，声明一个高度抽象的 AI 助手接口。该接口会自动组装底层模型、检索器、记忆组件和工具。

```java
package org.example.consultant.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * 2026-03-28 声明式的 AI 服务接口，集成大模型、流式输出、会话记忆与 RAG 检索器
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT, 
        chatModel = "openAiChatModel", 
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider", // 注入 Redis 会话记忆
        contentRetriever = "contentRetriever", // 注入 RAG 检索器
        tools = "reservationTool" // （可选）注入外部工具能力
)
public interface ConsultantService {

    /**
     * 2026-03-28 执行聊天交互，返回流式响应（Flux<String>），结合记忆 ID 实现多轮对话
     */
    @SystemMessage(fromResource = "system.txt") // 加载 resources 下的系统提示词
    public Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
```

## 5. 控制器入口 (ChatController.java)
通过 Web 接口对外提供服务，调用声明好的 `ConsultantService`，实现流式数据返回。

```java
package org.example.consultant.controller;

import org.example.consultant.aiservice.ConsultantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService;

    /**
     * 2026-03-28 接收用户的对话请求，调用 AI 服务并以流式（Flux）的方式返回生成的文本
     */
    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message){
        // 直接调用 AI 服务，LangChain4j 会自动在后台执行：
        // 1. 获取会话记忆
        // 2. 将 message 转为向量并检索相关文档 (RAG)
        // 3. 拼接系统提示词、检索到的上下文、历史记忆和当前问题
        // 4. 发送给大模型进行流式推理
        return consultantService.chat(memoryId, message);
    }
}
```

## 总结 RAG 流程逻辑
1. **数据准备阶段（Ingestion）**：启动时或手动触发，将 PDF 等文档利用 `DocumentSplitter` 切片，然后通过 `EmbeddingModel` 转化为向量，最后存入 `EmbeddingStore`（这里是 Redis）。
2. **检索阶段（Retrieval）**：当用户在 Controller 传入问题时，LangChain4j 拦截调用，并使用 `ContentRetriever` 将用户问题转化为向量，在 `EmbeddingStore` 中寻找最相似的知识切片。
3. **生成阶段（Generation）**：LangChain4j 将**检索到的知识切片** + **系统提示词 (SystemMessage)** + **历史聊天记录 (ChatMemory)** + **用户当前问题** 一起打包，发送给 LLM（如 qwen-plus），最终通过 `Flux` 响应式流返回生成的回答。

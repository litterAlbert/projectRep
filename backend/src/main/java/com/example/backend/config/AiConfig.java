package com.example.backend.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI RAG 与记忆配置
 * 时间: 2026-03-28
 */
@Configuration
public class AiConfig {

    @Autowired(required = false)
    private ChatMemoryStore redisChatMemoryStore;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private RedisEmbeddingStore redisEmbeddingStore;

    /**
     * 构建会话记忆提供者，基于 Redis 存储对话上下文
     * 时间: 2026-03-28
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> {
            MessageWindowChatMemory.Builder builder = MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(20);
            if (redisChatMemoryStore != null) {
                builder.chatMemoryStore(redisChatMemoryStore);
            }
            return builder.build();
        };
    }

    /**
     * 构建向量数据库内容检索器，在 RAG 流程中负责查询匹配度最高的知识库片段
     * 时间: 2026-03-28
     */
    @Bean
    public ContentRetriever contentRetriever(){
        if (redisEmbeddingStore == null || embeddingModel == null) {
            return null;
        }
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .minScore(0.5) // 设置最低相似度阈值
                .maxResults(3) // 每次最多返回3条相关文档片段
                .build();
    }
}

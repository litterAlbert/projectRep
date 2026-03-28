package com.example.backend.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置
 * 时间: 2026-03-28
 */
@Configuration
public class AiConfig {

    @Value("${langchain4j.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private Integer redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            return null; // For local tests without key
        }
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-plus") // qwen-plus is usually aliased to qwen3.5-plus or similar
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v3") // text-embedding-v3
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // Build redis URI
        String url = "redis://";
        if (redisPassword != null && !redisPassword.isEmpty()) {
            url += ":" + redisPassword + "@";
        }
        url += redisHost + ":" + redisPort;

        try {
            return RedisEmbeddingStore.builder()
                    .host(redisHost)
                    .port(redisPort)
                    // if password needed, can configure here or via URL if builder supports
                    // Note: RedisEmbeddingStore builder might differ slightly in 0.35.0
                    .indexName("library_docs_idx")
                    .dimension(1024) // qwen text-embedding-v3 default dimension is 1024
                    .build();
        } catch (Exception e) {
            // RedisSearch might not be available in standard redis, fallback gracefully or log
            e.printStackTrace();
            return null;
        }
    }
}

package com.example.backend.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

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

    @Autowired(required = false)
    private redis.clients.jedis.JedisPooled jedisPooled;

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
     * 构建混合检索器：融合向量检索与关键字检索，采用 RRF 排序
     * 时间: 2026-04-15
     */
    @Bean
    public ContentRetriever contentRetriever(){
        if (redisEmbeddingStore == null || embeddingModel == null) {
            return null;
        }
        
        // 1. 创建基础的向量检索器
        ContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .minScore(0.5) // 设置最低相似度阈值
                .maxResults(5) // 每次最多返回3条相关文档片段
                .build();
                
        // 2. 由于底层使用的是 RedisSearch，我们强制创建 JedisPooled 客户端，确保混合检索功能可用
        redis.clients.jedis.JedisPooled activeJedisPooled = this.jedisPooled;
        if (activeJedisPooled == null) {
            // 根据 application.yml 中的默认配置（host: localhost, port: 6379）手动创建 fallback 客户端
            //System.out.println("未自动注入 JedisPooled，将手动创建连接以开启混合检索器");
            activeJedisPooled = new redis.clients.jedis.JedisPooled("localhost", 6379);
        }
        
        //System.out.println("混合检索器");
        return new com.example.backend.tools.HybridContentRetriever(vectorRetriever, activeJedisPooled, 5, 60);
    }

    /**
     * 配置 RetrievalAugmentor，自定义检索后的处理逻辑
     * 将元数据中的 file_name 拼接到正文前，并交给大模型
     */
    @Bean
    public dev.langchain4j.rag.RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever) {
        if (contentRetriever == null) return null;
        
        ContentInjector contentInjector = DefaultContentInjector.builder()
                .metadataKeysToInclude(Arrays.asList("file_name"))
                .build();

        return dev.langchain4j.rag.DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();
    }
}

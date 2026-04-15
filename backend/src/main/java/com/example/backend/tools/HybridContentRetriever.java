package com.example.backend.tools;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 混合检索器：融合向量检索与关键字检索（BM25），采用 RRF (Reciprocal Rank Fusion) 排序
 * 时间: 2026-04-15
 */
public class HybridContentRetriever implements ContentRetriever {

    private final ContentRetriever vectorRetriever;
    private final JedisPooled jedisPooled;
    private final int maxResults;
    private final int rrfK;

    /**
     * 构造函数
     * 时间: 2026-04-15
     * @param vectorRetriever 底层的向量检索器
     * @param jedisPooled Redis 连接池，用于关键字检索
     * @param maxResults 最终返回的最大结果数
     * @param rrfK RRF平滑常数（通常取 60）
     */
    public HybridContentRetriever(ContentRetriever vectorRetriever, JedisPooled jedisPooled, int maxResults, int rrfK) {
        this.vectorRetriever = vectorRetriever;
        this.jedisPooled = jedisPooled;
        this.maxResults = maxResults;
        this.rrfK = rrfK;
    }

    /**
     * 执行混合检索
     * 时间: 2026-04-15
     */
    @Override
    public List<Content> retrieve(Query query) {
        // 1. 向量检索
        List<Content> vectorResults = vectorRetriever.retrieve(query);

        // 2. 关键字检索 (BM25)
        List<Content> keywordResults = performKeywordSearch(query.text());

        // 3. RRF (Reciprocal Rank Fusion) 融合排序
        Map<String, Content> contentMap = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 处理向量检索排名
        int rank = 1;
        for (Content c : vectorResults) {
            String text = c.textSegment().text();
            contentMap.putIfAbsent(text, c);
            double currentScore = rrfScores.getOrDefault(text, 0.0);
            double boost = 1.0;
            // 时间: 2026-04-15 新增管理员知识库优先级提升
            if ("admin".equals(c.textSegment().metadata().getString("uploader_role"))) {
                boost = 100.0; // 管理员知识库优先级更高
            }
            rrfScores.put(text, currentScore + (1.0 / (rrfK + rank)) * boost);
            rank++;
        }

        // 处理关键字检索排名
        rank = 1;
        for (Content c : keywordResults) {
            String text = c.textSegment().text();
            contentMap.putIfAbsent(text, c);
            double currentScore = rrfScores.getOrDefault(text, 0.0);
            double boost = 1.0;
            if ("admin".equals(c.textSegment().metadata().getString("uploader_role"))) {
                boost = 100.0; // 管理员知识库优先级更高
            }
            rrfScores.put(text, currentScore + (1.0 / (rrfK + rank)) * boost);
            rank++;
        }

        // 根据RRF分数降序排序并截取Top K
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> contentMap.get(e.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * 执行 Redisearch 的 FT.SEARCH 进行关键字检索
     * 时间: 2026-04-15
     */
    private List<Content> performKeywordSearch(String searchText) {
        List<Content> results = new ArrayList<>();
        if (jedisPooled == null) {
            System.err.println("JedisPooled is null, skipping keyword search.");
            return results;
        }
        
        try {
            // 清理搜索词，仅保留字母、数字和中文字符，避免 Redisearch 语法错误
            String safeQuery = searchText.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", " ").trim();
            if (safeQuery.isEmpty()) {
                safeQuery = searchText; // fallback
            }
            
            redis.clients.jedis.search.Query q = new redis.clients.jedis.search.Query(safeQuery)
                    .limit(0, 15)
                    .returnFields("text", "file_name", "uploader_role");
                    
            SearchResult searchResult = jedisPooled.ftSearch("embedding-index", q);
            
            for (Document doc : searchResult.getDocuments()) {
                String text = (String) doc.get("text");
                String fileName = (String) doc.get("file_name");
                String uploaderRole = (String) doc.get("uploader_role");
                
                if (text != null && !text.isEmpty()) {
                    Metadata metadata = new Metadata();
                    if (fileName != null) {
                        metadata.put("file_name", fileName);
                    }
                    if (uploaderRole != null) {
                        metadata.put("uploader_role", uploaderRole);
                    }
                    results.add(Content.from(TextSegment.from(text, metadata)));
                }
            }
        } catch (Exception e) {
            System.err.println("Keyword search failed: " + e.getMessage());
        }
        return results;
    }
}

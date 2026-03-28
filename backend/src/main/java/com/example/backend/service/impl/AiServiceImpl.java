package com.example.backend.service.impl;

import com.example.backend.service.AiService;
import com.example.backend.tools.OssTool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI 服务实现类
 * 时间: 2026-03-28
 */
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private OssTool ossTool;

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String uploadAndProcessDoc(MultipartFile file) throws Exception {
        // 1. 上传OSS
        String url = ossTool.upload(file);

        if (embeddingModel == null || embeddingStore == null) {
            return url; // 无AI配置直接返回
        }

        // 2. 提取文本
        Document document;
        try (InputStream is = file.getInputStream()) {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                document = new ApachePdfBoxDocumentParser().parse(is);
            } else {
                document = new ApachePoiDocumentParser().parse(is);
            }
        }

        // 3. 向量化并存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(document);

        return url;
    }

    @Override
    public String chatWithDoc(String sessionId, String message) {
        if (chatLanguageModel == null) return "AI 未配置";

        ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
                .chatLanguageModel(chatLanguageModel)
                .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, 5, 0.7))
                .build();

        return chain.execute(message);
    }

    @Override
    public Map<String, Object> dataAnalysis(String query) {
        if (chatLanguageModel == null) {
            Map<String, Object> map = new HashMap<>();
            map.put("error", "AI 未配置");
            return map;
        }

        // 1. 询问AI生成SQL
        String prompt = "你是一个图书管理系统的数据库助手。数据库包含以下表：\n" +
                "user(id, username, role)\n" +
                "book(id, title, author, stock, borrowed_count, reserved_count)\n" +
                "borrow_record(id, user_id, book_id, borrow_date, return_date, status)\n" +
                "reserve_record(id, user_id, book_id, reserve_date, status)\n" +
                "请根据用户的问题，生成可以执行的MySQL查询语句。只返回SQL语句，不要任何解释，也不要Markdown格式的包裹：\n" +
                "用户问题：" + query;
        
        String sql = chatLanguageModel.generate(prompt).replace("```sql", "").replace("```", "").trim();

        // 2. 执行SQL
        List<Map<String, Object>> data;
        try {
            data = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "SQL执行失败: " + e.getMessage());
            err.put("sql", sql);
            return err;
        }

        // 3. 询问AI生成前端需要的图表类型和数据结构
        try {
            String dataJson = new ObjectMapper().writeValueAsString(data);
            String chartPrompt = "根据以下查询结果数据，判断适合展示哪种图表（柱状图 bar / 饼图 pie / 折线图 line）。\n" +
                    "数据：" + dataJson + "\n" +
                    "请返回一个JSON对象，包含 chartType (字符串，值为 bar, pie 或 line) 和 data (数组，直接是传入的数据)。只返回JSON，不要其他文字。";
            
            String jsonRes = chatLanguageModel.generate(chartPrompt).replace("```json", "").replace("```", "").trim();
            return new ObjectMapper().readValue(jsonRes, Map.class);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("chartType", "table");
            res.put("data", data);
            return res;
        }
    }
}

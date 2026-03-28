package com.example.backend.service.impl;

import com.example.backend.service.AiService;
import com.example.backend.service.SmartAssistant;
import com.example.backend.tools.OssTool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 服务实现类
 * 时间: 2026-03-28
 */
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private OssTool ossTool;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    @SuppressWarnings("rawtypes")
    private EmbeddingStore embeddingStore;

    @Autowired(required = false)
    private SmartAssistant smartAssistant;

    /**
     * 上传并处理文档（知识库录入）
     * 时间: 2026-03-28
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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

    /**
     * AI 对话处理，支持图表类型识别
     * 时间: 2026-03-28
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Map<String, Object> chat(String sessionId, String message) {
        if (smartAssistant == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("type", "text");
            res.put("content", "AI 未配置或初始化失败");
            return res;
        }

        // 预处理：判断用户是否要求生成特定图表
        String prompt = message;
        String chartType = null;
        if (message.contains("折线图")) {
            chartType = "line";
        } else if (message.contains("柱状图")) {
            chartType = "bar";
        } else if (message.contains("饼图")) {
            chartType = "pie";
        }

        // 如果识别到图表需求，给 AI 补充明确的 JSON 格式指示
        if (chartType != null) {
            prompt += "\n（系统提示：用户要求生成" + chartType + "图表，请务必调用查询工具获取数据，并严格以JSON格式返回：{\"type\":\"chart\",\"chartType\":\"" + chartType + "\",\"data\":[...]}，不要返回任何多余的解释文本或 Markdown 标记）";
        } else {
            prompt += "\n（系统提示：如果是普通问答，请严格以JSON格式返回：{\"type\":\"text\",\"content\":\"你的回答\"}，不要返回多余文本或 Markdown 标记）";
        }

        try {
            // 调用声明式 AI 服务
            String response = smartAssistant.chat(sessionId, prompt);
            
            // 清理可能的 markdown 代码块标记
            response = response.replaceAll("```json", "").replaceAll("```", "").trim();
            
            ObjectMapper mapper = new ObjectMapper();
            return (Map<String, Object>) mapper.readValue(response, Map.class);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("type", "text");
            res.put("content", "处理失败: " + e.getMessage());
            return res;
        }
    }
}

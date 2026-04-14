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
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
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
            // 确保将文件名加入到文档的元数据中，以便大模型在检索时能够获取到来源文档名
            if (filename != null) {
                document.metadata().put("file_name", filename);
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
     * 删除知识库文档
     * 时间: 2026-04-14
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void deleteDoc(String fileUrl, String fileName) {
        // 从 OSS 删除
        if (fileUrl != null && !fileUrl.isEmpty()) {
            ossTool.delete(fileUrl);
        }

        // 从向量数据库中删除
        if (embeddingStore != null && fileName != null && !fileName.isEmpty()) {
            try {
                // 注意：在部分 Spring Data Redis / Langchain4j 底层封装中，参数如果本身没有特殊结构可能不需要被外层正则表达式深度转义
                // 如果发现查询删除不到，可直接尝试使用 fileName 原文进行删除，底层的 builder 可能会进行安全拼接
                // 我们优先使用转义，并忽略错误
                String escapedFileName = escapeRedisSearch(fileName);
                embeddingStore.removeAll(metadataKey("file_name").isEqualTo(escapedFileName));
            } catch (Exception e) {
                // 忽略索引不存在的报错（说明向量库中还没有任何数据）
                if (e.getMessage() != null && e.getMessage().contains("no such index")) {
                    // index not found, safely ignore
                } else if (e.getMessage() != null && e.getMessage().contains("wrong number of arguments for 'del' command")) {
                    // 如果搜索到了0个结果，底层 redis.clients.jedis.UnifiedJedis.del() 可能会报错，安全忽略
                } else {
                    e.printStackTrace();
                }
            }

            // 为了防止上面的转义导致找不到对应数据，这里补充执行一次不带转义的原文删除
            // 根据上面的测试，Jedis 底层如果没查到数据去执行 DEL 就会抛出 wrong number of arguments for 'del' command
            try {
                embeddingStore.removeAll(metadataKey("file_name").isEqualTo(fileName));
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("no such index") || e.getMessage().contains("wrong number of arguments for 'del' command"))) {
                    // safely ignore
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 转义 RedisSearch 字符串中的特殊字符
     * @param input 原始字符串
     * @return 转义后的字符串
     * 时间: 2026-04-14
     */
    private String escapeRedisSearch(String input) {
        if (input == null) return null;
        // Redis Search 要求对标点符号进行转义，最稳妥的方式是对所有非字母数字的字符进行转义
        return input.replaceAll("([^a-zA-Z0-9])", "\\\\$1");
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
            prompt += "\n（系统提示：用户要求生成" + chartType + "图表，请务必调用查询工具获取数据。并且为了前端渲染统一，请将返回数据中的计数字段的 key 统一转换为 'value'。严格以JSON格式返回：{\"type\":\"chart\",\"chartType\":\"" + chartType + "\",\"data\":[...]}，不要返回任何多余的解释文本或 Markdown 标记）";
        } else {
            prompt += "\n（系统提示：如果回答基于提供的文档知识，请返回 {\"type\":\"doc\",\"content\":\"回答\",\"source_nodes\":[\"文档名\"]}；如果是普通问答，请严格以JSON格式返回：{\"type\":\"text\",\"content\":\"你的回答\"}，不要返回多余文本或 Markdown 标记）";
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

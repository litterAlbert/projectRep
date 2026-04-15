package com.example.backend.service.impl;

import com.example.backend.service.AiService;
import com.example.backend.service.SmartAssistant;
import com.example.backend.tools.OssTool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

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
    private JedisPooled jedisPooled;

    @Autowired(required = false)
    private SmartAssistant smartAssistant;

    // 手动创建一个供补偿删除用的静态 JedisPooled（仅用于排查/备用）
    private JedisPooled fallbackJedisPooled = new JedisPooled("localhost", 6379);

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
        String filename = file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                document = new ApachePdfBoxDocumentParser().parse(is);
            } else {
                document = new ApachePoiDocumentParser().parse(is);
            }
            // 确保将文件名加入到文档的元数据中，以便大模型在检索时能够获取到来源文档名
            if (filename != null) {
                document.metadata().put("file_name", filename);
                // 移除可能引起干扰的 PDF 内置元数据，防止大模型将 title 误认为来源文件名
                document.metadata().remove("title");
                document.metadata().remove("author");
                document.metadata().remove("source");
            }
            
            // 时间: 2026-04-15 新增：将上传者角色放入元数据，用于处理知识库冲突时的优先级
            com.example.backend.tools.UserContext.UserContextInfo info = com.example.backend.tools.UserContext.get();
            if (info != null && info.getRole() != null) {
                document.metadata().put("uploader_role", info.getRole());
            }
        }

        // 3. 向量化并存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(new com.example.backend.tools.SemanticDocumentSplitter(embeddingModel, 0.3))
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
                // 确保我们一定有一个可用的 Jedis 客户端
                JedisPooled currentJedis = (jedisPooled != null) ? jedisPooled : fallbackJedisPooled;
                
                if (currentJedis != null) {
                    System.out.println("【向量删除排查】开始尝试删除文件: " + fileName);
                    long count = 0;
                    java.util.List<String> keysToDelete = new java.util.ArrayList<>();
                    
                    java.util.Set<String> allKeys = currentJedis.keys("embedding:*");
                    System.out.println("【向量删除排查】在 Redis 中找到了 " + (allKeys != null ? allKeys.size() : 0) + " 个以 'embedding:' 开头的 Key");
                    
                    if (allKeys != null) {
                        for (String key : allKeys) {
                             String storedName = null;
                             try {
                                 storedName = String.valueOf(currentJedis.jsonGet(key));
                             } catch (Exception e) {
                                 try {
                                     storedName = currentJedis.get(key);
                                 } catch (Exception e2) {
                                     try {
                                         storedName = String.valueOf(currentJedis.hgetAll(key));
                                     } catch (Exception e3) {
                                         System.err.println("【向量删除排查】Key " + key + " 无法解析任何内容: " + e3.getMessage());
                                     }
                                 }
                             }
                             
                             if (storedName != null && storedName.contains(fileName)) {
                                 keysToDelete.add(key);
                                 count++;
                             }
                        }
                    }
                    
                    System.out.println("【向量删除排查】经过匹配，包含文件名 '" + fileName + "' 的记录共有 " + keysToDelete.size() + " 条");
                    
                    if (!keysToDelete.isEmpty()) {
                        currentJedis.del(keysToDelete.toArray(new String[0]));
                        System.out.println("成功删除了文档 " + fileName + " 的 " + count + " 条向量记录");
                    } else {
                        System.out.println("未找到文档 " + fileName + " 的向量记录，可能匹配规则有问题或确实不存在");
                    }
                } else {
                    System.err.println("【向量删除排查】没有可用的 JedisPooled 实例，删除被跳过！");
                }
            } catch (Exception e) {
                System.err.println("【向量删除排查】发生致命错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
            // 时间: 2026-04-15 增加防止大模型捏造知识的补充提示
            prompt += "\n（系统提示：如果回答基于提供的文档知识，请返回 {\"type\":\"doc\",\"content\":\"回答\",\"source_nodes\":[\"文档名\"]},（注意：来源source_nodes必须提取每段内容开头包含的 file_name 字段中的名字)；如果知识库中没有相关知识，必须严格以JSON格式返回：{\"type\":\"text\",\"content\":\"知识库中没有相关知识\"}，绝不能给出错误的回答；如果是普通问答，请严格以JSON格式返回：{\"type\":\"text\",\"content\":\"你的回答\"}，不要返回多余文本或 Markdown 标记）";
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

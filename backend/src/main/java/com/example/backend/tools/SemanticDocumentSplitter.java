package com.example.backend.tools;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 语义文档分割器
 * 时间: 2026-04-15
 */
public class SemanticDocumentSplitter implements DocumentSplitter {

    private final EmbeddingModel embeddingModel;
    private final double similarityThreshold;
    private final int maxChunkSize;

    /**
     * 构造函数
     * 时间: 2026-04-15
     * @param embeddingModel 嵌入模型，用于计算句子向量
     * @param similarityThreshold 相似度阈值，低于该值说明话题改变，进行切分（例如：0.6）
     */
    public SemanticDocumentSplitter(EmbeddingModel embeddingModel, double similarityThreshold) {
        this(embeddingModel, similarityThreshold, 1000); // 默认最大1000字符
    }

    /**
     * 构造函数
     * 时间: 2026-04-15
     * @param embeddingModel 嵌入模型，用于计算句子向量
     * @param similarityThreshold 相似度阈值，低于该值说明话题改变，进行切分
     * @param maxChunkSize 最大分块长度（字符数），防止分块过大
     */
    public SemanticDocumentSplitter(EmbeddingModel embeddingModel, double similarityThreshold, int maxChunkSize) {
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
        this.maxChunkSize = maxChunkSize;
    }

    /**
     * 将文档分割成语义相关的文本片段
     * 时间: 2026-04-15
     * @param document 要分割的文档
     * @return 分割后的文本片段列表
     */
    @Override
    public List<TextSegment> split(Document document) {
        String text = document.text();
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 预处理文本：统一换行符
        text = text.replace("\r\n", "\n");
        // 将单个换行符替换为空格（解决 PDF 硬回车导致句子被截断的问题）
        // 保留两个及以上的换行符作为段落分隔
        text = text.replaceAll("(?<!\\n)\\n(?!\\n)", " ");

        // 2. 按句子拆分（按句号、叹号、问号分界，或者按段落分界）
        String[] sentencesArray = text.split("(?<=[。！？.!?])\\s+|\\n{2,}");
        List<String> validSentences = new ArrayList<>();
        for (String s : sentencesArray) {
            if (s != null && !s.trim().isEmpty()) {
                validSentences.add(s.trim());
            }
        }

        if (validSentences.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 对每个句子生成向量（批量处理以提高效率）
        List<TextSegment> sentenceSegments = validSentences.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
                
        Response<List<Embedding>> response = embeddingModel.embedAll(sentenceSegments);
        List<Embedding> embeddings = response.content();

        // 4. 根据相邻句子相似度进行分块
        List<TextSegment> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int lastValidIndex = -1; // 记录上一个有效长句的索引

        for (int i = 0; i < validSentences.size(); i++) {
            String sentence = validSentences.get(i);
            
            // 过滤极短文本或纯格式文本：比如论文末尾的文献编号 "[116] Wang Z..." 或是单个年份 "2023."
            // 论文常见特征：含有 "arxiv"、"preprint"等，这些短文本或非核心文本缺乏足够上下文，
            // 强行计算相似度极容易导致断崖式下跌而被单独切分出来。
            boolean isShortOrJunk = sentence.length() < 30 || 
                                    sentence.toLowerCase().contains("arxiv") || 
                                    sentence.toLowerCase().contains("preprint") ||
                                    sentence.toLowerCase().contains("doi:") ||
                                    sentence.matches("^\\[\\d+\\].*");
            
            if (isShortOrJunk) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
                continue;
            }

            // 初始化 currentChunk
            if (currentChunk.length() == 0) {
                currentChunk.append(sentence);
                lastValidIndex = i;
                continue;
            }

            // 5. 计算相邻长句子的相似度
            Embedding prevEmbedding = embeddings.get(lastValidIndex != -1 ? lastValidIndex : i - 1);
            Embedding currEmbedding = embeddings.get(i);
            
            double similarity = CosineSimilarity.between(prevEmbedding, currEmbedding);

            // 6. 判断是否切分：话题转换（低于阈值或无法计算），或长度超限
            boolean isTopicChange = similarity < similarityThreshold || Double.isNaN(similarity);
            boolean isTooLong = currentChunk.length() + sentence.length() > maxChunkSize;

            if (isTopicChange || isTooLong) {
                chunks.add(TextSegment.from(currentChunk.toString(), document.metadata()));
                currentChunk = new StringBuilder(sentence);
            } else {
                currentChunk.append(" ").append(sentence);
            }
            
            // 更新最后一个有效句子的索引
            lastValidIndex = i;
        }

        // 添加最后一个 chunk
        if (currentChunk.length() > 0) {
            chunks.add(TextSegment.from(currentChunk.toString(), document.metadata()));
        }

        return chunks;
    }
}

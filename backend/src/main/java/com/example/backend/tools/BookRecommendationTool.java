package com.example.backend.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.pojo.Book;
import com.example.backend.pojo.BorrowRecord;
import com.example.backend.service.BookService;
import com.example.backend.service.BorrowRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 图书推荐工具
 * 时间: 2026-04-15
 */
@Component
public class BookRecommendationTool implements ApplicationRunner {

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowRecordService borrowRecordService;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private redis.clients.jedis.JedisPooled jedisPooled;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private EmbeddingStore<TextSegment> bookEmbeddingStore;

    @PostConstruct
    public void init() {
        if (jedisPooled == null) {
            // 如果 Spring 容器中没有自动注入 JedisPooled，手动创建一个 Fallback 连接
            jedisPooled = new redis.clients.jedis.JedisPooled(redisHost, redisPort);
            System.out.println("BookRecommendationTool: 手动初始化 JedisPooled (" + redisHost + ":" + redisPort + ")");
        }
        
        if (embeddingModel != null && jedisPooled != null) {
            bookEmbeddingStore = RedisEmbeddingStore.builder()
                    .host(redisHost)
                    .port(redisPort)
                    .dimension(1024)
                    .indexName("book-index")
                    .prefix("book:")
                    .metadataKeys(Arrays.asList("book_id", "title", "author"))
                    .build();
            System.out.println("BookRecommendationTool: 图书向量存储器初始化成功");
        } else {
            System.err.println("BookRecommendationTool: 初始化失败，embeddingModel 或 jedisPooled 为空！");
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        syncBooksToVectorDb();
    }

    /**
     * 将图书同步到向量数据库
     * 时间: 2026-04-15
     */
    public void syncBooksToVectorDb() {
        try {
            if (jedisPooled == null || bookEmbeddingStore == null) {
                System.out.println("向量数据库或模型未配置，跳过图书向量初始化");
                return;
            }
            Set<String> keys = jedisPooled.keys("book:*");
            if (keys == null || keys.isEmpty()) {
                System.out.println("检测到图书向量索引为空，正在初始化图书向量...");
                List<Book> books = bookService.list();
                if (books == null || books.isEmpty()) {
                    System.out.println("数据库中暂无图书记录，跳过初始化。");
                    return;
                }
                for (Book book : books) {
                    addBookToVectorDb(book);
                }
                System.out.println("图书向量初始化完成，共处理 " + books.size() + " 本书");
            } else {
                System.out.println("图书向量索引已存在（包含 " + keys.size() + " 条记录），跳过初始化。");
            }
        } catch (Exception e) {
            System.err.println("同步图书到向量数据库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加单本图书到向量库
     * 时间: 2026-04-15
     */
    public void addBookToVectorDb(Book book) {
        try {
            if (embeddingModel == null || bookEmbeddingStore == null || book == null) {
                return;
            }
            String content = String.format("书名: %s, 作者: %s, 出版社: %s", 
                    book.getTitle(), book.getAuthor(), book.getPublisher());
            Metadata metadata = new Metadata();
            metadata.put("book_id", String.valueOf(book.getId()));
            metadata.put("title", book.getTitle() != null ? book.getTitle() : "");
            metadata.put("author", book.getAuthor() != null ? book.getAuthor() : "");
            TextSegment segment = TextSegment.from(content, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            bookEmbeddingStore.add(embedding, segment);
        } catch (Exception e) {
            System.err.println("添加图书 [" + book.getTitle() + "] 到向量库失败: " + e.getMessage());
        }
    }

    /**
     * 从向量库中删除图书
     * 时间: 2026-04-15
     */
    public void removeBookFromVectorDb(Long bookId) {
        try {
            if (jedisPooled == null || bookId == null) {
                return;
            }
            // 使用 Redis 原始连接根据 metadata 中的 book_id 进行模糊删除
            // 根据之前在 AiServiceImpl 中的补偿删除策略进行类似操作
            String bookIdStr = String.valueOf(bookId);
            Set<String> allKeys = jedisPooled.keys("book:*");
            List<String> keysToDelete = new ArrayList<>();
            
            if (allKeys != null) {
                for (String key : allKeys) {
                    String storedData = null;
                    try {
                        storedData = String.valueOf(jedisPooled.jsonGet(key));
                    } catch (Exception e) {
                        try {
                            storedData = jedisPooled.get(key);
                        } catch (Exception e2) {
                            try {
                                storedData = String.valueOf(jedisPooled.hgetAll(key));
                            } catch (Exception e3) {
                                // 忽略解析错误
                            }
                        }
                    }
                    
                    // 检查存储内容中是否包含该 book_id 的元数据信息
                    // 注意：因为是 JSON 存储（格式如 book_id=30 或 "book_id":"30"）
                    if (storedData != null && (storedData.contains("\"book_id\":\"" + bookIdStr + "\"") 
                            || storedData.contains("book_id=" + bookIdStr + ",") 
                            || storedData.contains("book_id=" + bookIdStr + "}"))) {
                        keysToDelete.add(key);
                    }
                }
            }
            
            if (!keysToDelete.isEmpty()) {
                jedisPooled.del(keysToDelete.toArray(new String[0]));
                System.out.println("成功从向量库删除了图书 ID 为 " + bookIdStr + " 的记录，共 " + keysToDelete.size() + " 条");
            }
        } catch (Exception e) {
            System.err.println("从向量库删除图书 ID [" + bookId + "] 失败: " + e.getMessage());
        }
    }

    /**
     * 更新图书在向量库中的信息
     * 时间: 2026-04-15
     */
    public void updateBookInVectorDb(Book book) {
        if (book == null || book.getId() == null) {
            return;
        }
        // 先删除旧数据
        removeBookFromVectorDb(book.getId());
        // 再添加新数据
        addBookToVectorDb(book);
    }

    /**
     * 根据用户借阅历史推荐书籍
     * 时间: 2026-04-15
     */
    @Tool("如果用户提问类似'有什么书籍推荐的吗'或需要书籍推荐时，调用此工具。工具会根据当前用户的历史借阅记录，通过向量相似度检索推荐书籍；如果没有借阅历史，则推荐被借阅数量排名靠前的热门书籍。注意：在向用户回答推荐的书籍后，必须在回答的最后加上一句话：如需要借阅，请提供《书名》。")
    public String recommendBooks() {
        try {
            UserContext.UserContextInfo userInfo = UserContext.get();
            if (userInfo == null || userInfo.getUserId() == null) {
                return "{\"error\": \"无法获取当前用户信息，推荐失败\"}";
            }

            Long userId = userInfo.getUserId();
            List<BorrowRecord> records = borrowRecordService.list(
                    new QueryWrapper<BorrowRecord>().eq("user_id", userId).orderByDesc("borrow_date")
            );

            List<Book> recommendedBooks = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();

            if (records == null || records.isEmpty() || embeddingModel == null || bookEmbeddingStore == null) {
                // 冷启动：无历史记录或 AI 配置缺失，推荐被借阅数量排名前 5 的热门书籍
                recommendedBooks = bookService.list(
                        new QueryWrapper<Book>().orderByDesc("borrowed_count").last("limit 5")
                );
                return mapper.writeValueAsString(recommendedBooks);
            }

            // 有借阅历史：获取最近借阅的书籍
            List<Long> borrowedBookIds = records.stream()
                    .map(BorrowRecord::getBookId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Book> recentBooks = bookService.listByIds(
                    borrowedBookIds.stream().limit(3).collect(Collectors.toList()) // 取最近的3本书作为兴趣画像
            );

            if (recentBooks.isEmpty()) {
                recommendedBooks = bookService.list(
                        new QueryWrapper<Book>().orderByDesc("borrowed_count").last("limit 5")
                );
                return mapper.writeValueAsString(recommendedBooks);
            }

            // 拼接兴趣画像
            StringBuilder interestBuilder = new StringBuilder();
            for (Book b : recentBooks) {
                interestBuilder.append(String.format("书名: %s, 作者: %s, 出版社: %s; ", 
                        b.getTitle(), b.getAuthor(), b.getPublisher()));
            }

            // 向量化兴趣画像并检索
            Embedding interestEmbedding = embeddingModel.embed(interestBuilder.toString()).content();
            
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(interestEmbedding)
                    .maxResults(10)
                    .minScore(0.5)
                    .build();
            EmbeddingSearchResult<TextSegment> searchResult = bookEmbeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            // 过滤掉用户已经借过的书
            List<Long> matchedBookIds = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                String bookIdStr = match.embedded().metadata().getString("book_id");
                if (bookIdStr != null) {
                    Long bid = Long.valueOf(bookIdStr);
                    if (!borrowedBookIds.contains(bid)) {
                        matchedBookIds.add(bid);
                    }
                }
            }

            if (matchedBookIds.isEmpty()) {
                // 如果过滤后没有，则返回热门书籍
                recommendedBooks = bookService.list(
                        new QueryWrapper<Book>().orderByDesc("borrowed_count").last("limit 5")
                );
            } else {
                // 取前 5 本
                List<Long> topIds = matchedBookIds.stream().limit(5).collect(Collectors.toList());
                recommendedBooks = bookService.listByIds(topIds);
            }

            return mapper.writeValueAsString(recommendedBooks);
        } catch (Exception e) {
            return "{\"error\": \"推荐生成失败: " + e.getMessage() + "\"}";
        }
    }
}

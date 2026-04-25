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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * 基于物品的协同过滤推荐（无明确目标时推荐）
     * 时间: 2026-04-25
     */
    @Tool("当用户泛泛地询问'有什么图书推荐的吗'或不需要特定主题的书籍推荐时调用此工具。使用基于物品的协同过滤算法进行推荐。注意：在向用户回答推荐的书籍后，必须在回答的最后加上一句话：如需要借阅，请提供《书名》。")
    public String recommendGeneralBooks() {
        try {
            UserContext.UserContextInfo userInfo = UserContext.get();
            if (userInfo == null || userInfo.getUserId() == null) {
                return "{\"error\": \"无法获取当前用户信息，推荐失败\"}";
            }

            Long userId = userInfo.getUserId();
            List<BorrowRecord> allRecords = borrowRecordService.list();
            ObjectMapper mapper = new ObjectMapper();

            // 用户 -> 借阅的书籍ID集合
            Map<Long, Set<Long>> userBooksMap = new HashMap<>();
            // 书籍 -> 借阅的用户ID集合
            Map<Long, Set<Long>> bookUsersMap = new HashMap<>();

            for (BorrowRecord record : allRecords) {
                userBooksMap.computeIfAbsent(record.getUserId(), k -> new HashSet<>()).add(record.getBookId());
                bookUsersMap.computeIfAbsent(record.getBookId(), k -> new HashSet<>()).add(record.getUserId());
            }

            Set<Long> targetUserBooks = userBooksMap.getOrDefault(userId, new HashSet<>());

            // 如果没有借阅记录，冷启动兜底：推荐被借阅数量排名前 5 的热门书籍
            if (targetUserBooks.isEmpty()) {
                List<Book> popularBooks = bookService.list(
                        new QueryWrapper<Book>().orderByDesc("borrowed_count").last("limit 5")
                );
                return mapper.writeValueAsString(popularBooks);
            }

            // 计算物品相似度得分
            Map<Long, Double> bookScores = new HashMap<>();
            for (Long targetBookId : targetUserBooks) {
                Set<Long> usersWhoBorrowedTarget = bookUsersMap.getOrDefault(targetBookId, new HashSet<>());
                for (Map.Entry<Long, Set<Long>> entry : bookUsersMap.entrySet()) {
                    Long candidateBookId = entry.getKey();
                    if (targetUserBooks.contains(candidateBookId)) {
                        continue; // 过滤掉已经借过的书籍
                    }
                    Set<Long> usersWhoBorrowedCandidate = entry.getValue();

                    // 计算交集大小
                    Set<Long> intersection = new HashSet<>(usersWhoBorrowedTarget);
                    intersection.retainAll(usersWhoBorrowedCandidate);

                    if (!intersection.isEmpty()) {
                        // 余弦相似度：交集大小 / sqrt(A集合大小 * B集合大小)
                        double similarity = intersection.size() / Math.sqrt(usersWhoBorrowedTarget.size() * usersWhoBorrowedCandidate.size());
                        bookScores.put(candidateBookId, bookScores.getOrDefault(candidateBookId, 0.0) + similarity);
                    }
                }
            }

            List<Book> recommendedBooks = new ArrayList<>();
            if (bookScores.isEmpty()) {
                recommendedBooks = bookService.list(
                        new QueryWrapper<Book>().orderByDesc("borrowed_count").last("limit 5")
                );
            } else {
                List<Long> recommendedBookIds = bookScores.entrySet().stream()
                        .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                        .limit(5)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                recommendedBooks = bookService.listByIds(recommendedBookIds);
            }

            return mapper.writeValueAsString(recommendedBooks);
        } catch (Exception e) {
            return "{\"error\": \"推荐生成失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 基于向量相似度的用户兴趣画像推荐（有明确目标时推荐）
     * 时间: 2026-04-25
     * @param query 用户的具体查询需求，例如“数据结构”、“科幻”
     */
    @Tool("当用户询问有什么具体的图书推荐时（例如'请推荐数据结构相关的图书'、'推荐一些科幻小说'），调用此工具。工具会结合用户的具体查询和历史兴趣画像，通过向量相似度进行精准推荐。参数 query 提取自用户的具体查询需求。注意：在向用户回答推荐的书籍后，必须在回答的最后加上一句话：如需要借阅，请提供《书名》。")
    public String recommendSpecificBooks(String query) {
        try {
            UserContext.UserContextInfo userInfo = UserContext.get();
            if (userInfo == null || userInfo.getUserId() == null) {
                return "{\"error\": \"无法获取当前用户信息，推荐失败\"}";
            }

            Long userId = userInfo.getUserId();
            List<BorrowRecord> records = borrowRecordService.list(
                    new QueryWrapper<BorrowRecord>().eq("user_id", userId).orderByDesc("borrow_date")
            );

            ObjectMapper mapper = new ObjectMapper();
            if (embeddingModel == null || bookEmbeddingStore == null) {
                // 模型未配置的降级处理：模糊查询
                List<Book> fallbackBooks = bookService.list(
                        new QueryWrapper<Book>().like("title", query).or().like("author", query).last("limit 5")
                );
                if (fallbackBooks == null || fallbackBooks.isEmpty()) {
                    return "抱歉，馆内暂无该类图书推荐。";
                }
                return mapper.writeValueAsString(fallbackBooks);
            }

            List<Long> borrowedBookIds = new ArrayList<>();
            StringBuilder searchContentBuilder = new StringBuilder();
            searchContentBuilder.append("用户查询需求: ").append(query).append("。 ");

            if (records != null && !records.isEmpty()) {
                borrowedBookIds = records.stream()
                        .map(BorrowRecord::getBookId)
                        .distinct()
                        .collect(Collectors.toList());

                List<Book> recentBooks = bookService.listByIds(
                        borrowedBookIds.stream().limit(3).collect(Collectors.toList())
                );

                if (!recentBooks.isEmpty()) {
                    searchContentBuilder.append("用户的近期兴趣画像: ");
                    for (Book b : recentBooks) {
                        searchContentBuilder.append(String.format("书名: %s, 作者: %s, 出版社: %s; ",
                                b.getTitle(), b.getAuthor(), b.getPublisher()));
                    }
                }
            }

            String searchContent = searchContentBuilder.toString();
            Embedding queryEmbedding = embeddingModel.embed(searchContent).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(10)
                    .minScore(0.5)
                    .build();
            EmbeddingSearchResult<TextSegment> searchResult = bookEmbeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

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
                return "抱歉，馆内暂无该类图书推荐。";
            }

            List<Long> topIds = matchedBookIds.stream().limit(5).collect(Collectors.toList());
            List<Book> recommendedBooks = bookService.listByIds(topIds);

            if (recommendedBooks == null || recommendedBooks.isEmpty()) {
                return "抱歉，馆内暂无该类图书推荐。";
            }

            return mapper.writeValueAsString(recommendedBooks);
        } catch (Exception e) {
            return "{\"error\": \"推荐生成失败: " + e.getMessage() + "\"}";
        }
    }
}

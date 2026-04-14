package com.example.backend;

import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@SpringBootTest
public class RedisSearchTest {

    @Autowired
    private EmbeddingStore embeddingStore;

    @Test
    public void testSearch() {
        try {
            System.out.println("====== STARTING DELETE TEST ======");
            String fileName = "test.pdf"; // 只是用作测试语法
            
            // 方式1：转义
            String escaped = fileName.replaceAll("([^a-zA-Z0-9])", "\\\\$1");
            System.out.println("ESCAPED 1: " + escaped);
            
            // 方式2：不转义，因为目前发现有些情况下，Spring Data Redis/Langchain4j Redis底层封装时不需要我们在外层额外进行正则表达式转义。
            System.out.println("ESCAPED 2 (No escape): " + fileName);

            // 如果底层自己做了处理，我们外层转义反而会导致查询变成了查找 "test\.pdf" 这个字面量，因此搜不到，也就没删掉！
            // 这里我们调用一个没转义的看看是否会报错
            embeddingStore.removeAll(metadataKey("file_name").isEqualTo(fileName));
            
            System.out.println("====== DELETE FINISHED ======");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
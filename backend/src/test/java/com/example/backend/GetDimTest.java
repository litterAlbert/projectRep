package com.example.backend;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GetDimTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    public void testDim() {
        System.out.println("====== DIMENSION: " + embeddingModel.embed("test").content().vector().length + " ======");
    }
}
package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.JedisPooled;

import java.util.Set;

@SpringBootTest
public class RedisBookInspectTest {

    @Autowired(required = false)
    private JedisPooled jedisPooled;

    @Test
    public void inspectBookData() {
        if (jedisPooled == null) {
            jedisPooled = new JedisPooled("localhost", 6379);
        }
        
        System.out.println("====== STARTING REDIS INSPECT ======");
        Set<String> keys = jedisPooled.keys("book:*");
        System.out.println("Found " + (keys != null ? keys.size() : 0) + " keys with prefix 'book:'");
        
        if (keys != null && !keys.isEmpty()) {
            String firstKey = keys.iterator().next();
            System.out.println("Inspecting key: " + firstKey);
            
            try {
                System.out.println("JSON GET: " + jedisPooled.jsonGet(firstKey));
            } catch (Exception e) {
                System.out.println("JSON GET failed: " + e.getMessage());
            }
            
            try {
                System.out.println("HGETALL: " + jedisPooled.hgetAll(firstKey));
            } catch (Exception e) {
                System.out.println("HGETALL failed: " + e.getMessage());
            }
        }
        System.out.println("====== INSPECT FINISHED ======");
    }
}
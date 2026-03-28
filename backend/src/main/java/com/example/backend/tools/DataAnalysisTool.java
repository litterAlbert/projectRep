package com.example.backend.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 数据库分析工具
 * 时间: 2026-03-28
 */
@Component
public class DataAnalysisTool {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Tool("执行MySQL查询语句并返回查询结果的JSON字符串。注意：请仅在需要查询统计数据（如借书数量前十的用户、热门借阅榜、分类借阅占比等）时调用此工具。数据库包含以下表：user(id, username, role)、book(id, title, author, stock, borrowed_count, reserved_count)、borrow_record(id, user_id, book_id, borrow_date, return_date, status)、reserve_record(id, user_id, book_id, reserve_date, status)")
    public String executeQuery(@P("可执行的MySQL查询语句") String sql) {
        try {
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("sql", sql);
            return new ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"SQL执行失败: " + e.getMessage() + "\"}";
        }
    }
}

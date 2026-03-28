package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.pojo.ChatMessage;
import com.example.backend.pojo.ChatSession;
import com.example.backend.pojo.Result;
import com.example.backend.service.AiService;
import com.example.backend.service.ChatMessageService;
import com.example.backend.service.ChatSessionService;
import com.example.backend.tools.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 助手控制器
 * 时间: 2026-03-28
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageService chatMessageService;

    /**
     * 上传文档进行向量化
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<String> uploadDoc(@RequestPart("file") MultipartFile file) {
        try {
            String url = aiService.uploadAndProcessDoc(file);
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 新建会话
     */
    @PostMapping("/session")
    public Result<ChatSession> createSession(@RequestBody ChatSession session) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        session.setId(UUID.randomUUID().toString());
        session.setUserId(info.getUserId());
        session.setCreatedAt(new Date());
        session.setUpdatedAt(new Date());
        if (session.getTitle() == null) {
            session.setTitle("新会话");
        }
        chatSessionService.save(session);
        return Result.success(session);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/session/list")
    public Result<List<ChatSession>> getSessions() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        QueryWrapper<ChatSession> query = new QueryWrapper<>();
        query.eq("user_id", info.getUserId()).orderByDesc("updated_at");
        return Result.success(chatSessionService.list(query));
    }

    /**
     * 会话聊天
     */
    @PostMapping("/chat/{sessionId}")
    public Result<Map<String, Object>> chat(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        // 记录用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setCreatedAt(new Date());
        chatMessageService.save(userMsg);

        // 调用AI服务
        Map<String, Object> aiResponse = aiService.chat(sessionId, message);

        // 记录AI消息
        try {
            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setSessionId(sessionId);
            aiMsg.setRole("ai");
            // 将整个对象转为JSON字符串存储，如果内容过长可能会超出 TEXT 限制，但通常普通聊天不会。
            String aiContent = new ObjectMapper().writeValueAsString(aiResponse);
            // 避免超出内容字段的限制
            if (aiContent != null && aiContent.length() > 65535) {
                aiContent = aiContent.substring(0, 65535);
            }
            aiMsg.setContent(aiContent);
            aiMsg.setCreatedAt(new Date());
            chatMessageService.save(aiMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.success(aiResponse);
    }

    /**
     * 获取会话历史记录
     */
    @GetMapping("/chat/{sessionId}/history")
    public Result<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByAsc("created_at");
        return Result.success(chatMessageService.list(query));
    }
}

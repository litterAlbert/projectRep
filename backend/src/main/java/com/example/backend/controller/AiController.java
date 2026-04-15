package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.pojo.ChatMessage;
import com.example.backend.pojo.ChatSession;
import com.example.backend.pojo.Result;
import com.example.backend.pojo.UploadedFile;
import com.example.backend.service.AiService;
import com.example.backend.service.ChatMessageService;
import com.example.backend.service.ChatSessionService;
import com.example.backend.service.UploadedFileService;
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

    @Autowired
    private UploadedFileService uploadedFileService;

    /**
     * 上传文档进行向量化
     * 时间: 2026-03-28
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<Map<String, String>> uploadDoc(@RequestPart("file") MultipartFile file) {
        try {
            UserContext.UserContextInfo info = UserContext.get();
            if (info == null) return Result.error(401, "未登录");

            String url = aiService.uploadAndProcessDoc(file);
            Map<String, String> data = new java.util.HashMap<>();
            data.put("fileUrl", url);
            data.put("fileName", file.getOriginalFilename());
            
            // 记录文件信息到数据库
            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setFileName(file.getOriginalFilename());
            uploadedFile.setFileUrl(url);
            uploadedFile.setUploader(info.getUsername());
            uploadedFile.setUploaderRole(info.getRole());
            uploadedFile.setCreatedAt(new Date());
            uploadedFileService.save(uploadedFile);
            
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 删除上传的知识库文档
     * 时间: 2026-04-14
     */
    @DeleteMapping("/document")
    public Result<String> deleteDoc(@RequestParam("fileUrl") String fileUrl, @RequestParam("fileName") String fileName) {
        try {
            UserContext.UserContextInfo info = UserContext.get();
            if (info == null) return Result.error(401, "未登录");

            QueryWrapper<UploadedFile> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_url", fileUrl);
            
            // 如果不是管理员，只能删除自己上传的文件
            if (!"admin".equals(info.getRole())) {
                queryWrapper.eq("uploader", info.getUsername());
                UploadedFile fileRecord = uploadedFileService.getOne(queryWrapper);
                if (fileRecord == null) {
                    return Result.error(403, "无权删除该文件或文件不存在");
                }
            }

            aiService.deleteDoc(fileUrl, fileName);
            
            // 删除数据库记录
            uploadedFileService.remove(queryWrapper);
            
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取已上传文件列表
     * 时间: 2026-04-15
     */
    @GetMapping("/document/list")
    public Result<List<UploadedFile>> getUploadedFiles() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        QueryWrapper<UploadedFile> query = new QueryWrapper<>();
        // 管理员可获取所有已上传文件，用户只能获取自己上传的文件
        if (!"admin".equals(info.getRole())) {
            query.eq("uploader", info.getUsername());
        }
        query.orderByDesc("created_at");
        return Result.success(uploadedFileService.list(query));
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
     * 删除会话
     * 时间: 2026-03-29
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<String> deleteSession(@PathVariable String sessionId) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        ChatSession existing = chatSessionService.getById(sessionId);
        if (existing == null) {
            return Result.error("会话不存在");
        }
        
        // 只有会话所有者可以删除
        if (!existing.getUserId().equals(info.getUserId())) {
            return Result.error(403, "无权操作");
        }

        // 删除会话及其关联消息
        chatMessageService.remove(new QueryWrapper<ChatMessage>().eq("session_id", sessionId));
        chatSessionService.removeById(sessionId);
        
        return Result.success("删除成功");
    }

    /**
     * 重命名会话
     * 时间: 2026-03-29
     */
    @PutMapping("/session/{sessionId}")
    public Result<String> renameSession(@PathVariable String sessionId, @RequestBody ChatSession session) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) return Result.error(401, "未登录");

        ChatSession existing = chatSessionService.getById(sessionId);
        if (existing == null) {
            return Result.error("会话不存在");
        }
        
        // 只有会话所有者可以重命名
        if (!existing.getUserId().equals(info.getUserId())) {
            return Result.error(403, "无权操作");
        }

        existing.setTitle(session.getTitle());
        existing.setUpdatedAt(new Date());
        chatSessionService.updateById(existing);
        
        return Result.success("重命名成功");
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

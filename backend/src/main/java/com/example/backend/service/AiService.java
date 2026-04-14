package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface AiService {
    String uploadAndProcessDoc(MultipartFile file) throws Exception;

    /**
     * 删除知识库文档
     * @param fileUrl 文档URL（用于删除OSS文件）
     * @param fileName 文档文件名（用于删除向量库数据）
     * 时间: 2026-04-14
     */
    void deleteDoc(String fileUrl, String fileName);

    Map<String, Object> chat(String sessionId, String message);
}

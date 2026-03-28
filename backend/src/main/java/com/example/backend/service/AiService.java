package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface AiService {
    String uploadAndProcessDoc(MultipartFile file) throws Exception;
    Map<String, Object> chat(String sessionId, String message);
}

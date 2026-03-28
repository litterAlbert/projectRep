package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface AiService {
    String uploadAndProcessDoc(MultipartFile file) throws Exception;
    String chatWithDoc(String sessionId, String message);
    Map<String, Object> dataAnalysis(String query);
}

package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String source;
    private java.util.Date createdAt;

    public ChatMessage() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return this.sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRole() { return this.role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return this.content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return this.source; }
    public void setSource(String source) { this.source = source; }
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
}

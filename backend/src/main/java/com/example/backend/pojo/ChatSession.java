package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

public class ChatSession {
    private String id;
    private Long userId;
    private String title;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;

    public ChatSession() {}

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
    public java.util.Date getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(java.util.Date updatedAt) { this.updatedAt = updatedAt; }
}

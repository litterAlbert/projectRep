package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

import com.baomidou.mybatisplus.annotation.TableField;

public class ReserveRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private java.util.Date reserveDate;
    private String status;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;

    @TableField(exist = false)
    private String title;

    @TableField(exist = false)
    private String username;

    public ReserveRecord() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return this.bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public java.util.Date getReserveDate() { return this.reserveDate; }
    public void setReserveDate(java.util.Date reserveDate) { this.reserveDate = reserveDate; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
    public java.util.Date getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(java.util.Date updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 获取书名
     * 时间: 2026-03-30
     */
    public String getTitle() { return this.title; }

    /**
     * 设置书名
     * 时间: 2026-03-30
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * 获取用户名
     * 时间: 2026-03-30
     */
    public String getUsername() { return this.username; }

    /**
     * 设置用户名
     * 时间: 2026-03-30
     */
    public void setUsername(String username) { this.username = username; }
}

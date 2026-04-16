package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

@TableName("book_donation")
public class BookDonation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date publishDate;
    private Integer count;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

    @TableField(exist = false)
    private String username;

    public BookDonation() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getIsbn() { return this.isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return this.author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return this.publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Date getPublishDate() { return this.publishDate; }
    public void setPublishDate(Date publishDate) { this.publishDate = publishDate; }
    public Integer getCount() { return this.count; }
    public void setCount(Integer count) { this.count = count; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 获取捐赠者用户名
     * 时间: 2026-04-16
     */
    public String getUsername() { return this.username; }

    /**
     * 设置捐赠者用户名
     * 时间: 2026-04-16
     */
    public void setUsername(String username) { this.username = username; }
}

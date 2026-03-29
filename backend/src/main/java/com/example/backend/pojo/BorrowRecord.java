package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;

public class BorrowRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private java.util.Date borrowDate;
    private java.util.Date returnDate;
    private String status;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;

    @TableField(exist = false)
    private String title;
    
    @TableField(exist = false)
    private String author;
    
    @TableField(exist = false)
    private String publisher;

    public BorrowRecord() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return this.bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public java.util.Date getBorrowDate() { return this.borrowDate; }
    public void setBorrowDate(java.util.Date borrowDate) { this.borrowDate = borrowDate; }
    public java.util.Date getReturnDate() { return this.returnDate; }
    public void setReturnDate(java.util.Date returnDate) { this.returnDate = returnDate; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
    public java.util.Date getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(java.util.Date updatedAt) { this.updatedAt = updatedAt; }
    
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return this.author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return this.publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
}

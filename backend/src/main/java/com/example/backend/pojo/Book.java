package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private java.util.Date publishDate;
    private Integer stock;
    private Integer borrowedCount;
    private Integer reservedCount;
    private String location;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;

    public Book() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getIsbn() { return this.isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return this.author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return this.publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public java.util.Date getPublishDate() { return this.publishDate; }
    public void setPublishDate(java.util.Date publishDate) { this.publishDate = publishDate; }
    public Integer getStock() { return this.stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getBorrowedCount() { return this.borrowedCount; }
    public void setBorrowedCount(Integer borrowedCount) { this.borrowedCount = borrowedCount; }
    public Integer getReservedCount() { return this.reservedCount; }
    public void setReservedCount(Integer reservedCount) { this.reservedCount = reservedCount; }
    public String getLocation() { return this.location; }
    public void setLocation(String location) { this.location = location; }
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
    public java.util.Date getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(java.util.Date updatedAt) { this.updatedAt = updatedAt; }
}

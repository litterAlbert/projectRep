package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 上传文件记录实体类
 * 时间: 2026-04-15
 */
public class UploadedFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String fileUrl;
    private String uploader;
    private String uploaderRole;
    private java.util.Date createdAt;

    public UploadedFile() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return this.fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFileUrl() { return this.fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    
    public String getUploader() { return this.uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }
    
    public String getUploaderRole() { return this.uploaderRole; }
    public void setUploaderRole(String uploaderRole) { this.uploaderRole = uploaderRole; }
    
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
}
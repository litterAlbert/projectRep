package com.example.backend.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String gender;
    private String phone;
    private String role;
    private java.util.Date createdAt;
    private java.util.Date updatedAt;

    public User() {}

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return this.password; }
    public void setPassword(String password) { this.password = password; }
    public String getGender() { return this.gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return this.phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return this.role; }
    public void setRole(String role) { this.role = role; }
    public java.util.Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(java.util.Date createdAt) { this.createdAt = createdAt; }
    public java.util.Date getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(java.util.Date updatedAt) { this.updatedAt = updatedAt; }
}

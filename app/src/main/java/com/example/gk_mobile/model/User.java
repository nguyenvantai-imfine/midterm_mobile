package com.example.gk_mobile.model;

import com.google.firebase.Timestamp;

public class User {
    private String uid;
    private String name;
    private int age;
    private String phone;
    private String status; // Normal or Locked
    private String role;   // admin, manager, employee
    private String email;
    private String avatarUrl;
    private Timestamp createdAt;

    public User() {
        // Required empty constructor for Firestore deserialization
    }

    public User(String uid, String name, int age, String phone, String status, String role, String email, String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.status = status;
        this.role = role;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

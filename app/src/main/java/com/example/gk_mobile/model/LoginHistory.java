package com.example.gk_mobile.model;

import com.google.firebase.Timestamp;

public class LoginHistory {
    private String id;
    private Timestamp loginAt;
    private String device;

    public LoginHistory() {
        // Required empty constructor for Firestore
    }

    public LoginHistory(String id, Timestamp loginAt, String device) {
        this.id = id;
        this.loginAt = loginAt;
        this.device = device;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(Timestamp loginAt) {
        this.loginAt = loginAt;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}

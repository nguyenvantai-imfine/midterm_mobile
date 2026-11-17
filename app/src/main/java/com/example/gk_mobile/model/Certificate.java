package com.example.gk_mobile.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class Certificate implements Serializable {
    private String id;
    private String name;
    private String issuer;
    private transient Timestamp issueDate;
    private transient Timestamp expiryDate;
    private String note;

    public Certificate() {
        // Required for Firestore
    }

    public Certificate(String id, String name, String issuer, Timestamp issueDate, Timestamp expiryDate, String note) {
        this.id = id;
        this.name = name;
        this.issuer = issuer;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Timestamp getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Timestamp issueDate) {
        this.issueDate = issueDate;
    }

    public Timestamp getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Timestamp expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

package com.example.gk_mobile.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class Student implements Serializable {
    private String studentCode; // Document ID
    private String name;
    private int age;
    private String gender;
    private String className;
    private String faculty;
    private String email;
    private String phone;
    private String address;
    private transient Timestamp createdAt; // transient to prevent NotSerializableException in Intent extras

    public Student() {
        // Required for Firestore
    }

    public Student(String studentCode, String name, int age, String gender, String className, String faculty, String email, String phone, String address) {
        this.studentCode = studentCode;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.className = className;
        this.faculty = faculty;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

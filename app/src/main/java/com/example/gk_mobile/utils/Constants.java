package com.example.gk_mobile.utils;

public class Constants {
    // Firestore Collections & Subcollections
    public static final String COLLECTION_USERS = "users";
    public static final String SUBCOLLECTION_LOGIN_HISTORY = "loginHistory";
    public static final String COLLECTION_STUDENTS = "students";
    public static final String SUBCOLLECTION_CERTIFICATES = "certificates";

    // User Roles
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_EMPLOYEE = "employee";

    // User Statuses
    public static final String STATUS_NORMAL = "Normal";
    public static final String STATUS_LOCKED = "Locked";

    public static boolean isStatusLocked(String status) {
        if (status == null) return false;
        String s = status.toLowerCase().trim();
        return s.contains("lock") || s.contains("khóa") || s.contains("khoa");
    }
}

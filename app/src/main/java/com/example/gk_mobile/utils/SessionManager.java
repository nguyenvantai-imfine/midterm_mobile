package com.example.gk_mobile.utils;

import com.example.gk_mobile.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null && currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && Constants.ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isManager() {
        return currentUser != null && Constants.ROLE_MANAGER.equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isEmployee() {
        return currentUser != null && Constants.ROLE_EMPLOYEE.equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isLocked() {
        return currentUser != null && Constants.STATUS_LOCKED.equalsIgnoreCase(currentUser.getStatus());
    }

    public boolean canManageStudents() {
        return isAdmin() || isManager();
    }

    public boolean canManageUsers() {
        return isAdmin();
    }

    public void logout() {
        currentUser = null;
        FirebaseAuth.getInstance().signOut();
    }
}

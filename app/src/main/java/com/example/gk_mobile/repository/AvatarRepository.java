package com.example.gk_mobile.repository;

import com.example.gk_mobile.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Cập nhật ảnh đại diện của chính người dùng. Mọi role (kể cả employee) đều được phép:
 * Security Rules chỉ cho phép người dùng thường đổi duy nhất trường avatarUrl của mình.
 */
public class AvatarRepository {
    private final FirebaseFirestore db;

    public interface OnActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public AvatarRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void updateAvatar(String uid, String avatarUrl, OnActionListener listener) {
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("avatarUrl", avatarUrl) // chỉ đụng tới đúng 1 trường
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
}

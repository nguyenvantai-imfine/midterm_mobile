package com.example.gk_mobile.repository;

import android.os.Build;
import com.example.gk_mobile.model.LoginHistory;
import com.example.gk_mobile.utils.Constants;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db;

    public interface OnLoginHistoryLoadedListener {
        void onSuccess(List<LoginHistory> historyList);
        void onError(Exception e);
    }

    public interface OnActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Records a login history entry for the given user.
     * Document includes loginAt (serverTimestamp) and device (Build.MODEL).
     */
    public void recordLoginHistory(String uid, OnActionListener listener) {
        if (uid == null || uid.isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("loginAt", FieldValue.serverTimestamp());
        map.put("device", Build.MODEL != null ? Build.MODEL : "Unknown Device");

        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .collection(Constants.SUBCOLLECTION_LOGIN_HISTORY)
                .add(map)
                .addOnSuccessListener(docRef -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }

    /**
     * Retrieves login history list for a user, ordered by loginAt DESC, limited to 50 items.
     */
    public ListenerRegistration getLoginHistoryRealtime(String uid, OnLoginHistoryLoadedListener listener) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .collection(Constants.SUBCOLLECTION_LOGIN_HISTORY)
                .orderBy("loginAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<LoginHistory> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            LoginHistory item = doc.toObject(LoginHistory.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                list.add(item);
                            }
                        }
                    }
                    listener.onSuccess(list);
                });
    }

    /**
     * Deletes a user document and their loginHistory subcollection.
     * Prevents admin from deleting themselves.
     */
    public void deleteUserWithHistory(String targetUid, OnActionListener listener) {
        String currentUid = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUid != null && currentUid.equals(targetUid)) {
            listener.onError(new IllegalStateException("Không được phép tự xóa tài khoản của chính mình!"));
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(targetUid)
                .collection(Constants.SUBCOLLECTION_LOGIN_HISTORY)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();

                    // Delete login history subcollection docs
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    // Delete main user document
                    batch.delete(db.collection(Constants.COLLECTION_USERS).document(targetUid));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }
}

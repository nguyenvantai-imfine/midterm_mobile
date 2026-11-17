package com.example.gk_mobile.repository;

import com.example.gk_mobile.model.Certificate;
import com.example.gk_mobile.utils.Constants;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý chứng chỉ của sinh viên, lưu ở subcollection students/{studentCode}/certificates.
 */
public class CertificateRepository {
    private final FirebaseFirestore db;

    public interface OnCertificatesChangedListener {
        void onSuccess(List<Certificate> certificates);
        void onError(Exception e);
    }

    public interface OnActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public CertificateRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference certificatesOf(String studentCode) {
        return db.collection(Constants.COLLECTION_STUDENTS)
                .document(studentCode)
                .collection(Constants.SUBCOLLECTION_CERTIFICATES);
    }

    /**
     * Lắng nghe realtime danh sách chứng chỉ của một sinh viên.
     * Sắp xếp trong bộ nhớ theo ngày cấp giảm dần (không cần tạo index Firestore).
     * Nhớ gọi ListenerRegistration.remove() khi màn hình dừng.
     */
    public ListenerRegistration getCertificatesRealtime(String studentCode, OnCertificatesChangedListener listener) {
        return certificatesOf(studentCode).addSnapshotListener((value, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }

            List<Certificate> list = new ArrayList<>();
            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Certificate c = doc.toObject(Certificate.class);
                    if (c != null) {
                        c.setId(doc.getId());
                        list.add(c);
                    }
                }
            }

            Collections.sort(list, (c1, c2) -> {
                if (c1.getIssueDate() == null && c2.getIssueDate() == null) return 0;
                if (c1.getIssueDate() == null) return 1;
                if (c2.getIssueDate() == null) return -1;
                return c2.getIssueDate().compareTo(c1.getIssueDate());
            });

            listener.onSuccess(list);
        });
    }

    /**
     * Thêm mới (id rỗng) hoặc cập nhật (đã có id) một chứng chỉ.
     */
    public void saveCertificate(String studentCode, Certificate certificate, OnActionListener listener) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", certificate.getName());
        map.put("issuer", certificate.getIssuer());
        map.put("issueDate", certificate.getIssueDate());
        // Không có ngày hết hạn => bỏ trường này (chứng chỉ không thời hạn).
        if (certificate.getExpiryDate() != null) {
            map.put("expiryDate", certificate.getExpiryDate());
        }
        map.put("note", certificate.getNote() != null ? certificate.getNote() : "");

        CollectionReference col = certificatesOf(studentCode);
        if (certificate.getId() == null || certificate.getId().isEmpty()) {
            col.add(map)
                    .addOnSuccessListener(ref -> listener.onSuccess())
                    .addOnFailureListener(listener::onError);
        } else {
            // set() ghi đè cả document nên ngày hết hạn đã xóa sẽ biến mất đúng ý.
            col.document(certificate.getId())
                    .set(map)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(listener::onError);
        }
    }

    public void deleteCertificate(String studentCode, String certificateId, OnActionListener listener) {
        certificatesOf(studentCode)
                .document(certificateId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
}

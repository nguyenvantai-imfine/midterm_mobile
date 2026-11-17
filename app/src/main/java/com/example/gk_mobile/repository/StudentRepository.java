package com.example.gk_mobile.repository;

import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.utils.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentRepository {
    private final FirebaseFirestore db;

    public interface OnStudentsChangedListener {
        void onSuccess(List<Student> students);
        void onError(Exception e);
    }

    public interface OnActionListener {
        void onSuccess();
        void onError(Exception e);
    }

    public StudentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Realtime listener for students collection.
     * Fetches all documents and sorts in-memory so documents created manually without 'createdAt' field are also included.
     */
    public ListenerRegistration getStudentsRealtime(OnStudentsChangedListener listener) {
        return db.collection(Constants.COLLECTION_STUDENTS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<Student> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Student s = doc.toObject(Student.class);
                            if (s != null) {
                                s.setStudentCode(doc.getId());
                                list.add(s);
                            }
                        }
                    }

                    // Sort in memory by createdAt descending (nulls last)
                    Collections.sort(list, (s1, s2) -> {
                        if (s1.getCreatedAt() == null && s2.getCreatedAt() == null) return 0;
                        if (s1.getCreatedAt() == null) return 1;
                        if (s2.getCreatedAt() == null) return -1;
                        return s2.getCreatedAt().compareTo(s1.getCreatedAt());
                    });

                    listener.onSuccess(list);
                });
    }

    /**
     * Adds or updates a student in Firestore using studentCode as document ID.
     */
    public void saveStudent(Student student, OnActionListener listener) {
        Map<String, Object> map = new HashMap<>();
        map.put("studentCode", student.getStudentCode());
        map.put("name", student.getName());
        map.put("age", student.getAge());
        map.put("gender", student.getGender());
        map.put("className", student.getClassName());
        map.put("faculty", student.getFaculty());
        map.put("email", student.getEmail());
        map.put("phone", student.getPhone());
        map.put("address", student.getAddress());
        if (student.getCreatedAt() != null) {
            map.put("createdAt", student.getCreatedAt());
        } else {
            map.put("createdAt", FieldValue.serverTimestamp());
        }

        db.collection(Constants.COLLECTION_STUDENTS)
                .document(student.getStudentCode())
                .set(map, SetOptions.merge())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /**
     * Deletes student document and all documents in its 'certificates' subcollection using WriteBatch.
     */
    public void deleteStudentWithCertificates(String studentCode, OnActionListener listener) {
        db.collection(Constants.COLLECTION_STUDENTS)
                .document(studentCode)
                .collection(Constants.SUBCOLLECTION_CERTIFICATES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();

                    // Delete all certificates in subcollection
                    for (DocumentSnapshot certDoc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(certDoc.getReference());
                    }

                    // Delete main student document
                    batch.delete(db.collection(Constants.COLLECTION_STUDENTS).document(studentCode));

                    // Commit batch atomically
                    batch.commit()
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }
}

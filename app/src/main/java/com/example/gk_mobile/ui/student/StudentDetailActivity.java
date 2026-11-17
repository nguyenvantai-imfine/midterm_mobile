package com.example.gk_mobile.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.gk_mobile.adapter.CertificateAdapter;
import com.example.gk_mobile.databinding.ActivityStudentDetailBinding;
import com.example.gk_mobile.model.Certificate;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.repository.CertificateRepository;
import com.example.gk_mobile.repository.StudentRepository;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class StudentDetailActivity extends AppCompatActivity {

    private ActivityStudentDetailBinding binding;
    private Student student;
    private StudentRepository studentRepository;
    private CertificateRepository certificateRepository;
    private CertificateAdapter certificateAdapter;
    private ListenerRegistration certificatesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentRepository = new StudentRepository();
        certificateRepository = new CertificateRepository();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (getIntent().hasExtra("student")) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                student = getIntent().getSerializableExtra("student", Student.class);
            } else {
                student = (Student) getIntent().getSerializableExtra("student");
            }
            displayStudent();
            setupCertificates();
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListeningCertificates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Gỡ listener khi màn hình dừng để không tốn lượt đọc Firestore và không rò bộ nhớ.
        if (certificatesListener != null) {
            certificatesListener.remove();
            certificatesListener = null;
        }
    }

    // ===== Chứng chỉ của sinh viên =====

    private void setupCertificates() {
        // Employee chỉ xem; admin và manager mới thêm/sửa/xóa (đồng bộ với Security Rules).
        boolean canManage = SessionManager.getInstance().canManageStudents();

        certificateAdapter = new CertificateAdapter(canManage, new CertificateAdapter.OnCertificateActionListener() {
            @Override
            public void onEdit(Certificate certificate) {
                showCertificateForm(certificate);
            }

            @Override
            public void onDelete(Certificate certificate) {
                confirmDeleteCertificate(certificate);
            }
        });
        binding.rvCertificates.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCertificates.setAdapter(certificateAdapter);

        binding.btnAddCertificate.setVisibility(canManage ? View.VISIBLE : View.GONE);
        binding.btnAddCertificate.setOnClickListener(v -> showCertificateForm(null));
    }

    private void startListeningCertificates() {
        if (student == null || certificateRepository == null || certificatesListener != null) return;

        certificatesListener = certificateRepository.getCertificatesRealtime(
                student.getStudentCode(), new CertificateRepository.OnCertificatesChangedListener() {
                    @Override
                    public void onSuccess(List<Certificate> certificates) {
                        certificateAdapter.setCertificates(certificates);
                        binding.tvCertificatesEmpty.setVisibility(certificates.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(StudentDetailActivity.this,
                                "Không tải được chứng chỉ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCertificateForm(Certificate existing) {
        CertificateFormDialog.show(this, existing, certificate ->
                certificateRepository.saveCertificate(student.getStudentCode(), certificate, new CertificateRepository.OnActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(StudentDetailActivity.this,
                                existing == null ? "Đã thêm chứng chỉ!" : "Đã cập nhật chứng chỉ!",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(StudentDetailActivity.this,
                                "Lỗi khi lưu chứng chỉ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void confirmDeleteCertificate(Certificate certificate) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa chứng chỉ \"" + certificate.getName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) ->
                        certificateRepository.deleteCertificate(student.getStudentCode(), certificate.getId(), new CertificateRepository.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(StudentDetailActivity.this, "Đã xóa chứng chỉ!", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(StudentDetailActivity.this,
                                        "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void displayStudent() {
        if (student == null) return;

        binding.tvStudentName.setText(student.getName());
        binding.tvStudentCode.setText(String.format("Mã SV: %s", student.getStudentCode()));
        binding.tvClassFaculty.setText(String.format("Lớp: %s | Khoa: %s", student.getClassName(), student.getFaculty()));
        binding.tvGenderAge.setText(String.format(java.util.Locale.getDefault(), "Giới tính: %s | Tuổi: %d", student.getGender(), student.getAge()));
        binding.tvEmail.setText(String.format("Email: %s", student.getEmail()));
        binding.tvPhone.setText(String.format("Số điện thoại: %s", student.getPhone()));
        binding.tvAddress.setText(String.format("Địa chỉ: %s", student.getAddress()));

        boolean canManage = SessionManager.getInstance().canManageStudents();
        binding.layoutActions.setVisibility(canManage ? View.VISIBLE : View.GONE);

        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDetailActivity.this, StudentFormActivity.class);
            intent.putExtra("student", student);
            startActivity(intent);
            finish();
        });

        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sinh viên này cùng toàn bộ chứng chỉ của họ?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    studentRepository.deleteStudentWithCertificates(student.getStudentCode(), new StudentRepository.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(StudentDetailActivity.this, "Đã xóa sinh viên!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(StudentDetailActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}

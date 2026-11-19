package com.example.gk_mobile.ui.student;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.gk_mobile.adapter.CertificateAdapter;
import com.example.gk_mobile.databinding.ActivityStudentDetailBinding;
import com.example.gk_mobile.databinding.DialogCertificateImportBinding;
import com.example.gk_mobile.model.Certificate;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.repository.CertificateRepository;
import com.example.gk_mobile.repository.StudentRepository;
import com.example.gk_mobile.ui.common.BaseActivity;
import com.example.gk_mobile.utils.CertificateExporter;
import com.example.gk_mobile.utils.CertificateFileImporter;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class StudentDetailActivity extends BaseActivity {

    /** Số dòng lỗi tối đa hiển thị trong hộp thoại import. */
    private static final int MAX_ERROR_LINES_SHOWN = 15;

    private ActivityStudentDetailBinding binding;
    private Student student;
    private StudentRepository studentRepository;
    private CertificateRepository certificateRepository;
    private CertificateAdapter certificateAdapter;
    private ListenerRegistration certificatesListener;

    // Bản sao danh sách chứng chỉ hiện tại, dùng khi xuất file.
    private final List<Certificate> currentCertificates = new ArrayList<>();

    // Chọn file để import (CSV hoặc XLSX, tự nhận biết định dạng)
    private final ActivityResultLauncher<String[]> importCertificatesLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) showCertificateImportDialog(uri);
            });

    // Chọn nơi lưu file CSV
    private final ActivityResultLauncher<String> exportCsvLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    boolean ok = CertificateExporter.exportToCsv(this, uri, currentCertificates);
                    Toast.makeText(this, ok ? "Xuất file CSV thành công!" : "Xuất file CSV thất bại!",
                            Toast.LENGTH_LONG).show();
                }
            });

    // Chọn nơi lưu file Excel
    private final ActivityResultLauncher<String> exportExcelLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(CertificateExporter.XLSX_MIME_TYPE),
            uri -> {
                if (uri != null) {
                    boolean ok = CertificateExporter.exportToExcel(this, uri, currentCertificates);
                    Toast.makeText(this, ok ? "Xuất file Excel thành công!" : "Xuất file Excel thất bại!",
                            Toast.LENGTH_LONG).show();
                }
            });

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

        // Nút "Thử lại" của trạng thái lỗi: đăng ký lại listener Firestore.
        binding.certEmptyState.btnEmptyAction.setOnClickListener(v -> {
            if (certificatesListener != null) {
                certificatesListener.remove();
                certificatesListener = null;
            }
            startListeningCertificates();
        });

        // Import chỉ dành cho admin/manager; xuất file thì mọi role đều dùng được.
        binding.btnImportCertificates.setVisibility(canManage ? View.VISIBLE : View.GONE);
        binding.btnImportCertificates.setOnClickListener(v -> importCertificatesLauncher.launch(new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/vnd.ms-excel",
                "text/plain",
                CertificateExporter.XLSX_MIME_TYPE,
                "application/octet-stream"
        }));

        binding.btnExportCertificatesCsv.setOnClickListener(v -> {
            if (currentCertificates.isEmpty()) {
                showToast("Chưa có chứng chỉ nào để xuất!");
                return;
            }
            exportCsvLauncher.launch("ChungChi_" + student.getStudentCode() + ".csv");
        });

        binding.btnExportCertificatesExcel.setOnClickListener(v -> {
            if (currentCertificates.isEmpty()) {
                showToast("Chưa có chứng chỉ nào để xuất!");
                return;
            }
            exportExcelLauncher.launch("ChungChi_" + student.getStudentCode() + ".xlsx");
        });
    }

    private void startListeningCertificates() {
        if (student == null || certificateRepository == null || certificatesListener != null) return;

        binding.progressCertificates.setVisibility(View.VISIBLE);
        certificatesListener = certificateRepository.getCertificatesRealtime(
                student.getStudentCode(), new CertificateRepository.OnCertificatesChangedListener() {
                    @Override
                    public void onSuccess(List<Certificate> certificates) {
                        binding.progressCertificates.setVisibility(View.GONE);
                        currentCertificates.clear();
                        currentCertificates.addAll(certificates);
                        certificateAdapter.setCertificates(certificates);
                        showCertificateState(certificates.isEmpty() ? "Sinh viên này chưa có chứng chỉ nào" : null, false);
                    }

                    @Override
                    public void onError(Exception e) {
                        binding.progressCertificates.setVisibility(View.GONE);
                        showCertificateState("Không tải được chứng chỉ: " + e.getMessage(), true);
                    }
                });
    }

    /** Hiện trạng thái trống/lỗi dưới danh sách chứng chỉ; message = null thì ẩn đi. */
    private void showCertificateState(String message, boolean showRetry) {
        if (message == null) {
            binding.certEmptyState.getRoot().setVisibility(View.GONE);
            return;
        }
        binding.certEmptyState.tvEmptyMessage.setText(message);
        binding.certEmptyState.btnEmptyAction.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        binding.certEmptyState.getRoot().setVisibility(View.VISIBLE);
    }

    /** Hộp thoại tiến độ và kết quả import, liệt kê rõ từng dòng bị lỗi. */
    private void showCertificateImportDialog(Uri fileUri) {
        DialogCertificateImportBinding d = DialogCertificateImportBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(d.getRoot())
                .setCancelable(false)
                .create();
        d.btnCertImportClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        CertificateFileImporter.importFile(this, fileUri, student.getStudentCode(),
                new CertificateFileImporter.ImportCallback() {
                    @Override
                    public void onProgress(String statusMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        d.tvCertImportStatus.setText(statusMessage);
                    }

                    @Override
                    public void onSuccess(int successCount, int duplicateCount, List<String> rowErrors) {
                        if (isFinishing() || isDestroyed()) return;
                        d.progressBarCertImport.setVisibility(View.GONE);
                        d.tvCertImportStatus.setText("Import hoàn tất!");
                        d.layoutCertImportStats.setVisibility(View.VISIBLE);
                        d.tvCertSuccessCount.setText("• Thêm mới thành công: " + successCount + " chứng chỉ");
                        d.tvCertDuplicateCount.setText("• Trùng (bỏ qua): " + duplicateCount + " chứng chỉ");
                        d.tvCertErrorCount.setText("• Dòng lỗi: " + rowErrors.size() + " dòng");

                        if (!rowErrors.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            int shown = Math.min(rowErrors.size(), MAX_ERROR_LINES_SHOWN);
                            for (int i = 0; i < shown; i++) {
                                if (i > 0) sb.append('\n');
                                sb.append(rowErrors.get(i));
                            }
                            if (rowErrors.size() > shown) {
                                sb.append("\n... và ").append(rowErrors.size() - shown).append(" lỗi khác");
                            }
                            d.tvCertImportErrors.setText(sb.toString());
                            d.scrollCertImportErrors.setVisibility(View.VISIBLE);
                        }
                        d.btnCertImportClose.setEnabled(true);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        d.progressBarCertImport.setVisibility(View.GONE);
                        d.tvCertImportStatus.setText("Lỗi: " + errorMessage);
                        d.btnCertImportClose.setEnabled(true);
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
        showConfirmDialog("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa chứng chỉ \"" + certificate.getName() + "\"?",
                "Xóa",
                () -> certificateRepository.deleteCertificate(student.getStudentCode(), certificate.getId(),
                        new CertificateRepository.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                showToast("Đã xóa chứng chỉ!");
                            }

                            @Override
                            public void onError(Exception e) {
                                showToast("Lỗi khi xóa: " + e.getMessage());
                            }
                        }));
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
        showConfirmDialog("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa sinh viên này cùng toàn bộ chứng chỉ của họ?",
                "Xóa",
                () -> {
                    showLoading("Đang xóa sinh viên...");
                    studentRepository.deleteStudentWithCertificates(student.getStudentCode(), new StudentRepository.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            hideLoading();
                            showToast("Đã xóa sinh viên!");
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            hideLoading();
                            showToast("Lỗi khi xóa: " + e.getMessage());
                        }
                    });
                });
    }
}

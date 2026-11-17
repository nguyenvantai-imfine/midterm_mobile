package com.example.gk_mobile.ui.student;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gk_mobile.R;
import com.example.gk_mobile.adapter.StudentAdapter;
import com.example.gk_mobile.databinding.ActivityStudentListBinding;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.utils.ExcelExporter;
import com.example.gk_mobile.utils.FileExporter;
import com.example.gk_mobile.utils.StudentFileImporter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentListActivity extends AppCompatActivity implements StudentAdapter.OnStudentActionListener {

    private ActivityStudentListBinding binding;
    private StudentAdapter adapter;
    private FirebaseFirestore db;
    private final List<Student> studentList = new ArrayList<>();

    // Biến cờ phân biệt đang import Excel hay CSV
    private boolean isImportingExcel = true;

    // Launcher mở file Import (Hỗ trợ cả Excel và CSV)
    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    showImportProgressDialog(uri, isImportingExcel);
                }
            }
    );

    // Launcher tạo file để Export CSV
    private final ActivityResultLauncher<String> createCsvLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    FileExporter.exportStudentsToCsv(this, uri, studentList);
                }
            }
    );

    // Launcher tạo file để Export Excel
    private final ActivityResultLauncher<String> createExcelLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(ExcelExporter.MIME_TYPE),
            uri -> {
                if (uri != null) {
                    boolean ok = ExcelExporter.exportStudentsToExcel(this, uri, studentList);
                    Toast.makeText(this,
                            ok ? "Xuất file Excel thành công!" : "Xuất file Excel thất bại!",
                            Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupRecyclerView();
        setupActions();
        loadStudentsFromFirestore();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Danh sách Sinh viên");
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter(this);
        binding.recyclerViewStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewStudents.setAdapter(adapter);
    }

    private void setupActions() {
        // Nút thêm sinh viên mới
        binding.fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(StudentListActivity.this, StudentFormActivity.class);
            startActivity(intent);
        });

        // Ô tìm kiếm realtime
        binding.edtSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Nút Sắp xếp đa tiêu chí
        binding.btnSort.setOnClickListener(v -> showMultiCriteriaSortDialog());

        // Nút Lọc nâng cao
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        // --- CÁC NÚT IMPORT / EXPORT (EXCEL & CSV) ---
        binding.btnImportExcel.setOnClickListener(v -> {
            isImportingExcel = true;
            filePickerLauncher.launch(new String[]{
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            });
        });

        binding.btnExportExcel.setOnClickListener(v -> {
            if (studentList.isEmpty()) {
                Toast.makeText(this, "Không có dữ liệu sinh viên để xuất!", Toast.LENGTH_SHORT).show();
                return;
            }
            createExcelLauncher.launch("DanhSachSinhVien.xlsx");
        });

        binding.btnImportCsv.setOnClickListener(v -> {
            isImportingExcel = false;
            filePickerLauncher.launch(new String[]{
                    "text/csv",
                    "text/comma-separated-values",
                    "application/vnd.ms-excel",
                    "text/plain"
            });
        });

        binding.btnExportCsv.setOnClickListener(v -> {
            createCsvLauncher.launch("DanhSachSinhVien.csv");
        });
    }

    private void loadStudentsFromFirestore() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("students")
                .addSnapshotListener((value, error) -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    studentList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Student student = doc.toObject(Student.class);
                            studentList.add(student);
                        }
                    }

                    if (studentList.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                    }

                    if (adapter != null) {
                        adapter.setStudents(studentList);
                    }
                });
    }

    private void showMultiCriteriaSortDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sort_students, null);

        android.widget.CheckBox chkFaculty = dialogView.findViewById(R.id.chkFaculty);
        android.widget.CheckBox chkClassName = dialogView.findViewById(R.id.chkClassName);
        android.widget.CheckBox chkName = dialogView.findViewById(R.id.chkName);
        android.widget.CheckBox chkAge = dialogView.findViewById(R.id.chkAge);
        android.widget.CheckBox chkGender = dialogView.findViewById(R.id.chkGender);
        android.widget.CheckBox chkStudentCode = dialogView.findViewById(R.id.chkStudentCode);

        android.widget.RadioButton radioAZ = dialogView.findViewById(R.id.radioAZ);
        android.widget.RadioButton radioSmallToLarge = dialogView.findViewById(R.id.radioSmallToLarge);

        new AlertDialog.Builder(this)
                .setTitle("Tùy chỉnh sắp xếp đa tiêu chí")
                .setView(dialogView)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    boolean isAscendingString = radioAZ.isChecked();
                    boolean isAscendingNumber = radioSmallToLarge.isChecked();

                    if (adapter != null) {
                        adapter.sortWithTwoGroups(
                                chkFaculty.isChecked(),
                                chkClassName.isChecked(),
                                chkName.isChecked(),
                                chkAge.isChecked(),
                                chkGender.isChecked(),
                                chkStudentCode.isChecked(),
                                isAscendingString,
                                isAscendingNumber
                        );
                        Toast.makeText(this, "Đã sắp xếp danh sách thành công!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Đặt lại gốc", (dialog, which) -> {
                    if (adapter != null) {
                        adapter.sortMultiCriteria(true);
                        Toast.makeText(this, "Đã đưa về danh sách gốc", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_students, null);

        AutoCompleteTextView actvFaculty = dialogView.findViewById(R.id.actvFaculty);
        AutoCompleteTextView actvClassName = dialogView.findViewById(R.id.actvClassName);
        AutoCompleteTextView actvName = dialogView.findViewById(R.id.actvName);
        com.google.android.material.textfield.TextInputEditText edtAge = dialogView.findViewById(R.id.edtAge);
        AutoCompleteTextView actvStudentCode = dialogView.findViewById(R.id.actvStudentCode);

        android.widget.RadioButton radioGenderMale = dialogView.findViewById(R.id.radioGenderMale);

        List<String> facultyList = new ArrayList<>();
        List<String> classList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        List<String> codeList = new ArrayList<>();

        for (Student s : studentList) {
            if (s.getFaculty() != null && !facultyList.contains(s.getFaculty())) facultyList.add(s.getFaculty());
            if (s.getClassName() != null && !classList.contains(s.getClassName())) classList.add(s.getClassName());
            if (s.getName() != null && !nameList.contains(s.getName())) nameList.add(s.getName());
            if (s.getStudentCode() != null && !codeList.contains(s.getStudentCode())) codeList.add(s.getStudentCode());
        }

        actvFaculty.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, facultyList));
        actvFaculty.setThreshold(1);

        final android.widget.ArrayAdapter<String>[] classAdapter = new android.widget.ArrayAdapter[]{
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, classList)
        };
        actvClassName.setAdapter(classAdapter[0]);
        actvClassName.setThreshold(1);

        actvName.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nameList));
        actvName.setThreshold(1);

        actvStudentCode.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, codeList));
        actvStudentCode.setThreshold(1);

        actvFaculty.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFaculty = parent.getItemAtPosition(position).toString();
            List<String> filteredClasses = new ArrayList<>();
            for (Student s : studentList) {
                if (selectedFaculty.equalsIgnoreCase(s.getFaculty()) && s.getClassName() != null && !filteredClasses.contains(s.getClassName())) {
                    filteredClasses.add(s.getClassName());
                }
            }
            classAdapter[0] = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredClasses);
            actvClassName.setAdapter(classAdapter[0]);
            actvClassName.setText("", false);
        });

        new AlertDialog.Builder(this)
                .setTitle("Bộ lọc tìm kiếm sinh viên")
                .setView(dialogView)
                .setPositiveButton("Lọc", (dialog, which) -> {
                    String fVal = actvFaculty.getText().toString();
                    String cVal = actvClassName.getText().toString();
                    String nVal = actvName.getText().toString();
                    String aVal = edtAge.getText().toString();
                    String sVal = actvStudentCode.getText().toString();

                    String gVal = "Tất cả";
                    if (radioGenderMale.isChecked()) gVal = "Nam";
                    else if (((android.widget.RadioButton) dialogView.findViewById(R.id.radioGenderFemale)).isChecked()) gVal = "Nữ";

                    if (adapter != null) {
                        adapter.filterMultiCriteria(fVal, cVal, nVal, aVal, gVal, sVal);
                        Toast.makeText(this, "Đã áp dụng bộ lọc thành công!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Xóa bộ lọc", (dialog, which) -> {
                    if (adapter != null) {
                        adapter.filterMultiCriteria("", "", "", "", "Tất cả", "");
                        Toast.makeText(this, "Đã xóa bộ lọc, hiển thị toàn bộ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    // Hiển thị Dialog tiến độ khi Import file
    private void showImportProgressDialog(Uri fileUri, boolean isExcel) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        ProgressBar progressBar = dialogView.findViewById(R.id.progressBarImport);
        TextView tvStatus = dialogView.findViewById(R.id.tvImportStatus);
        LinearLayout layoutStats = dialogView.findViewById(R.id.layoutImportStats);
        TextView tvSuccess = dialogView.findViewById(R.id.tvSuccessCount);
        TextView tvDuplicate = dialogView.findViewById(R.id.tvDuplicateCount);
        TextView tvError = dialogView.findViewById(R.id.tvErrorCount);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnImportClose);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        StudentFileImporter.importFile(this, fileUri, isExcel, new StudentFileImporter.ImportCallback() {
            @Override
            public void onProgress(String statusMessage) {
                runOnUiThread(() -> tvStatus.setText(statusMessage));
            }

            @Override
            public void onSuccess(int successCount, int duplicateCount, int errorCount) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Import hoàn tất!");
                    layoutStats.setVisibility(View.VISIBLE);
                    tvSuccess.setText("• Thêm mới thành công: " + successCount + " dòng");
                    tvDuplicate.setText("• Trùng mã (bỏ qua): " + duplicateCount + " dòng");
                    tvError.setText("• Lỗi định dạng/thiếu trường: " + errorCount + " dòng");
                    btnClose.setEnabled(true);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Lỗi: " + errorMessage);
                    btnClose.setEnabled(true);
                });
            }
        });
    }

    @Override
    public void onItemClick(Student student) {
        Toast.makeText(this, "Chọn sinh viên: " + student.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditStudent(Student student) {
        Intent intent = new Intent(StudentListActivity.this, StudentFormActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDeleteStudent(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa sinh viên " + student.getName() + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Xử lý xóa trên Firestore
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
package com.example.gk_mobile.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gk_mobile.R;
import com.example.gk_mobile.adapter.StudentAdapter;
import com.example.gk_mobile.databinding.ActivityStudentListBinding;
import com.example.gk_mobile.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

public class StudentListActivity extends AppCompatActivity implements StudentAdapter.OnStudentActionListener {

    private ActivityStudentListBinding binding;
    private StudentAdapter adapter;
    private FirebaseFirestore db;
    private final List<Student> studentList = new ArrayList<>();

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
        binding.btnSort.setOnClickListener(v -> {
            showMultiCriteriaSortDialog();
        });

        // nút lọc
        binding.btnFilter.setOnClickListener(v -> {
            showFilterDialog();
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
                            // Đảm bảo gán ID từ document vào model nếu cần
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
        android.widget.CheckBox chkName = dialogView.findViewById(R.id.chkName); // Thêm Tên
        android.widget.CheckBox chkAge = dialogView.findViewById(R.id.chkAge);
        android.widget.CheckBox chkGender = dialogView.findViewById(R.id.chkGender);
        android.widget.CheckBox chkStudentCode = dialogView.findViewById(R.id.chkStudentCode);

        android.widget.RadioButton radioAZ = dialogView.findViewById(R.id.radioAZ);
        android.widget.RadioButton radioSmallToLarge = dialogView.findViewById(R.id.radioSmallToLarge);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tùy chỉnh sắp xếp đa tiêu chí")
                .setView(dialogView)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    boolean isAscendingString = radioAZ.isChecked();
                    boolean isAscendingNumber = radioSmallToLarge.isChecked();

                    if (adapter != null) {
                        adapter.sortWithTwoGroups(
                                chkFaculty.isChecked(),
                                chkClassName.isChecked(),
                                chkName.isChecked(), // Truyền thêm trạng thái chọn Tên
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

        android.widget.RadioButton radioGenderAll = dialogView.findViewById(R.id.radioGenderAll);
        android.widget.RadioButton radioGenderMale = dialogView.findViewById(R.id.radioGenderMale);

        // 1. Trích xuất danh sách độc lập các Khoa, Lớp, Tên, Mã SV từ studentList hiện có để làm gợi ý AutoComplete
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

        // Đổ dữ liệu gợi ý vào các AutoCompleteTextView
        android.widget.ArrayAdapter<String> facultyAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, facultyList);
        actvFaculty.setAdapter(facultyAdapter);
        actvFaculty.setThreshold(1); // Gợi ý ngay từ ký tự đầu tiên

        final android.widget.ArrayAdapter<String>[] classAdapter = new android.widget.ArrayAdapter[]{
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, classList)
        };
        actvClassName.setAdapter(classAdapter[0]);
        actvClassName.setThreshold(1);

        actvName.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nameList));
        actvName.setThreshold(1);

        actvStudentCode.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, codeList));
        actvStudentCode.setThreshold(1);

        // 2. LOGIC RÀNG BUỘC: Khi chọn/nhập Khoa, tự động lọc danh sách Lớp tương ứng trong khoa đó
        actvFaculty.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFaculty = parent.getItemAtPosition(position).toString();
            List<String> filteredClasses = new ArrayList<>();
            for (Student s : studentList) {
                if (selectedFaculty.equalsIgnoreCase(s.getFaculty()) && s.getClassName() != null && !filteredClasses.contains(s.getClassName())) {
                    filteredClasses.add(s.getClassName());
                }
            }
            // Cập nhật lại Adapter cho ô Lớp chỉ hiển thị lớp trong Khoa vừa chọn
            classAdapter[0] = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredClasses);
            actvClassName.setAdapter(classAdapter[0]);
            actvClassName.setText("", false); // Xóa text cũ của lớp khi đổi khoa
        });

        // 3. Hiển thị Dialog Lọc
        new androidx.appcompat.app.AlertDialog.Builder(this)
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
                        // Đưa về danh sách đầy đủ ban đầu
                        adapter.filterMultiCriteria("", "", "", "", "Tất cả", "");
                        Toast.makeText(this, "Đã xóa bộ lọc, hiển thị toàn bộ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    @Override
    public void onItemClick(Student student) {
        // Xử lý khi bấm vào item xem chi tiết
        Toast.makeText(this, "Chọn sinh viên: " + student.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditStudent(Student student) {
        // Chuyển sang màn hình sửa sinh viên
        Intent intent = new Intent(StudentListActivity.this, StudentFormActivity.class);
        // Truyền studentCode hoặc ID sang form sửa nếu cần
        startActivity(intent);
    }

    @Override
    public void onDeleteStudent(Student student) {
        // Xử lý xóa sinh viên
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa sinh viên " + student.getName() + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Thực hiện xóa trên Firestore theo studentCode hoặc documentId
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
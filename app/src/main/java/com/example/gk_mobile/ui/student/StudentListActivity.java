package com.example.gk_mobile.ui.student;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.gk_mobile.adapter.StudentAdapter;
import com.example.gk_mobile.databinding.ActivityStudentListBinding;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.repository.StudentRepository;
import com.example.gk_mobile.utils.FileExporter;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class StudentListActivity extends AppCompatActivity implements StudentAdapter.OnStudentClickListener {

    private ActivityStudentListBinding binding;
    private StudentRepository studentRepository;
    private StudentAdapter adapter;
    private ListenerRegistration realtimeListener;
    private List<Student> currentStudentList = new ArrayList<>();

    private final ActivityResultLauncher<String> createCsvLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    exportToUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentRepository = new StudentRepository();

        setupUI();
        loadStudentsRealtime();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new StudentAdapter(this);
        binding.recyclerViewStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewStudents.setAdapter(adapter);

        // Visibility permissions
        boolean canManage = SessionManager.getInstance().canManageStudents();
        binding.fabAddStudent.setVisibility(canManage ? View.VISIBLE : View.GONE);

        binding.fabAddStudent.setOnClickListener(v ->
                startActivity(new Intent(StudentListActivity.this, StudentFormActivity.class))
        );

        binding.btnExportCsv.setOnClickListener(v -> {
            if (currentStudentList.isEmpty()) {
                Toast.makeText(this, "Không có dữ liệu sinh viên để xuất!", Toast.LENGTH_SHORT).show();
                return;
            }
            createCsvLauncher.launch("danh_sach_sinh_vien.csv");
        });
    }

    private void loadStudentsRealtime() {
        binding.progressBar.setVisibility(View.VISIBLE);
        realtimeListener = studentRepository.getStudentsRealtime(new StudentRepository.OnStudentsChangedListener() {
            @Override
            public void onSuccess(List<Student> students) {
                binding.progressBar.setVisibility(View.GONE);
                currentStudentList = students;
                adapter.setStudents(students);
                binding.tvEmpty.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(StudentListActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportToUri(Uri uri) {
        boolean success = FileExporter.exportStudentsToCsv(this, uri, currentStudentList);
        if (success) {
            Toast.makeText(this, "Xuất file CSV thành công!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Xuất file CSV thất bại!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(Student student) {
        Intent intent = new Intent(this, StudentDetailActivity.class);
        intent.putExtra("student", student);
        startActivity(intent);
    }

    @Override
    public void onEditClick(Student student) {
        Intent intent = new Intent(this, StudentFormActivity.class);
        intent.putExtra("student", student);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sinh viên " + student.getName() + " (" + student.getStudentCode() + ") cùng toàn bộ chứng chỉ của sinh viên này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    studentRepository.deleteStudentWithCertificates(student.getStudentCode(), new StudentRepository.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(StudentListActivity.this, "Đã xóa sinh viên thành công!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(StudentListActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeListener != null) {
            realtimeListener.remove(); // Unregister listener on Destroy to prevent leaks
        }
    }
}

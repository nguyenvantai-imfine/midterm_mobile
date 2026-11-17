package com.example.gk_mobile.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gk_mobile.databinding.ActivityStudentDetailBinding;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.repository.StudentRepository;
import com.example.gk_mobile.utils.SessionManager;

public class StudentDetailActivity extends AppCompatActivity {

    private ActivityStudentDetailBinding binding;
    private Student student;
    private StudentRepository studentRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentRepository = new StudentRepository();

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
        } else {
            finish();
        }
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

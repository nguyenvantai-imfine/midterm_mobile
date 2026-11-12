package com.example.gk_mobile.ui.student;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gk_mobile.databinding.ActivityStudentFormBinding;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.repository.StudentRepository;

public class StudentFormActivity extends AppCompatActivity {

    private ActivityStudentFormBinding binding;
    private StudentRepository studentRepository;
    private Student editingStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentRepository = new StudentRepository();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupGenderDropdown();

        if (getIntent().hasExtra("student")) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                editingStudent = getIntent().getSerializableExtra("student", Student.class);
            } else {
                editingStudent = (Student) getIntent().getSerializableExtra("student");
            }
            populateForm(editingStudent);
        }

        binding.btnSave.setOnClickListener(v -> saveStudent());
    }

    private void setupGenderDropdown() {
        String[] genders = new String[]{"Nam", "Nữ", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        binding.actvGender.setAdapter(adapter);
    }

    private void populateForm(Student student) {
        if (student == null) return;
        binding.etStudentCode.setText(student.getStudentCode());
        binding.etStudentCode.setEnabled(false); // Code cannot be modified during edit

        binding.etName.setText(student.getName());
        binding.etAge.setText(String.valueOf(student.getAge()));
        binding.actvGender.setText(student.getGender(), false);
        binding.etClassName.setText(student.getClassName());
        binding.etFaculty.setText(student.getFaculty());
        binding.etEmail.setText(student.getEmail());
        binding.etPhone.setText(student.getPhone());
        binding.etAddress.setText(student.getAddress());
        binding.btnSave.setText("CẬP NHẬT SINH VIÊN");
    }

    private void saveStudent() {
        String code = binding.etStudentCode.getText() != null ? binding.etStudentCode.getText().toString().trim() : "";
        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String ageStr = binding.etAge.getText() != null ? binding.etAge.getText().toString().trim() : "0";
        String gender = binding.actvGender.getText() != null ? binding.actvGender.getText().toString().trim() : "Nam";
        String className = binding.etClassName.getText() != null ? binding.etClassName.getText().toString().trim() : "";
        String faculty = binding.etFaculty.getText() != null ? binding.etFaculty.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
        String address = binding.etAddress.getText() != null ? binding.etAddress.getText().toString().trim() : "";

        if (code.isEmpty()) {
            binding.etStudentCode.setError("Vui lòng nhập mã sinh viên");
            return;
        }

        if (name.isEmpty()) {
            binding.etName.setError("Vui lòng nhập họ tên sinh viên");
            return;
        }

        if (className.isEmpty()) {
            binding.etClassName.setError("Vui lòng nhập lớp");
            return;
        }

        if (faculty.isEmpty()) {
            binding.etFaculty.setError("Vui lòng nhập khoa");
            return;
        }

        int age = 0;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException ignored) {}

        Student student = new Student(code, name, age, gender, className, faculty, email, phone, address);
        if (editingStudent != null) {
            student.setCreatedAt(editingStudent.getCreatedAt());
        }

        binding.btnSave.setEnabled(false);
        studentRepository.saveStudent(student, new StudentRepository.OnActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(StudentFormActivity.this, "Lưu thông tin sinh viên thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                binding.btnSave.setEnabled(true);
                Toast.makeText(StudentFormActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

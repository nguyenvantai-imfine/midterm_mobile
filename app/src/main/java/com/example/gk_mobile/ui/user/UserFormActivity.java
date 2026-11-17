package com.example.gk_mobile.ui.user;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gk_mobile.databinding.ActivityUserFormBinding;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserFormActivity extends AppCompatActivity {

    private ActivityUserFormBinding binding;
    private Calendar calendarDob;
    private SimpleDateFormat dateFormatter;

    private String editingUserId = null; // Biến lưu UID khi ở chế độ Sửa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kiểm tra quyền Admin trước khi cho phép thao tác
        if (!SessionManager.getInstance().isAdmin()) {
            Toast.makeText(this, "Bạn không có quyền thực hiện chức năng này!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Kiểm tra xem có truyền userId sang không để xác định là Thêm hay Sửa
        if (getIntent() != null && getIntent().hasExtra("USER_ID")) {
            editingUserId = getIntent().getStringExtra("USER_ID");
            setupEditMode(editingUserId); // Gọi hàm load dữ liệu cũ lên form
        }

        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        calendarDob = Calendar.getInstance();

        setupToolbar();
        setupDatePicker();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarUserForm);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbarUserForm.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDatePicker() {
        binding.edtDob.setOnClickListener(v -> {
            int year = calendarDob.get(Calendar.YEAR);
            int month = calendarDob.get(Calendar.MONTH);
            int day = calendarDob.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        calendarDob.set(selectedYear, selectedMonth, selectedDay);
                        binding.edtDob.setText(dateFormatter.format(calendarDob.getTime()));
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void setupListeners() {
        binding.btnSaveUser.setOnClickListener(v -> validateAndCreateUser());
    }

    private void validateAndCreateUser() {
        String fullName = binding.edtFullName.getText() != null ? binding.edtFullName.getText().toString().trim() : "";
        String email = binding.edtEmail.getText() != null ? binding.edtEmail.getText().toString().trim() : "";
        String password = binding.edtPassword.getText() != null ? binding.edtPassword.getText().toString().trim() : "";
        String phone = binding.edtPhone.getText() != null ? binding.edtPhone.getText().toString().trim() : "";
        String dobStr = binding.edtDob.getText() != null ? binding.edtDob.getText().toString().trim() : "";
        String address = binding.edtAddress.getText() != null ? binding.edtAddress.getText().toString().trim() : "";

        // Lấy Role từ RadioGroup
        String role = binding.rbManager.isChecked() ? "manager" : "employee";

        // Lấy Status từ RadioGroup
        String status = binding.rbLocked.isChecked() ? "Locked" : "Normal";

        // Validate dữ liệu đầu vào cơ bản
        if (TextUtils.isEmpty(fullName)) {
            binding.tilFullName.setError("Vui lòng nhập họ và tên");
            binding.edtFullName.requestFocus();
            return;
        }
        binding.tilFullName.setError(null);

        if (TextUtils.isEmpty(dobStr)) {
            binding.tilDob.setError("Vui lòng chọn ngày sinh");
            return;
        }
        binding.tilDob.setError(null);

        // Parse ngày sinh sang Timestamp
        Date parsedDate = null;
        try {
            parsedDate = dateFormatter.parse(dobStr);
        } catch (ParseException ignored) {}
        final Timestamp dobTimestamp = new Timestamp(parsedDate != null ? parsedDate : new Date());
        final int age = calculateAge(calendarDob.getTime());

        setLoading(true);

        // Phân nhánh: Nếu editingUserId == null -> Thêm mới. Ngược lại -> Cập nhật (Sửa)
        if (editingUserId == null) {
            // === CHẾ ĐỘ THÊM MỚI ===
            if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                setLoading(false);
                binding.tilEmail.setError("Email không hợp lệ");
                binding.edtEmail.requestFocus();
                return;
            }
            binding.tilEmail.setError(null);

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                setLoading(false);
                binding.tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
                binding.edtPassword.requestFocus();
                return;
            }
            binding.tilPassword.setError(null);

            createNewUserInFirebase(fullName, email, password, phone, dobTimestamp, address, role, status, age);

        } else {
            // === CHẾ ĐỘ CẬP NHẬT (SỬA) ===
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("name", fullName);
            updateMap.put("phone", phone);
            updateMap.put("address", address);
            updateMap.put("date", dobTimestamp);
            updateMap.put("role", role);
            updateMap.put("status", status);
            updateMap.put("age", age);

            FirebaseFirestore.getInstance().collection("users")
                    .document(editingUserId)
                    .update(updateMap)
                    .addOnSuccessListener(aVoid -> {
                        setLoading(false);
                        Toast.makeText(UserFormActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(UserFormActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void createNewUserInFirebase(String fullName, String email, String password,
                                         String phone, Timestamp dobTimestamp, String address,
                                         String role, String status, int age) {

        String secondaryAppName = "SecondaryAppForUserCreation";
        FirebaseAuth secondaryAuth;

        try {
            FirebaseApp existingApp = null;
            try {
                existingApp = FirebaseApp.getInstance(secondaryAppName);
            } catch (Exception ignored) {}

            if (existingApp != null) {
                existingApp.delete();
            }

            FirebaseApp secondaryApp = FirebaseApp.initializeApp(this, FirebaseApp.getInstance().getOptions(), secondaryAppName);
            secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Lỗi cấu hình khởi tạo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo tài khoản trên Auth phụ
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();

                        // Chuẩn bị dữ liệu lưu vào Firestore collection "users"
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", uid);
                        userMap.put("name", fullName);
                        userMap.put("email", email);
                        userMap.put("phone", phone);
                        userMap.put("address", address);
                        userMap.put("date", dobTimestamp);
                        userMap.put("role", role);
                        userMap.put("status", status);
                        userMap.put("avatarUrl", "");
                        userMap.put("createdAt", Timestamp.now());
                        userMap.put("age", age);

                        FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    setLoading(false);
                                    Toast.makeText(UserFormActivity.this, "Thêm người dùng thành công!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(UserFormActivity.this, "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        Toast.makeText(UserFormActivity.this, "Tạo tài khoản thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int calculateAge(Date birthDate) {
        Calendar dob = Calendar.getInstance();
        dob.setTime(birthDate);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return Math.max(age, 0);
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBarSaveUser.setVisibility(View.VISIBLE);
            binding.btnSaveUser.setEnabled(false);
        } else {
            binding.progressBarSaveUser.setVisibility(View.GONE);
            binding.btnSaveUser.setEnabled(true);
        }
    }

    private void setupEditMode(String userId) {
        binding.toolbarUserForm.setTitle("Cập nhật thông tin");
        binding.btnSaveUser.setText("Lưu thay đổi");
        binding.tilPassword.setVisibility(View.GONE); // Không sửa mật khẩu Auth bằng form này

        setLoading(true);
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        binding.edtFullName.setText(documentSnapshot.getString("name"));
                        binding.edtEmail.setText(documentSnapshot.getString("email"));
                        binding.edtEmail.setEnabled(false); // Khóa không cho sửa email định danh
                        binding.edtPhone.setText(documentSnapshot.getString("phone"));
                        binding.edtAddress.setText(documentSnapshot.getString("address"));

                        // Load ngày sinh (nếu có)
                        Timestamp dobTimestamp = documentSnapshot.getTimestamp("date");
                        if (dobTimestamp != null) {
                            Date date = dobTimestamp.toDate();
                            calendarDob.setTime(date);
                            binding.edtDob.setText(dateFormatter.format(date));
                        }

                        // Load Role
                        if ("manager".equals(documentSnapshot.getString("role"))) {
                            binding.rbManager.setChecked(true);
                        } else {
                            binding.rbEmployee.setChecked(true);
                        }

                        // Load Status
                        if ("Locked".equals(documentSnapshot.getString("status"))) {
                            binding.rbLocked.setChecked(true);
                        } else {
                            binding.rbNormal.setChecked(true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
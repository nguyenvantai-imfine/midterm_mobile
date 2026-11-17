package com.example.gk_mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.gk_mobile.databinding.ActivityMainBinding;
import com.example.gk_mobile.model.User;
import com.example.gk_mobile.repository.AvatarRepository;
import com.example.gk_mobile.ui.auth.LoginActivity;
import com.example.gk_mobile.ui.student.StudentListActivity;
import com.example.gk_mobile.ui.user.LoginHistoryActivity;
import com.example.gk_mobile.ui.user.UserListActivity;
import com.example.gk_mobile.utils.ImageUtils;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // Đổi ảnh đại diện (mọi role đều được phép)
    private final AvatarRepository avatarRepository = new AvatarRepository();
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private boolean avatarUpdating = false;
    private final ActivityResultLauncher<PickVisualMediaRequest> avatarPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onAvatarPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupUserHeader(currentUser);
        setupAvatarChange();
        setupNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageExecutor.shutdown();
    }

    // ===== Đổi ảnh đại diện =====

    private void setupAvatarChange() {
        binding.imgAvatar.setOnClickListener(v -> {
            if (avatarUpdating) return;
            avatarPicker.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    private void onAvatarPicked(Uri uri) {
        if (uri == null) return; // người dùng đóng bộ chọn ảnh

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
            return;
        }

        avatarUpdating = true;
        Toast.makeText(this, "Đang cập nhật ảnh đại diện...", Toast.LENGTH_SHORT).show();

        // Đọc và nén ảnh ở luồng nền để không làm đứng giao diện.
        imageExecutor.execute(() -> {
            try {
                String dataUri = ImageUtils.toAvatarDataUri(getContentResolver(), uri);
                runOnUiThread(() -> saveAvatar(firebaseUser.getUid(), dataUri));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    avatarUpdating = false;
                    Toast.makeText(this, "Không xử lý được ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveAvatar(String uid, String dataUri) {
        if (isFinishing() || isDestroyed()) return;

        avatarRepository.updateAvatar(uid, dataUri, new AvatarRepository.OnActionListener() {
            @Override
            public void onSuccess() {
                avatarUpdating = false;
                User current = SessionManager.getInstance().getCurrentUser();
                if (current != null) current.setAvatarUrl(dataUri);
                if (!isFinishing() && !isDestroyed()) {
                    loadAvatar(dataUri);
                    Toast.makeText(MainActivity.this, "Đã đổi ảnh đại diện!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                avatarUpdating = false;
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(MainActivity.this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** Hiển thị ảnh đại diện; hỗ trợ cả URL thường và chuỗi data URI (Base64). */
    private void loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return;
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .into(binding.imgAvatar);
    }

    private void setupUserHeader(User user) {
        binding.tvUserName.setText(user.getName() != null ? user.getName() : "Người dùng");
        binding.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        StringBuilder roleSb = new StringBuilder("Chức vụ: ");
        if (SessionManager.getInstance().isAdmin()) {
            roleSb.append("Admin");
        } else if (SessionManager.getInstance().isManager()) {
            roleSb.append("Manager (Quản lý)");
        } else {
            roleSb.append("Employee (Nhân viên)");
        }
        binding.tvUserRole.setText(roleSb.toString());

        loadAvatar(user.getAvatarUrl());
    }

    private void setupNavigation() {
        // Students card
        binding.cardStudents.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, StudentListActivity.class))
        );

        // Login history card
        binding.cardLoginHistory.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, LoginHistoryActivity.class))
        );

        // User management card (visible to admin only)
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        binding.cardUsers.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        binding.cardUsers.setOnClickListener(v -> {
            if (isAdmin) {
                //Toast.makeText(MainActivity.this, "Màn hình Quản lý Người dùng (Thành viên 1)", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, UserListActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "Chỉ Admin mới có quyền quản lý người dùng!", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            SessionManager.getInstance().logout();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

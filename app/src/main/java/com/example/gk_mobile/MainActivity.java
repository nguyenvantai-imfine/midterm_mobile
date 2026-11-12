package com.example.gk_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.gk_mobile.databinding.ActivityMainBinding;
import com.example.gk_mobile.model.User;
import com.example.gk_mobile.ui.auth.LoginActivity;
import com.example.gk_mobile.ui.student.StudentListActivity;
import com.example.gk_mobile.ui.user.LoginHistoryActivity;
import com.example.gk_mobile.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

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
        setupNavigation();
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

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .into(binding.imgAvatar);
        }
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
                Toast.makeText(MainActivity.this, "Màn hình Quản lý Người dùng (Thành viên 1)", Toast.LENGTH_SHORT).show();
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

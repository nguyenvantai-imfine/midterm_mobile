package com.example.gk_mobile.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gk_mobile.adapter.UserAdapter;
import com.example.gk_mobile.databinding.ActivityUserListBinding;
import com.example.gk_mobile.model.User;
import com.example.gk_mobile.repository.UserRepository;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {

    private ActivityUserListBinding binding;
    private UserAdapter adapter;
    private UserRepository userRepository;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.getInstance().isAdmin()) {
            Toast.makeText(this, "Chỉ Admin mới có quyền truy cập chức năng này!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRepository = new UserRepository();

        setupRecyclerView();
        setupSearch();
        setupActions();
        listenToUsersRealtime();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(this);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupActions() {
        binding.fabAddUser.setOnClickListener(v -> {
            //Toast.makeText(this, "Mở form thêm người dùng mới", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserListActivity.this, UserFormActivity.class);
            startActivity(intent);
        });
    }

    private void listenToUsersRealtime() {
        userListener = userRepository.getUsersRealtime(new UserRepository.OnUsersLoadedListener() {
            @Override
            public void onSuccess(List<User> userList) {
                adapter.setUsers(userList);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(UserListActivity.this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditUser(User user) {
        // Mở UserFormActivity ở chế độ Sửa, truyền theo UID của user cần sửa
        Intent intent = new Intent(UserListActivity.this, UserFormActivity.class);
        intent.putExtra("USER_ID", user.getUid()); // <-- BẮT BUỘC PHẢI CÓ DÒNG NÀY
        startActivity(intent);
    }

    @Override
    public void onDeleteUser(User user) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(user.getUid())) {
            Toast.makeText(this, "Không thể tự xóa tài khoản Admin đang đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa tài khoản \"" + user.getName() + "\" và toàn bộ lịch sử đăng nhập của họ?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteUserFromFirestore(user))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteUserFromFirestore(User user) {
        userRepository.deleteUserWithHistory(user.getUid(), new UserRepository.OnActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(UserListActivity.this, "Đã xóa người dùng " + user.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                String msg = e != null && e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
                Toast.makeText(UserListActivity.this, "Xóa thất bại: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(User user) {
        Intent intent = new Intent(this, LoginHistoryActivity.class);
        intent.putExtra("targetUid", user.getUid());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
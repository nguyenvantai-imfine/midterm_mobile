package com.example.gk_mobile.ui.user;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.gk_mobile.adapter.LoginHistoryAdapter;
import com.example.gk_mobile.databinding.ActivityLoginHistoryBinding;
import com.example.gk_mobile.model.LoginHistory;
import com.example.gk_mobile.repository.UserRepository;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public class LoginHistoryActivity extends AppCompatActivity {

    private ActivityLoginHistoryBinding binding;
    private UserRepository userRepository;
    private LoginHistoryAdapter adapter;
    private ListenerRegistration listenerRegistration;
    private String targetUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRepository = new UserRepository();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (getIntent().hasExtra("targetUid")) {
            targetUid = getIntent().getStringExtra("targetUid");
        } else if (SessionManager.getInstance().getCurrentUser() != null) {
            targetUid = SessionManager.getInstance().getCurrentUser().getUid();
        }

        if (targetUid == null || targetUid.isEmpty()) {
            Toast.makeText(this, "Không xác định được tài khoản!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        loadLoginHistory();
    }

    private void setupRecyclerView() {
        adapter = new LoginHistoryAdapter();
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewHistory.setAdapter(adapter);
    }

    private void loadLoginHistory() {
        binding.progressBar.setVisibility(View.VISIBLE);
        listenerRegistration = userRepository.getLoginHistoryRealtime(targetUid, new UserRepository.OnLoginHistoryLoadedListener() {
            @Override
            public void onSuccess(List<LoginHistory> historyList) {
                binding.progressBar.setVisibility(View.GONE);
                adapter.setHistoryList(historyList);
                binding.tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginHistoryActivity.this, "Lỗi tải lịch sử đăng nhập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}

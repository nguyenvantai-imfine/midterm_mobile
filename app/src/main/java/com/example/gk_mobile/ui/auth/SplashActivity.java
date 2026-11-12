package com.example.gk_mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gk_mobile.MainActivity;
import com.example.gk_mobile.databinding.ActivitySplashBinding;
import com.example.gk_mobile.model.User;
import com.example.gk_mobile.utils.Constants;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkSession();
    }

    private void checkSession() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            navigateToLogin();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(fbUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUid(documentSnapshot.getId());

                            if (Constants.isStatusLocked(user.getStatus())) {
                                FirebaseAuth.getInstance().signOut();
                                Toast.makeText(SplashActivity.this, "Tài khoản của bạn đã bị khóa!", Toast.LENGTH_LONG).show();
                                navigateToLogin();
                                return;
                            }

                            SessionManager.getInstance().setCurrentUser(user);
                            navigateToMain();
                            return;
                        }
                    }
                    // User doc doesn't exist or is invalid
                    FirebaseAuth.getInstance().signOut();
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SplashActivity.this, "Lỗi kết nối cơ sở dữ liệu. Vui lòng thử lại sau!", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
    }

    private void navigateToLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }

    private void navigateToMain() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}

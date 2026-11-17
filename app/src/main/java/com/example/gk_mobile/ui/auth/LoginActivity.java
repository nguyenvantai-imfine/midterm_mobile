package com.example.gk_mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gk_mobile.MainActivity;
import com.example.gk_mobile.databinding.ActivityLoginBinding;
import com.example.gk_mobile.model.User;
import com.example.gk_mobile.repository.UserRepository;
import com.example.gk_mobile.utils.Constants;
import com.example.gk_mobile.utils.SessionManager;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRepository = new UserRepository();

        binding.btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.etEmail.setError("Vui lòng nhập Email");
            binding.etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email không đúng định dạng (ví dụ: example@gmail.com)");
            binding.etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.etPassword.setError("Vui lòng nhập mật khẩu");
            binding.etPassword.requestFocus();
            return;
        }

        setLoading(true);

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser != null) {
                        fetchUserAndRecordLogin(fbUser.getUid());
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String friendlyError = getFriendlyErrorMessage(e);
                    Toast.makeText(LoginActivity.this, friendlyError, Toast.LENGTH_LONG).show();
                });
    }

    private void fetchUserAndRecordLogin(String uid) {
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUid(documentSnapshot.getId());

                            if (Constants.isStatusLocked(user.getStatus())) {
                                FirebaseAuth.getInstance().signOut();
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, "Tài khoản của bạn đã bị khóa!", Toast.LENGTH_LONG).show();
                                return;
                            }

                            SessionManager.getInstance().setCurrentUser(user);

                            // Record login history
                            userRepository.recordLoginHistory(uid, new UserRepository.OnActionListener() {
                                @Override
                                public void onSuccess() {
                                    setLoading(false);
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onError(Exception e) {
                                    setLoading(false);
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                            return;
                        }
                    }
                    FirebaseAuth.getInstance().signOut();
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin tài khoản trên hệ thống!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối cơ sở dữ liệu. Vui lòng thử lại sau!", Toast.LENGTH_SHORT).show();
                });
    }

    private String getFriendlyErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
                return "Email không đúng định dạng!";
            } else if ("ERROR_WRONG_PASSWORD".equals(errorCode)) {
                return "Mật khẩu không chính xác!";
            }
            return "Email hoặc mật khẩu không chính xác!";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(errorCode)) {
                return "Tài khoản của bạn đã bị vô hiệu hóa!";
            }
            return "Tài khoản không tồn tại trên hệ thống!";
        } else if (e instanceof FirebaseNetworkException) {
            return "Lỗi kết nối mạng! Vui lòng kiểm tra lại Wifi/4G.";
        } else if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            if (errorCode != null) {
                switch (errorCode) {
                    case "ERROR_USER_NOT_FOUND":
                        return "Tài khoản không tồn tại trên hệ thống!";
                    case "ERROR_WRONG_PASSWORD":
                        return "Mật khẩu không chính xác!";
                    case "ERROR_INVALID_EMAIL":
                        return "Email không đúng định dạng!";
                    case "ERROR_USER_DISABLED":
                        return "Tài khoản của bạn đã bị vô hiệu hóa!";
                    case "ERROR_TOO_MANY_REQUESTS":
                        return "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau!";
                    case "ERROR_OPERATION_NOT_ALLOWED":
                        return "Phương thức đăng nhập này chưa được bật!";
                }
            }
        }

        // Check raw message strings for keywords
        String msg = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("user-not-found") || msg.contains("no user record")) {
            return "Tài khoản không tồn tại trên hệ thống!";
        } else if (msg.contains("wrong-password") || msg.contains("invalid-credential") || msg.contains("invalid credential")) {
            return "Email hoặc mật khẩu không chính xác!";
        } else if (msg.contains("invalid-email") || msg.contains("badly formatted")) {
            return "Email không đúng định dạng!";
        } else if (msg.contains("network") || msg.contains("connection")) {
            return "Lỗi kết nối mạng! Vui lòng kiểm tra lại Wifi/4G.";
        } else if (msg.contains("too-many-requests")) {
            return "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút!";
        } else if (msg.contains("user-disabled")) {
            return "Tài khoản đã bị vô hiệu hóa!";
        }

        return "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!";
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
    }
}

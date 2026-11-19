package com.example.gk_mobile.ui.common;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gk_mobile.databinding.DialogLoadingBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Lớp cha dùng chung cho các màn hình: hộp thoại xác nhận (MaterialAlertDialog),
 * hộp thoại loading và helper Toast/Snackbar để mọi màn hình hiển thị thông báo giống nhau.
 * Màn hình nào cần các tiện ích này chỉ việc extends BaseActivity thay cho AppCompatActivity.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private AlertDialog loadingDialog;
    private TextView loadingMessageView;

    // ===== Thông báo =====

    protected void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showLongToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showSnackbar(@NonNull View anchor, @NonNull String message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    // ===== Hộp thoại xác nhận =====

    /** Hỏi xác nhận trước khi làm việc nguy hiểm (ví dụ xóa). onConfirm chạy khi người dùng bấm nút đồng ý. */
    protected void showConfirmDialog(@NonNull String title, @NonNull String message,
                                     @NonNull String positiveText, @NonNull Runnable onConfirm) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> onConfirm.run())
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ===== Hộp thoại loading =====

    protected void showLoading(@NonNull String message) {
        if (isFinishing() || isDestroyed()) return;
        if (loadingDialog == null) {
            DialogLoadingBinding b = DialogLoadingBinding.inflate(getLayoutInflater());
            loadingMessageView = b.tvLoadingMessage;
            loadingDialog = new MaterialAlertDialogBuilder(this)
                    .setView(b.getRoot())
                    .setCancelable(false)
                    .create();
        }
        loadingMessageView.setText(message);
        if (!loadingDialog.isShowing()) loadingDialog.show();
    }

    protected void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        hideLoading(); // tránh rò cửa sổ khi Activity bị đóng lúc hộp thoại đang hiện
        super.onDestroy();
    }
}

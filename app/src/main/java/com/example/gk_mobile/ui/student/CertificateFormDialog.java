package com.example.gk_mobile.ui.student;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gk_mobile.databinding.DialogCertificateFormBinding;
import com.example.gk_mobile.model.Certificate;
import com.example.gk_mobile.utils.DateUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Hộp thoại thêm / sửa chứng chỉ. Dùng hộp thoại thay vì Activity mới để không phải khai báo thêm trong Manifest.
 * existing = null  => thêm mới; existing != null => sửa (giữ nguyên id).
 */
public class CertificateFormDialog {

    public interface OnSaveListener {
        void onSave(Certificate certificate);
    }

    private CertificateFormDialog() {}

    public static void show(AppCompatActivity activity, Certificate existing, OnSaveListener listener) {
        DialogCertificateFormBinding b = DialogCertificateFormBinding.inflate(LayoutInflater.from(activity));

        final Timestamp[] issueDate = {existing != null ? existing.getIssueDate() : null};
        final Timestamp[] expiryDate = {existing != null ? existing.getExpiryDate() : null};

        if (existing != null) {
            b.etCertName.setText(existing.getName());
            b.etCertIssuer.setText(existing.getIssuer());
            b.etCertNote.setText(existing.getNote());
        }
        b.etCertIssueDate.setText(DateUtils.formatShortDate(issueDate[0]));
        b.etCertExpiryDate.setText(DateUtils.formatShortDate(expiryDate[0]));

        b.etCertIssueDate.setOnClickListener(v ->
                pickDate(activity, "Chọn ngày cấp", issueDate[0], ts -> {
                    issueDate[0] = ts;
                    b.etCertIssueDate.setText(DateUtils.formatShortDate(ts));
                    b.tilCertIssueDate.setError(null);
                }));

        b.etCertExpiryDate.setOnClickListener(v ->
                pickDate(activity, "Chọn ngày hết hạn", expiryDate[0], ts -> {
                    expiryDate[0] = ts;
                    b.etCertExpiryDate.setText(DateUtils.formatShortDate(ts));
                    b.tilCertExpiryDate.setError(null);
                }));

        b.btnClearExpiry.setOnClickListener(v -> {
            expiryDate[0] = null;
            b.etCertExpiryDate.setText("");
            b.tilCertExpiryDate.setError(null);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(existing == null ? "Thêm chứng chỉ" : "Sửa chứng chỉ")
                .setView(b.getRoot())
                .setPositiveButton("Lưu", null) // gán listener bên dưới để kiểm tra dữ liệu mà không tự đóng hộp thoại
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = textOf(b.etCertName);
            String issuer = textOf(b.etCertIssuer);
            String note = textOf(b.etCertNote);

            boolean valid = true;
            b.tilCertName.setError(null);
            b.tilCertIssuer.setError(null);
            b.tilCertIssueDate.setError(null);
            b.tilCertExpiryDate.setError(null);

            if (name.isEmpty()) {
                b.tilCertName.setError("Vui lòng nhập tên chứng chỉ");
                valid = false;
            }
            if (issuer.isEmpty()) {
                b.tilCertIssuer.setError("Vui lòng nhập đơn vị cấp");
                valid = false;
            }
            if (issueDate[0] == null) {
                b.tilCertIssueDate.setError("Vui lòng chọn ngày cấp");
                valid = false;
            }
            if (issueDate[0] != null && expiryDate[0] != null
                    && expiryDate[0].compareTo(issueDate[0]) < 0) {
                b.tilCertExpiryDate.setError("Ngày hết hạn phải sau ngày cấp");
                valid = false;
            }
            if (!valid) return;

            Certificate result = new Certificate(
                    existing != null ? existing.getId() : null,
                    name, issuer, issueDate[0], expiryDate[0], note);
            dialog.dismiss();
            listener.onSave(result);
        }));

        dialog.show();
    }

    private static String textOf(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static void pickDate(AppCompatActivity activity, String title, Timestamp current, Consumer<Timestamp> callback) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker().setTitleText(title);
        if (current != null) {
            builder.setSelection(localToUtcMidnight(current.toDate().getTime()));
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection ->
                callback.accept(new Timestamp(new Date(utcMidnightToLocal(selection)))));
        picker.show(activity.getSupportFragmentManager(), "certificateDatePicker");
    }

    // MaterialDatePicker làm việc với 00:00 UTC; đổi qua lại để ngày hiển thị không bị lệch múi giờ.
    private static long localToUtcMidnight(long localMillis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localMillis);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    private static long utcMidnightToLocal(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local.getTimeInMillis();
    }
}

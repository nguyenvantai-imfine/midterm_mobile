package com.example.gk_mobile.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gk_mobile.databinding.ItemCertificateBinding;
import com.example.gk_mobile.model.Certificate;
import com.example.gk_mobile.utils.DateUtils;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.ViewHolder> {

    /** Số ngày trước hạn để hiển thị nhãn "Sắp hết hạn". */
    private static final long EXPIRING_SOON_DAYS = 30;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    public interface OnCertificateActionListener {
        void onEdit(Certificate certificate);
        void onDelete(Certificate certificate);
    }

    private final List<Certificate> certificates = new ArrayList<>();
    private final OnCertificateActionListener listener;
    private boolean canManage;

    public CertificateAdapter(boolean canManage, OnCertificateActionListener listener) {
        this.canManage = canManage;
        this.listener = listener;
    }

    public void setCertificates(List<Certificate> newList) {
        certificates.clear();
        if (newList != null) certificates.addAll(newList);
        notifyDataSetChanged();
    }

    public void setCanManage(boolean canManage) {
        this.canManage = canManage;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(certificates.get(position));
    }

    @Override
    public int getItemCount() {
        return certificates.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCertificateBinding binding;

        ViewHolder(ItemCertificateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Certificate c) {
            binding.tvCertName.setText(c.getName());
            binding.tvCertIssuer.setText(String.format("Đơn vị cấp: %s", c.getIssuer()));

            String issue = DateUtils.formatShortDate(c.getIssueDate());
            String expiry = c.getExpiryDate() != null
                    ? DateUtils.formatShortDate(c.getExpiryDate())
                    : "Không thời hạn";
            binding.tvCertDates.setText(String.format("Cấp: %s | Hết hạn: %s", issue, expiry));

            String note = c.getNote();
            if (note == null || note.trim().isEmpty()) {
                binding.tvCertNote.setVisibility(View.GONE);
            } else {
                binding.tvCertNote.setVisibility(View.VISIBLE);
                binding.tvCertNote.setText(String.format("Ghi chú: %s", note));
            }

            bindStatus(c.getExpiryDate());

            binding.layoutCertActions.setVisibility(canManage ? View.VISIBLE : View.GONE);
            binding.btnCertEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(c);
            });
            binding.btnCertDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(c);
            });
        }

        private void bindStatus(Timestamp expiryDate) {
            String text;
            int color;
            if (expiryDate == null) {
                text = "Không thời hạn";
                color = Color.parseColor("#1976D2");
            } else {
                long remaining = expiryDate.toDate().getTime() - System.currentTimeMillis();
                if (remaining < 0) {
                    text = "Đã hết hạn";
                    color = Color.parseColor("#D32F2F");
                } else if (remaining <= EXPIRING_SOON_DAYS * DAY_MILLIS) {
                    text = "Sắp hết hạn";
                    color = Color.parseColor("#F57C00");
                } else {
                    text = "Còn hiệu lực";
                    color = Color.parseColor("#2E7D32");
                }
            }
            binding.tvCertStatus.setText(text);
            binding.tvCertStatus.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }
}

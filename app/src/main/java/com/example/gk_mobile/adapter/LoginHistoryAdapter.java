package com.example.gk_mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gk_mobile.databinding.ItemLoginHistoryBinding;
import com.example.gk_mobile.model.LoginHistory;
import com.example.gk_mobile.utils.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class LoginHistoryAdapter extends RecyclerView.Adapter<LoginHistoryAdapter.HistoryViewHolder> {

    private List<LoginHistory> historyList = new ArrayList<>();

    public void setHistoryList(List<LoginHistory> list) {
        this.historyList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoginHistoryBinding binding = ItemLoginHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(historyList.get(position));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoginHistoryBinding binding;

        public HistoryViewHolder(ItemLoginHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LoginHistory history) {
            binding.tvLoginTime.setText(DateUtils.formatTimestamp(history.getLoginAt()));
            binding.tvDevice.setText(String.format("Thiết bị: %s", history.getDevice() != null ? history.getDevice() : "N/A"));
        }
    }
}

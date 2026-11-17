package com.example.gk_mobile.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gk_mobile.databinding.ItemUserBinding;
import com.example.gk_mobile.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onEditUser(User user);
        void onDeleteUser(User user);
        void onItemClick(User user);
    }

    private final List<User> fullList = new ArrayList<>();
    private final List<User> filteredList = new ArrayList<>();
    private final OnUserActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private String currentQuery = "";

    public UserAdapter(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        fullList.clear();
        if (users != null) {
            fullList.addAll(users);
        }
        filter(currentQuery);
    }

    public void filter(String query) {
        currentQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        filteredList.clear();

        if (currentQuery.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            for (User u : fullList) {
                String name = u.getName() != null ? u.getName().toLowerCase(Locale.getDefault()) : "";
                String email = u.getEmail() != null ? u.getEmail().toLowerCase(Locale.getDefault()) : "";
                String phone = u.getPhone() != null ? u.getPhone().toLowerCase(Locale.getDefault()) : "";
                String role = u.getRole() != null ? u.getRole().toLowerCase(Locale.getDefault()) : "";
                String address = u.getAddress() != null ? u.getAddress().toLowerCase(Locale.getDefault()) : "";
                String dob = u.getDate() != null ? dateFormat.format(u.getDate().toDate()) : "";

                if (name.contains(currentQuery)
                        || email.contains(currentQuery)
                        || phone.contains(currentQuery)
                        || role.contains(currentQuery)
                        || address.contains(currentQuery)
                        || dob.contains(currentQuery)) {
                    filteredList.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(filteredList.get(position));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            // Tên, Email, SĐT
            binding.tvUserName.setText(user.getName() != null && !user.getName().isEmpty()
                    ? user.getName() : "Chưa đặt tên");
            binding.tvUserEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty()
                    ? user.getEmail() : "Chưa có email");
            binding.tvUserPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                    ? user.getPhone() : "Chưa có SĐT");

            // Ngày sinh (Timestamp -> dd/MM/yyyy)
            if (user.getDate() != null) {
                binding.tvUserDob.setText(dateFormat.format(user.getDate().toDate()));
            } else {
                binding.tvUserDob.setText("Chưa cập nhật ngày sinh");
            }

            // Ngày sinh & Tự động tính tuổi
            if (user.getDate() != null) {
                Date birthDate = user.getDate().toDate();
                binding.tvUserDob.setText(dateFormat.format(birthDate));

                int age = calculateAge(birthDate);
                binding.tvUserAge.setText("• " + age + " tuổi");
            } else {
                binding.tvUserDob.setText("Chưa cập nhật ngày sinh");
                binding.tvUserAge.setText(""); // Ẩn text tuổi nếu chưa có ngày sinh
            }

            // Địa chỉ
            binding.tvUserAddress.setText(user.getAddress() != null && !user.getAddress().isEmpty()
                    ? user.getAddress() : "Chưa cập nhật địa chỉ");

            // Role
            String role = user.getRole() != null ? user.getRole().toUpperCase(Locale.getDefault()) : "EMPLOYEE";
            binding.tvUserRole.setText(role);

            // Trạng thái (Normal / Locked)
            String status = user.getStatus() != null ? user.getStatus() : "Normal";
            binding.tvUserStatus.setText("• " + status);
            if ("Locked".equalsIgnoreCase(status)) {
                binding.tvUserStatus.setTextColor(Color.parseColor("#D32F2F"));
            } else {
                binding.tvUserStatus.setTextColor(Color.parseColor("#2E7D32"));
            }

            // Sự kiện click
            binding.btnEditUser.setOnClickListener(v -> {
                if (listener != null) listener.onEditUser(user);
            });

            binding.btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteUser(user);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(user);
            });
        }
    }

    private int calculateAge(Date birthDate) {
        Calendar dob = Calendar.getInstance();
        dob.setTime(birthDate);

        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        // Nếu chưa tới tháng sinh hoặc cùng tháng nhưng chưa tới ngày sinh thì trừ 1 tuổi
        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)
                || (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return Math.max(age, 0);
    }
}
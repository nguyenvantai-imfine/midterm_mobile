package com.example.gk_mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gk_mobile.databinding.ItemStudentBinding;
import com.example.gk_mobile.model.Student;
import com.example.gk_mobile.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> students = new ArrayList<>();
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onItemClick(Student student);
        void onEditClick(Student student);
        void onDeleteClick(Student student);
    }

    public StudentAdapter(OnStudentClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        holder.bind(students.get(position));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;

        public StudentViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Student student) {
            binding.tvStudentCode.setText(student.getStudentCode());
            binding.tvStudentName.setText(student.getName());
            binding.tvClassName.setText(student.getClassName());
            binding.tvFacultyAndEmail.setText(String.format("%s | %s", student.getFaculty(), student.getEmail()));

            // Manage visibility based on Role
            boolean canManage = SessionManager.getInstance().canManageStudents();
            binding.layoutActions.setVisibility(canManage ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(student);
            });

            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(student);
            });

            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(student);
            });
        }
    }
}

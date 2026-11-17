package com.example.gk_mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gk_mobile.databinding.ItemStudentBinding;
import com.example.gk_mobile.model.Student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    public interface OnStudentActionListener {
        void onItemClick(Student student);
        void onEditStudent(Student student);
        void onDeleteStudent(Student student);
    }

    private final List<Student> fullList = new ArrayList<>();
    private final List<Student> filteredList = new ArrayList<>();
    private final OnStudentActionListener listener;
    private String currentQuery = "";
    private boolean canManage = true; // false => ẩn nút Sửa/Xóa (employee chỉ xem)

    public StudentAdapter(OnStudentActionListener listener) {
        this.listener = listener;
    }

    public void setCanManage(boolean canManage) {
        this.canManage = canManage;
        notifyDataSetChanged();
    }

    public void setStudents(List<Student> students) {
        fullList.clear();
        if (students != null) {
            fullList.addAll(students);
        }
        filter(currentQuery);
    }

    public void filter(String query) {
        currentQuery = query == null ? "" : query.trim();
        filteredList.clear();
        String lowerQuery = currentQuery.toLowerCase(Locale.getDefault());

        if (lowerQuery.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            for (Student s : fullList) {
                String name = s.getName() != null ? s.getName().toLowerCase(Locale.getDefault()) : "";
                String code = s.getStudentCode() != null ? s.getStudentCode().toLowerCase(Locale.getDefault()) : "";
                String faculty = s.getFaculty() != null ? s.getFaculty().toLowerCase(Locale.getDefault()) : "";
                String className = s.getClassName() != null ? s.getClassName().toLowerCase(Locale.getDefault()) : "";

                if (name.contains(lowerQuery) || code.contains(lowerQuery) ||
                        faculty.contains(lowerQuery) || className.contains(lowerQuery)) {
                    filteredList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void sortMultiCriteria(boolean ascending) {
        filteredList.clear();
        filteredList.addAll(fullList);

        Collections.sort(filteredList, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                String f1 = s1.getFaculty() != null ? s1.getFaculty() : "";
                String f2 = s2.getFaculty() != null ? s2.getFaculty() : "";
                int comp = f1.compareToIgnoreCase(f2);
                if (comp != 0) return ascending ? comp : -comp;

                comp = Integer.compare(s1.getAge(), s2.getAge());
                if (comp != 0) return ascending ? comp : -comp;

                String c1 = s1.getClassName() != null ? s1.getClassName() : "";
                String c2 = s2.getClassName() != null ? s2.getClassName() : "";
                comp = c1.compareToIgnoreCase(c2);
                if (comp != 0) return ascending ? comp : -comp;

                String g1 = s1.getGender() != null ? s1.getGender() : "";
                String g2 = s2.getGender() != null ? s2.getGender() : "";
                comp = g1.compareToIgnoreCase(g2);
                if (comp != 0) return ascending ? comp : -comp;

                String code1 = s1.getStudentCode() != null ? s1.getStudentCode() : "";
                String code2 = s2.getStudentCode() != null ? s2.getStudentCode() : "";
                comp = code1.compareToIgnoreCase(code2);
                return ascending ? comp : -comp;
            }
        });
        notifyDataSetChanged();
    }

    /**
     * Sắp xếp với 2 nhóm độc lập:
     * - isAscendingString: true (A -> Z), false (Z -> A) cho các trường chuỗi
     * - isAscendingNumber: true (Bé đến lớn), false (Lớn đến bé) cho trường số (tuổi)
     */
    public void sortWithTwoGroups(boolean useFaculty, boolean useClassName, boolean useName, boolean useAge, boolean useGender, boolean useStudentCode, boolean isAscendingString, boolean isAscendingNumber) {
        filteredList.clear();
        filteredList.addAll(fullList);

        Collections.sort(filteredList, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                int comp = 0;

                // 1. Khoa (Nhóm chữ cái)
                if (useFaculty) {
                    String f1 = s1.getFaculty() != null ? s1.getFaculty() : "";
                    String f2 = s2.getFaculty() != null ? s2.getFaculty() : "";
                    comp = f1.compareToIgnoreCase(f2);
                    if (comp != 0) return isAscendingString ? comp : -comp;
                }

                // 2. Lớp (Nhóm chữ cái)
                if (useClassName) {
                    String c1 = s1.getClassName() != null ? s1.getClassName() : "";
                    String c2 = s2.getClassName() != null ? s2.getClassName() : "";
                    comp = c1.compareToIgnoreCase(c2);
                    if (comp != 0) return isAscendingString ? comp : -comp;
                }

                // 3. Tên (Nhóm chữ cái)
                if (useName) {
                    String n1 = s1.getName() != null ? s1.getName() : "";
                    String n2 = s2.getName() != null ? s2.getName() : "";
                    comp = n1.compareToIgnoreCase(n2);
                    if (comp != 0) return isAscendingString ? comp : -comp;
                }

                // 4. Tuổi (Nhóm số)
                if (useAge) {
                    comp = Integer.compare(s1.getAge(), s2.getAge());
                    if (comp != 0) return isAscendingNumber ? comp : -comp;
                }

                // 5. Giới tính (Nhóm chữ cái)
                if (useGender) {
                    String g1 = s1.getGender() != null ? s1.getGender() : "";
                    String g2 = s2.getGender() != null ? s2.getGender() : "";
                    comp = g1.compareToIgnoreCase(g2);
                    if (comp != 0) return isAscendingString ? comp : -comp;
                }

                // 6. Mã sinh viên (Nhóm chữ cái)
                if (useStudentCode) {
                    String code1 = s1.getStudentCode() != null ? s1.getStudentCode() : "";
                    String code2 = s2.getStudentCode() != null ? s2.getStudentCode() : "";
                    comp = code1.compareToIgnoreCase(code2);
                    if (comp != 0) return isAscendingString ? comp : -comp;
                }

                return 0;
            }
        });
        notifyDataSetChanged();
    }

    /**
     * Lọc đa tiêu chí trên danh sách gốc (fullList)
     */
    /**
     * Lọc đa tiêu chí với cơ chế chứa chuỗi (contains) linh hoạt cho tất cả các trường
     */
    public void filterMultiCriteria(String facultyFilter, String classFilter, String nameFilter,
                                    String ageFilter, String genderFilter, String studentCodeFilter) {
        filteredList.clear();

        // Chuẩn hóa từ khóa về chữ thường
        String fQuery = facultyFilter != null ? facultyFilter.trim().toLowerCase(Locale.getDefault()) : "";
        String cQuery = classFilter != null ? classFilter.trim().toLowerCase(Locale.getDefault()) : "";
        String nQuery = nameFilter != null ? nameFilter.trim().toLowerCase(Locale.getDefault()) : "";
        String aQuery = ageFilter != null ? ageFilter.trim() : "";
        String gQuery = genderFilter != null ? genderFilter.trim().toLowerCase(Locale.getDefault()) : "";
        String sQuery = studentCodeFilter != null ? studentCodeFilter.trim().toLowerCase(Locale.getDefault()) : "";

        for (Student s : fullList) {
            boolean match = true;

            // 1. Lọc theo Khoa (Chứa chuỗi)
            if (!fQuery.isEmpty()) {
                String fac = s.getFaculty() != null ? s.getFaculty().toLowerCase(Locale.getDefault()) : "";
                if (!fac.contains(fQuery)) match = false;
            }

            // 2. Lọc theo Lớp (Chứa chuỗi - ví dụ gõ "DTH" hoặc "01" đều ra)
            if (match && !cQuery.isEmpty()) {
                String cls = s.getClassName() != null ? s.getClassName().toLowerCase(Locale.getDefault()) : "";
                if (!cls.contains(cQuery)) match = false;
            }

            // 3. Lọc theo Tên (Chứa chuỗi - gõ một phần tên vẫn tìm thấy)
            if (match && !nQuery.isEmpty()) {
                String name = s.getName() != null ? s.getName().toLowerCase(Locale.getDefault()) : "";
                if (!name.contains(nQuery)) match = false;
            }

            // 4. Lọc theo Tuổi (Khớp chính xác số tuổi)
            if (match && !aQuery.isEmpty()) {
                try {
                    int targetAge = Integer.parseInt(aQuery);
                    if (s.getAge() != targetAge) match = false;
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu nhập sai định dạng số
                }
            }

            // 5. Lọc theo Giới tính
            if (match && !gQuery.isEmpty() && !gQuery.equals("tất cả")) {
                String gender = s.getGender() != null ? s.getGender().toLowerCase(Locale.getDefault()) : "";
                if (!gender.equals(gQuery)) match = false;
            }

            // 6. Lọc theo Mã sinh viên (Chứa chuỗi - ví dụ gõ "524" sẽ ra các mã chứa "524")
            if (match && !sQuery.isEmpty()) {
                String code = s.getStudentCode() != null ? s.getStudentCode().toLowerCase(Locale.getDefault()) : "";
                if (!code.contains(sQuery)) match = false;
            }

            if (match) {
                filteredList.add(s);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = filteredList.get(position);
        holder.binding.tvStudentName.setText(student.getName());
        holder.binding.tvStudentCode.setText(student.getStudentCode());
        holder.binding.tvClassName.setText(student.getClassName());

        String facultyStr = student.getFaculty() != null ? student.getFaculty() : "";
        String emailStr = student.getEmail() != null ? student.getEmail() : "";
        holder.binding.tvFacultyAndEmail.setText(facultyStr + " | " + emailStr);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(student);
        });

        int actionVisibility = canManage ? View.VISIBLE : View.GONE;
        holder.binding.btnEdit.setVisibility(actionVisibility);
        holder.binding.btnDelete.setVisibility(actionVisibility);

        holder.binding.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditStudent(student);
        });

        holder.binding.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteStudent(student);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;
        public StudentViewHolder(@NonNull ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
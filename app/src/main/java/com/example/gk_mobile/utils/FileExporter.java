package com.example.gk_mobile.utils;

import android.content.Context;
import android.net.Uri;
import com.example.gk_mobile.model.Student;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileExporter {

    /**
     * Exports list of students to a CSV file at destination Uri.
     * Includes UTF-8 BOM (\uFEFF) so Microsoft Excel displays Vietnamese characters properly.
     */
    public static boolean exportStudentsToCsv(Context context, Uri destinationUri, List<Student> studentList) {
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

            // Write UTF-8 BOM for Excel compatibility
            writer.write('\uFEFF');

            // Header
            writer.write("Mã SV,Họ và tên,Tuổi,Giới tính,Lớp,Khoa,Email,Số điện thoại,Địa chỉ\n");

            // Rows
            for (Student s : studentList) {
                StringBuilder sb = new StringBuilder();
                sb.append(escapeCsv(s.getStudentCode())).append(",");
                sb.append(escapeCsv(s.getName())).append(",");
                sb.append(s.getAge()).append(",");
                sb.append(escapeCsv(s.getGender())).append(",");
                sb.append(escapeCsv(s.getClassName())).append(",");
                sb.append(escapeCsv(s.getFaculty())).append(",");
                sb.append(escapeCsv(s.getEmail())).append(",");
                sb.append(escapeCsv(s.getPhone())).append(",");
                sb.append(escapeCsv(s.getAddress())).append("\n");

                writer.write(sb.toString());
            }

            writer.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}

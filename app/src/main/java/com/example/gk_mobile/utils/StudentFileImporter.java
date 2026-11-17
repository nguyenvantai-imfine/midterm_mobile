package com.example.gk_mobile.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Import danh sách sinh viên từ file CSV hoặc XLSX (không dùng thư viện ngoài).
 *
 * Thứ tự cột (dòng đầu là tiêu đề, bị bỏ qua):
 * Mã SV | Họ và tên | Tuổi | Giới tính | Lớp | Khoa | Email | Số điện thoại | Địa chỉ
 */
public class StudentFileImporter {

    private static final String TAG = "StudentFileImporter";
    private static final int COLS = 9;
    private static final int BATCH_SIZE = 400; // WriteBatch tối đa 500 thao tác

    public interface ImportCallback {
        void onProgress(String statusMessage);
        void onSuccess(int successCount, int duplicateCount, int errorCount);
        void onError(String errorMessage);
    }

    // ------------------------------------------------------------------
    // Điểm vào
    // ------------------------------------------------------------------

    public static void importFile(Context context, Uri uri, boolean isExcel, ImportCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onProgress("Đang đọc file..."));

                List<String[]> rows;
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) {
                        mainHandler.post(() -> callback.onError("Không thể mở file."));
                        return;
                    }
                    rows = isExcel ? parseXlsxRows(inputStream) : parseCsvRows(inputStream);
                }

                if (rows.size() <= 1) {
                    mainHandler.post(() -> callback.onError("File không có dữ liệu hoặc sai định dạng."));
                    return;
                }

                // Bỏ dòng tiêu đề (i = 0), kiểm tra từng dòng dữ liệu
                List<Map<String, Object>> students = new ArrayList<>();
                int invalidCount = 0;
                for (int i = 1; i < rows.size(); i++) {
                    String[] row = rows.get(i);
                    if (isBlankRow(row)) continue; // dòng trống không tính là lỗi
                    Map<String, Object> student = rowToStudent(row);
                    if (student == null) {
                        invalidCount++;
                    } else {
                        students.add(student);
                    }
                }

                if (students.isEmpty()) {
                    mainHandler.post(() -> callback.onError("Không có dòng dữ liệu hợp lệ (cần có Mã SV và Họ tên)."));
                    return;
                }

                mainHandler.post(() -> callback.onProgress("Đang kiểm tra trùng mã và ghi dữ liệu lên Firestore..."));
                writeStudents(students, invalidCount, callback, mainHandler);

            } catch (Exception e) {
                Log.e(TAG, "Import error", e);
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                mainHandler.post(() -> callback.onError("Lỗi đọc file: " + msg));
            }
        });
        executor.shutdown(); // tác vụ đã gửi vẫn chạy xong, luồng được giải phóng sau đó
    }

    // ------------------------------------------------------------------
    // CSV
    // ------------------------------------------------------------------

    // Package-private để CertificateFileImporter dùng lại bộ đọc CSV này.
    static List<String[]> parseCsvRows(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1); // bỏ BOM
        }
        return parseCsvText(text);
    }

    /** Bộ đọc CSV hỗ trợ ô đặt trong dấu ngoặc kép, dấu phẩy trong ô, xuống dòng trong ô. */
    static List<String[]> parseCsvText(String text) {
        // Excel ở một số vùng lưu CSV với dấu ';' thay vì ','
        int firstLineEnd = text.indexOf('\n');
        String firstLine = firstLineEnd >= 0 ? text.substring(0, firstLineEnd) : text;
        char delimiter = count(firstLine, ';') > count(firstLine, ',') ? ';' : ',';

        List<String[]> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"'); // "" -> "
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == delimiter) {
                current.add(field.toString());
                field.setLength(0);
            } else if (ch == '\r') {
                // bỏ qua
            } else if (ch == '\n') {
                current.add(field.toString());
                field.setLength(0);
                rows.add(toRow(current));
                current.clear();
            } else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(toRow(current));
        }
        return rows;
    }

    private static int count(String s, char c) {
        int k = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) k++;
        return k;
    }

    private static String[] toRow(List<String> cells) {
        String[] row = new String[Math.max(COLS, cells.size())];
        Arrays.fill(row, "");
        for (int i = 0; i < cells.size(); i++) row[i] = cells.get(i);
        return row;
    }

    // ------------------------------------------------------------------
    // XLSX (đọc file zip + XML bằng thư viện có sẵn của Android)
    // ------------------------------------------------------------------

    // Package-private để CertificateFileImporter dùng lại bộ đọc XLSX này.
    static List<String[]> parseXlsxRows(InputStream inputStream) throws Exception {
        List<String> sharedStrings = new ArrayList<>();
        List<String[]> rawRows = null;
        boolean foundSheet1 = false;

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("xl/sharedStrings.xml")) {
                    sharedStrings = parseSharedStrings(new NonClosingStream(zis));
                } else if (name.equals("xl/worksheets/sheet1.xml")) {
                    rawRows = parseSheetRaw(new NonClosingStream(zis));
                    foundSheet1 = true;
                } else if (!foundSheet1 && rawRows == null
                        && name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
                    rawRows = parseSheetRaw(new NonClosingStream(zis)); // sheet đầu tiên nếu không có sheet1.xml
                }
            }
        }
        if (rawRows == null) return new ArrayList<>();

        // Giải mã giá trị ô sau khi đã đọc xong sharedStrings (thứ tự trong file zip không cố định)
        List<String[]> rows = new ArrayList<>();
        for (String[] raw : rawRows) {
            String[] row = new String[raw.length];
            for (int i = 0; i < raw.length; i++) {
                row[i] = decodeCell(raw[i], sharedStrings);
            }
            rows.add(row);
        }
        return rows;
    }

    /** Bộ đọc XML có thể đóng luồng khi đọc hết; bọc lại để không đóng ZipInputStream giữa chừng. */
    private static class NonClosingStream extends FilterInputStream {
        NonClosingStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // không đóng luồng zip
        }
    }

    private static String decodeCell(String raw, List<String> sharedStrings) {
        if (raw == null || raw.length() < 2) return "";
        String value = raw.substring(2);
        switch (raw.charAt(0)) {
            case 's':
                try {
                    int index = Integer.parseInt(value.trim());
                    return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
                } catch (NumberFormatException e) {
                    return "";
                }
            case 'n':
                return plainNumber(value);
            default:
                return value;
        }
    }

    /** "20.0" -> "20", "5.2100123E7" -> "52100123". */
    private static String plainNumber(String s) {
        try {
            return new BigDecimal(s.trim()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return s;
        }
    }

    /** Mỗi <si> là một chuỗi; ghép tất cả <t> bên trong (văn bản nhiều định dạng). */
    private static List<String> parseSharedStrings(InputStream is) throws Exception {
        List<String> strings = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(is, "UTF-8");

        StringBuilder current = new StringBuilder();
        boolean inSi = false, inT = false, inPhonetic = false;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String tag = parser.getName();
            if (event == XmlPullParser.START_TAG) {
                if ("si".equals(tag)) {
                    inSi = true;
                    current.setLength(0);
                } else if ("rPh".equals(tag)) {
                    inPhonetic = true; // bỏ phần phiên âm (furigana)
                } else if ("t".equals(tag) && inSi && !inPhonetic) {
                    inT = true;
                }
            } else if (event == XmlPullParser.TEXT) {
                if (inT) current.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                if ("t".equals(tag)) {
                    inT = false;
                } else if ("rPh".equals(tag)) {
                    inPhonetic = false;
                } else if ("si".equals(tag)) {
                    inSi = false;
                    strings.add(current.toString());
                }
            }
            event = parser.next();
        }
        return strings;
    }

    /**
     * Đọc sheet, đặt mỗi ô đúng cột theo thuộc tính r="B3" (ô trống không làm lệch cột).
     * Giá trị thô có tiền tố loại: "s:" chuỗi dùng chung, "n:" số, "i:" chuỗi nội tuyến, "v:" khác.
     */
    private static List<String[]> parseSheetRaw(InputStream is) throws Exception {
        List<String[]> rows = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(is, "UTF-8");

        String[] currentRow = null;
        int colIndex = -1;
        String cellType = null;
        StringBuilder value = new StringBuilder();
        boolean inV = false, inT = false, inIs = false;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String tag = parser.getName();
            if (event == XmlPullParser.START_TAG) {
                if ("row".equals(tag)) {
                    currentRow = new String[COLS];
                    Arrays.fill(currentRow, "");
                    colIndex = -1;
                } else if ("c".equals(tag)) {
                    cellType = parser.getAttributeValue(null, "t");
                    String ref = parser.getAttributeValue(null, "r");
                    colIndex = ref != null ? columnIndex(ref) : colIndex + 1;
                    value.setLength(0);
                } else if ("v".equals(tag)) {
                    inV = true;
                } else if ("is".equals(tag)) {
                    inIs = true;
                } else if ("t".equals(tag) && inIs) {
                    inT = true;
                }
            } else if (event == XmlPullParser.TEXT) {
                if (inV || inT) value.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                if ("v".equals(tag)) {
                    inV = false;
                } else if ("t".equals(tag)) {
                    inT = false;
                } else if ("is".equals(tag)) {
                    inIs = false;
                } else if ("c".equals(tag)) {
                    if (currentRow != null && colIndex >= 0 && colIndex < COLS && value.length() > 0) {
                        currentRow[colIndex] = typePrefix(cellType) + value;
                    }
                } else if ("row".equals(tag)) {
                    if (currentRow != null) rows.add(currentRow);
                    currentRow = null;
                }
            }
            event = parser.next();
        }
        return rows;
    }

    private static String typePrefix(String cellType) {
        if ("s".equals(cellType)) return "s:";
        if ("inlineStr".equals(cellType)) return "i:";
        if (cellType == null || "n".equals(cellType)) return "n:";
        return "v:"; // str (kết quả công thức), b (boolean), e (lỗi)...
    }

    /** "A1" -> 0, "B7" -> 1, "AA3" -> 26. */
    private static int columnIndex(String cellRef) {
        int index = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                index = index * 26 + (ch - 'A' + 1);
            } else if (ch >= 'a' && ch <= 'z') {
                index = index * 26 + (ch - 'a' + 1);
            } else {
                break;
            }
        }
        return index - 1;
    }

    // ------------------------------------------------------------------
    // Chuyển một dòng thành dữ liệu sinh viên
    // ------------------------------------------------------------------

    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) return false;
        }
        return true;
    }

    /** Trả về null nếu dòng không hợp lệ (thiếu mã SV / họ tên, hoặc tuổi không phải số). */
    private static Map<String, Object> rowToStudent(String[] c) {
        String studentCode = cell(c, 0);
        String name = cell(c, 1);
        if (studentCode.isEmpty() || name.isEmpty()) return null;

        int age = 0;
        String ageText = cell(c, 2);
        if (!ageText.isEmpty()) {
            try {
                age = (int) Double.parseDouble(ageText);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String phone = cell(c, 7);
        // Excel lưu số điện thoại dạng số nên mất số 0 đầu: 901234567 -> 0901234567
        if (phone.matches("\\d{9}")) {
            phone = "0" + phone;
        }

        Map<String, Object> student = new HashMap<>();
        student.put("studentCode", studentCode);
        student.put("name", name);
        student.put("age", age);
        student.put("gender", cell(c, 3));
        student.put("className", cell(c, 4));
        student.put("faculty", cell(c, 5));
        student.put("email", cell(c, 6));
        student.put("phone", phone);
        student.put("address", cell(c, 8));
        student.put("createdAt", Timestamp.now());
        return student;
    }

    private static String cell(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index].trim() : "";
    }

    // ------------------------------------------------------------------
    // Ghi lên Firestore
    // ------------------------------------------------------------------

    /**
     * Bỏ qua mã đã có trên Firestore và mã lặp trong file (tính vào duplicateCount),
     * ghi phần còn lại theo từng WriteBatch, chỉ báo kết quả khi TẤT CẢ batch đã xong.
     */
    private static void writeStudents(List<Map<String, Object>> students, int invalidCount,
                                      ImportCallback callback, Handler mainHandler) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").get().addOnSuccessListener(snapshot -> {
            Set<String> existingCodes = new HashSet<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                existingCodes.add(doc.getId());
            }

            List<Map<String, Object>> toWrite = new ArrayList<>();
            int duplicateCount = 0;
            for (Map<String, Object> student : students) {
                String code = (String) student.get("studentCode");
                if (existingCodes.contains(code)) {
                    duplicateCount++;
                } else {
                    existingCodes.add(code); // chặn mã lặp ngay trong file
                    toWrite.add(student);
                }
            }

            if (toWrite.isEmpty()) {
                final int dup = duplicateCount;
                mainHandler.post(() -> callback.onSuccess(0, dup, invalidCount));
                return;
            }

            final int dupFinal = duplicateCount;
            int batchCount = (toWrite.size() + BATCH_SIZE - 1) / BATCH_SIZE;
            AtomicInteger pending = new AtomicInteger(batchCount);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            for (int start = 0; start < toWrite.size(); start += BATCH_SIZE) {
                List<Map<String, Object>> chunk =
                        new ArrayList<>(toWrite.subList(start, Math.min(start + BATCH_SIZE, toWrite.size())));

                WriteBatch batch = db.batch();
                for (Map<String, Object> student : chunk) {
                    DocumentReference ref = db.collection("students").document((String) student.get("studentCode"));
                    batch.set(ref, student);
                }

                batch.commit().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        success.addAndGet(chunk.size());
                    } else {
                        failed.addAndGet(chunk.size());
                        Log.e(TAG, "Batch failed", task.getException());
                    }
                    if (pending.decrementAndGet() == 0) {
                        mainHandler.post(() -> callback.onSuccess(
                                success.get(), dupFinal, invalidCount + failed.get()));
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Cannot read existing students", e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            mainHandler.post(() -> callback.onError("Không đọc được danh sách sinh viên hiện có: " + msg));
        });
    }
}
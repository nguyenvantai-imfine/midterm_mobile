package com.example.gk_mobile.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Import danh sách chứng chỉ của MỘT sinh viên từ file CSV hoặc XLSX.
 * Tự nhận biết định dạng (XLSX là file zip bắt đầu bằng "PK"), dùng lại bộ đọc file của StudentFileImporter.
 *
 * Thứ tự cột (dòng đầu là tiêu đề, bị bỏ qua) - giống file mẫu certificates_sample.csv:
 * Tên chứng chỉ | Đơn vị cấp | Ngày cấp | Ngày hết hạn | Ghi chú
 * Ngày viết dạng dd/MM/yyyy (cũng nhận d/M/yyyy, yyyy-MM-dd và ô ngày của Excel).
 * Chứng chỉ trùng (cùng tên + đơn vị cấp + ngày cấp) với chứng chỉ đã có hoặc trùng trong file sẽ bị bỏ qua.
 */
public class CertificateFileImporter {

    private static final String TAG = "CertificateFileImporter";
    private static final int BATCH_SIZE = 400; // WriteBatch tối đa 500 thao tác

    public interface ImportCallback {
        void onProgress(String statusMessage);

        /** rowErrors: mô tả từng dòng bị lỗi, ví dụ "Dòng 5: Thiếu tên chứng chỉ". */
        void onSuccess(int successCount, int duplicateCount, List<String> rowErrors);

        void onError(String errorMessage);
    }

    private CertificateFileImporter() {}

    // ------------------------------------------------------------------
    // Điểm vào
    // ------------------------------------------------------------------

    public static void importFile(Context context, Uri uri, String studentCode, ImportCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onProgress("Đang đọc file..."));

                byte[] data = readAll(context, uri);
                if (data == null) {
                    mainHandler.post(() -> callback.onError("Không thể mở file."));
                    return;
                }

                boolean isXlsx = data.length >= 2 && data[0] == 'P' && data[1] == 'K';
                List<String[]> rows = isXlsx
                        ? StudentFileImporter.parseXlsxRows(new ByteArrayInputStream(data))
                        : StudentFileImporter.parseCsvRows(new ByteArrayInputStream(data));

                if (rows.size() <= 1) {
                    mainHandler.post(() -> callback.onError("File không có dữ liệu hoặc sai định dạng."));
                    return;
                }

                // Kiểm tra từng dòng dữ liệu (dòng 1 là tiêu đề nên dữ liệu bắt đầu từ dòng 2).
                List<Map<String, Object>> certificates = new ArrayList<>();
                List<String> rowErrors = new ArrayList<>();
                for (int i = 1; i < rows.size(); i++) {
                    String[] row = rows.get(i);
                    if (isBlankRow(row)) continue; // dòng trống không tính là lỗi

                    String error = validateRow(row);
                    if (error != null) {
                        rowErrors.add("Dòng " + (i + 1) + ": " + error);
                    } else {
                        certificates.add(rowToCertificate(row));
                    }
                }

                if (certificates.isEmpty()) {
                    String detail = rowErrors.isEmpty() ? "" : "\n" + rowErrors.get(0);
                    mainHandler.post(() -> callback.onError("Không có dòng hợp lệ để import." + detail));
                    return;
                }

                mainHandler.post(() -> callback.onProgress("Đang kiểm tra trùng và ghi dữ liệu lên Firestore..."));
                writeCertificates(studentCode, certificates, rowErrors, callback, mainHandler);

            } catch (Exception e) {
                Log.e(TAG, "Import error", e);
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                mainHandler.post(() -> callback.onError("Lỗi đọc file: " + msg));
            }
        });
        executor.shutdown(); // tác vụ đã gửi vẫn chạy xong, luồng được giải phóng sau đó
    }

    private static byte[] readAll(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            return buffer.toByteArray();
        }
    }

    // ------------------------------------------------------------------
    // Kiểm tra và chuyển dòng thành dữ liệu
    // ------------------------------------------------------------------

    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) return false;
        }
        return true;
    }

    private static String cell(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index].trim() : "";
    }

    /** Trả về mô tả lỗi, hoặc null nếu dòng hợp lệ. */
    private static String validateRow(String[] c) {
        if (cell(c, 0).isEmpty()) return "Thiếu tên chứng chỉ";
        if (cell(c, 1).isEmpty()) return "Thiếu đơn vị cấp";

        String issueText = cell(c, 2);
        if (issueText.isEmpty()) return "Thiếu ngày cấp";
        Timestamp issue = parseDate(issueText);
        if (issue == null) return "Ngày cấp \"" + issueText + "\" không hợp lệ (cần dạng dd/MM/yyyy)";

        String expiryText = cell(c, 3);
        if (!expiryText.isEmpty()) {
            Timestamp expiry = parseDate(expiryText);
            if (expiry == null) return "Ngày hết hạn \"" + expiryText + "\" không hợp lệ (cần dạng dd/MM/yyyy)";
            if (expiry.compareTo(issue) < 0) return "Ngày hết hạn trước ngày cấp";
        }
        return null;
    }

    /** Chỉ gọi sau khi validateRow trả về null. */
    private static Map<String, Object> rowToCertificate(String[] c) {
        Map<String, Object> cert = new HashMap<>();
        cert.put("name", cell(c, 0));
        cert.put("issuer", cell(c, 1));
        cert.put("issueDate", parseDate(cell(c, 2)));
        String expiryText = cell(c, 3);
        if (!expiryText.isEmpty()) {
            cert.put("expiryDate", parseDate(expiryText));
        }
        cert.put("note", cell(c, 4));
        return cert;
    }

    /**
     * Nhận dd/MM/yyyy (và d/M/yyyy), yyyy-MM-dd, hoặc số thứ tự ngày của Excel (ô kiểu Date).
     * Trả về 00:00 giờ địa phương của ngày đó, hoặc null nếu không hợp lệ.
     */
    static Timestamp parseDate(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;

        try {
            int year, month, day;
            if (s.matches("\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4}")) {
                String[] p = s.split("[/.-]");
                day = Integer.parseInt(p[0]);
                month = Integer.parseInt(p[1]);
                year = Integer.parseInt(p[2]);
            } else if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                String[] p = s.split("-");
                year = Integer.parseInt(p[0]);
                month = Integer.parseInt(p[1]);
                day = Integer.parseInt(p[2]);
            } else if (s.matches("\\d{5}(\\.\\d+)?")) {
                // Chỉ nhận số 5 chữ số (khoảng năm 1927-2173) để một con số bất kỳ như "2024" không bị hiểu nhầm là ngày.
                // Excel lưu ngày là số ngày kể từ 30/12/1899.
                long serial = (long) Double.parseDouble(s);
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.clear();
                utc.set(1899, Calendar.DECEMBER, 30);
                utc.add(Calendar.DAY_OF_MONTH, (int) serial);
                year = utc.get(Calendar.YEAR);
                month = utc.get(Calendar.MONTH) + 1;
                day = utc.get(Calendar.DAY_OF_MONTH);
            } else {
                return null;
            }
            return toTimestamp(year, month, day);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Timestamp toTimestamp(int year, int month, int day) {
        if (year < 1900 || year > 2200 || month < 1 || month > 12 || day < 1 || day > 31) return null;
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false); // 31/02/2024 sẽ bị từ chối thay vì tự đổi sang tháng sau
        cal.clear();
        try {
            cal.set(year, month - 1, day);
            return new Timestamp(cal.getTime());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Ghi lên Firestore
    // ------------------------------------------------------------------

    private static String keyOf(String name, String issuer, Timestamp issueDate) {
        long day = issueDate == null ? 0 : issueDate.toDate().getTime() / (24L * 60 * 60 * 1000);
        return name.trim().toLowerCase(Locale.ROOT) + "|" + issuer.trim().toLowerCase(Locale.ROOT) + "|" + day;
    }

    /**
     * Bỏ qua chứng chỉ đã có trên Firestore hoặc lặp trong file (tính vào duplicateCount),
     * ghi phần còn lại theo từng WriteBatch, chỉ báo kết quả khi TẤT CẢ batch đã xong.
     */
    private static void writeCertificates(String studentCode, List<Map<String, Object>> certificates,
                                          List<String> rowErrors, ImportCallback callback, Handler mainHandler) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = db.collection(Constants.COLLECTION_STUDENTS)
                .document(studentCode)
                .collection(Constants.SUBCOLLECTION_CERTIFICATES);

        col.get().addOnSuccessListener(snapshot -> {
            Set<String> existing = new HashSet<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                String name = doc.getString("name");
                String issuer = doc.getString("issuer");
                if (name != null && issuer != null) {
                    existing.add(keyOf(name, issuer, doc.getTimestamp("issueDate")));
                }
            }

            List<Map<String, Object>> toWrite = new ArrayList<>();
            int duplicateCount = 0;
            for (Map<String, Object> cert : certificates) {
                String key = keyOf((String) cert.get("name"), (String) cert.get("issuer"),
                        (Timestamp) cert.get("issueDate"));
                if (existing.contains(key)) {
                    duplicateCount++;
                } else {
                    existing.add(key); // chặn trùng ngay trong file
                    toWrite.add(cert);
                }
            }

            final int dupFinal = duplicateCount;
            if (toWrite.isEmpty()) {
                mainHandler.post(() -> callback.onSuccess(0, dupFinal, rowErrors));
                return;
            }

            int batchCount = (toWrite.size() + BATCH_SIZE - 1) / BATCH_SIZE;
            AtomicInteger pending = new AtomicInteger(batchCount);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            for (int start = 0; start < toWrite.size(); start += BATCH_SIZE) {
                List<Map<String, Object>> chunk =
                        new ArrayList<>(toWrite.subList(start, Math.min(start + BATCH_SIZE, toWrite.size())));

                WriteBatch batch = db.batch();
                for (Map<String, Object> cert : chunk) {
                    DocumentReference ref = col.document(); // id tự sinh
                    batch.set(ref, cert);
                }

                batch.commit().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        success.addAndGet(chunk.size());
                    } else {
                        failed.addAndGet(chunk.size());
                        Log.e(TAG, "Batch failed", task.getException());
                    }
                    if (pending.decrementAndGet() == 0) {
                        List<String> errors = new ArrayList<>(rowErrors);
                        if (failed.get() > 0) {
                            errors.add("Không ghi được " + failed.get() + " chứng chỉ lên Firestore (kiểm tra quyền/Rules).");
                        }
                        mainHandler.post(() -> callback.onSuccess(success.get(), dupFinal, errors));
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Cannot read existing certificates", e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            mainHandler.post(() -> callback.onError("Không đọc được chứng chỉ hiện có: " + msg));
        });
    }
}

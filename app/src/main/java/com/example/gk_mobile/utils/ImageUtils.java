package com.example.gk_mobile.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Xử lý ảnh đại diện: cắt vuông, thu nhỏ, nén JPEG rồi đổi thành chuỗi data URI (Base64).
 * Ảnh sau khi nén chỉ vài chục KB nên lưu thẳng trong document users/{uid}.avatarUrl
 * (giới hạn 1 MB/document), không cần Firebase Storage (có thể đòi gói trả phí).
 */
public final class ImageUtils {
    private static final int AVATAR_SIZE = 320;
    private static final int JPEG_QUALITY = 80;

    private ImageUtils() {}

    /** Nên gọi ở luồng nền: đọc và nén ảnh khá tốn thời gian. */
    public static String toAvatarDataUri(ContentResolver resolver, Uri uri) throws IOException {
        // 1) Đọc kích thước gốc, chọn hệ số thu nhỏ để không nạp ảnh quá lớn vào bộ nhớ.
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Không mở được ảnh");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        while (bounds.outWidth / sample > AVATAR_SIZE * 2 || bounds.outHeight / sample > AVATAR_SIZE * 2) {
            sample *= 2;
        }

        // 2) Đọc ảnh đã thu nhỏ.
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap decoded;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Không mở được ảnh");
            decoded = BitmapFactory.decodeStream(in, null, opts);
        }
        if (decoded == null) throw new IOException("Tệp đã chọn không phải ảnh hợp lệ");

        // 3) Xoay đúng chiều theo EXIF (ảnh chụp từ camera thường bị xoay).
        decoded = rotateByExif(resolver, uri, decoded);

        // 4) Cắt hình vuông ở giữa rồi thu nhỏ.
        int side = Math.min(decoded.getWidth(), decoded.getHeight());
        int x = (decoded.getWidth() - side) / 2;
        int y = (decoded.getHeight() - side) / 2;
        Bitmap square = Bitmap.createBitmap(decoded, x, y, side, side);
        int target = Math.min(AVATAR_SIZE, side);
        Bitmap scaled = Bitmap.createScaledBitmap(square, target, target, true);

        // 5) Nén JPEG và mã hóa Base64.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    private static Bitmap rotateByExif(ContentResolver resolver, Uri uri, Bitmap bitmap) {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return bitmap;
            int orientation = new ExifInterface(in)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degrees;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
                default:
                    return bitmap;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException | RuntimeException e) {
            return bitmap; // Không đọc được EXIF thì dùng ảnh nguyên bản.
        }
    }
}

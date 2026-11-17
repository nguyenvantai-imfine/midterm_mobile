package com.example.gk_mobile.utils;

import android.content.Context;
import android.net.Uri;

import com.example.gk_mobile.model.Student;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Xuất danh sách sinh viên ra file .xlsx mà không cần Apache POI.
 * File .xlsx thực chất là một file zip chứa các file XML, nên chỉ cần ZipOutputStream.
 *
 * Cách dùng: cho người dùng chọn nơi lưu bằng ActivityResultContracts.CreateDocument
 * (MIME {@link #MIME_TYPE}), rồi gọi {@link #exportStudentsToExcel(Context, Uri, List)}.
 */
public class ExcelExporter {

    public static final String MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String[] HEADERS = {
            "Mã SV", "Họ và tên", "Tuổi", "Giới tính", "Lớp", "Khoa", "Email", "Số điện thoại", "Địa chỉ"
    };

    // Độ rộng cột (số ký tự) tương ứng với HEADERS
    private static final int[] WIDTHS = {12, 26, 8, 12, 14, 22, 30, 16, 36};

    /**
     * Ghi danh sách sinh viên vào Uri do người dùng chọn.
     *
     * @return true nếu ghi thành công
     */
    public static boolean exportStudentsToExcel(Context context, Uri destinationUri, List<Student> studentList) {
        if (studentList == null) return false;

        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) return false;
            writeWorkbook(out, studentList);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Ghi workbook ra bất kỳ OutputStream nào (tách riêng để dễ kiểm thử). */
    public static void writeWorkbook(OutputStream out, List<Student> students) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);

        putEntry(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                        + "</Types>");

        putEntry(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                        + "</Relationships>");

        putEntry(zip, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                        + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                        + "<sheets><sheet name=\"Danh_sach_sinh_vien\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                        + "</workbook>");

        putEntry(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                        + "</Relationships>");

        // Style 0: mặc định, style 1: chữ đậm cho dòng tiêu đề
        putEntry(zip, "xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                        + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                        + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
                        + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
                        + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                        + "<cellXfs count=\"2\">"
                        + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                        + "</cellXfs>"
                        + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                        + "</styleSheet>");

        // Sheet dữ liệu
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<cols>");
        for (int i = 0; i < WIDTHS.length; i++) {
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(WIDTHS[i]).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        // Dòng tiêu đề
        sb.append("<row r=\"1\">");
        for (int i = 0; i < HEADERS.length; i++) {
            appendText(sb, i, 1, HEADERS[i], 1);
        }
        sb.append("</row>");

        // Dòng dữ liệu
        int rowNumber = 2;
        for (Student s : students) {
            sb.append("<row r=\"").append(rowNumber).append("\">");
            appendText(sb, 0, rowNumber, s.getStudentCode(), 0);
            appendText(sb, 1, rowNumber, s.getName(), 0);
            appendNumber(sb, 2, rowNumber, s.getAge());
            appendText(sb, 3, rowNumber, s.getGender(), 0);
            appendText(sb, 4, rowNumber, s.getClassName(), 0);
            appendText(sb, 5, rowNumber, s.getFaculty(), 0);
            appendText(sb, 6, rowNumber, s.getEmail(), 0);
            appendText(sb, 7, rowNumber, s.getPhone(), 0);   // ghi dạng chuỗi để giữ số 0 đầu
            appendText(sb, 8, rowNumber, s.getAddress(), 0);
            sb.append("</row>");
            rowNumber++;
        }
        sb.append("</sheetData></worksheet>");
        putEntry(zip, "xl/worksheets/sheet1.xml", sb.toString());

        zip.finish();
        zip.flush();
    }

    private static void putEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Ô chuỗi dạng inline (không cần file sharedStrings). Giá trị null/rỗng thì bỏ qua ô. */
    private static void appendText(StringBuilder sb, int col, int row, String value, int style) {
        if (value == null || value.isEmpty()) return;
        sb.append("<c r=\"").append(columnName(col)).append(row).append("\" t=\"inlineStr\"");
        if (style != 0) sb.append(" s=\"").append(style).append("\"");
        sb.append("><is><t xml:space=\"preserve\">").append(escapeXml(value)).append("</t></is></c>");
    }

    private static void appendNumber(StringBuilder sb, int col, int row, int value) {
        sb.append("<c r=\"").append(columnName(col)).append(row).append("\"><v>")
                .append(value).append("</v></c>");
    }

    /** 0 -> A, 1 -> B, 26 -> AA. */
    private static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return name.toString();
    }

    /** Escape ký tự đặc biệt của XML và bỏ ký tự điều khiển không hợp lệ. */
    private static String escapeXml(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch);
            }
        }
        return sb.toString();
    }
}
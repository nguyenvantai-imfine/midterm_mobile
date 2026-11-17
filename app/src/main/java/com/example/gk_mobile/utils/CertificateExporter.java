package com.example.gk_mobile.utils;

import android.content.Context;
import android.net.Uri;

import com.example.gk_mobile.model.Certificate;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Xuất danh sách chứng chỉ của một sinh viên ra CSV hoặc Excel (.xlsx, không cần Apache POI).
 *
 * Cách dùng: cho người dùng chọn nơi lưu bằng ActivityResultContracts.CreateDocument
 * (MIME {@link #XLSX_MIME_TYPE} hoặc "text/csv"), rồi gọi exportToCsv / exportToExcel.
 * Các cột trùng với file mẫu import (certificates_sample.csv) nên file xuất ra import lại được.
 */
public class CertificateExporter {

    public static final String XLSX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String[] HEADERS = {"Tên chứng chỉ", "Đơn vị cấp", "Ngày cấp", "Ngày hết hạn", "Ghi chú"};
    private static final int[] WIDTHS = {34, 28, 14, 14, 36};

    private CertificateExporter() {}

    /** Ghi CSV UTF-8 có BOM để Excel hiển thị đúng tiếng Việt. @return true nếu thành công */
    public static boolean exportToCsv(Context context, Uri destinationUri, List<Certificate> certificates) {
        if (certificates == null) return false;
        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri);
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            writer.write('﻿');
            writer.write("Tên chứng chỉ,Đơn vị cấp,Ngày cấp,Ngày hết hạn,Ghi chú\n");
            for (Certificate c : certificates) {
                String[] cells = toCells(c);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cells.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(escapeCsv(cells[i]));
                }
                sb.append('\n');
                writer.write(sb.toString());
            }
            writer.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Ghi file .xlsx vào Uri do người dùng chọn. @return true nếu thành công */
    public static boolean exportToExcel(Context context, Uri destinationUri, List<Certificate> certificates) {
        if (certificates == null) return false;
        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) return false;
            writeWorkbook(out, certificates);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String[] toCells(Certificate c) {
        return new String[]{
                nullToEmpty(c.getName()),
                nullToEmpty(c.getIssuer()),
                DateUtils.formatShortDate(c.getIssueDate()),
                DateUtils.formatShortDate(c.getExpiryDate()),
                nullToEmpty(c.getNote())
        };
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ------------------------------------------------------------------
    // XLSX: một file zip chứa các file XML
    // ------------------------------------------------------------------

    static void writeWorkbook(OutputStream out, List<Certificate> certificates) throws Exception {
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
                        + "<sheets><sheet name=\"Chung_chi\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
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

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>");
        for (int i = 0; i < WIDTHS.length; i++) {
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(WIDTHS[i]).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        sb.append("<row r=\"1\">");
        for (int i = 0; i < HEADERS.length; i++) {
            appendText(sb, i, 1, HEADERS[i], 1);
        }
        sb.append("</row>");

        int rowNumber = 2;
        for (Certificate c : certificates) {
            String[] cells = toCells(c);
            sb.append("<row r=\"").append(rowNumber).append("\">");
            for (int i = 0; i < cells.length; i++) {
                appendText(sb, i, rowNumber, cells[i], 0); // ngày ghi dạng chuỗi dd/MM/yyyy để import lại được
            }
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

    /** Ô chuỗi dạng inline (không cần sharedStrings). Giá trị rỗng thì bỏ qua ô. */
    private static void appendText(StringBuilder sb, int col, int row, String value, int style) {
        if (value == null || value.isEmpty()) return;
        sb.append("<c r=\"").append((char) ('A' + col)).append(row).append("\" t=\"inlineStr\"");
        if (style != 0) sb.append(" s=\"").append(style).append("\"");
        sb.append("><is><t xml:space=\"preserve\">").append(escapeXml(value)).append("</t></is></c>");
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

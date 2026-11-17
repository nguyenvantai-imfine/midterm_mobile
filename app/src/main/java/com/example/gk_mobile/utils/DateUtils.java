package com.example.gk_mobile.utils;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        return DATE_FORMAT.format(timestamp.toDate());
    }

    public static String formatShortDate(Timestamp timestamp) {
        if (timestamp == null) return "";
        return SHORT_DATE_FORMAT.format(timestamp.toDate());
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }
}

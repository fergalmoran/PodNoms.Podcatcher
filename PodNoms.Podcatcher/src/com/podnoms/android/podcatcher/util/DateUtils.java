package com.podnoms.android.podcatcher.util;

import android.content.Context;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class DateUtils {
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static final Object[] sTimeArgs = new Object[5];
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    public static final String PODCAST_DATE_FORMAT = "yyyyMMdd HH:mm:ss";

    public static String makeTimeString(Context context, long secs) {
        String durationformat = context.getString(
                secs < 3600 ? R.string.durationformatshort : R.string.durationformatlong);

        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }

    private static Date _stringToDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat(PODCAST_DATE_FORMAT);
        try {
            Date ret = format.parse(date);
            return ret;
        } catch (ParseException e) {
            LogHandler.reportError("Error formatting date", e);
        }
        return null;
    }

    public static String getShortDate(String date) {
        return getShortDate(_stringToDate(date));
    }

    public static String getShortDate(Date date) {
        if (date != null) {
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(PodNomsApplication.getContext());
            String ret = dateFormat.format(date);
            return ret;
        }
        return null;
    }
    public static String getContentValueToday() {
        return getContentValue(Calendar.getInstance().getTime());
    }
    public static String getContentValue(Date date){
        String ret = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        return ret;
    }
}

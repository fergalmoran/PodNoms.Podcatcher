package com.podnoms.android.podcatcher.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.podnoms.android.podcatcher.PodNomsApplication;

public class LogHandler {
    public static final String INTENT_ERROR = "com.podnoms.android.ERROR";
    private static Intent errorIntent = new Intent(INTENT_ERROR);

    public static void showLog(String logText) {
        if (PodNomsApplication.isDebug()) {
            Log.e(PodNomsApplication.PACKAGE, logText);
        } else
            Log.d(PodNomsApplication.PACKAGE, logText);
    }

    public static void reportError(Context context, String message, Exception ex, Boolean showUser) {
        String display = String.format("%s\n%s\n%s\n%s ", message, ex.getClass().toString(), ex.getCause(), ex.getMessage());
        showMessage(context, display, showUser);
    }

    public static void reportError(Context context, String message, Exception ex) {
        reportError(context, message, ex, true);
    }

    public static void reportError(String message, Exception ex, boolean showUser) {
        reportError(PodNomsApplication.getContext(), message, ex, showUser);
    }

    public static void reportError(String message, Exception ex) {
        reportError(PodNomsApplication.getContext(), message, ex, true);
    }

    public static void showMessage(Context context, String message, Boolean showUser) {
        Log.e(PodNomsApplication.PACKAGE, message);
        if (showUser) {
            errorIntent.putExtra("message", message);
            context.sendOrderedBroadcast(errorIntent, null);
        }
    }

    public static void showMessage(String message) {
        showMessage(PodNomsApplication.getContext(), message, false);
    }

    public static void showMessage(Context context, String message) {
        showMessage(context, message, true);
    }
}

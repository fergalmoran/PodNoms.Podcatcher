package com.podnoms.android.podcatcher.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import com.podnoms.android.podcatcher.PodNomsApplication;

public class DeviceUtils {
    public static Boolean runningOnEmulator() {
        Boolean ret = false;
        /*
        if (PodNomsApplication.isDebug())
            return true;
        */
        TelephonyManager man = (TelephonyManager) PodNomsApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (man != null) {
            String devId = man.getDeviceSoftwareVersion();
            ret = (devId == null);
        }

        return ret;
    }

    public static int getCardId(Context context) {
        ContentResolver res = context.getContentResolver();
        Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            id = c.getInt(0);
            c.close();
        }
        return id;
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder) {
        return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder, int limit) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            if (limit > 0) {
                uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }

    }


}

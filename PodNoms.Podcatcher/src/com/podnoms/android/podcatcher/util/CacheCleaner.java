package com.podnoms.android.podcatcher.util;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.Loader;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;

import java.io.File;

public class CacheCleaner {

    private Context _context;
    public CacheCleaner(Context context) {
        _context = context;
    }

    public void cleanCache(){
        Cursor cursor = _context.getContentResolver().query(PodNomsInterface.Entry.PURGE_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String fileName = cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_LOCAL_FILE));
                File f = new File(fileName);
                if (f.exists()) {
                    f.delete();
                }
            } while (cursor.moveToNext());
        }
    }

}

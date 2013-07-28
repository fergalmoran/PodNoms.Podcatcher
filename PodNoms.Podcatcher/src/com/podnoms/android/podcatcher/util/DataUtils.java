package com.podnoms.android.podcatcher.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.podnoms.android.podcatcher.providers.DataStore;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.PodNomsProvider;

public class DataUtils {
    //We'll keep any data methods in here that
    //we don't wish to have exposed in our provider
    //housekeeping and deletes and such

    public static String getPodcastUri(Context context, long podcastId){
        Cursor cursor = context.getContentResolver().query(
                Uri.parse(PodNomsInterface.Podcast.ID2URI_URL + "/" + podcastId),
                null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(PodNomsInterface.Podcast.COLUMN_NAME_RESOURCE_URI));
        }
        return "";
    }

    public static boolean deletePodcast(Context context, long podcastId) {
        SQLiteDatabase db = new DataStore(context).getWritableDatabase();
        if (db != null) {
            db.execSQL(String.format("DELETE FROM podcast_entry WHERE podcast_id = %d", podcastId));
            db.execSQL(String.format("DELETE FROM podcast WHERE _id = %d", podcastId));

            context.getContentResolver().notifyChange(PodNomsInterface.Podcast.CONTENT_URI, null);
            context.getContentResolver().notifyChange(PodNomsInterface.Entry.CONTENT_URI, null);
        }
        return false;
    }
}

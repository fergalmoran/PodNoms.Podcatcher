package com.podnoms.android.podcatcher.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

import java.util.Arrays;
import java.util.HashMap;

public final class PodNomsInterface {
    public static final String AUTHORITY = "com.podnoms.android.data";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    //Loaders
    public static final int URI_MATCH_PODCASTS = 1;
    public static final int URI_MATCH_PODCASTENTRIES_ALL = 2;
    public static final int URI_MATCH_PODCASTENTRIES = 3;
    public static final int URI_MATCH_PODCASTENTRY = 4;
    public static final int URI_MATCH_DOWNLOADQUEUE = 5;
    public static final int URI_MATCH_UPDATE_PODCAST = 6;
    public static final int URI_MATCH_SEARCH = 7;
    public static final int URI_MATCH_PODCASTENTRIES_DOWNLOADED = 8;
    public static final int URI_MATCH_PODCASTENTRIES_ALL_LIST = 9;
    public static final int URI_MATCH_PURGE = 10;
    public static final int URI_MATCH_ID2URI= 11;


    public static final int LOADER_AUDIO_ITEM = 1;
    public static final int LOADER_ENTRY_LIST_DEFAULT = 2;
    public static final int LOADER_ENTRY_LIST_SORT_DATE = 3;
    public static final int LOADER_ENTRY_LIST_SORT_DATE_ASC = 4;
    public static final int LOADER_ENTRY_LIST_SORT_PLAYED = 5;
    public static final int LOADER_ENTRY_LIST_SORT_PLAYED_ASC = 6;
    public static final int LOADER_MANAGE_PODCASTS = 7;
    public static final int LOADER_PODCAST_LIST = 8;

    public static class PodcastBase {
        protected static final String SCHEME = "content://";
    }

    public static final class Podcast extends PodcastBase implements BaseColumns {

        public static final Uri ID2URI_URL = Uri.parse(SCHEME + AUTHORITY + "/id2uri");

        public static HashMap<String, String> PROJECTION = new HashMap<String, String>();

        public static String[] getProjectionMap() {
            Object[] ret = PROJECTION.keySet().toArray();
            return Arrays.asList(ret).toArray(new String[ret.length]);
            //return Arrays.copyOf(ret, ret.length, String[].class);
        }

        public static final String TABLE_NAME = "podcast";
        private static final String PATH_PODCASTS = "/podcasts";
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_PODCASTS);
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_RESOURCE_URI = "resource_uri";
        public static final String COLUMN_NAME_IMAGE = "image";
        public static final String COLUMN_NAME_DATE_CREATED = "date_created";
        public static final String COLUMN_NAME_DATE_UPDATED = "date_updated";

        public static final String DEFAULT_SORT_ORDER = COLUMN_NAME_DATE_UPDATED + " DESC";

        static {
            PROJECTION.put(_ID, _ID);
            PROJECTION.put(COLUMN_NAME_TITLE, COLUMN_NAME_TITLE);
            PROJECTION.put(COLUMN_NAME_DESCRIPTION, COLUMN_NAME_DESCRIPTION);
            PROJECTION.put(COLUMN_NAME_URL, COLUMN_NAME_URL);
            PROJECTION.put(COLUMN_NAME_IMAGE, COLUMN_NAME_IMAGE);
            PROJECTION.put(COLUMN_NAME_RESOURCE_URI, COLUMN_NAME_RESOURCE_URI);
        }
    }

    public static final class Entry extends PodcastBase implements BaseColumns {
        public static HashMap<String, String> PROJECTION = new HashMap<String, String>();


        public static String[] getProjectionMap() {
            Object[] ret = PROJECTION.keySet().toArray();
            return Arrays.copyOf(ret, ret.length, String[].class);
        }

        public static final String TABLE_NAME = "podcast_entry";

        private static final String PATH_ENTRIES = "/entries";
        private static final String PATH_DOWNLOADED = "/downloaded";
        private static final String PATH_ALL = "/all";
        private static final String PATH_DETAILS = "/entry/details";
        private static final String PATH_PURGE = "/purge";
        private static final String PATH_DOWNLOADQUEUE = "/downloadqueue";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_ENTRIES);
        public static final Uri DETAILS_URI = Uri.parse(SCHEME + AUTHORITY + PATH_DETAILS);
        public static final Uri PURGE_URI = Uri.parse(SCHEME + AUTHORITY + PATH_PURGE);
        public static final Uri DOWNLOADED_URI = Uri.parse(SCHEME + AUTHORITY + PATH_DOWNLOADED);
        public static final Uri DOWNLOADQUEUE_URI = Uri.parse(SCHEME + AUTHORITY + PATH_DOWNLOADQUEUE);
        public static final Uri ALLENTRIES_URI = Uri.parse(SCHEME + AUTHORITY + PATH_ENTRIES + PATH_ALL);

        public static final String DEFAULT_SORT_ORDER = "date_updated DESC";

        public static final String COLUMN_NAME_PODCASTID = "podcast_id";
        public static final String COLUMN_NAME_GUID = "guid";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_ENCLOSURE = "enclosure";
        public static final String COLUMN_NAME_IMAGE = "image";
        public static final String COLUMN_NAME_FILE_LENGTH = "file_length";
        public static final String COLUMN_NAME_ENTRY_LENGTH = "entry_length";
        public static final String COLUMN_NAME_POSITION = "position";
        public static final String COLUMN_NAME_LOCAL_FILE = "local_file";
        public static final String COLUMN_NAME_DOWNLOADED = "downloaded";
        public static final String COLUMN_NAME_PLAYCOUNT = "playcount";
        public static final String COLUMN_NAME_PODCAST_TITLE = "title";
        public static final String COLUMN_NAME_DATE_CREATED = "date_created";
        public static final String COLUMN_NAME_DATE_UPDATED = "date_updated";

        //virtual columns
        public static final String V_COLUMN_NAME_PERCENTAGE_PLAYED = "percentage_played";

        public static final String SQL_DOWNLOADQUEUE =
                String.format("SELECT\n" +
                        "   a._id, a.podcast_id, a.title, a.enclosure, a.local_file, a.entry_length, a.file_length, a.downloaded\n" +
                        "FROM\n" +
                        "   podcast_entry AS a\n" +
                        "LEFT JOIN\n" +
                        "   podcast_entry AS a2\n" +
                        "       ON a.podcast_id = a2.podcast_id AND\n" +
                        "       a.date_created <= a2.date_created \n" +
                        "GROUP BY\n" +
                        "   a._id\n" +
                        "HAVING\n" +
                        "   COUNT(*) <= %d", PersistentStateHandler.I().getInt(Constants.MAX_DOWNLOADS, 1));

        public static String[] QUEUE_PROJECTION = new String[]{
                _ID,
                COLUMN_NAME_PODCASTID,
                COLUMN_NAME_TITLE,
                COLUMN_NAME_ENCLOSURE,
                COLUMN_NAME_LOCAL_FILE,
                COLUMN_NAME_FILE_LENGTH,
                COLUMN_NAME_DOWNLOADED

        };

        static {
            PROJECTION.put(_ID, _ID);
            PROJECTION.put(COLUMN_NAME_PODCASTID, COLUMN_NAME_PODCASTID);
            PROJECTION.put(COLUMN_NAME_TITLE, COLUMN_NAME_TITLE);
            PROJECTION.put(COLUMN_NAME_DESCRIPTION, COLUMN_NAME_DESCRIPTION);
            PROJECTION.put(COLUMN_NAME_ENCLOSURE, COLUMN_NAME_ENCLOSURE);
            PROJECTION.put(COLUMN_NAME_LOCAL_FILE, COLUMN_NAME_LOCAL_FILE);
            PROJECTION.put(COLUMN_NAME_IMAGE, COLUMN_NAME_IMAGE);
            PROJECTION.put(COLUMN_NAME_FILE_LENGTH, COLUMN_NAME_FILE_LENGTH);
            PROJECTION.put(COLUMN_NAME_ENTRY_LENGTH, COLUMN_NAME_ENTRY_LENGTH);
            PROJECTION.put(COLUMN_NAME_TITLE, COLUMN_NAME_TITLE);
            PROJECTION.put(COLUMN_NAME_DOWNLOADED, COLUMN_NAME_DOWNLOADED);
            PROJECTION.put(COLUMN_NAME_POSITION, COLUMN_NAME_POSITION);
            PROJECTION.put(COLUMN_NAME_PLAYCOUNT, COLUMN_NAME_PLAYCOUNT);
        }

        public static Uri buildPodcastEntriesUri(long podcastId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(podcastId)).build();
        }

        public static Uri buildPodcastDetailsUri(long podcastId) {
            return DETAILS_URI.buildUpon().appendPath(Long.toString(podcastId)).build();
        }

        public static void prune(SQLiteDatabase db, String podcastId, Integer id) {
            String sql = String.format(
                    "DELETE FROM %s WHERE %s NOT IN (SELECT %s FROM %s WHERE %s = %d ORDER BY %s LIMIT %d) AND %s = %d",
                    TABLE_NAME,
                    _ID,
                    _ID,
                    TABLE_NAME,
                    podcastId,
                    id,
                    DEFAULT_SORT_ORDER,
                    5,
                    podcastId,
                    id);
            db.execSQL(sql);
        }

        public static void setListened(Context context, long id) {
            String sql = String.format("UPDATE %s SET %s = %s WHERE %s = %d",
                    TABLE_NAME,
                    COLUMN_NAME_POSITION,
                    COLUMN_NAME_ENTRY_LENGTH,
                    _ID,
                    id);
            new DataStore(context).getWritableDatabase().execSQL(sql);
        }
    }
}



package com.podnoms.android.podcatcher.providers;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import com.podnoms.android.podcatcher.util.DateUtils;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

import java.util.HashMap;

public class PodNomsProvider extends ContentProvider {

    private DataStore _datastore;
    private static final UriMatcher _uriMatcher;

    static {
        _uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "podcasts", PodNomsInterface.URI_MATCH_PODCASTS);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "entries", PodNomsInterface.URI_MATCH_PODCASTENTRIES_ALL);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "purge", PodNomsInterface.URI_MATCH_PURGE);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "entries/all", PodNomsInterface.URI_MATCH_PODCASTENTRIES_ALL_LIST);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "entries/*", PodNomsInterface.URI_MATCH_PODCASTENTRY);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "downloaded", PodNomsInterface.URI_MATCH_PODCASTENTRIES_DOWNLOADED);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "entry/details/*", PodNomsInterface.URI_MATCH_PODCASTENTRY);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "downloadqueue", PodNomsInterface.URI_MATCH_DOWNLOADQUEUE);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, "id2uri/*", PodNomsInterface.URI_MATCH_ID2URI);
        _uriMatcher.addURI(PodNomsInterface.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, PodNomsInterface.URI_MATCH_SEARCH);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = "";
        String sql = "";
        int match = _uriMatcher.match(uri);
        switch (match) {
            case PodNomsInterface.URI_MATCH_PODCASTS:
                HashMap<String, String> projectionMap = PodNomsInterface.Podcast.PROJECTION;
                qb.setProjectionMap(projectionMap);
                qb.setTables(PodNomsInterface.Podcast.TABLE_NAME);
                orderBy = TextUtils.isEmpty(sortOrder) ? PodNomsInterface.Podcast.DEFAULT_SORT_ORDER : sortOrder;
                break;
            case PodNomsInterface.URI_MATCH_PURGE:
                sql = _getPurgeSql();
                break;
            case PodNomsInterface.URI_MATCH_ID2URI:
                sql = _getId2UriSql(ContentUris.parseId(uri));
                break;
            case PodNomsInterface.URI_MATCH_PODCASTENTRIES_ALL:
                qb.setProjectionMap(PodNomsInterface.Entry.PROJECTION);
                qb.setTables(PodNomsInterface.Entry.TABLE_NAME);
                orderBy = TextUtils.isEmpty(sortOrder) ? PodNomsInterface.Entry.DEFAULT_SORT_ORDER : sortOrder;
                break;
            case PodNomsInterface.URI_MATCH_PODCASTENTRIES_DOWNLOADED:
                return _getDownloadedPodcasts(uri, sortOrder);
            case PodNomsInterface.URI_MATCH_PODCASTENTRIES_ALL_LIST:
                return _getAllPodcastEntries(uri, sortOrder);
            case PodNomsInterface.URI_MATCH_PODCASTENTRIES:
                sql = String.format(_getPodcastListSql() +
                        "WHERE\n" +
                        "  podcast_entry.podcast_id = %d\n" +
                        "ORDER BY\n" +
                        "  podcast_entry.date_updated DESC", ContentUris.parseId(uri));
                break;
            case PodNomsInterface.URI_MATCH_PODCASTENTRY:
                sql = String.format(_getPodcastListSql() +
                        "WHERE\n" +
                        "  podcast_entry._id = %d\n" +
                        "ORDER BY\n" +
                        "  podcast_entry.date_updated DESC", ContentUris.parseId(uri));
                break;
            case PodNomsInterface.URI_MATCH_DOWNLOADQUEUE:
                return _datastore.getReadableDatabase().rawQuery(PodNomsInterface.Entry.SQL_DOWNLOADQUEUE, null);
            case PodNomsInterface.URI_MATCH_SEARCH:
                return _getSearchSuggestions(selectionArgs[0]);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        try {
            SQLiteDatabase db = _datastore.getReadableDatabase();
            Cursor c;
            if (TextUtils.isEmpty(sql))
                c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
            else
                c = db.rawQuery(sql, null);

            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (Exception ex) {
            LogHandler.reportError("Error resolving content: ", ex);
            return null;
        }
    }

    private String _getPurgeSql() {
        String ret = String.format("SELECT \n" +
                "     _id,\n" +
                "     description,\n" +
                "     local_file     \n" +
                "FROM \n" +
                "     podcast_entry\n" +
                "WHERE (((CAST(podcast_entry.position AS REAL)/ CAST(podcast_entry.entry_length AS REAL)) * 100) > %d) OR podcast_entry.position = -9999",
                PersistentStateHandler.I().getInt(Constants.MARK_PLAYED_AT, Constants.MARK_PLAYED_AT_DEFAULT));
        return ret;
    }

    private String _getId2UriSql(long id){
        return String.format("SELECT resource_uri FROM podcast WHERE _id = %d", id);
    }

    private Cursor _getDownloadedPodcasts(Uri uri, String sortOrder) {
        String sql = _getPodcastListSql() +
                String.format("WHERE " +
                        "   local_file <> '' AND local_file IS NOT NULL AND\n" +
                        "   (position <> -9999 OR position IS NULL) AND\n" +
                        "   (percentage_played < %d OR percentage_played IS NULL)\n" +
                        (TextUtils.isEmpty(sortOrder) ? "" : "ORDER BY \n" + sortOrder),
                        PersistentStateHandler.I().getInt(Constants.MARK_PLAYED_AT, Constants.MARK_PLAYED_AT_DEFAULT));
        SQLiteDatabase db = _datastore.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    private Cursor _getAllPodcastEntries(Uri uri, String sortOrder) {
        String sql = _getPodcastListSql() +
                (TextUtils.isEmpty(sortOrder) ? "" : "ORDER BY \n" +   sortOrder);
        SQLiteDatabase db = _datastore.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private String _getPodcastListSql() {
        return "SELECT \n" +
                "  podcast_entry._id,\n" +
                "  podcast_entry.title,\n" +
                "  podcast_entry.description,\n" +
                "  podcast_entry.enclosure,\n" +
                "  podcast_entry.local_file,\n" +
                "  podcast_entry.url,\n" +
                "  IFNULL(podcast_entry.image, podcast.image) AS image,\n" +
                "  podcast_entry.date_created,\n" +
                "  podcast_entry.date_updated,\n" +
                "  podcast_entry.file_length,\n" +
                "  podcast_entry.entry_length,\n" +
                "  podcast_entry.position,\n" +
                "  podcast_entry.downloaded,\n" +
                "  podcast_entry.playcount,\n" +
                "  (CAST(podcast_entry.position AS REAL)/ CAST(podcast_entry.entry_length AS REAL)) * 100 AS percentage_played\n" +
                "FROM\n" +
                "  podcast\n" +
                "INNER JOIN " +
                "  podcast_entry ON " +
                "    (podcast.\"_id\" = podcast_entry.podcast_id)\n";
    }

    private Cursor _getSearchSuggestions(String query) {
        query = query.toLowerCase();
        String sql = String.format("SELECT _id, title as %s, description as %s, _id AS %s FROM %s WHERE %s LIKE('%%%s%%') OR %s LIKE('%%%s%%')",
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                PodNomsInterface.Podcast.TABLE_NAME,
                PodNomsInterface.Podcast.COLUMN_NAME_TITLE,
                query,
                PodNomsInterface.Podcast.COLUMN_NAME_DESCRIPTION,
                query);
        return _datastore.getReadableDatabase().rawQuery(sql, null);

    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    private Uri _insert(Uri uri, ContentValues contentValues) {
        /* We can do a generic insert method here */
        long insertedId = -1;
        SQLiteDatabase db = _datastore.getWritableDatabase();
        String table = uri.getLastPathSegment();
        try {
            db.beginTransaction();
            insertedId = db.insertWithOnConflict(
                    table,
                    null,
                    contentValues,
                    SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful();
        } catch (Exception ex) {
            LogHandler.reportError(String.format("Error inserting: %s", table), ex);
        } finally {
            db.endTransaction();
        }

        return uri.buildUpon().appendPath(Long.toString(insertedId)).build();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long insertedId = -1;
        final SQLiteDatabase db = _datastore.getWritableDatabase();
        final int match = _uriMatcher.match(uri);
        switch (match) {
            case PodNomsInterface.URI_MATCH_PODCASTS:
                if (values.containsKey("title") || values.containsKey("url")) {
                    db.beginTransaction();
                    try {
                        if (values.containsKey("_id")) {
                            Long id = values.getAsLong("_id");
                            if (db.update(
                                    PodNomsInterface.Podcast.TABLE_NAME,
                                    values,
                                    String.format("_id = %d", id),
                                    null) > 0)
                                insertedId = id;
                        } else {
                            insertedId = db.insertWithOnConflict(
                                    PodNomsInterface.Podcast.TABLE_NAME,
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_IGNORE);

                        }
                        db.setTransactionSuccessful();
                    } catch (SQLiteConstraintException ex) {
                        db.beginTransaction();
                        String filter = String.format("url = '%s'", values.get("url").toString());
                        int nRecords = db.update(PodNomsInterface.Podcast.TABLE_NAME,
                                values,
                                filter,
                                null);
                        if (nRecords == 1) {
                            insertedId = _getInsertedId(db, PodNomsInterface.Podcast.TABLE_NAME, filter);
                        }
                        db.setTransactionSuccessful();
                    } catch (Exception ex) {
                        LogHandler.reportError("Error inserting podcast", ex);
                    } finally {
                        if (db.inTransaction()) {
                            db.endTransaction();
                        }
                    }
                }
                return uri.buildUpon().appendPath(Long.toString(insertedId)).build();
            default:
                break;
        }
        return _insert(uri, values);
    }

    private long _getInsertedId(SQLiteDatabase db, String table, String filter) {
        SQLiteStatement stmt = db.compileStatement(String.format("SELECT _id FROM %s WHERE %s", table, filter));
        long ret = stmt.simpleQueryForLong();
        stmt.close();
        return ret;
    }

    @Override
    public boolean onCreate() {
        _datastore = new DataStore(getContext());
        return false;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        arg1.put("date_updated", DateUtils.getContentValueToday());
        int ret = this._datastore.getReadableDatabase().update(
                PodNomsInterface.Entry.TABLE_NAME,
                arg1,
                arg2,
                arg3);

        getContext().getContentResolver().notifyChange(PodNomsInterface.Podcast.CONTENT_URI, null);
        getContext().getContentResolver().notifyChange(PodNomsInterface.Entry.CONTENT_URI, null);
        return ret;
    }
}

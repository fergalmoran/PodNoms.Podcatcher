package com.podnoms.android.podcatcher.services;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.sync.rss.Channel;
import com.podnoms.android.podcatcher.providers.sync.rss.Item;
import com.podnoms.android.podcatcher.providers.sync.rss.RssHandler;
import com.podnoms.android.podcatcher.util.DateUtils;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PodcastRSSQueryService extends IntentService {

    private static final int TEMP_MAX_ELEMENTS = 10;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_ERROR = 3;
    private final SimpleDateFormat _dateParser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.getDefault());

    public PodcastRSSQueryService() {
        super("com.podnoms.android.Sync");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final ResultReceiver receiver = intent.getParcelableExtra("receiver");
        String command = intent.getStringExtra("command");
        Bundle b = new Bundle();
        b.putString("payload", getResources().getString(R.string.sync_starting));
        assert receiver != null;
        assert command != null;
        receiver.send(STATUS_RUNNING, b);
        if (command.equals("query")) {
            String url = intent.getStringExtra("url");
            String resourceUri = intent.getStringExtra("resource_uri");
            _grabPodcast(resourceUri, url, receiver, -1);
        } else if (command.equals("queryall")) {
            Cursor c = getContentResolver().query(PodNomsInterface.Podcast.CONTENT_URI, PodNomsInterface.Podcast.getProjectionMap(), null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        String url = c.getString(c.getColumnIndex(PodNomsInterface.Podcast.COLUMN_NAME_URL));
                        String resourceUri = c.getString(c.getColumnIndex(PodNomsInterface.Podcast.COLUMN_NAME_RESOURCE_URI));
                        _grabPodcast(resourceUri, url, receiver, c.getLong(c.getColumnIndex(PodNomsInterface.Podcast._ID)));
                        c.moveToNext();
                    }
                }
                c.close();
            } else {
                LogHandler.showLog("No podcasts available to sync");
            }
        }
        b.putString("payload", getResources().getString(R.string.sync_finished));
        receiver.send(STATUS_FINISHED, b);
        this.stopSelf();
    }

    private void _grabPodcast(String resourceUri, String urlHref, ResultReceiver receiver, long id) {
        Bundle b = new Bundle();
        try {
            if (!TextUtils.isEmpty(urlHref)) {
                URL url = new URL(urlHref);
                RssHandler handler = new RssHandler();
                Channel feed = handler.parse(url, TEMP_MAX_ELEMENTS);
                if (feed != null) {
                    ContentValues podcastValues = new ContentValues();
                    podcastValues.put("title", feed.getTitle());
                    podcastValues.put("description", feed.getDescription());
                    podcastValues.put("url", urlHref);
                    podcastValues.put("image", feed.getImage());
                    podcastValues.put("resource_uri", resourceUri);
                    podcastValues.put(PodNomsInterface.Podcast.COLUMN_NAME_DATE_CREATED, DateUtils.getContentValueToday());

                    if (id != -1)
                        podcastValues.put("_id", id);

                    Uri podcastInsertUri = getContentResolver().insert(PodNomsInterface.Podcast.CONTENT_URI, podcastValues);
                    long podcastId = ContentUris.parseId(podcastInsertUri);
                    short nItem = 0;
                    int maxElements = PersistentStateHandler.I().getInt(Constants.MAX_EPISODES, 10);
                    int elementCount = Math.min(feed.getItems().size(), maxElements);
                    if (feed.getItems() != null) {
                        for (Item item : feed.getItems()) {
                            b.putString("payload", String.format("Adding entry: %s", item.getTitle()));
                            receiver.send(STATUS_RUNNING, b);
                            ContentValues entryValues = new ContentValues();
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_PODCASTID, podcastId);
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_GUID, item.getGuid());
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_TITLE, item.getTitle());
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_DESCRIPTION, item.getDescription());
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_ENCLOSURE, item.getEnclosure());
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_FILE_LENGTH, item.getLength());
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_IMAGE, item.getImage());
                            entryValues.put(PodNomsInterface.Entry.COLUMN_NAME_DATE_CREATED, item.getDateForSql(_dateParser, --elementCount));
                            getContentResolver().insert(Uri.parse(PodNomsInterface.CONTENT_URI + "/podcast_entry"), entryValues);
                            if (nItem++ == maxElements) {
                                break;
                            }
                        }
                    }
                }
                b.putString("payload", String.format("%s : %s", getResources().getString(R.string.syncing), feed.getTitle()));
                receiver.send(STATUS_RUNNING, b);
            }

        } catch (Exception e) {
            b.putString(Intent.EXTRA_TEXT, e.toString() + " " + urlHref);
            receiver.send(STATUS_ERROR, b);
        }

    }
}

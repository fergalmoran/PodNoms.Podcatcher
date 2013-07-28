package com.podnoms.android.podcatcher.services;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityPodcasts;
import com.podnoms.android.podcatcher.util.Downloader;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class DownloadService extends Service {
    private NotificationManager _notificationManager;
    Notification _notifyWrapper;
    private int _currentProgress = 0;
    public static boolean serviceState = false;
    public static final int MESSAGE_STARTDOWNLOAD = 1;
    public static final int MESSAGE_STARTDOWNLOADFOREPISODE = 2;
    private final Messenger _messenger = new Messenger(new DownloadHandlerReceiver());
    private int _lastPercent = -1;
    private long _currentQueue;
    private final DownloadManager _downloadManager = (DownloadManager) PodNomsApplication.getContext().getSystemService(DOWNLOAD_SERVICE);

    class DownloadHandlerReceiver extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STARTDOWNLOAD:
                    new DownloadFilesTask().execute(PodNomsInterface.Entry.DOWNLOADQUEUE_URI);
                    break;
                case MESSAGE_STARTDOWNLOADFOREPISODE:
                    new DownloadFilesTask().execute(PodNomsInterface.Entry.buildPodcastDetailsUri(msg.arg1));
                default:
                    break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _messenger.getBinder();
    }

    @Override
    public void onCreate() {
        serviceState = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String command = bundle.getString("command");
                if (command.equals("downloadentry")) {
                    long entryId = bundle.getLong("entry_id");
                    new DownloadFilesTask().execute(PodNomsInterface.Entry.buildPodcastDetailsUri(entryId));
                } else if (command.equals("background_download")) {
                    new DownloadFilesTask().execute(PodNomsInterface.Entry.DOWNLOADQUEUE_URI);
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_notificationManager != null)
            _notificationManager.cancel(42);
    }

    private void _setupNotifier(String message, int current, int max) {
        Intent intent = new Intent(this, ActivityPodcasts.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        _notifyWrapper = new Notification(R.drawable.ic_launcher, getResources().getString(R.string.app_name), System.currentTimeMillis());
        _notifyWrapper.flags = _notifyWrapper.flags | Notification.FLAG_ONGOING_EVENT;
        _notifyWrapper.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notifier_download_progress);
        _notifyWrapper.contentIntent = pendingIntent; //TODO
        _notifyWrapper.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_launcher);
        _notifyWrapper.contentView.setTextViewText(R.id.status_text, message);
        _notifyWrapper.contentView.setProgressBar(R.id.status_progress, max, current, false);
        _notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        _notificationManager.notify(42, _notifyWrapper);
    }

    private class DownloadInfo {
        double percent;
        String file;
        long total;
        long current;

        public DownloadInfo(double percent, String file) {
            this.percent = percent;
            this.file = file;

        }
    }


    private class DownloadFilesTask extends AsyncTask<Uri, DownloadInfo, Long> implements Observer {

        @Override
        protected Long doInBackground(Uri... uris) {
            long nCount = 1;
            for (Uri uri : uris) {
                Cursor cur =
                        DownloadService.this.getContentResolver().query(
                                uri,
                                PodNomsInterface.Entry.QUEUE_PROJECTION, null, null, null);
                _downloadEntry(cur, this);
                getContentResolver().notifyChange(uri, null);
                cur.close();
            }
            return nCount;
        }

        @Override
        protected void onProgressUpdate(DownloadInfo... values) {
            int percent = (int) values[0].percent;
            _setupNotifier(
                    String.format("%s %s", getResources().getString(R.string.downloading), values[0].file),
                    percent,
                    100
            );
        }

        @Override
        protected void onPostExecute(Long aLong) {
            if (_notificationManager != null)
                _notificationManager.cancel(42);
            Toast.makeText(DownloadService.this, getResources().getString(R.string.download_complete), Toast.LENGTH_LONG).show();
        }

        @Override
        public void update(final Observable observable, final Object o) {
            final Downloader d = (Downloader) observable;
            if (d.getStatus() == Downloader.ERROR) {
                _updateContentProvider(d.getItemId(), d.getDownloaded());
            } else if (d.getStatus() == Downloader.COMPLETE) {
                _updateContentProvider(d.getItemId(), d.getDownloaded());
                _moveCacheFile(d.getItemId(), d.getCacheFile());
            } else if (d.getStatus() == Downloader.DOWNLOADING) {
                int percent = (int) d.getProgress();
                if (percent != _lastPercent) {
                    _lastPercent = percent;
                    publishProgress(new DownloadInfo(percent, d.getDescription()));
                }
            }
        }
    }

    private void _downloadEntry(Cursor cursor, Observer observer) {
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                try {
                    String urlPath = cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_ENCLOSURE));
                    String description = cursor.getString(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_TITLE));
                    int id = cursor.getInt(cursor.getColumnIndex(PodNomsInterface.Entry._ID));
                    int size = cursor.getInt(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_FILE_LENGTH));
                    int downloaded = cursor.getInt(cursor.getColumnIndex(PodNomsInterface.Entry.COLUMN_NAME_DOWNLOADED));
                    if ((downloaded != size) || (downloaded == 0 && size == 0)) {
                        String fileName = String.format("%s/%s.mp3",
                                PodNomsApplication.getCacheDirectory(),
                                UUID.randomUUID().toString());
                        //user standard downloader
                        if (PodNomsApplication.getBuildVersion() <= 99999) {
                            Downloader downloader = new Downloader(urlPath, fileName, description, id);
                            downloader.addObserver(observer);
                            _lastPercent = -1;
                            downloader.download();
                        } else {
                            _addToDownloadQueue(urlPath, description, id, fileName);
                        }
                    }
                    cursor.moveToNext();
                } catch (Exception e) {
                    LogHandler.reportError("Error downloading", e);
                }
            }
        }
    }

    private void _addToDownloadQueue(String urlPath, final String description, final int id, final String fileName) {
        int downloadFlags = DownloadManager.Request.NETWORK_WIFI;

        Boolean wifiOnly = PersistentStateHandler.I().getBoolean(Constants.WIFI_ONLY, true);
        if (!wifiOnly)
            downloadFlags |= DownloadManager.Request.NETWORK_MOBILE;

        Uri destination = Uri.parse("file://" + fileName);
        _currentQueue = _downloadManager.enqueue(
                new DownloadManager.Request(Uri.parse(urlPath))
                        .setAllowedNetworkTypes(downloadFlags)
                        .setAllowedOverRoaming(false)
                        .setTitle(getResources().getString(R.string.app_name))
                        .setDescription(description)
                        .setDestinationUri(destination));

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                DownloadManager.Query query = new DownloadManager.Query();
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if (downloadId != 0) {
                    query.setFilterById(downloadId);
                    Cursor c = _downloadManager.query(query);
                    if (c.moveToFirst()) {
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                            // <!-- FM
                            String downloadedFile = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            String file = _moveCacheFile(id, downloadedFile);
                            if (TextUtils.isEmpty(file)) {
                                LogHandler.showLog("Error moving file");
                            }
                        }
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private String _moveCacheFile(int itemId, String cacheFile) {
        File downloadsDir = new File(PodNomsApplication.getDownloadsDirectory());
        File downloadedFile = new File(cacheFile);
        if (downloadedFile.exists() && downloadsDir.exists()) {
            File newFile = new File(downloadsDir, downloadedFile.getName());
            if (newFile.exists()) {
                newFile.delete();
            }
            if (downloadedFile.renameTo(newFile)) {
                long actualSize = newFile.length();
                _updateContentProvider(itemId, newFile.getAbsolutePath(), actualSize);
                return newFile.getAbsolutePath();
            } else {
                LogHandler.showLog(String.format("Error renaming %s to %s", cacheFile, newFile.getAbsolutePath()));
            }
        }
        return "";
    }

    private void _updateContentProvider(int id, int downloaded) {
        _updateContentProvider(id, "", downloaded);
    }

    private void _updateContentProvider(int id, String fileName, long downloaded) {
        ContentValues values = new ContentValues();
        values.put(PodNomsInterface.Entry.COLUMN_NAME_LOCAL_FILE, fileName);
        values.put(PodNomsInterface.Entry.COLUMN_NAME_FILE_LENGTH, downloaded);
        values.put(PodNomsInterface.Entry.COLUMN_NAME_DOWNLOADED, downloaded);
        getContentResolver().update(PodNomsInterface.Podcast.CONTENT_URI, values, String.format("_id = %d", id), null);
        getContentResolver().notifyChange(PodNomsInterface.Podcast.CONTENT_URI, null);
        getContentResolver().notifyChange(PodNomsInterface.Entry.CONTENT_URI, null);
    }
}
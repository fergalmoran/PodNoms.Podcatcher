package com.podnoms.android.podcatcher.providers.sync;

import android.content.*;
import android.os.*;
import android.text.TextUtils;
import android.widget.Toast;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.sync.api.ApiHandler;
import com.podnoms.android.podcatcher.services.DownloadService;
import com.podnoms.android.podcatcher.services.PodcastRSSQueryService;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.NotifierHelper;
import com.podnoms.android.podcatcher.util.state.Constants;

public class PodNomsSyncAdapter implements PodNomsSyncResultReceiver.Receiver {
    public PodNomsSyncResultReceiver _receiver;
    private Messenger _serviceHandler;
    private final Context _context;
    private final String _authenticationToken;
    private final int _action;

    private ServiceConnection _downloaderConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            _serviceHandler = new Messenger(service);
            try {
                _serviceHandler.send(Message.obtain(null, _action));
            } catch (RemoteException e) {
                LogHandler.reportError("Error sending sync message", e);
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            _serviceHandler = null;
        }
    };


    public PodNomsSyncAdapter(Context context, int action, String authenticationToken) {
        _context = context;
        _action = action;
        _authenticationToken = authenticationToken;
    }

    private static final int MSG_STARTQUERY = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_STARTQUERY) {
                _receiver = new PodNomsSyncResultReceiver(new Handler());
                _receiver.setReceiver(PodNomsSyncAdapter.this);

                final Intent intent = new Intent(Intent.ACTION_SYNC, null, _context, PodcastRSSQueryService.class);
                intent.putExtra("receiver", _receiver);
                intent.putExtra("command", "queryall");
                _context.startService(intent);
            }
        }
    };

    public void startSync() {

        NotifierHelper.sendNotification(_context, null, R.string.app_name, _context.getResources().getString(R.string.sync_starting), 1, false, false);
        new Thread() {
            @Override
            public void run() {
                ApiHandler.ApiToken token = _getApiKey();
                if (token != null && (!TextUtils.isEmpty(token.username) && !TextUtils.isEmpty(token.token))) {
                    _getPodcastsFromServer(token);
                }
                //send message to start the sync.
                handler.sendEmptyMessage(MSG_STARTQUERY);
            }
        }.start();

    }

    private ApiHandler.ApiToken _getApiKey() {
        ApiHandler handler = new ApiHandler();
        return handler.getApiKey(
                String.format("%s?token=%s&audience=%s",
                        PodNomsApplication.getApiAuthTokenAddress(),
                        _authenticationToken,
                        Constants.AUDIENCE));
    }

    public void startSyncForPodcast(Long id) {
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, _context, DownloadService.class);
        intent.putExtra("receiver", _receiver);
        intent.putExtra("command", "syncpodcast");
        intent.putExtra("entry_id", id);
        _context.startService(intent);
    }

    public void startSyncForEntry(long entryId) {
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, _context, DownloadService.class);
        intent.putExtra("receiver", _receiver);
        intent.putExtra("command", "downloadentry");
        intent.putExtra("entry_id", entryId);
        _context.startService(intent);
    }

    private void _getPodcastsFromServer(ApiHandler.ApiToken token) {
        ApiHandler handler = new ApiHandler(token);
        ApiHandler.ApiPodcast podcasts = handler.getPodcastList(PodNomsApplication.getPodcastListAddress());
        if (podcasts != null) {
            for (ApiHandler.ApiPodcast.ApiPodcastEntry entry : podcasts.objects) {
                ContentValues podcastValues = new ContentValues();
                podcastValues.put("url", entry.url);
                podcastValues.put("resource_uri", entry.resource_uri);
                podcastValues.put("description", entry.description);
                _context.getContentResolver().insert(PodNomsInterface.Podcast.CONTENT_URI, podcastValues);
            }
        }
    }

    private void _downloadEpisodes() {
        Intent serviceIntent = new Intent(_context, DownloadService.class);
        try {
            _context.bindService(new Intent(_context, DownloadService.class), _downloaderConnection, DownloadService.MESSAGE_STARTDOWNLOAD);
        } catch (ReceiverCallNotAllowedException ex) {
            //call  has come from Broadcast receiver
            //start the service rather than trying to bind to it
            serviceIntent.putExtra("command", "background_download");
            _context.startService(serviceIntent);
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        String payload = "";
        if (resultData.containsKey("payload"))
            payload = resultData.get("payload").toString();
        switch (resultCode) {
            case PodcastRSSQueryService.STATUS_RUNNING:
                NotifierHelper.sendNotification(_context, null, R.string.app_name, payload, 1, false, false);
                break;
            case PodcastRSSQueryService.STATUS_FINISHED:
                NotifierHelper.killNotification(_context);
                Toast.makeText(_context, _context.getResources().getString(R.string.sync_finished), Toast.LENGTH_LONG).show();
                _downloadEpisodes();
                break;
            case PodcastRSSQueryService.STATUS_ERROR:
                String error = "";
                if (resultData.containsKey(Intent.EXTRA_TEXT))
                    error = resultData.getString(Intent.EXTRA_TEXT);
                Toast.makeText(_context, _context.getResources().getString(R.string.sync_error) + error, Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

}

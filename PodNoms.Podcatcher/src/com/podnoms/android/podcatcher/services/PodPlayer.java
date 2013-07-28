package com.podnoms.android.podcatcher.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityEntryDetails;
import com.podnoms.android.podcatcher.util.LogHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PodPlayer extends Service {
    private MediaPlayer _player = null;

    private ArrayList<Messenger> _clients = new ArrayList<Messenger>();
    public static final int MESSAGE_CONNECT = 1;
    public static final int MESSAGE_DISCONNECT = 2;

    public static final int MESSAGE_PREVIOUS = 3;
    public static final int MESSAGE_REWIND = 4;
    public static final int MESSAGE_PLAY = 5;
    public static final int MESSAGE_PAUSE = 6;
    public static final int MESSAGE_FFWD = 7;
    public static final int MESSAGE_NEXT = 8;
    public static final int MESSAGE_UPDATEPROGRESS = 9;
    public static final int MESSAGE_UPDATEBUFFER = 10;

    public static final int MESSAGE_SETLENGTH = 11;
    public static final int MESSAGE_INFO = 12;
    public static final int MESSAGE_TRACK_LOCATION = 13;
    public static final int MESSAGE_SETPOSITION = 14;
    public static final int MESSAGE_ERROR = 15;
    public static final int MESSAGE_TRACK_NAME = 16;
    public static final int MESSAGE_NEWTRACK = 17;

    private final Messenger _messenger = new Messenger(new IncomingHandler());
    private Timer _timer;
    private static boolean _isRunning = false;
    private String _trackLocation = "";
    private String _trackName;
    private int _trackId;

    private static final int NOTIFIER_ID = 0x23067;

    @Override
    public IBinder onBind(Intent intent) {
        return _messenger.getBinder();
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CONNECT:
                    try {
                        Messenger client = msg.replyTo;
                        client.send(Message.obtain(null, MESSAGE_TRACK_NAME, TextUtils.isEmpty(_trackName) ? "" : _trackName));
                        _clients.add(client);
                    } catch (RemoteException e) {
                        LogHandler.reportError("Error sending connection message to client: ", e);
                    }
                    break;
                case MESSAGE_DISCONNECT:
                    _clients.remove(msg.replyTo);
                    break;
                case MESSAGE_TRACK_LOCATION:
                    _trackLocation = (String) msg.obj;
                    _trackLocation = (String) msg.obj;
                    break;
                case MESSAGE_PLAY:
                    _startPlayer(msg.arg1);
                    break;
                case MESSAGE_PAUSE:
                    if (_player != null) {
                        _player.pause();
                    }
                    _cancelNotification();
                    break;
                case MESSAGE_SETPOSITION:
                    if (_player != null)
                        _player.seekTo(msg.arg1);
                    break;
                case MESSAGE_TRACK_NAME:
                    _trackName = (String) msg.obj;
                    _trackId = msg.arg1;
                    break;
                case MESSAGE_NEWTRACK:
                    _stopPlayer();
                    _trackName = (String) msg.obj;
                    _startPlayer(msg.arg1);
                    break;
                default:
                    LogHandler.showLog(String.format("Unknown message type: %d", msg.what));
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _isRunning = true;
    }

    private void _notifySubscribers(int message, String payload) {
        for (Messenger messenger : _clients) {
            try {
                messenger.send(Message.obtain(null, message, payload));
            } catch (RemoteException e) {
                LogHandler.showLog("Removing stale client");
                _clients.remove(messenger);
            }
        }
    }

    private void _notifySubscribers(int message, int payload) {
        _notifySubscribers(message, payload, -1);
    }

    private void _notifySubscribers(int message, int payload1, int payload2) {
        for (Messenger messenger : _clients) {
            try {
                messenger.send(Message.obtain(null, message, payload1, payload2));
            } catch (RemoteException e) {
                LogHandler.showLog("Removing stale client");
                _clients.remove(messenger);
            }
        }
    }

    private void _cancelNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFIER_ID);
        nm.cancelAll();
    }

    private void _showNotification() {
        Intent intent = new Intent(this, ActivityEntryDetails.class);
        final Uri entriesUri = PodNomsInterface.Entry.buildPodcastEntriesUri(_trackId);
        intent.putExtra("uri", entriesUri);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFIER_ID, intent, PendingIntent.FLAG_ONE_SHOT);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.service_label))
                .setContentText(_trackName)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .getNotification();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(getApplicationContext().getResources().getString(R.string.service_started), NOTIFIER_ID, notification);
    }

    private void _startTimer() {
        _timer = new Timer();
        _timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onTimerTick();
            }
        }, 0, 100L);
    }

    private void _startPlayer(int currentPosition) {
        if (!TextUtils.isEmpty(_trackLocation)) {
            if (!new File(_trackLocation).exists()) {
                _notifySubscribers(MESSAGE_ERROR, String.format("PodPlayer: File %s does not exist", _trackLocation));
                return;
            }
            if (_player == null) {
                _player = MediaPlayer.create(this, Uri.parse(_trackLocation));
                if (_player == null) {
                    _notifySubscribers(MESSAGE_ERROR, -1);
                } else {
                    _player.seekTo(currentPosition);
                    _player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            _notifySubscribers(MESSAGE_SETLENGTH, _player.getDuration());
                            _startTimer();
                        }
                    });
                    _player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                            _notifySubscribers(MESSAGE_UPDATEBUFFER, i);
                        }
                    });
                    _player.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                        @Override
                        public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                            _notifySubscribers(MESSAGE_INFO, i, i1);
                            return false;
                        }
                    });
                    _player.start();
                }
            } else{
                _player.start();
            }
            _showNotification();
        } else {
            LogHandler.showLog("Starting player with empty track location..");
        }

    }

    private void _stopPlayer() {
        _player.stop();
        _player = null;
        _cancelNotification();
        _trackName = "";
        _timer.cancel();
    }

    private void onTimerTick() {
        int position = _player.getCurrentPosition();
        _notifySubscribers(MESSAGE_UPDATEPROGRESS, position);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static boolean isRunning() {
        return _isRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_player != null) {
            _player.stop();
            _player.release();
        }
        if (_timer != null) {
            _timer.cancel();
        }
        _cancelNotification();
        LogHandler.showLog("Service Stopped.");
        _isRunning = false;
    }

}
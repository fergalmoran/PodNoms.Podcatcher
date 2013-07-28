package com.podnoms.android.podcatcher.aud;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RemoteControlClient;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.*;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityEntryDetails;
import com.podnoms.android.podcatcher.ui.widgets.home.PodNomsPlayerAppWidgetProvider;
import com.podnoms.android.podcatcher.util.DeviceUtils;
import com.podnoms.android.podcatcher.util.LogHandler;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.Vector;

/**
 * Provides "background" aud playback capabilities, allowing the
 * user to switch between activities without stopping playback.
 */
public class MediaPlayerService extends Service {
    /**
     * used to specify whether enqueue() should start playing
     * the new list of files right away, next or once all the currently
     * queued files have been played
     */
    public static final int NOW = 1;
    public static final int NEXT = 2;
    public static final int LAST = 3;
    public static final int PLAYBACKSERVICE_STATUS = 1;

    public static final int SHUFFLE_NONE = 0;
    public static final int SHUFFLE_NORMAL = 1;
    public static final int SHUFFLE_AUTO = 2;

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_CURRENT = 1;
    public static final int REPEAT_ALL = 2;

    public static final String SERVICECMD = "com.podnoms.android.podcatcher.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPLAY = "play";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";
    public static final String CMDRWND = "rwnd";
    public static final String CMDFFWD = "ffwd";

    public static final String PLAYSTATE_CHANGED = "com.podnoms.android.podcatcher.playstatechanged";
    public static final String META_CHANGED = "com.podnoms.android.podcatcher.metachanged";
    public static final String TOGGLEPAUSE_ACTION = "com.podnoms.android.podcatcher.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION = "com.podnoms.android.podcatcher.musicservicecommand.pause";
    public static final String FFWD_ACTION = "com.podnoms.android.podcatcher.musicservicecommand.ffwd";
    public static final String RWND_ACTION = "com.podnoms.android.podcatcher.musicservicecommand.rwnd";
    public static final String PREVIOUS_ACTION = "com.podnoms.android.podcatcher.musicservicecommand.previous";
    public static final String NEXT_ACTION = "com.podnoms.android.podcatcher.musicservicecommand.next";

    private static final int TRACK_ENDED = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int FOCUSCHANGE = 4;
    private static final int FADEDOWN = 5;
    private static final int FADEUP = 6;
    private static final int TRACK_WENT_TO_NEXT = 7;
    private static final int MAX_HISTORY_SIZE = 100;

    private static final int TRACK_PROGRESS = 1;

    private MultiPlayer _player;
    private String _fileToPlay;
    private int mShuffleMode = SHUFFLE_NONE;
    private int _repeatMode = REPEAT_NONE;
    private int _mediaMountedCount = 0;
    private long[] mAutoShuffleList = null;
    private long[] mPlayList = null;
    private int mPlayListLen = 0;
    private Vector<Integer> _history = new Vector<Integer>(MAX_HISTORY_SIZE);
    private int mPlayPos = -1;
    private int mNextPlayPos = -1;
    private static final String LOGTAG = "MediaPlaybackService";
    private final Shuffler mRand = new Shuffler();
    private int mOpenFailedCounter = 0;
    private String _title;
    private String _description;

    private final static int IDCOLIDX = 0;
    private final static int PODCASTCOLIDX = 8;
    private final static int BOOKMARKCOLIDX = 9;
    private BroadcastReceiver mUnmountReceiver = null;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean _isSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    private AudioManager _audioManager;
    // used to track what type of aud focus loss caused the playback to pause
    private boolean mPausedByTransientLossOfFocus = false;
    Bitmap _albumArt;

    RemoteControlClientCompat mRemoteControlClientCompat;

    String[] mCursorCols = new String[]{
            "aud._id AS _id",             // index must match IDCOLIDX below
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.IS_PODCAST, // index must match PODCASTCOLIDX below
            MediaStore.Audio.Media.BOOKMARK    // index must match BOOKMARKCOLIDX below
    };
    // We use this to distinguish between different cards when saving/restoring playlists.
    // This will have to change if we want to support multiple simultaneous cards.
    private int mCardId;

    private PodNomsPlayerAppWidgetProvider mAppWidgetProvider = PodNomsPlayerAppWidgetProvider.getInstance();

    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;

    private Handler _mediaPlayerHandle = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            LogHandler.showLog("_mediaPlayerHandle.handleMessage " + msg.what);
            switch (msg.what) {
                case FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        _mediaPlayerHandle.sendEmptyMessageDelayed(FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    _player.setVolume(mCurrentVolume);
                    break;
                case FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        _mediaPlayerHandle.sendEmptyMessageDelayed(FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    _player.setVolume(mCurrentVolume);
                    break;
                case SERVER_DIED:
                    break;
                case TRACK_WENT_TO_NEXT:
                    mPlayPos = mNextPlayPos;
                    notifyChange(META_CHANGED);
                    updateNotification();
                    break;
                case TRACK_ENDED:
                    if (_repeatMode == REPEAT_CURRENT) {
                        seek(0);
                        play();
                    } else {
                        stop();
                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;

                case FOCUSCHANGE:
                    // This code is here so we can better synchronize it with the code that
                    // handles fade-in
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                            if (isPlaying()) {
                                mPausedByTransientLossOfFocus = false;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            _mediaPlayerHandle.removeMessages(FADEUP);
                            _mediaPlayerHandle.sendEmptyMessage(FADEDOWN);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                            if (isPlaying()) {
                                mPausedByTransientLossOfFocus = true;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                            if (!isPlaying() && mPausedByTransientLossOfFocus) {
                                mPausedByTransientLossOfFocus = false;
                                mCurrentVolume = 0f;
                                _player.setVolume(mCurrentVolume);
                                play(); // also queues a fade-in
                            } else {
                                _mediaPlayerHandle.removeMessages(FADEDOWN);
                                _mediaPlayerHandle.sendEmptyMessage(FADEUP);
                            }
                            break;
                        default:
                            Log.e(LOGTAG, "Unknown aud focus change code");
                    }
                    break;

                default:
                    break;
            }
        }
    };
    protected Handler _progressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TRACK_PROGRESS:
                    long pos = MediaPlayerService.this.position();
                    if (MediaPlayerService.this.isPlaying()) {
                        msg = obtainMessage(TRACK_PROGRESS);
                        ContentValues values = new ContentValues();
                        values.put(PodNomsInterface.Entry.COLUMN_NAME_POSITION, pos);

                        getContentResolver().update(PodNomsInterface.Entry.CONTENT_URI, values, String.format("_id = %d", getAudioId()), null);
                        updateNotification();
                        sendMessageDelayed(msg, 10000 - (pos % 1000));
                    }
                    break;
            }
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            LogHandler.showLog("mIntentReceiver.onReceive " + action + " / " + cmd);
            if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDRWND.equals(cmd) || RWND_ACTION.equals(action)) {
                rewind(-1, 5000);
            } else if (CMDFFWD.equals(cmd) || FFWD_ACTION.equals(action)) {
                fast_forward(-1, 5000);
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            } else if (PodNomsPlayerAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets, probably
                // because they were just added.
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider.performUpdate(MediaPlayerService.this, appWidgetIds);
            }
        }
    };

    private OnAudioFocusChangeListener _audioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            _mediaPlayerHandle.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };
    private long _audioId = -1;
    private int _audioSessionId = -1;
    private ComponentName _mediaButtonIntentReciver;

    public MediaPlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //_albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.lock_screen_header);
        _audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        _mediaButtonIntentReciver = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        _audioManager.registerMediaButtonEventReceiver(_mediaButtonIntentReciver);

        mCardId = DeviceUtils.getCardId(this);

        registerExternalStorageListener();

        // Needs to be done in this thread, since otherwise ApplicationContext.getPowerManager() crashes.
        _player = new MultiPlayer();
        _player.setHandler(_mediaPlayerHandle);

        notifyChange(META_CHANGED);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(PLAYSTATE_CHANGED);
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(RWND_ACTION);
        commandFilter.addAction(FFWD_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        Message msg = _delayedStopHandler.obtainMessage();
        _delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    private void _createRemoteControlClient() {
        // Create the media remote service
        if (mRemoteControlClientCompat == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(_mediaButtonIntentReciver);
            mRemoteControlClientCompat = new RemoteControlClientCompat(
                    PendingIntent.getBroadcast(this /*context*/,
                            0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
            RemoteControlHelper.registerRemoteControlClient(_audioManager,
                    mRemoteControlClientCompat);
        }

        mRemoteControlClientCompat.setPlaybackState(
                RemoteControlClient.PLAYSTATE_PLAYING);

        mRemoteControlClientCompat.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD |
                        RemoteControlClient.FLAG_KEY_MEDIA_REWIND |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                        RemoteControlClient.FLAG_KEY_MEDIA_STOP);

        // Update the remote controls
        mRemoteControlClientCompat.editMetadata(true)
                .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, _title)
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, _title)
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, _description)
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                        _player.duration())
                        // TODO: fetch real item artwork
                /*.putBitmap(
                        RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                        _albumArt)*/
                .apply();
    }

    @Override
    public void onDestroy() {
        // Check that we're not being destroyed while something is still playing.
        if (isPlaying()) {
            Log.e(LOGTAG, "Service being destroyed while still playing.");
        }
        // release all MediaPlayer resources, including the native player and wakelocks
        Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(i);
        _player.release();
        _player = null;

        _audioManager.abandonAudioFocus(_audioFocusListener);

        // make sure there aren't any other messages coming
        _delayedStopHandler.removeCallbacksAndMessages(null);
        _mediaPlayerHandle.removeCallbacksAndMessages(null);

        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
        mWakeLock.release();
        super.onDestroy();
    }

    private final char hexdigits[] = new char[]{
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    @Override
    public IBinder onBind(Intent intent) {
        _delayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        _delayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        _delayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            LogHandler.showLog("onStartCommand " + action + " / " + cmd);

            if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDFFWD.equals(cmd) || FFWD_ACTION.equals(action)) {
                fast_forward(-1, 5000);
            } else if (CMDRWND.equals(cmd) || RWND_ACTION.equals(action)) {
                rewind(-1, 5000);
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            }
            updateNotification();
        }

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        _delayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = _delayedStopHandler.obtainMessage();
        _delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

        // Take a snapshot of the current playlist
        if (isPlaying() || mPausedByTransientLossOfFocus) {
            // something is currently playing, or will be playing once 
            // an in-progress action requesting aud focus ends, so don't stop the service now.
            return true;
        }

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between tracks.
        if (mPlayListLen > 0 || _mediaPlayerHandle.hasMessages(TRACK_ENDED)) {
            Message msg = _delayedStopHandler.obtainMessage();
            _delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }

    private Handler _delayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (isPlaying() || mPausedByTransientLossOfFocus || mServiceInUse
                    || _mediaPlayerHandle.hasMessages(TRACK_ENDED)) {
                return;
            }
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            stopSelf(mServiceStartId);
        }
    };

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     *
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications.
     * The intent will call closeExternalStorageFiles() if the external media
     * is going to be ejected, so applications can clean up any files they have open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        _mediaMountedCount++;
                        mCardId = DeviceUtils.getCardId(MediaPlayerService.this);
                        notifyChange(META_CHANGED);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     * The intent that is sent contains the following data
     * for the currently playing track:
     * "id" - Integer: the database row ID
     * "artist" - String: the name of the artist
     * "album" - String: the name of the album
     * "track" - String: the name of the track
     * The intent has an action that is one of
     * "com.podnoms.android.podcatcher.metachanged"
     * "com.podnoms.android.podcatcher.playbackcomplete"
     * "com.podnoms.android.podcatcher.playstatechanged"
     * respectively indicating that a new track has
     * started playing, that the playback queue has
     * changed, that playback has stopped because
     * the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {

        Intent i = new Intent(what);
        i.putExtra("id", Long.valueOf(getAudioId()));
        i.putExtra("artist", getTrackTitle());
        i.putExtra("album", getTrackDescription());
        i.putExtra("playing", isPlaying());
        sendStickyBroadcast(i);

        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, what);
    }

    private void ensurePlayListCapacity(int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            long[] newlist = new long[size * 2];
            int len = mPlayList != null ? mPlayList.length : mPlayListLen;
            for (int i = 0; i < len; i++) {
                newlist[i] = mPlayList[i];
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }

    // insert the list of songs at the specified position in the playlist
    private void addToPlayList(long[] list, int position) {
        int addlen = list.length;
        if (position < 0) { // overwrite
            mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(mPlayListLen + addlen);
        if (position > mPlayListLen) {
            position = mPlayListLen;
        }

        // move part of list after insertion point
        int tailsize = mPlayListLen - position;
        for (int i = tailsize; i > 0; i--) {
            mPlayList[position + i] = mPlayList[position + i - addlen];
        }

        // copy list into playlist
        for (int i = 0; i < addlen; i++) {
            mPlayList[position + i] = list[i];
        }
        mPlayListLen += addlen;
        if (mPlayListLen == 0) {
            notifyChange(META_CHANGED);
        }
    }

    /**
     * Opens the specified file and readies it for playback.
     *
     * @param path The full path of the file to be opened.
     */
    public boolean open(String path) {
        synchronized (this) {
            if (path == null) {
                return false;
            }

            _fileToPlay = path;
            _player.setDataSource(_fileToPlay);
            if (_player.isInitialized()) {
                mOpenFailedCounter = 0;
                _createRemoteControlClient();
                return true;
            }
            stop(true);
            return false;
        }
    }

    public void fast_forward(long positionOverride, int milliseconds) {
        long pos = positionOverride < 0 ? position() : positionOverride;
        pos += milliseconds;
        seek(pos);
    }

    public void rewind(long positionOverride, int milliseconds) {
        long pos = positionOverride < 0 ? position() : positionOverride;
        pos -= milliseconds;
        seek(pos);
    }

    public void play() {
        _progressHandler.sendEmptyMessage(TRACK_PROGRESS);

        _audioManager.requestAudioFocus(_audioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        _audioManager.registerMediaButtonEventReceiver(new ComponentName(this.getPackageName(),
                MediaButtonIntentReceiver.class.getName()));

        if (_player.isInitialized()) {
            // if we are at the end of the song, go to the next song first
            long duration = _player.duration();
            if (_repeatMode != REPEAT_CURRENT && duration > 2000 &&
                    _player.position() >= duration - 2000) {
                seek(0);
            }

            _player.start();
            // make sure we fade in, in case a previous fadein was stopped because
            // of another focus loss
            _mediaPlayerHandle.removeMessages(FADEDOWN);
            _mediaPlayerHandle.sendEmptyMessage(FADEUP);

            updateNotification();
            if (!_isSupposedToBePlaying) {
                _isSupposedToBePlaying = true;
                notifyChange(PLAYSTATE_CHANGED);
            }
        }
        if (mRemoteControlClientCompat != null)
            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);
    }

    public void updateNotification() {

        Intent intent = new Intent(this, ActivityEntryDetails.class);
        intent.putExtra("file", _fileToPlay);
        intent.putExtra("title", getTrackTitle());
        intent.putExtra("description", getTrackDescription());
        intent.putExtra("position", position());
        intent.putExtra("entry_id", getAudioId());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(0)
                .setContentTitle(_title)
                .setContentText(_description)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                ;// <!-- FM .setPriority(NotificationCompat.FLAG_HIGH_PRIORITY);

        Intent pauseIntent = new Intent(this, MediaPlayerService.class);
        pauseIntent.setAction(TOGGLEPAUSE_ACTION);
        pauseIntent.putExtra("command", CMDTOGGLEPAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);

        Intent forwardIntent = new Intent(this, MediaPlayerService.class);
        forwardIntent.setAction(FFWD_ACTION);
        forwardIntent.putExtra("command", CMDFFWD);
        PendingIntent forwardPendingIntent = PendingIntent.getService(this, 0, forwardIntent, 0);

        /*
        if (isPlaying())
            builder.addAction(R.drawable.player_pause, getString(R.string.player_pause), pausePendingIntent);
        else
            builder.addAction(R.drawable.player_play, getString(R.string.player_play), pausePendingIntent);
        builder.addAction(R.drawable.player_ffwd, getString(R.string.player_ffwd), forwardPendingIntent);
        */
        Notification notification = builder.build();
        startForeground(PLAYBACKSERVICE_STATUS, notification);

    }

    private long getAudioId() {
        return _audioId;
    }

    private void setAudioId(long id) {
        _audioId = id;
    }

    private int getAudioSessionId() {
        return _audioSessionId;
    }

    private void setAudioSessionId(int id) {
        _audioSessionId = id;
    }

    private void stop(boolean remove_status_icon) {
        if (_player.isInitialized()) {
            _player.stop();
        }
        _fileToPlay = null;

        if (remove_status_icon) {
            gotoIdleState();
        } else {
            stopForeground(false);
        }
        if (remove_status_icon) {
            _isSupposedToBePlaying = false;
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        synchronized (this) {
            _mediaPlayerHandle.removeMessages(FADEUP);
            if (isPlaying()) {
                _player.pause();
                gotoIdleState();
                _isSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
            }
            if (mRemoteControlClientCompat != null)
                mRemoteControlClientCompat.setPlaybackState(
                        RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    /**
     * Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     *         we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return _isSupposedToBePlaying;
    }


    private void gotoIdleState() {
        _delayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = _delayedStopHandler.obtainMessage();
        _delayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        //stopForeground(true);
    }


    // check that the specified idx is not in the history (but only look at at
    // most lookbacksize entries in the history)
    private boolean wasRecentlyUsed(int idx, int lookbacksize) {

        // early exit to prevent infinite loops in case idx == mPlayPos
        if (lookbacksize == 0) {
            return false;
        }

        int histsize = _history.size();
        if (histsize < lookbacksize) {
            Log.d(LOGTAG, "lookback too big");
            lookbacksize = histsize;
        }
        int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            long entry = _history.get(maxidx - i);
            if (entry == idx) {
                return true;
            }
        }
        return false;
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int previous;
        private Random random = new Random();

        public int nextInt(int interval) {
            int ret;
            do {
                ret = random.nextInt(interval);
            } while (ret == previous && interval > 1);
            previous = ret;
            return ret;
        }
    }

    ;

    private boolean makeAutoShuffleList() {
        ContentResolver res = getContentResolver();
        Cursor c = null;
        try {
            c = res.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID}, MediaStore.Audio.Media.IS_MUSIC + "=1",
                    null, null);
            if (c == null || c.getCount() == 0) {
                return false;
            }
            int len = c.getCount();
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                c.moveToNext();
                list[i] = c.getLong(0);
            }
            mAutoShuffleList = list;
            return true;
        } catch (RuntimeException ex) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    public int getMediaMountedCount() {
        return _mediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if
     * no file is currently playing.
     */
    public String getPath() {
        return _fileToPlay;
    }

    public void setTrackTitle(String title) {
        _title = title;
    }

    public void setTrackDescription(String description) {
        _description = description;
    }

    public String getTrackTitle() {
        return _title;
    }


    public String getTrackDescription() {
        return _description;
    }

    /**
     * Returns the duration of the file in milliseconds.
     * Currently this method returns -1 for the duration of MIDI files.
     */
    public long duration() {
        if (_player.isInitialized()) {
            return _player.duration();
        }
        return -1;
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (_player != null) {
            if (_player.isInitialized()) {
                return _player.position();
            }
        }
        return -1;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        if (_player.isInitialized()) {
            if (pos < 0) pos = 0;
            if (pos > _player.duration()) pos = _player.duration();
            return _player.seek(pos);
        }
        return -1;
    }


    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */
    private class MultiPlayer {
        private CompatMediaPlayer _currentMediaPlayer = new CompatMediaPlayer();
        private CompatMediaPlayer _nextMediaPlayer;
        private Handler _handler;
        private boolean _isInitialised = false;

        public MultiPlayer() {
            _currentMediaPlayer.setWakeMode(MediaPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSource(String path) {
            _isInitialised = setDataSourceImpl(_currentMediaPlayer, path);
            if (_isInitialised) {
                setNextDataSource(null);
            }
        }

        private boolean setDataSourceImpl(MediaPlayer player, String path) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(MediaPlayerService.this, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            }
            player.setOnCompletionListener(listener);
            player.setOnErrorListener(errorListener);
            Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(i);
            return true;
        }

        public void setNextDataSource(String path) {
            _currentMediaPlayer.setNextMediaPlayer(null);
            if (_nextMediaPlayer != null) {
                _nextMediaPlayer.release();
                _nextMediaPlayer = null;
            }
            if (path == null) {
                return;
            }
            _nextMediaPlayer = new CompatMediaPlayer();
            _nextMediaPlayer.setWakeMode(MediaPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
            _nextMediaPlayer.setAudioSessionId(getAudioSessionId());
            if (setDataSourceImpl(_nextMediaPlayer, path)) {
                _currentMediaPlayer.setNextMediaPlayer(_nextMediaPlayer);
            } else {
                // failed to open next, we'll transition the old fashioned way,
                // which will skip over the faulty file
                _nextMediaPlayer.release();
                _nextMediaPlayer = null;
            }
        }

        public boolean isInitialized() {
            return _isInitialised;
        }

        public void start() {
            LogHandler.showLog("MultiPlayer.start called");
            _currentMediaPlayer.start();
        }

        public void stop() {
            _currentMediaPlayer.reset();
            _isInitialised = false;
            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            _currentMediaPlayer.release();
        }

        public void pause() {
            _currentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            _handler = handler;
        }

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if (mp == _currentMediaPlayer && _nextMediaPlayer != null) {
                    _currentMediaPlayer.release();
                    _currentMediaPlayer = _nextMediaPlayer;
                    _nextMediaPlayer = null;
                    _handler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                } else {
                    // Acquire a temporary wakelock, since when we return from
                    // this callback the MediaPlayer will release its wakelock
                    // and allow the device to go to sleep.
                    // This temporary wakelock is released when the RELEASE_WAKELOCK
                    // message is processed, but just in case, put a timeout on it.
                    mWakeLock.acquire(30000);
                    _handler.sendEmptyMessage(TRACK_ENDED);
                    _handler.sendEmptyMessage(RELEASE_WAKELOCK);
                }
            }
        };

        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        _isInitialised = false;
                        _currentMediaPlayer.release();
                        // Creating a new MediaPlayer and settings its wakemode does not
                        // require the media service, so it's OK to do this now, while the
                        // service is still being restarted
                        _currentMediaPlayer = new CompatMediaPlayer();
                        _currentMediaPlayer.setWakeMode(MediaPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
                        _handler.sendMessageDelayed(_handler.obtainMessage(SERVER_DIED), 2000);
                        return true;
                    default:
                        Log.d("MultiPlayer", "Error: " + what + "," + extra);
                        break;
                }
                return false;
            }
        };

        public long duration() {
            return _currentMediaPlayer.getDuration();
        }

        public long position() {
            return _currentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            _currentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            _currentMediaPlayer.setVolume(vol, vol);
        }

        public void setAudioSessionId(int sessionId) {
            _currentMediaPlayer.setAudioSessionId(sessionId);
        }

        public int getAudioSessionId() {
            return _currentMediaPlayer.getAudioSessionId();
        }
    }

    static class CompatMediaPlayer extends MediaPlayer implements OnCompletionListener {

        private boolean mCompatMode = true;
        private MediaPlayer mNextPlayer;
        private OnCompletionListener mCompletion;

        public CompatMediaPlayer() {
            try {
                MediaPlayer.class.getMethod("setNextMediaPlayer", MediaPlayer.class);
                mCompatMode = false;
            } catch (NoSuchMethodException e) {
                mCompatMode = true;
                super.setOnCompletionListener(this);
            }
        }

        public void setNextMediaPlayer(MediaPlayer next) {
            if (mCompatMode) {
                mNextPlayer = next;
            } else {
                super.setNextMediaPlayer(next);
            }
        }

        @Override
        public void setOnCompletionListener(OnCompletionListener listener) {
            if (mCompatMode) {
                mCompletion = listener;
            } else {
                super.setOnCompletionListener(listener);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mNextPlayer != null) {
                // as it turns out, starting a new MediaPlayer on the completion
                // of a previous player ends up slightly overlapping the two
                // playbacks, so slightly delaying the start of the next player
                // gives a better user experience
                SystemClock.sleep(50);
                mNextPlayer.start();
            }
            mCompletion.onCompletion(this);
        }
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends IMediaPlaybackService.Stub {
        WeakReference<MediaPlayerService> mService;

        ServiceStub(MediaPlayerService service) {
            mService = new WeakReference<MediaPlayerService>(service);
        }

        public void openFile(String path) {
            mService.get().open(path);
        }

        public boolean isPlaying() {
            return mService.get().isPlaying();
        }

        public void stop() {
            mService.get().stop();
        }

        public void pause() {
            mService.get().pause();
        }

        public void play() {
            mService.get().play();
        }

        public void setTrackTitle(String title) {
            mService.get().setTrackTitle(title);
        }

        public void setTrackDescription(String description) {
            mService.get().setTrackDescription(description);
        }

        public int getAudioSessionId() {
            return mService.get().getAudioSessionId();
        }

        @Override
        public void setAudioSessionId(int audioSessionId) throws RemoteException {
            mService.get().setAudioSessionId(audioSessionId);
        }

        public long getAudioId() {
            return mService.get().getAudioId();
        }

        @Override
        public void setAudioId(long audioId) throws RemoteException {
            mService.get().setAudioId(audioId);
        }

        public String getTrackTitle() {
            return mService.get().getTrackTitle();
        }

        public String getTrackDescription() {
            return mService.get().getTrackDescription();
        }


        public String getPath() {
            return mService.get().getPath();
        }

        public long position() {
            return mService.get().position();
        }

        public long duration() {
            return mService.get().duration();
        }

        public long seek(long pos) {
            return mService.get().seek(pos);
        }

        @Override
        public void jumpForward(long positionOverride, int milliseconds) throws RemoteException {
            mService.get().fast_forward(positionOverride, milliseconds);
        }

        @Override
        public void jumpBackward(long positionOverride, int milliseconds) throws RemoteException {
            mService.get().rewind(positionOverride, milliseconds);
        }

        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }

    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    private final IBinder mBinder = new ServiceStub(this);
}

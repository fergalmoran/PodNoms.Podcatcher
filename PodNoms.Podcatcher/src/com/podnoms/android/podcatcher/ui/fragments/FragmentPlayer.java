package com.podnoms.android.podcatcher.ui.fragments;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.aud.IMediaPlaybackService;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.aud.MediaPlayerService;
import com.podnoms.android.podcatcher.services.binder.BinderUtils;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityPodcasts;
import com.podnoms.android.podcatcher.ui.widgets.RepeatingImageButton;
import com.podnoms.android.podcatcher.util.DateUtils;
import com.podnoms.android.podcatcher.util.LogHandler;

public class FragmentPlayer extends BaseFragment {
    private IMediaPlaybackService _playbackService = null;
    private ImageButton _btnPlay;
    private BinderUtils.ServiceToken _serviceToken;
    private boolean mSeeking = false;
    private long _startSeekPos = 0;
    private long _lastSeekEventTime;
    private long _positionOverride = -1;
    private long _duration;
    private boolean _fromTouch = false;
    private TextView _currentTime;
    private SeekBar _ctlProgress;

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;
    private boolean paused;

    @Override
    public void onResume() {
        super.onResume();
        paused = false;
        _serviceToken = BinderUtils.bindToService(this.getActivity(), osc);
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlayerService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlayerService.META_CHANGED);
        getActivity().registerReceiver(mStatusListener, new IntentFilter(f));

        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        paused = true;
        mHandler.removeMessages(REFRESH);
        getActivity().unregisterReceiver(mStatusListener);
        BinderUtils.unbindFromService(_serviceToken);
        _playbackService = null;
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlayerService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                setPauseButtonImage();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlayerService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            }
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    break;

                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;

                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(FragmentPlayer.this.getActivity())
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            getActivity().finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                    break;

                default:
                    break;
            }
        }
    };

    private ServiceConnection osc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            _playbackService = IMediaPlaybackService.Stub.asInterface(obj);
            //start the loader going
            createPlayer();
            long next = refreshNow();
            try {
                // Assume something is playing when the service says it is,
                // but also if the aud ID is valid but the service is paused.
                if (_playbackService.getAudioId() >= 0 || _playbackService.getPath() != null) {
                    return;
                }
            } catch (RemoteException ignored) {
            }
            // Service is dead or not playing anything. If we got here as part
            // of a "play this file" Intent, exit. Otherwise go to the Music
            // app start screen.
            if (getActivity().getIntent().getData() == null) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(getActivity(), ActivityPodcasts.class);
                startActivity(intent);
            }
            getActivity().finish();
        }

        public void onServiceDisconnected(ComponentName classname) {
            _playbackService = null;
        }
    };
    private String _trackLocation = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _rootView = (ViewGroup) inflater.inflate(R.layout.fragment_player, null);

        _createHandlers();
        _currentTime = (TextView) _rootView.findViewById(R.id.fragment_player_time_elapsed);
        return _rootView;
    }

    private void _createHandlers() {
        _ctlProgress = (SeekBar) _rootView.findViewById(R.id.fragment_player_seekbar);
        _ctlProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar bar) {
                _lastSeekEventTime = 0;
                _fromTouch = true;
            }

            public void onStopTrackingTouch(SeekBar bar) {
                _positionOverride = -1;
                _fromTouch = false;
            }

            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                if (!fromuser || (_playbackService == null)) return;
                long now = SystemClock.elapsedRealtime();
                if ((now - _lastSeekEventTime) > 250) {
                    _lastSeekEventTime = now;
                    _positionOverride = _duration * progress / 1000;
                    try {
                        _playbackService.seek(_positionOverride);
                    } catch (RemoteException ignored) {
                    }

                    // trackball event, allow progress updates
                    if (!_fromTouch) {
                        refreshNow();
                        _positionOverride = -1;
                    }
                }
            }
        });
        _ctlProgress.setMax(1000);
        _rootView.findViewById(R.id.fragment_player_btn_player_back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
            }
        });

        _rootView.findViewById(R.id.fragment_player_btn_player_rwnd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
            }
        });

        _btnPlay = (ImageButton) _rootView.findViewById(R.id.fragment_player_btn_player_play);
        _btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doPauseResume();
            }
        });

        ((RepeatingImageButton) _rootView.findViewById(R.id.fragment_player_btn_player_rwnd)).setRepeatListener(new RepeatingImageButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long howlong, int repcnt) {
                scanBackward(repcnt, howlong);
            }
        }, 260);
        _rootView.findViewById(R.id.fragment_player_btn_player_rwnd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    _playbackService.jumpBackward(_positionOverride, 5000);
                } catch (RemoteException e) {
                    LogHandler.showLog(e.getMessage());
                }
            }
        });

        ((RepeatingImageButton) _rootView.findViewById(R.id.fragment_player_btn_player_ffwd)).setRepeatListener(new RepeatingImageButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long howlong, int repcnt) {
                scanForward(repcnt, howlong);
            }
        }, 260);

        _rootView.findViewById(R.id.fragment_player_btn_player_ffwd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    _playbackService.jumpForward(_positionOverride, 5000);
                } catch (RemoteException e) {
                    LogHandler.showLog(e.getMessage());
                }
            }
        });
        _rootView.findViewById(R.id.fragment_player_btn_player_back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        _rootView.findViewById(R.id.fragment_player_btn_player_next).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
    }


    private void scanForward(int repcnt, long delta) {
        if (_playbackService == null) return;
        try {
            if (repcnt == 0) {
                _startSeekPos = _playbackService.position();
                _lastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10;
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = _startSeekPos + delta;
                long duration = _playbackService.duration();
                if (newpos >= duration) {
                    // move to next track
                    _startSeekPos -= duration; // is OK to go negative
                    newpos -= duration;
                }
                if (((delta - _lastSeekEventTime) > 250) || repcnt < 0) {
                    _playbackService.seek(newpos);
                    _lastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    _positionOverride = newpos;
                } else {
                    _positionOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ignored) {
        }
    }

    private void scanBackward(int repcnt, long delta) {
        if (_playbackService == null) return;
        try {
            if (repcnt == 0) {
                _startSeekPos = _playbackService.position();
                _lastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10;
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = _startSeekPos - delta;
                if (newpos < 0) {
                    long duration = _playbackService.duration();
                    _startSeekPos += duration;
                    newpos += duration;
                }
                if (((delta - _lastSeekEventTime) > 250) || repcnt < 0) {
                    _playbackService.seek(newpos);
                    _lastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    _positionOverride = newpos;
                } else {
                    _positionOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ignored) {
        }
    }

    private long refreshNow() {
        if (_playbackService == null)
            return 500;
        try {
            long pos = _positionOverride < 0 ? _playbackService.position() : _positionOverride;
            if ((pos >= 0) && (_duration > 0)) {
                _currentTime.setText(DateUtils.makeTimeString(this.getActivity(), pos / 1000));
                int progress = (int) (1000 * pos / _duration);
                _ctlProgress.setProgress(progress);
                if (_playbackService.isPlaying()) {
                    _currentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = _currentTime.getVisibility();
                    _currentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    return 500;
                }
            } else {
                _currentTime.setText("--:--");
                _ctlProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            long remaining = 1000 - (pos % 1000);

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = _ctlProgress.getWidth();
            if (width == 0) width = 320;
            long smoothrefreshtime = _duration / width;

            if (smoothrefreshtime > remaining) return remaining;
            if (smoothrefreshtime < 20) return 20;
            return smoothrefreshtime;
        } catch (RemoteException ignored) {
        }
        return 500;
    }

    private void doPauseResume() {
        try {
            if (_playbackService != null) {
                if (_playbackService.isPlaying()) {
                    _playbackService.pause();
                } else {
                    _playbackService.play();
                }
                setPauseButtonImage();
            }
        } catch (RemoteException ignored) {
        }
    }

    private void setPauseButtonImage() {
        try {
            if (_playbackService != null && _playbackService.isPlaying()) {
                _btnPlay.setImageResource(R.drawable.player_pause);
            } else {
                _btnPlay.setImageResource(R.drawable.player_play);
            }
        } catch (RemoteException ignored) {
        }
    }

    private void createPlayer() {

        if (_playbackService == null)
            return;
        Intent intent = getActivity().getIntent();
        Bundle extras = intent.getExtras();
        try {
            String playingFile = _playbackService.getPath();
            String incomingFile = extras != null ? extras.getString("file") : playingFile;
            String trackName = extras != null ? extras.getString("title") : _playbackService.getTrackTitle();
            long entryId = extras != null ? extras.getLong("entry_id") : _playbackService.getAudioId();
            String trackDescription = extras != null ? extras.getString("description") : _playbackService.getTrackDescription();
            long position = extras != null ? extras.getLong("position") : _playbackService.position();

            if (!TextUtils.isEmpty(playingFile) && !TextUtils.isEmpty(incomingFile)) {
                if (!playingFile.equals(incomingFile)) {
                    if (_playbackService.isPlaying())
                        _playbackService.stop();
                }
            }
            _trackLocation = incomingFile;

            if (!_playbackService.isPlaying()) {
                //we're coming here fresh rather than from a widget/notification intent
                _playbackService.stop();
                _playbackService.setAudioId(entryId);
                _playbackService.setTrackTitle(trackName);
                _playbackService.setTrackDescription(trackDescription);
                _playbackService.openFile(_trackLocation);
                if (position != 0)
                    _playbackService.seek(position);
            }

            _duration = _playbackService.duration();
            ContentValues values = new ContentValues();
            values.put(PodNomsInterface.Entry.COLUMN_NAME_ENTRY_LENGTH, _duration);
            getActivity().getContentResolver().update(PodNomsInterface.Podcast.CONTENT_URI, values, String.format("_id = %d", entryId), null);

            ((TextView) _rootView.findViewById(R.id.fragment_player_time_complete)).setText(DateUtils.makeTimeString(this.getActivity(), _duration / 1000));
            getActivity().setIntent(new Intent());
        } catch (Exception ex) {
            Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
        }
        //TODO
        //updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }

}

package com.podnoms.android.podcatcher.aud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityPodcasts;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int LONG_PRESS_DELAY = 1000;

    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;

    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                        Context context = (Context)msg.obj;
                        Intent i = new Intent();
                        i.putExtra("autoshuffle", "true");
                        i.setClass(context, ActivityPodcasts.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        mLaunched = true;
                    }
                    break;
            }
        }
    };
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            Intent i = new Intent(context, MediaPlayerService.class);
            i.setAction(MediaPlayerService.SERVICECMD);
            i.putExtra(MediaPlayerService.CMDNAME, MediaPlayerService.CMDPAUSE);
            context.startService(i);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();

            // single quick press: pause/resume. 
            // double press: next track
            // long press: start auto-shuffle mode.
            
            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MediaPlayerService.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MediaPlayerService.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    command = MediaPlayerService.CMDFFWD;
                    break;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    command = MediaPlayerService.CMDRWND;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MediaPlayerService.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MediaPlayerService.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MediaPlayerService.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MediaPlayerService.CMDPLAY;
                    break;
            }

            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown) {
                        if ((MediaPlayerService.CMDTOGGLEPAUSE.equals(command) ||
                                MediaPlayerService.CMDPLAY.equals(command))
                                && mLastClickTime != 0 
                                && eventtime - mLastClickTime > LONG_PRESS_DELAY) {
                            mHandler.sendMessage(
                                    mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context));
                        }
                    } else if (event.getRepeatCount() == 0) {
                        // only consider the first event in a sequence, not the repeat events,
                        // so that we don't trigger in cases where the first event went to
                        // a different app (e.g. when the user ends a phone call by
                        // long pressing the headset button)

                        // The service may or may not be running, but we need to send it
                        // a command.
                        Intent i = new Intent(context, MediaPlayerService.class);
                        i.setAction(MediaPlayerService.SERVICECMD);
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK &&
                                eventtime - mLastClickTime < 300) {
                            i.putExtra(MediaPlayerService.CMDNAME, MediaPlayerService.CMDNEXT);
                            context.startService(i);
                            mLastClickTime = 0;
                        } else {
                            i.putExtra(MediaPlayerService.CMDNAME, command);
                            context.startService(i);
                            mLastClickTime = eventtime;
                        }

                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                    mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        }
    }
}

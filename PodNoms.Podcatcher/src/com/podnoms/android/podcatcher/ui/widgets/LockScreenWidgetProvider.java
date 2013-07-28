package com.podnoms.android.podcatcher.ui.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.os.Build;

public class LockScreenWidgetProvider {
    private RemoteControlClient _remoteControlClient;

    @TargetApi(14)
    public void initialiseLockScreenControls(Context context){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            return;

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    }
}

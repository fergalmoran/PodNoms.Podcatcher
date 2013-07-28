package com.podnoms.android.podcatcher.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.podnoms.android.podcatcher.providers.sync.PodNomsSyncOrchestrator;
import com.podnoms.android.podcatcher.util.LogHandler;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogHandler.showLog("Recurring alarm; requesting download service.");
        // start the download
        new PodNomsSyncOrchestrator(context).doSync();
    }

}
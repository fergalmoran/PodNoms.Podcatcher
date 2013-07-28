package com.podnoms.android.podcatcher.services.push;

import android.content.Context;
import android.content.Intent;
import com.google.android.gcm.GCMBaseIntentService;
import com.podnoms.android.podcatcher.util.LogHandler;

public class GCMIntentService extends GCMBaseIntentService {

    public GCMIntentService() {
        super("GCMIntentService");
    }

    protected GCMIntentService(String senderId) {
        super(senderId);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        LogHandler.showLog("GCM (message): ");
    }

    @Override
    protected void onError(Context context, String s) {
        LogHandler.showLog("GCM (error): " + s);
    }

    @Override
    protected void onRegistered(Context context, String regId) {
        LogHandler.showLog("GCM (registered): Message");
        GCMHelpers.sendIdToServer(this, regId);
    }

    @Override
    protected void onUnregistered(Context context, String s) {
        LogHandler.showLog("GCM (unregistered): Message");
    }
}

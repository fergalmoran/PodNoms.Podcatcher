package com.podnoms.android.podcatcher.providers.sync;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.podnoms.android.podcatcher.auth.AbstractGoogleApiTask;
import com.podnoms.android.podcatcher.auth.GetAuthTokenFromServer;
import com.podnoms.android.podcatcher.services.AlarmReceiver;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

import java.util.Calendar;

public class PodNomsSyncOrchestrator implements AbstractGoogleApiTask.GoogleApiTaskListener{
    public static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    public static final int RESULT_PLAY_LIB = 1;

    private final Context _context;

    public PodNomsSyncOrchestrator(Context context) {
        _context = context;
    }
    public static void setRecurringAlarm(Context context, Calendar updateTime, long interval) {
        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context, 0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                updateTime.getTimeInMillis(),
                interval,
                recurringDownload);
    }
    public void doSync() {
        String email = PersistentStateHandler.I().getString(Constants.ACCOUNT_NAME);
        if (!TextUtils.isEmpty(email)) {
            getTask(_context, this, email, Constants.SCOPE, Constants.REQUEST_CODE_AUTHENTICATE).execute();
        }
    }

    private void handleAuthorizeResult(int resultCode, Intent data) {
        if (data == null) {
            LogHandler.showMessage(_context, "Unknown error, click the button again");
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            LogHandler.showLog("Retrying");

            getTask(_context, this, PersistentStateHandler.I().getString(Constants.ACCOUNT_NAME), Constants.SCOPE, REQUEST_CODE_RECOVER_FROM_AUTH_ERROR).execute();
            return;
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            LogHandler.showMessage(_context, "User rejected authorization.");
            return;
        }
        LogHandler.showMessage(_context, "Unknown error, click the button again");
    }

    protected AbstractGoogleApiTask getTask(Context context, AbstractGoogleApiTask.GoogleApiTaskListener resultListener, String email, String scope, int requestCode) {
        return new GetAuthTokenFromServer(context, resultListener, email, scope, requestCode);
    }


    @Override
    public void onGoogleApiTaskComplete(String result) {
        if (!TextUtils.isEmpty(result)) {
            PersistentStateHandler.I().putString(Constants.AUTH_TOKEN, result);
            new StartSyncTask(_context).execute(result);
        } else
            PersistentStateHandler.I().putString(Constants.AUTH_TOKEN, "");
    }
}

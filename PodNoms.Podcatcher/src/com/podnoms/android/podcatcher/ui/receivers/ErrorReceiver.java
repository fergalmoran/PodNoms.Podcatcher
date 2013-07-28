package com.podnoms.android.podcatcher.ui.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityPodcasts;


public class ErrorReceiver extends BroadcastReceiver {
    private static final int NOTIFY_ME_ID = 1337;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager mgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification note = new Notification(R.drawable.ic_launcher,
                "Yoo-hoo! Wake up!",
                System.currentTimeMillis());
        PendingIntent i = PendingIntent.getActivity(context, 0,
                new Intent(context, ActivityPodcasts.class),
                0);

        note.setLatestEventInfo(context, "You Care About This!",
                "...but not enough to keep the activity running",
                i);

        mgr.notify(NOTIFY_ME_ID, note);
    }
}

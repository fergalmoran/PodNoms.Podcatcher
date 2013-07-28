package com.podnoms.android.podcatcher.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import com.podnoms.android.podcatcher.R;

public class NotifierHelper {
    private static final int NOTIFY_1 = 0x1001;

    public static void sendNotification(Context caller, Class<?> activityToLaunch, int title, String msg, int numberOfEvents, boolean flashLed, boolean vibrate) {
        NotificationManager notifier = (NotificationManager) caller.getSystemService(Context.NOTIFICATION_SERVICE);

        final Notification notify = new Notification(R.drawable.ic_launcher, "", System.currentTimeMillis());

        notify.icon = R.drawable.ic_launcher;
        notify.tickerText = "PodNoms: Sync Starting";
        notify.when = System.currentTimeMillis();
        notify.number = numberOfEvents;
        notify.flags |= Notification.FLAG_AUTO_CANCEL;

        if (flashLed) {
            // add lights
            notify.flags |= Notification.FLAG_SHOW_LIGHTS;
            notify.ledARGB = Color.CYAN;
            notify.ledOnMS = 500;
            notify.ledOffMS = 500;
        }

        if (vibrate) {
            notify.vibrate = new long[]{100, 200, 200, 200, 200, 200, 1000, 200, 200, 200, 1000, 200};
        }

        Intent toLaunch;
        if (activityToLaunch == null)
            toLaunch = new Intent();
        else
            toLaunch = new Intent(caller, activityToLaunch);
        PendingIntent intentBack = PendingIntent.getActivity(caller, 0, toLaunch, 0);
        String s = caller.getString(title);
        notify.setLatestEventInfo(caller, s, msg, intentBack);
        notifier.notify(NOTIFY_1, notify);
    }

    public static void killNotification(Context caller) {
        NotificationManager notifier = (NotificationManager) caller.getSystemService(Context.NOTIFICATION_SERVICE);
        notifier.cancel(NOTIFY_1);
    }
}
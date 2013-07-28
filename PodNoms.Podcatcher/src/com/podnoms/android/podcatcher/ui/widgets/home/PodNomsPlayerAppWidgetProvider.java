package com.podnoms.android.podcatcher.ui.widgets.home;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.aud.MediaPlayerService;
import com.podnoms.android.podcatcher.ui.activities.phone.ActivityEntryDetails;

public class PodNomsPlayerAppWidgetProvider extends AppWidgetProvider {
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";
    private static PodNomsPlayerAppWidgetProvider _instance;

    public static synchronized PodNomsPlayerAppWidgetProvider getInstance() {
        if (_instance == null) {
            _instance = new PodNomsPlayerAppWidgetProvider();
        }
        return _instance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        /*Intent updateIntent = new Intent(MediaPlaybackService.SERVICECMD);
        updateIntent.putExtra(MediaPlaybackService.CMDNAME,
                MediaAppWidgetProvider.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent); */
    }

    public void notifyChange(MediaPlayerService service, String what) {
        if (hasInstances(service)) {
            if (MediaPlayerService.META_CHANGED.equals(what) ||
                    MediaPlayerService.PLAYSTATE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }

    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, MediaPlayerService.class);

        intent = new Intent(context, ActivityEntryDetails.class);
        pendingIntent = PendingIntent.getActivity(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.appwidget_player_layout, pendingIntent);

        intent = new Intent(MediaPlayerService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.appwidget_player_play, pendingIntent);

    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    public void performUpdate(MediaPlayerService service, int[] appWidgetIds) {
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.appwidget_player);

        CharSequence podcastTitle = service.getTrackTitle();
        CharSequence podcastDescription = service.getTrackDescription();
        CharSequence errorState = null;

        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
                status.equals(Environment.MEDIA_UNMOUNTED)) {
            if (android.os.Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_busy_title);
            } else {
                errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
            }
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            if (android.os.Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_missing_title);
            } else {
                errorState = res.getText(R.string.sdcard_missing_title_nosdcard);
            }
        } else if (podcastTitle == null) {
            errorState = res.getText(R.string.emptyplaylist);
        }

        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.include_podcast_entry_desc_firstline, View.GONE);
            views.setTextViewText(R.id.include_podcast_entry_desc_secondline, errorState);

        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.include_podcast_entry_desc_firstline, View.VISIBLE);
            views.setViewVisibility(R.id.include_podcast_entry_desc_secondline, View.VISIBLE);
            views.setTextViewText(R.id.include_podcast_entry_desc_firstline, podcastTitle);
            views.setTextViewText(R.id.include_podcast_entry_desc_secondline, podcastDescription);
        }

        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.appwidget_player_play, R.drawable.player_pause);
        } else {
            views.setImageViewResource(R.id.appwidget_player_play, R.drawable.player_play);
        }

        // Link actions buttons to intents
        linkButtons(service, views, playing);

        pushUpdate(service, appWidgetIds, views);
    }
}

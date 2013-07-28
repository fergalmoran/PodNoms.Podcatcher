package com.podnoms.android.podcatcher.ui.activities.phone;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.sync.PodNomsSyncOrchestrator;
import com.podnoms.android.podcatcher.ui.widgets.preferences.TimePreference;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;

import java.util.Calendar;
import java.util.TimeZone;

public class ActivityPreferences extends SherlockPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        ListPreference schedulePreference = (ListPreference) findPreference("download_schedule");
        schedulePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PodNomsSyncOrchestrator.setRecurringAlarm(
                        ActivityPreferences.this,
                        getUpdateTime(),
                        Long.parseLong(newValue.toString())
                );
                return true;
            }
        });

        TimePreference timePref = (TimePreference) findPreference("download_schedule_start_time");
        timePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PodNomsSyncOrchestrator.setRecurringAlarm(
                        ActivityPreferences.this,
                        getUpdateTime(newValue.toString()),
                        PersistentStateHandler.I().getInt(Constants.SCHEDULE_FREQUENCY, 86400000)
                );
                return true;
            }
        });
    }

    public static Calendar getUpdateTime() {
        String updateTime = PersistentStateHandler.I().getString(Constants.SCHEDULE_START_TIME, "00:00");
        return getUpdateTime(updateTime);
    }

    public static Calendar getUpdateTime(String updateTime) {
        String[] parts = updateTime.split(":");
        Calendar ret = Calendar.getInstance();
        ret.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (parts.length == 2) {
            ret.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            ret.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        } else {
            ret.set(Calendar.HOUR_OF_DAY, 0);
            ret.set(Calendar.MINUTE, 0);
        }
        return ret;
    }
}

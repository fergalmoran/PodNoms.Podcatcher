package com.podnoms.android.podcatcher.util.state;

import com.podnoms.android.podcatcher.PodNomsApplication;

public class Constants {
    public static final String ACCOUNT_NAME = "accountName";
    public static final String AUTH_COOKIE = "authCookie";
    public static final String DEVICE_REGISTRATION_ID = "deviceRegistrationID";
    public static final String UPDATE_UI_INTENT = PodNomsApplication.class.getPackage().getName() + ".UPDATE_UI";
    public static final String MAX_DOWNLOADS = "max_downloads";
    public static final String MAX_EPISODES = "max_episodes";
    public static final String WIFI_ONLY = "download_wifi";
    public static final String SCHEDULE_FREQUENCY = "schedule_frequency";
    public static final String SCHEDULE_START_TIME = "download_schedule_start_time";
    public static final String MARK_PLAYED_AT = "mark_played_at";
    public static final int MARK_PLAYED_AT_DEFAULT = 95;
    public static final String DEBUG_MODE = "debug_mode";

    public static final String AUDIENCE = "794539138576.apps.googleusercontent.com";
    public static final String SCOPE = "audience:server:client_id:" + AUDIENCE;
    //public static final String SCOPE = "audience:server:client_id:794539138576-n3nolahklhsk1u2s24ra883i1mac18t8.apps.googleusercontent.com";
    public static final int REQUEST_CODE_AUTHENTICATE = 1001;

    public static final String AUTH_TOKEN = "auth_token";
}

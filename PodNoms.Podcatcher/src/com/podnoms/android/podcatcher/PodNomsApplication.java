package com.podnoms.android.podcatcher;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import com.crittercism.app.Crittercism;
import com.google.android.gcm.GCMRegistrar;
import com.podnoms.android.podcatcher.exceptions.NoWriteableStorageException;
import com.podnoms.android.podcatcher.util.FileUtils;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

public class PodNomsApplication extends Application {
    public static final String PACKAGE = "com.podnoms.android";
    private static SharedPreferences _prefs;
    private static PodNomsApplication _instance = null;
    public static final String SENDER_ID = "794539138576";

    public PodNomsApplication() {
        if (_instance == null)
            _instance = this;
    }

    public static boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isDebug() {
        return PersistentStateHandler.I().getBoolean(Constants.DEBUG_MODE, false);
        /*
        if (_instance != null) {
            int applicationFlags = _instance.getApplicationContext().getApplicationInfo().flags;
            return (applicationFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } else
            return false;
        */
    }

    public static String getWorkingDir() {

        String workingDir = Environment.getExternalStorageDirectory().toString() + "/data/" + getContext().getPackageName();
        File workingFile = new File(workingDir);
        if (!workingFile.exists())
            workingFile.mkdirs();

        return workingDir;

    }

    public static String getCacheDirectory() {
        return getWorkingDir() + "/cache/";
    }

    public static String getDownloadsDirectory() {
        return getWorkingDir() + "/downloads";
    }

    public static String getServiceHost() {
        return isDebug() ?
               "http://192.168.1.111:8000/" : "http://www.podnoms.com/";
    }

    public static String getApiServiceHost() {
        return getServiceHost() + "api/v2/";
    }

    @Deprecated
    public static String getApiAuthTokenAddress() {
        return getApiServiceHost() + "getkey/";
    }

    @Deprecated
    public static String getApiStatusAddress() {
        return getApiServiceHost() + "status";
    }

    public static String getApiServiceAddress() {
        return getApiServiceHost() + "podcast/";
    }

    @Deprecated
    public static String getValidatorServiceAddress() {
        return getApiServiceHost() + "api/validate/podcast/";
    }

    @Override
    public void onCreate() {

        if (isDebug())
            LogHandler.showLog("PodNoms: Running in debug mode.");
        else
            LogHandler.showLog("PodNoms: Running in release mode.");
        /*
        JSONObject crittercismConfig = new JSONObject();
        try {
            crittercismConfig.put("shouldCollectLogcat", true); // send logcat data for devices with API Level 16 and higher
        } catch (JSONException je) {
        }
        Crittercism.init(getApplicationContext(), "511d0a421abc69100a00012a", crittercismConfig);
        */
        if (_prefs == null)
            _prefs = getSharedPreferences("PODNOMS_PREFS", 0);

        registerForPush();
        setupCacheDirectory();
        setupDownloadsDirectory();
        //DebugSettingsWrapper.init(this.getApplicationContext());
    }

    private void registerForPush() {
        GCMRegistrar.unregister(getContext());
        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
        final String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            GCMRegistrar.register(this, SENDER_ID);
        } else {
            LogHandler.showLog("Already registered");
        }
    }

    private void setupCacheDirectory() {
        File folder = new File(getCacheDirectory());
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (!success) {
            LogHandler.reportError("Unable to create cache directory", new FileNotFoundException());
        }
    }


    private void setupDownloadsDirectory() {
        File folder = new File(getDownloadsDirectory());
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (!success) {
            NoWriteableStorageException exception = new NoWriteableStorageException();
            LogHandler.reportError("Unable to create downloads directory", exception);
            Crittercism.logHandledException(exception);
        }
    }

    public static Context getContext() {
        return _instance.getApplicationContext();
    }

    @Deprecated
    public static String getPodcastListAddress() {
        return PodNomsApplication.getApiServiceAddress();
    }

    public static String getGcmServerAddress() {
        return String.format("%spush/", getApiServiceHost());
    }

    public static int getBuildVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static void clearCacheDirectory() {
        FileUtils.clearDirectory(getCacheDirectory());
    }

    public static void clearDownloadsDirectory() {
        FileUtils.clearDirectory(getDownloadsDirectory());
    }

    public static class DebugSettingsWrapper {
        public static void init(Context context) {
            _setupPowerSettings();

            if (isDebug()) {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                                /*.detectDiskWrites()*/
                                /*.detectCustomSlowCalls()*/
                        .detectNetwork()
                        .penaltyLog()
                                /*.penaltyDialog()*/
                        .build());
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .penaltyLog()
                                /*.penaltyDeath()*/
                        .build());
            } else {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
        }

        private static void _setupPowerSettings() {
            /*
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
            wl.acquire();
            */
        }
    }
}

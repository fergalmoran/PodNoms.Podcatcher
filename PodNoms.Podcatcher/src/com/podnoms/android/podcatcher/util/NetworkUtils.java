package com.podnoms.android.podcatcher.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.podnoms.android.podcatcher.PodNomsApplication;

import java.net.*;

public class NetworkUtils {
    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) PodNomsApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static boolean isOnWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                PodNomsApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean checkService(String urlToCheck) {
        try {
            ConnectivityManager cm = (ConnectivityManager) PodNomsApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();

            if (netInfo != null && netInfo.isConnected()) {
                //Network is available but check if we can get access from the network.
                URL url = new URL(urlToCheck);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(2000); // Timeout 2 seconds.
                urlc.connect();

                int responseCode = urlc.getResponseCode();
                if (responseCode == 200 || responseCode == 401) {
                    return true;
                } else {
                    Log.d("NO INTERNET", "NO INTERNET");
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }
}

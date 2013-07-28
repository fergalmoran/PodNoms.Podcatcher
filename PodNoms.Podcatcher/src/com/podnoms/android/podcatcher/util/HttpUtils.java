package com.podnoms.android.podcatcher.util;

import android.webkit.URLUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtils {

    public static int getFileLength(String url) {
        try {
            return getFileLength(new URL(url));
        } catch (MalformedURLException e) {
            LogHandler.reportError("Error getting file length", e);
        }
        return -1;
    }

    public static int getFileLength(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
            return urlConnection.getContentLength();
        } catch (MalformedURLException e) {
            LogHandler.reportError("Error getting file length", e);
        } catch (IOException e) {
            LogHandler.reportError("Error getting file length", e);
        }
        return -1;
    }
    public static boolean isValidUrl(String url){
        return URLUtil.isValidUrl(url);
    }
}

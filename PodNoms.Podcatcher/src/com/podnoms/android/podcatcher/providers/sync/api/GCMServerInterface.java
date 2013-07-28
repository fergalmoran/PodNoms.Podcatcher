package com.podnoms.android.podcatcher.providers.sync.api;

import android.util.Log;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class GCMServerInterface extends ApiHandler {
    public void registerDevice(String registrationId) {
        String url = String.format(
                "%s/%s/%s",
                PodNomsApplication.getGcmServerAddress(),
                registrationId,
                PersistentStateHandler.I().getString(Constants.ACCOUNT_NAME, null));
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(url);
        try {

            HttpResponse getResponse = client.execute(getRequest);
            final int statusCode = getResponse.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                LogHandler.showLog("Device registered successfully");
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(getClass().getSimpleName(), "Error for URL " + url, e);
        }
    }
}

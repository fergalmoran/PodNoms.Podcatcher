package com.podnoms.android.podcatcher.services.push;

import android.os.AsyncTask;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.util.LogHandler;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SendRegistrationIdTask extends
        AsyncTask<String, Void, HttpResponse> {
    private String mRegId;

    public SendRegistrationIdTask(String regId) {
        mRegId = regId;
    }

    @Override
    protected HttpResponse doInBackground(String... regIds) {
        String url = PodNomsApplication.getGcmServerAddress() + "register/";
        HttpPost httppost = new HttpPost(url);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("reg_id", mRegId));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpClient httpclient = new DefaultHttpClient();
            return httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            LogHandler.reportError("Protocol error sed", e);
        } catch (IOException e) {
            LogHandler.reportError("IO exception", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(HttpResponse response) {
        if (response == null) {
            LogHandler.showMessage("HttpResponse is null");
            return;
        }

        StatusLine httpStatus = response.getStatusLine();
        if (httpStatus.getStatusCode() != 200) {
            LogHandler.showMessage("Status: " + httpStatus.getStatusCode());
            //Do Something Here
            return;
        }

        String status = PodNomsApplication.getContext().getString(R.string.server_registration, mRegId);
    }
}

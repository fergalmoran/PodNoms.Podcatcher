/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.podnoms.android.podcatcher.auth;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.podnoms.android.podcatcher.util.LogHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Display personalized greeting. This class contains boilerplate code to consume the token but
 * isn't integral to getting the tokens.
 */
public abstract class AbstractGoogleApiTask extends AsyncTask<Void, Void, String> {


    public interface GoogleApiTaskListener{
        public void onGoogleApiTaskComplete(String result);
    }
    private static final String TAG = "TokenInfoTask";
    private static final String NAME_KEY = "given_name";
    protected final Context _context;
    private final GoogleApiTaskListener _resultListener;

    protected String _scope;
    protected String _email;
    protected int _requestCode;

    AbstractGoogleApiTask(Context context, GoogleApiTaskListener resultListener, String email, String scope, int requestCode) {
        this._context = context;
        this._resultListener = resultListener;
        this._scope = scope;
        this._email = email;
        this._requestCode = requestCode;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return fetchAuthenticationTokenFromServer();
        } catch (IOException ex) {
            onError("Following Error occured, please try again. " + ex.getMessage(), ex);
        } catch (JSONException e) {
            onError("Bad response: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (_resultListener != null){
            _resultListener.onGoogleApiTaskComplete(s);
        }
    }

    protected void onError(String msg, Exception e) {
        if (e != null) {
            Log.e(TAG, "Exception: ", e);
        }
        LogHandler.reportError(_context, String.format("Google API error.\n%s", msg), e);  // will be run in UI thread
    }

    /**
     * Get a authentication token if one is not available. If the error is not recoverable then
     * it displays the error message on parent activity.
     */
    protected abstract String fetchToken() throws IOException;

    /**
     * Contacts the user info server to get the profile of the user and extracts the first name
     * of the user from the profile. In order to authenticate with the user info server the method
     * first fetches an access token from Google Play services.
     *
     * @throws java.io.IOException    if communication with user info server failed.
     * @throws org.json.JSONException if the response from the server could not be parsed.
     */
    private String fetchAuthenticationTokenFromServer() throws IOException, JSONException {
        String token = fetchToken();
        if (token == null) {
            // error has already been handled in fetchToken()
            return "";
        }
        return token;
    }

    /**
     * Reads the response from the input stream and returns it as a string.
     */
    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    /**
     * Parses the response and returns the first name of the user.
     *
     * @throws org.json.JSONException if the response is not JSON or if first name does not exist in response
     */
    private String getFirstName(String jsonResponse) throws JSONException {
        JSONObject profile = new JSONObject(jsonResponse);
        return profile.getString(NAME_KEY);
    }
}

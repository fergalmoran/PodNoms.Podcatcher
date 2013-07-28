package com.podnoms.android.podcatcher.providers.sync.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.NetworkUtils;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.util.List;

public class ApiHandler {

    private ApiToken _token;

    public ApiHandler(ApiToken token) {
        _token = token;
    }


    public ApiHandler() {
        _token = getApiKey(String.format("%s?token=%s&audience=%s",
                PodNomsApplication.getApiAuthTokenAddress(),
                PersistentStateHandler.I().getString(Constants.AUTH_TOKEN),
                Constants.AUDIENCE));
    }

    public class ApiToken {
        public String username;
        public String token;
    }

    public class ApiPodcast {

        public ApiPodcastMeta meta;
        public List<ApiPodcastEntry> objects;

        public class ApiPodcastMeta {
            public int limit;
            public int next;
            public int offset;
            public int previous;
            public int total_count;
        }

        public class ApiPodcastEntry {
            public String url;
            public String resource_uri;
            public String description;
        }
    }

    public class ApiValidatorResult {
        public int error_count;
        public int warning_count;
    }

    public boolean checkApiService() {
        return NetworkUtils.checkService(PodNomsApplication.getApiServiceAddress());
    }

    public ApiToken getApiKey(String url) {
        ApiToken token = new Gson().fromJson(_getReader(url, false), ApiToken.class);
        if (token != null)
            return token;
        return null;
    }

    public ApiPodcast getPodcastList(String url) {
        try {
            Reader reader = _getReader(url, true);
            ApiPodcast ret = new Gson().fromJson(reader, ApiPodcast.class);
            return ret;
        } catch (JsonSyntaxException ex) {
            LogHandler.reportError("Error parsing json", ex);
        } catch (Exception ex) {
            LogHandler.reportError("Error parsing json", ex);
        }
        return null;
    }

    public boolean deletePodcast(String resourceUri) {
        try {
            if (checkApiService()) {
                String vUrl = String.format("%s%s", PodNomsApplication.getServiceHost(), resourceUri);
                HttpResponse response = _postDelete(vUrl);
                int statusCode = response.getStatusLine().getStatusCode();
                //will accept a 404 here as we should still delete local
                //entry if resource is not on server
                return (statusCode == HttpStatus.SC_NO_CONTENT ||
                        statusCode == HttpStatus.SC_OK ||
                        statusCode == HttpStatus.SC_ACCEPTED ||
                        statusCode == HttpStatus.SC_NOT_FOUND);
            }
        } catch (Exception ex) {
            LogHandler.reportError("Error deleting podcast", ex);
        }
        return false;
    }

    public ApiValidatorResult addPodcast(String url) {
        try {
            if (checkApiService()) {
                String vUrl = PodNomsApplication.getApiServiceAddress();
                JSONObject json = new JSONObject();
                json.put("url", url);
                InputStreamReader reader = _postReader(vUrl, true, json);
                ApiValidatorResult ret = new Gson().fromJson(reader, ApiValidatorResult.class);
                return ret;
            }
        } catch (Exception ex) {
            LogHandler.reportError("Error adding podcast", ex);
        }
        return null;
    }


    public boolean validatePodcastFeed(String url) {
        try {
            if (checkApiService()) {
                String vUrl = String.format("%s?url=%s", PodNomsApplication.getValidatorServiceAddress(), url);
                Reader reader = _getReader(vUrl, false);
                ApiValidatorResult ret = new Gson().fromJson(reader, ApiValidatorResult.class);
                if (ret != null)
                    return ret.error_count == 0;
            }
        } catch (JsonSyntaxException ex) {
            LogHandler.reportError("Error validating feed", ex, true);
        } catch (Exception ex) {
            LogHandler.reportError("Error validating feed", ex, true);
        }
        return false;
    }

    private InputStreamReader _getReader(String url, Boolean usesKey) {
        InputStream source = _retrieveStream(url, usesKey, new HttpGet(), null);
        InputStreamReader reader = new InputStreamReader(source);
        return reader;
    }

    private InputStreamReader _postReader(String url, Boolean usesKey, JSONObject params) {
        DefaultHttpClient client = new DefaultHttpClient();

        InputStream source = _retrieveStream(url, usesKey, new HttpPost(), params);
        InputStreamReader reader = new InputStreamReader(source);
        return reader;
    }

    private HttpResponse _postDelete(String url) {
        HttpDelete request = new HttpDelete();
        HttpResponse response = _retrieveResponse(url, true, request, null);
        return response;
    }

    private HttpResponse _retrieveResponse(String url, Boolean usesKey, HttpRequestBase request, JSONObject data) {
        DefaultHttpClient client = new DefaultHttpClient();
        if (usesKey && _token != null) {
            request.setHeader("Authorization", String.format("ApiKey %s:%s", _token.username, _token.token));
            //TODO remove this krud once header based authorization works
            //url = String.format("%s?username=%s&api_key=%s", url, _token.username, _token.token);
            request.setHeader("X_PODNOMS_APIKEY", _token.token);
            request.setHeader("X_PODNOMS_USERNAME", _token.username);
        }
        request.setURI(URI.create(url));

        if (data != null && request instanceof HttpPost) {
            StringEntity se = null;
            try {
                se = new StringEntity(data.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                request.setHeader("Content-Type", "application/json");
                ((HttpPost) request).setEntity(se);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            HttpResponse getResponse = client.execute(request);
            final int statusCode = getResponse.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                LogHandler.showMessage("Error " + statusCode + " for URL " + url);
            }

            return getResponse;
        } catch (IOException e) {
            request.abort();
            LogHandler.reportError("Error for URL " + url, e, true);
        }

        return null;
    }

    private InputStream _retrieveStream(String url, Boolean usesKey, HttpRequestBase request, JSONObject data) {
        try {
            HttpResponse getResponse = _retrieveResponse(url, usesKey, request, data);
            HttpEntity getResponseEntity = getResponse.getEntity();
            return getResponseEntity.getContent();
        } catch (IOException e) {
            LogHandler.reportError("Error for URL " + url, e, true);
        }
        return null;
    }
}

package com.podnoms.android.podcatcher.util;

import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Observable;

public class Downloader extends Observable {

    // Max _size of download buffer.
    public static final int MAX_BUFFER_SIZE = 1024;

    // These are the _status names.
    public static final String STATUSES[] = {"Downloading",
            "Paused", "Complete", "Cancelled", "Error"};

    // These are the _status codes.
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private String _url; // download URL
    private int _size; // _size of download in bytes
    private int _downloaded; // number of bytes downloaded
    private int _status; // current _status of download
    private String _cacheFile;
    private int _entryId;
    private String _description;

    // Constructor for Download.
    public Downloader(String url, String cacheFile, String description, int entryId) {
        this._cacheFile = cacheFile;
        this._url = url;
        this._entryId = entryId;
        this._description = description;
        _size = -1;
        _downloaded = 0;
    }
    public String getCacheFile(){
        return _cacheFile;
    }
   // Get this download's URL.
    public String getUrl() {
        return _url.toString();
    }

    // Get this download's _size.
    public int getSize() {
        return _size;
    }

    // Get this download's progress.
    public float getProgress() {
        return ((float) _downloaded / _size) * 100;
    }

    public int getDownloaded() {
        return _downloaded;
    }

    // Get this download's _status.
    public int getStatus() {
        return _status;
    }

    // Pause this download.
    public void pause() {
        _status = PAUSED;
        stateChanged();
    }

    // Resume this download.
    public void resume(int downloaded) {
        this._downloaded = downloaded;
        _status = DOWNLOADING;
        stateChanged();
        download();
    }

    // Cancel this download.
    public void cancel() {
        _status = CANCELLED;
        stateChanged();
    }

    // Mark this download as having an error.
    private void error() {
        _status = ERROR;
        stateChanged();
    }

    public Boolean download() {
        Boolean ret = false;
        try {
            _status = DOWNLOADING;
            Boolean wifiOnly = PersistentStateHandler.I().getBoolean(Constants.WIFI_ONLY, true);
            if ((wifiOnly && !NetworkUtils.isOnWifi()) && !DeviceUtils.runningOnEmulator()){
                LogHandler.showLog("Refusing download as not on wifi");
                return false;
            }

            HttpClient conexion = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(_url);

            HttpResponse response = conexion.execute(getRequest);
            _size = Integer.parseInt(response.getHeaders("Content-length")[0].getValue());

            InputStream is = response.getEntity().getContent();
            OutputStream out  = new BufferedOutputStream(new FileOutputStream(_cacheFile));
            byte data[] = new byte[8192];
            int count;
            while ((count = is.read(data)) != -1) {
                if (count == -1) break;

                out.write(data, 0, count);
                _downloaded += count;
                stateChanged();
            }
            is.close();
            out.close();
            if (_status == DOWNLOADING) {
                _status = COMPLETE;
                stateChanged();
                ret = true;
            }
        } catch (Exception e) {
            LogHandler.reportError("Error downloading file", e);
        }
        return ret;
    }
    // Notify observers that this download's _status has changed.
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }

    public int getItemId() {
        return _entryId;
    }

    public String getDescription() {
        return _description;
    }
}

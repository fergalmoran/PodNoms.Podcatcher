package com.podnoms.android.podcatcher.providers.sync;

import android.content.Context;
import android.os.AsyncTask;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.services.DownloadService;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.NetworkUtils;

public class StartSyncTask extends AsyncTask<String, Void, Boolean> {
    private String _authenticationToken;
    private Context context;

    public StartSyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String... voids) {
        _authenticationToken = voids[0];
        return NetworkUtils.checkService(PodNomsApplication.getApiStatusAddress());
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            PodNomsSyncAdapter adapter = new PodNomsSyncAdapter(context, DownloadService.MESSAGE_STARTDOWNLOAD, _authenticationToken);
            adapter.startSync();
        } else {
            String message = String.format("%s\n%s",
                    context.getResources().getString(R.string.device_offline),
                    PodNomsApplication.isDebug() ? PodNomsApplication.getApiServiceAddress() : "");
            LogHandler.showMessage(context, message, true);
        }
    }
}

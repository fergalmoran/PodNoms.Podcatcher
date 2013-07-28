package com.podnoms.android.podcatcher.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.sync.PodNomsSyncOrchestrator;
import com.podnoms.android.podcatcher.ui.widgets.AlertDialogs;
import com.podnoms.android.podcatcher.util.LogHandler;

public abstract class BaseActivity extends SherlockFragmentActivity {


    public void show(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogHandler.showMessage(BaseActivity.this, message);
            }
        });
    }

    protected Boolean _checkPlayServices() {
        int playServicesStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (playServicesStatus == ConnectionResult.SERVICE_MISSING ||
                playServicesStatus == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                playServicesStatus == ConnectionResult.SERVICE_DISABLED) {
            Dialog dlg = GooglePlayServicesUtil.getErrorDialog(playServicesStatus, this, PodNomsSyncOrchestrator.RESULT_PLAY_LIB);
            if (dlg == null) {
                AlertDialogs.InfoDialog(this, "Unable to check for google play services, please install it from the market manually.");
            } else {
                dlg.show();
            }
            return false;
        }
        return ConnectionResult.SUCCESS == playServicesStatus;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_default, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void showErrorDialog(final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog d = GooglePlayServicesUtil.getErrorDialog(
                        code,
                        BaseActivity.this,
                        PodNomsSyncOrchestrator.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                d.show();
            }
        });
    }

    public void openActivityOrFragment(Intent intent) {
        startActivity(intent);
    }

    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

}

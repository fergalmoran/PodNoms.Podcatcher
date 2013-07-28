package com.podnoms.android.podcatcher.ui.activities.phone;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.ui.BaseActivity;
import com.podnoms.android.podcatcher.ui.widgets.AlertDialogs;

public class ActivityEntryList extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcastentries);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);    //To change body of overridden methods use File | Settings | File Templates.
    }
}

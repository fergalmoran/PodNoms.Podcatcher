package com.podnoms.android.podcatcher.ui.activities.phone;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.podnoms.android.podcatcher.ui.BaseSinglePaneActivity;
import com.podnoms.android.podcatcher.ui.fragments.FragmentAudioItemView;

public class ActivityEntryDetails extends BaseSinglePaneActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Fragment onCreatePane() {
        return new FragmentAudioItemView();
    }
}

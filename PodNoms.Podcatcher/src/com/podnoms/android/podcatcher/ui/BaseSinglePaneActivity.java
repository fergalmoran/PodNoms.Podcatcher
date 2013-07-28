package com.podnoms.android.podcatcher.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.podnoms.android.podcatcher.R;

public abstract class BaseSinglePaneActivity extends BaseActivity {
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);

        if (savedInstanceState == null) {
            mFragment = onCreatePane();
            mFragment.setArguments(intentToFragmentArguments(getIntent()));

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.root_container, mFragment)
                    .commit();
        }
    }

    /**
     * Called in <code>onCreate</code> when the fragment constituting this activity is needed.
     * The returned fragment's arguments will be set to the intent used to invoke this activity.
     */
    protected abstract Fragment onCreatePane();
}

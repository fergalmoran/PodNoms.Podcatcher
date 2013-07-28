package com.podnoms.android.podcatcher.ui.activities.phone;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import com.actionbarsherlock.view.MenuItem;
import com.crittercism.app.Crittercism;
import com.google.android.gcm.GCMConstants;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.providers.PodNomsInterface;
import com.podnoms.android.podcatcher.providers.sync.PodNomsSyncOrchestrator;
import com.podnoms.android.podcatcher.services.push.GCMHelpers;
import com.podnoms.android.podcatcher.ui.BaseActivity;
import com.podnoms.android.podcatcher.ui.activities.auth.AccountsActivity;
import com.podnoms.android.podcatcher.ui.fragments.FragmentEntryList;
import com.podnoms.android.podcatcher.ui.fragments.FragmentPodcastList;
import com.podnoms.android.podcatcher.util.CacheCleaner;
import com.podnoms.android.podcatcher.util.EulaHelper;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import com.actionbarsherlock.app.ActionBar;

import java.security.InvalidParameterException;

public class ActivityPodcasts extends BaseActivity implements ActionBar.TabListener {


    private static final boolean RUNNING_IN_SERVICE = false;
    private Messenger _serviceHandler;
    private GCMReceiver mGCMReceiver;
    private IntentFilter mOnRegisteredFilter;
    private ViewPager _pager;
    private PodcastListPagerAdapter _pagerAdapter;

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
        _pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

    }


    private class GCMReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String regId = intent.getStringExtra("registration_id");
            GCMHelpers.sendIdToServer(ActivityPodcasts.this, regId);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setTitle("PodNoms");
        actionBar.setSubtitle("Podcatcher");
        if (!EulaHelper.hasAcceptedEula(this)) {
            EulaHelper.showEula(false, this);
        }

        setContentView(R.layout.activity_home);

        _pagerAdapter = new PodcastListPagerAdapter(getSupportFragmentManager());

        _pager = (ViewPager) findViewById(R.id.pager);
        _pager.setAdapter(_pagerAdapter);

        mGCMReceiver = new GCMReceiver();
        mOnRegisteredFilter = new IntentFilter();
        mOnRegisteredFilter.addAction(GCMConstants.INTENT_FROM_GCM_REGISTRATION_CALLBACK);

        String email = PersistentStateHandler.I().getString(Constants.ACCOUNT_NAME);
        if (TextUtils.isEmpty(email)) {
            Intent accountIntent = new Intent(this, AccountsActivity.class);
            startActivity(accountIntent);
        }

        _pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < _pagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(_pagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            //chances are that we're here as part of a search suggestion
            //fire off an intent to view the podcast
            Uri entriesUri = intent.getData();
            final Intent viewIntent = new Intent(this, ActivityEntryList.class);
            viewIntent.putExtra("content_uri", entriesUri);
            openActivityOrFragment(viewIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_checkPlayServices())
            registerReceiver(mGCMReceiver, mOnRegisteredFilter);
    }


    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mGCMReceiver);
        } catch (RuntimeException e) {
        }
    }

    private void triggerRefresh() {
        new PodNomsSyncOrchestrator(this).doSync();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            triggerRefresh();
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            if (true) {
                Crittercism.logHandledException(new InvalidParameterException("We done goofed"));
            }
            startActivity(new Intent(this, ActivityPreferences.class));
        } else if (item.getItemId() == R.id.menu_clean_cache) {
            new CacheCleaner(this).cleanCache();
            LogHandler.showMessage(this, "Cache cleaned succesfully");
        }
        return super.onOptionsItemSelected(item);
    }

    public class PodcastListPagerAdapter extends FragmentPagerAdapter {

        public PodcastListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Episodes";
                case 1:
                    return "Podcasts";
            }
            return "Undefined";
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return FragmentEntryList.newInstance(PodNomsInterface.Entry.DOWNLOADED_URI);
                case 1:
                    return FragmentPodcastList.newInstance(PodNomsInterface.Podcast.CONTENT_URI);
                /*
                case 2:
                    return FragmentEntryList.newInstance(PodNomsInterface.Entry.ALLENTRIES_URI);
                */
                default:
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}

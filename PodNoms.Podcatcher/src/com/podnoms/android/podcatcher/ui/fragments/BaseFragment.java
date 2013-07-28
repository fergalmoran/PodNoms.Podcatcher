package com.podnoms.android.podcatcher.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.UIHelper;

public class BaseFragment extends SherlockFragment {
    protected ViewGroup _rootView;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                try {
                    new UIHelper(BaseFragment.this.getActivity()).showErrorDialogOnGuiThread(message);
                } catch (Exception e) {
                }
                abortBroadcast();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(LogHandler.INTENT_ERROR);
        filter.setPriority(2);
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }
}

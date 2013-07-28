package com.podnoms.android.podcatcher.providers.sync;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class PodNomsSyncResultReceiver extends ResultReceiver {
    private Receiver _receiver;

    public PodNomsSyncResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        _receiver = receiver;
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (_receiver != null) {
            _receiver.onReceiveResult(resultCode, resultData);
        }
    }
}

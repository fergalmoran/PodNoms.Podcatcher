package com.podnoms.android.podcatcher.services.push;

import android.content.Context;
import com.podnoms.android.podcatcher.R;

public class GCMHelpers {
    public static void sendIdToServer(Context context, String regId) {
        String status = context.getString(R.string.gcm_registration, regId);
        (new SendRegistrationIdTask(regId)).execute();
    }
}

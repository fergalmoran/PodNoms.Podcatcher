/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.podnoms.android.podcatcher.ui.activities.auth;

import android.content.Context;
import android.content.Intent;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.state.Constants;

public class DeviceRegistrar {
    public static final String STATUS_EXTRA = "Status";

    public static final int REGISTERED_STATUS = 1;

    public static final int UNREGISTERED_STATUS = 2;

    public static final int ERROR_STATUS = 3;

    private static final String TAG = "DeviceRegistrar";

    public static void registerOrUnregister(final Context context, final String deviceRegistrationId, final boolean register) {
        final Intent updateUIIntent = new Intent(Constants.UPDATE_UI_INTENT);
        LogHandler.reportError("We are registering", new Exception("Hello Sailor"));
    }

}

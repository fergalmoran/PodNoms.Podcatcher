/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.podnoms.android.podcatcher.auth;

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.podnoms.android.podcatcher.util.LogHandler;
import com.podnoms.android.podcatcher.util.NotifierHelper;

import java.io.IOException;

public class GetAuthTokenFromServer extends AbstractGoogleApiTask {

    public GetAuthTokenFromServer(Context context, GoogleApiTaskListener resultListener, String email, String scope, int requestCode) {
        super(context, resultListener, email, scope, requestCode);
    }

    @Override
    protected String fetchToken() throws IOException {
        try {
            return GoogleAuthUtil.getTokenWithNotification(_context, _email, _scope, null);
        } catch (GooglePlayServicesAvailabilityException playEx) {
            // GooglePlayServices.apk is either old, disabled, or not present.
            LogHandler.reportError(_context, "Google services are unavailable.", playEx);
        } catch (UserRecoverableAuthException userRecoverableException) {
            // Unable to authenticate, but the user can fix this.
            // Forward the user to the appropriate activity.
            LogHandler.reportError(_context, "User recoverable auth excetion.", userRecoverableException);
        } catch (GoogleAuthException fatalException) {
            onError("Unknown google auth token exception.\n" + fatalException.getMessage(), fatalException);
            Crittercism.logHandledException(fatalException);
        }
        return null;
    }
}

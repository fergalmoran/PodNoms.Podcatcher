
package com.podnoms.android.podcatcher.ui.activities.auth;


import android.accounts.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.util.state.Constants;
import com.podnoms.android.podcatcher.util.state.PersistentStateHandler;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Account selections activity - handles device registration and unregistration.
 */
public class AccountsActivity extends Activity {
    /**
     * Tag for logging.
     */
    private static final String TAG = "AccountsActivity";

    /**
     * Cookie name for authorization.
     */
    private static final String AUTH_COOKIE_NAME = "SACSID";

    /**
     * The selected position in the ListView of accounts.
     */
    private int mAccountSelectedPosition = 0;

    /**
     * True if we are waiting for App Engine authorization.
     */
    private boolean mPendingAuth = false;

    /**
     * The current context.
     */
    private Context mContext = this;

    /**
     * Begins the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceRegistrationID = PersistentStateHandler.I().getString(Constants.ACCOUNT_NAME);
        if (TextUtils.isEmpty(deviceRegistrationID)) {
            // Show the 'connect' screen if we are not connected
            setScreenContent(R.layout.connect);
        } else {
            // Show the 'disconnect' screen if we are connected
            setScreenContent(R.layout.disconnect);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String deviceRegistrationID = PersistentStateHandler.I().getString(Constants.DEVICE_REGISTRATION_ID);
        if (deviceRegistrationID == null) {
            setScreenContent(R.layout.connect);
        } else {
            setScreenContent(R.layout.disconnect);
        }
        return true;
    }

    /**
     * Resumes the activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mPendingAuth) {
            mPendingAuth = false;
        }
    }

    // Manage UI Screens

    /**
     * Sets up the 'connect' screen content.
     */
    private void setConnectScreenContent() {
        List<String> accounts = getGoogleAccounts();
        if (accounts.size() == 0) {
            // Show a dialog and invoke the "Add Account" activity if requested
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(R.string.needs_account);
            builder.setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                }
            });
            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setTitle(R.string.attention);
            builder.show();
        } else {
            final ListView listView = (ListView) findViewById(R.id.select_account);
            listView.setAdapter(new ArrayAdapter<String>(mContext, R.layout.account, accounts));
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(mAccountSelectedPosition, true);

            final Button connectButton = (Button) findViewById(R.id.connect);
            connectButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // Register in the background and terminate the activity
                    mAccountSelectedPosition = listView.getCheckedItemPosition();
                    TextView account = (TextView) listView.getChildAt(mAccountSelectedPosition);
                    register((String) account.getText());
                    finish();
                }
            });

            final Button exitButton = (Button) findViewById(R.id.exit);
            exitButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    /**
     * Sets up the 'disconnected' screen.
     */
    private void setDisconnectScreenContent() {
        String accountName = PersistentStateHandler.I().getString(Constants.ACCOUNT_NAME, "error");

        // Format the disconnect message with the currently connected account
        // name
        TextView disconnectText = (TextView) findViewById(R.id.disconnect_text);
        String message = getResources().getString(R.string.disconnect_text);
        String formatted = String.format(message, accountName);
        disconnectText.setText(formatted);

        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Delete the current account from shared preferences
                PersistentStateHandler.I().putString(Constants.AUTH_COOKIE, null);
                PersistentStateHandler.I().putString(Constants.DEVICE_REGISTRATION_ID, null);
                PersistentStateHandler.I().putString(Constants.ACCOUNT_NAME, null);

                // Unregister in the background and terminate the activity
                finish();
            }
        });

        Button exitButton = (Button) findViewById(R.id.exit);
        exitButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Sets the screen content based on the screen id.
     */
    private void setScreenContent(int screenId) {
        setContentView(screenId);
        switch (screenId) {
            case R.layout.disconnect:
                setDisconnectScreenContent();
                break;
            case R.layout.connect:
                setConnectScreenContent();
                break;
        }
    }

    // Register and Unregister

    /**
     * Registers for C2DM messaging with the given account name.
     *
     * @param accountName a String containing a Google account name
     */
    private void register(final String accountName) {
        // Store the account name in shared preferences
        PersistentStateHandler.I().putString(Constants.ACCOUNT_NAME, accountName);
        PersistentStateHandler.I().putString(Constants.AUTH_COOKIE, null);

        // Obtain an auth token and register
        AccountManager mgr = AccountManager.get(mContext);
        Account[] accts = mgr.getAccountsByType("com.google");
        for (Account acct : accts) {
            if (acct.name.equals(accountName)) {
                if (PodNomsApplication.isDebug()) {
                    // Use a fake cookie for the dev mode app engine server
                    // The cookie has the form email:isAdmin:userId
                    // We set the userId to be the same as the account name
                    String authCookie = "dev_appserver_login=" + accountName + ":false:" + accountName;
                    PersistentStateHandler.I().putString(Constants.AUTH_COOKIE, authCookie);
                } else {
                    // Get the auth token from the AccountManager and convert
                    // it into a cookie for the appengine server
                    mgr.getAuthToken(acct, "ah", null, this, new AccountManagerCallback<Bundle>() {
                        public void run(AccountManagerFuture<Bundle> future) {
                            try {
                                Bundle authTokenBundle = future.getResult();
                                final String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String authCookie = getAuthCookie(authToken);
                                        PersistentStateHandler.I().putString(Constants.AUTH_COOKIE, authCookie);
                                    }
                                }).start();
                            } catch (AuthenticatorException e) {
                                Log.w(TAG, "Got AuthenticatorException " + e);
                                Log.w(TAG, Log.getStackTraceString(e));
                            } catch (IOException e) {
                                Log.w(TAG, "Got IOException " + Log.getStackTraceString(e));
                                Log.w(TAG, Log.getStackTraceString(e));
                            } catch (OperationCanceledException e) {
                                Log.w(TAG, "Got OperationCanceledException " + e);
                                Log.w(TAG, Log.getStackTraceString(e));
                            }
                        }
                    }, null);
                }
                break;
            }
        }
    }

    // Utility Methods

    /**
     * Retrieves the authorization cookie associated with the given token. This
     * method should only be used when running against a production appengine
     * backend (as opposed to a dev mode server).
     */
    private String getAuthCookie(String authToken) {
        try {
            // Get SACSID cookie
            DefaultHttpClient client = new DefaultHttpClient();
            String continueURL = PodNomsApplication.getApiServiceHost();
            URI uri = new URI(PodNomsApplication.getApiServiceHost() + "/_ah/login?continue="
                    + URLEncoder.encode(continueURL, "UTF-8") + "&auth=" + authToken);
            HttpGet method = new HttpGet(uri);
            final HttpParams getParams = new BasicHttpParams();
            HttpClientParams.setRedirecting(getParams, false);
            method.setParams(getParams);

            HttpResponse res = client.execute(method);
            Header[] headers = res.getHeaders("Set-Cookie");
            if (res.getStatusLine().getStatusCode() != 302 || headers.length == 0) {
                return null;
            }

            for (Cookie cookie : client.getCookieStore().getCookies()) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return AUTH_COOKIE_NAME + "=" + cookie.getValue();
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Got IOException " + e);
            Log.w(TAG, Log.getStackTraceString(e));
        } catch (URISyntaxException e) {
            Log.w(TAG, "Got URISyntaxException " + e);
            Log.w(TAG, Log.getStackTraceString(e));
        }

        return null;
    }

    /**
     * Returns a list of registered Google account names. If no Google accounts
     * are registered on the device, a zero-length list is returned.
     */
    private List<String> getGoogleAccounts() {
        ArrayList<String> result = new ArrayList<String>();
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        for (Account account : accounts) {
            if (account.type.equals("com.google")) {
                result.add(account.name);
            }
        }

        return result;
    }
}

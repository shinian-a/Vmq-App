package com.shinian.pay.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class AccountAuthenticatorService extends Service {
    private AbstractAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new AbstractAccountAuthenticator(this) {
            @Override
            public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
                return null;
            }

            @Override
            public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
                return null;
            }

            @Override
            public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
                return null;
            }

            @Override
            public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
                return null;
            }

            @Override
            public String getAuthTokenLabel(String authTokenType) {
                return null;
            }

            @Override
            public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
                return null;
            }

            @Override
            public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
                return null;
            }
        };
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            return authenticator.getIBinder();
        }
        return null;
    }
}

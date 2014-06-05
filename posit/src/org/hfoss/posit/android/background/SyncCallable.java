package org.hfoss.posit.android.background;

import java.util.concurrent.Callable;

import org.hfoss.posit.android.sync.SyncAdapter;
import org.hfoss.posit.android.sync.SyncServer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

public class SyncCallable implements Callable<Void> {
    
    private Context mContext;
    private Account mAccount;
    private AccountManager mAccountManager;
    private static final String TAG = "SyncCallable";

    public SyncCallable(Context context, Account account,
            AccountManager accountManager)
    {
        mContext = context;
        mAccount = account;
        mAccountManager = accountManager;
    }

    public Void call() throws Exception
    {
        // use the account manager to request the credentials
        String authToken = mAccountManager.blockingGetAuthToken(mAccount,
                SyncAdapter.AUTHTOKEN_TYPE, true);
        Log.i(TAG, "auth token: " + authToken);
        
        SyncServer syncServer = new SyncServer(mContext);
        syncServer.sync(authToken);
        return null;
    }

}

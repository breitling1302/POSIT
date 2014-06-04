package org.hfoss.posit.android.sync;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.background.BackgroundListener;
import org.hfoss.posit.android.background.BackgroundManager;
import org.hfoss.posit.android.background.IsServerReachableCallable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * This class is used to handle sync requests with the server.
 * @author ericenns
 *
 */
public class SyncActivity extends OrmLiteBaseActivity<DbManager> {

	public static final String TAG = "SyncActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");

        AccountManager manager = AccountManager.get(SyncActivity.this);
        Account[] accounts = manager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
        
        BackgroundListener<Boolean> bkgListener =
                new BackgroundListener<Boolean>() {
            public void onBackgroundResult(Boolean retval)
            {
                Account account = (Account)getExtra(0);
                
                if (!retval) {
                    Log.i(TAG, "Sync not requested. Server not reachable");
                    Toast.makeText(SyncActivity.this,
                            "Sync not requested. Server not reachable",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                
                Log.i(TAG, "Requesting sync");
                if (!ContentResolver.getSyncAutomatically(account,
                    getResources().getString(R.string.contentAuthority))) {
                    Log.i(TAG, "Sync not requested. " +
                        SyncAdapter.ACCOUNT_TYPE + " is not ON");
                    Toast.makeText(SyncActivity.this,
                        "Sync not requested: " + SyncAdapter.ACCOUNT_TYPE +
                        " is not ON", Toast.LENGTH_LONG).show();
                } else {
                    Bundle extras = new Bundle();
                    ContentResolver
                    .requestSync(
                            account,
                            getResources().getString(R.string.contentAuthority),
                            extras);
                    Toast.makeText(SyncActivity.this,
                            "Sync requested", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                }
            }
        };
        bkgListener.putExtra(accounts[0]);
		
		// Avoids index-out-of-bounds error if no such account
		// Must be a better way to do this?
		if (accounts.length != 0) {
		    BackgroundManager.runTask(new IsServerReachableCallable(this),
                bkgListener);
		} else {
			Log.i(TAG, "Sync not requested. Unable to get " 
			        + SyncAdapter.ACCOUNT_TYPE);
			Toast.makeText(this, "Sync error: Unable to get "
			        + SyncAdapter.ACCOUNT_TYPE, Toast.LENGTH_LONG).show();
		}
		finish();
	}


}

package org.hfoss.posit.android.background;

import java.util.concurrent.Callable;

import org.hfoss.posit.android.sync.Communicator;

import android.content.Context;

public class IsServerReachableCallable implements Callable<Boolean> {
    private Context mContext;
    
    public IsServerReachableCallable(Context context)
    {
        mContext = context;
    }
    
    public Boolean call() throws Exception
    {
		return Communicator.isServerReachable(mContext);
    }
}

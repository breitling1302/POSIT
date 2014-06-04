package org.hfoss.posit.android.background;

import java.util.concurrent.Callable;

import org.hfoss.posit.android.sync.Communicator;

import android.content.Context;

public class GetAuthKeyCallable implements Callable<String> {
    private Context mContext;
    
    public GetAuthKeyCallable(Context context)
    {
        mContext = context;
    }
    
    public String call() throws Exception
    {
		return Communicator.getAuthKey(mContext);
    }
}

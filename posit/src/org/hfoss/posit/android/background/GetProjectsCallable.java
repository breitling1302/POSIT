package org.hfoss.posit.android.background;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.hfoss.posit.android.sync.SyncServer;

import android.content.Context;

public class GetProjectsCallable implements
        Callable<List<HashMap<String, Object>>> {
    
    private Context mContext;
    
    public GetProjectsCallable(Context context)
    {
        mContext = context;
    }

    public List<HashMap<String, Object>> call() throws Exception
    {
		SyncServer service = new SyncServer(mContext);
		return service.getProjects();
    }
}

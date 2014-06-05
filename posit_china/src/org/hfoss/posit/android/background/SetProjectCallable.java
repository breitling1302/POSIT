package org.hfoss.posit.android.background;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.hfoss.posit.android.sync.SyncServer;

import android.content.Context;

public class SetProjectCallable implements Callable<Boolean> {
    private Context mContext;
    private List<HashMap<String,Object>> mProjectsHash;
    private int mItemPosition;

    public SetProjectCallable(Context context,
            List<HashMap<String, Object>> projectsHash, int itemPosition)
    {
        mContext = context;
        mProjectsHash = projectsHash;
        mItemPosition = itemPosition;
    }

    public Boolean call() throws Exception
    {
        SyncServer service = new SyncServer(mContext);
        return service.setProject(mProjectsHash.get(mItemPosition));
    }
}

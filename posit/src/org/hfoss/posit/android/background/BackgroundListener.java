package org.hfoss.posit.android.background;

import java.util.LinkedList;
import java.util.List;

public abstract class BackgroundListener<T> {
    private List<Object> mExtras = null;
    public abstract void onBackgroundResult(T response);
    
    public void putExtra(Object o)
    {
        if (mExtras == null)
            mExtras = new LinkedList<Object>();
        mExtras.add(o);
    }
    
    public Object getExtra(int location)
    {
        if (mExtras == null || mExtras.isEmpty())
            return null;
        else
            return mExtras.get(location);
    }
}

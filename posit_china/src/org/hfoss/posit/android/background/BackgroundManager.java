package org.hfoss.posit.android.background;

import java.util.concurrent.Callable;

import android.os.AsyncTask;
import android.util.Log;

public class BackgroundManager {
    private static String TAG = "BackgroundManager";
    
    @SuppressWarnings("unchecked")
    public static <T> void runTask(Callable<T> func,
            BackgroundListener<T> bkgListener)
    {
        AsyncTask<Callable<T>, Void, T> task
            = new BackgroundTask<T>(bkgListener);
        
        task.execute(func);
    }
    
    private static class BackgroundTask<T>
        extends AsyncTask<Callable<T>, Void, T> {
        
        BackgroundListener<T> mListener;
        
        protected BackgroundTask(BackgroundListener<T> bkgListener)
        {
            mListener = bkgListener;
        }

        @Override
        protected T doInBackground(Callable<T>... params)
        {
            T response = null;
            try {
                response = params[0].call();
            } catch (Exception e) {
                Log.e(TAG, "BackgroundTask: " + e.getMessage());
                e.printStackTrace();
            }
            
            return response;
        }

        @Override
        protected void onPostExecute(T response)
        {
            mListener.onBackgroundResult(response);
        }
    }
}
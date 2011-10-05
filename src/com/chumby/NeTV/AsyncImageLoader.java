package com.chumby.NeTV;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class AsyncImageLoader
{
    interface AsyncImageCallback
    {
        void onImageReceived(String url, Bitmap bm);
    }
    
    private String mURL;
    private AsyncImageCallback mCallback;
    private boolean busy = false;
  
    public AsyncImageLoader(String url, AsyncImageCallback cb)
    {
        super();
        mURL = url;
        mCallback = cb;
        busy = false;
        reload();
    }
    
    public void reload()
    {
    	if (busy || mURL.length() <= 0)
    		return;
    	
    	(new Thread(doScreenshot)).start();
    }
    
    public void changePath(String newPath)
    {
    	if (newPath.length() <= 0)
    		return;
    	mURL = newPath;
    }
    
    /* 
     * Do not call this directly, call reload() instead
     * 
     * @see java.lang.Thread#run()
     */
    private Runnable doScreenshot = new Runnable()
	{
		public void run()
		{
			if (mURL.length() > 0)
			{
				busy = true;
		        try
		        {
		            HttpURLConnection conn = (HttpURLConnection)(new URL(mURL)).openConnection();
		            conn.setDoInput(true);
		            conn.connect();
		            mCallback.onImageReceived(mURL, BitmapFactory.decodeStream(conn.getInputStream()));
		        }
		        catch (IOException e)
		        {
		            mCallback.onImageReceived(mURL, null);
		        }
		        busy = false;
			}
		}
	};
}

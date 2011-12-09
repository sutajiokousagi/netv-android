package com.bunniestudios.NeTV;

import java.io.File;

import android.util.Log;

public class UtilRootChecker
{
	private static final String TAG = "UtilRootChecker";
	private static final String rootCheckPath = "/data";
	
	public UtilRootChecker()
	{
		Log.d(TAG, "onCreate()");
	}
	
	static public boolean isRooted()
	{
		File file = new File(rootCheckPath);
		return file.exists() && file.canRead();
	}
}

package com.chumby.util;

import android.util.Log;

public class ChumbyLog {
	private static final String TAG="NeTV";

	
	public static void d(String msg) {
		if (msg!=null) d(TAG,msg);
	}

	public static void d(String tag, String msg) {
		if (Log.isLoggable(tag, Log.DEBUG) && msg!=null) {
			Log.d(tag, msg);
		}
	}

	public static void i(String msg) {
		if (msg!=null) i(TAG,msg);
	}
	
	public static void i(String tag, String msg) {
		if (Log.isLoggable(tag, Log.INFO) && msg!=null) {
			Log.i(tag, msg);
		}
	}

	public static void e(String msg) {
		if (msg!=null) e(TAG,msg);
	}

	public static void e(String tag, String msg) {
		if (Log.isLoggable(tag, Log.ERROR) && msg!=null) {
			Log.e(tag, msg);
		}
	}

	public static void v(String msg) {
		if (msg!=null) v(TAG,msg);
	}

	public static void v(String tag, String msg) {
		if (Log.isLoggable(tag, Log.VERBOSE) && msg!=null) {
			Log.v(tag, msg);
		}
	}

	public static void w(String msg) {
		if (msg!=null) w(TAG,msg);
	}

	public static void w(String tag, String msg) {
		if (Log.isLoggable(tag, Log.WARN) && msg!=null) {
			Log.w(tag, msg);
		}
	}
}


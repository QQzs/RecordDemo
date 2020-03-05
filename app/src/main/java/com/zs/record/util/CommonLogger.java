package com.zs.record.util;

import android.util.Log;


public class CommonLogger {
    private static String TAG = CommonLogger.class.getSimpleName(); // 界面可以自定义tag的显示

	public static void d(String msg) {
		d(TAG, msg);
	}

	public static void d(String TAG, String msg) {
		Log.i(TAG, msg);
	}

}
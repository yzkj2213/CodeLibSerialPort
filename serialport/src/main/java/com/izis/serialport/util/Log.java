package com.izis.serialport.util;

public class Log {
    private static final String TAG = "串口调试";
    public static void d(String message) {
        android.util.Log.d(TAG, message);
    }

    public static void i(String message) {
        android.util.Log.i(TAG, message);
    }

    public static void w(String message) {
        android.util.Log.w(TAG, message);
    }

    public static void e(String message) {
        android.util.Log.e(TAG, message);
    }
}

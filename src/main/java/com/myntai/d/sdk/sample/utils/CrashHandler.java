package com.myntai.d.sdk.sample.utils;

import android.content.Context;
import android.util.Log;


public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static CrashHandler INSTANCE ;
    private Context context;

    private CrashHandler(){

    }

    public static synchronized CrashHandler getInstance(){
        if (INSTANCE == null)
            INSTANCE = new CrashHandler();
        return INSTANCE;
    }

    public void init(Context context){
        this.context = context;
    }


    public void uncaughtException(Thread thread, Throwable throwable) {
        Log.e(TAG, "程序挂掉了!!! ");

        android.os.Process.killProcess(android.os.Process.myPid());
    }

}

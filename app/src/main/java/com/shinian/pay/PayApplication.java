package com.shinian.pay;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * 应用程序全局管理类
 */
public class PayApplication extends Application {
    
    private static final String TAG = "PayApplication";
    private static Context mContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Log.d(TAG, "Application initialized");
    }
    
    public static Context getContext() {
        return mContext;
    }
}

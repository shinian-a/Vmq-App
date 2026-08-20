package com.shinian.pay.helper;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class BatteryHelper {
    private final Context context;
    private Handler handler;
    private Runnable runnable;

    public BatteryHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public int getLevel() {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
    }

    /**
     * 定时更新电量TextView，自动在destroy时停止
     */
    public void startPeriodicUpdate(TextView textView, long intervalMs) {
        stop();
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                textView.setText("当前电量：" + getLevel() + "%");
                Handler currentHandler = handler;
                if (currentHandler != null) {
                    currentHandler.postDelayed(this, intervalMs);
                }
            }
        };
        handler.post(runnable);
    }

    public void stop() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        handler = null;
        runnable = null;
    }
}
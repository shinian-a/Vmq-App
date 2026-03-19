package com.shinian.pay.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.annotation.Nullable;

import com.shinian.pay.R;

/** 移除前台Service通知栏标志，这个Service选择性使用
 *
 * Created by jiandongguo on 2017/7/7.


/** 移除前台Service通知栏标志，这个Service选择性使用
 *
 * Created by jianddongguo on 2017/7/7.
 * http://blog.csdn.net/andrexpert
 */

public class CancelNoticeService extends Service {
    private static final String CHANNEL_ID = "cancel_notice_channel";
    private static final String CHANNEL_NAME = "取消通知服务";
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            createNotificationChannel();
            Notification.Builder builder = createNotificationBuilder();
            startForeground(DaemonService.NOTICE_ID, builder.build());
            // 开启一条线程，去移除 DaemonService 弹出的通知
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 延迟 1s
                    SystemClock.sleep(1000);
                    // 取消 CancelNoticeService 的前台
                    stopForeground(true);
                    // 移除 DaemonService 弹出的通知
                    NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    manager.cancel(DaemonService.NOTICE_ID);
                    // 任务完成，终止自己
                    stopSelf();
                }
            }).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }
        
    /**
     * 创建通知渠道 (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN // 使用最低重要性，完全隐藏通知
            );
            channel.setDescription("用于临时移除前台通知");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
                
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
        
    /**
     * 创建通知构建器 (兼容不同版本)
     */
    private Notification.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setPriority(Notification.PRIORITY_MIN);
        } else {
            return new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

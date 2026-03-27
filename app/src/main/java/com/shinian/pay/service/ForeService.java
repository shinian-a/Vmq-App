package com.shinian.pay.service;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.shinian.pay.R;
import com.shinian.pay.ui.MainActivity;


public class ForeService extends Service {

    /**
     * 通知栏常驻类
     */
    private static final String TAG = "ForeService";
    private static final String NOTIFICATION_CHANNEL_ID = "vmq_core_service";
    private static final String NOTIFICATION_CHANNEL_NAME = "V免签监控端_Pro 核心服务";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
        // 注意：不在这里调用 setNotification()
        // Android 14 要求在 onStartCommand 中快速调用 startForeground()
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        // Android 14 关键修复：立即调用 startForeground()
        // 必须在 5 秒内完成，越快越好
        setNotification();
        // 确保服务被杀死后重启
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        stopForeground(true);
        super.onDestroy();
    }

    /**
     * 设置前台服务通知
     */
    public void setNotification() {
        Log.d(TAG, "设置前台通知");

        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                Log.e(TAG, "通知管理器不可用");
                return;
            }

            // 创建通知渠道（Android O 及以上）
            createNotificationChannel(notificationManager);

            // 构建通知
            Notification notification = buildNotification();

            // 启动前台服务，指定前台服务类型
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要指定前台服务类型
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                Log.d(TAG, "使用 Android 12+ 方式启动前台服务");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "使用 Android 8.0+ 方式启动前台服务");
            } else {
                // Android 7.x 及以下
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "使用旧版本方式启动前台服务");
            }
        } catch (Exception e) {
            Log.e(TAG, "启动前台服务失败：" + e.getMessage(), e);
            // 如果启动失败，尝试降级方案
            try {
                Notification notification = buildNotification();
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "使用降级方案启动前台服务成功");
            } catch (Exception e2) {
                Log.e(TAG, "降级方案也失败了：" + e2.getMessage(), e2);
            }
        }
    }
    
    /**
     * 创建通知渠道（Android O 及以上必需）
     * @param notificationManager 通知管理器
     */
    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 构建前台服务通知
     * @return 通知对象
     */
    private Notification buildNotification() {
        String content = "服务正在后台运行中！";
        Intent intent = new Intent(this, MainActivity.class);

        // Android 14 兼容性修复：使用 FLAG_IMMUTABLE
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                pendingIntentFlags
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                   .setSmallIcon(R.drawable.ic_launcher)
                   .setContentTitle(getString(R.string.app_name))
                   .setContentText(content)
                   .setWhen(System.currentTimeMillis())
                   .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true); // 设置为常驻通知
            // Android 10+ setTicker 已废弃，但为了兼容保留
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                builder.setTicker(content);
            }
            Notification notification = builder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            return notification;
        } else {
            Notification.Builder builder = new Notification.Builder(this);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                   .setSmallIcon(R.drawable.ic_launcher)
                   .setContentTitle(getString(R.string.app_name))
                   .setContentText(content)
                   .setTicker(content)
                   .setWhen(System.currentTimeMillis())
                   .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true);
            Notification notification = builder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            return notification;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

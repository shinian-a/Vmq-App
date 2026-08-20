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

    private static final String TAG = "ForeService";
    private static final String NOTIFICATION_CHANNEL_ID = "vmq_core_service";
    private static final String NOTIFICATION_CHANNEL_NAME = "V免签监控端_Pro 核心服务";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        // ⚠️ 必须在 5 秒内调用 startForeground()，否则系统杀进程
        if (!setNotification()) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        // ✅ 修复：API 33+ 使用新 API，旧版用布尔参数
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        super.onDestroy();
    }

    /**
     * 启动前台服务通知（兼容 Android 7 ~ 16）
     */
    private boolean setNotification() {
        Log.d(TAG, "设置前台通知");

        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                Log.e(TAG, "通知管理器不可用");
                return false;
            }

            // Android 13+ 未授予通知权限时，通知可能不显示在通知栏，
            // 但仍必须调用 startForeground，否则服务会被系统终止。
            createNotificationChannel(notificationManager);
            Notification notification = buildNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // ✅ Android 14+（API 34）：必须指定 foregroundServiceType
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                Log.d(TAG, "Android 14+ 方式启动前台服务");

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // ✅ Android 8.0 ~ 13（API 26-33）：无需指定 type
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Android 8.0+ 方式启动前台服务");

            } else {
                // ✅ Android 7.x（API 24-25）
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Android 7.x 方式启动前台服务");
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, "启动前台服务失败：" + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建通知渠道（Android O+ 必需）
     */
    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ✅ 修复：前台服务常驻通知应使用 IMPORTANCE_LOW，避免发出声音
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setDescription("支付监控核心后台服务");
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建前台服务通知（兼容 Android 7 ~ 16）
     */
    private Notification buildNotification() {
        String content = "服务正在后台运行中！";

        Intent intent = new Intent(this, MainActivity.class);
        // ✅ FLAG_IMMUTABLE 从 API 31 开始强制要求
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, pendingIntentFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ✅ Android 8.0+ 使用带 channelId 的构造器
            return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)       // ✅ 只需这一个即可，不需要再手动设 flags
                    .build();
        } else {
            // ✅ Android 7.x 使用旧构造器
            return new Notification.Builder(this)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(content)
                    .setTicker(content)     // API < 26 有效
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
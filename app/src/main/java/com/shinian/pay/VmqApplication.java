package com.shinian.pay;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import com.shinian.pay.manager.AppConstants;

/**
 * 全局 Application 类
 * 负责初始化全局组件和配置
 */
public class VmqApplication extends Application {

    private static final String TAG = "VmqApplication";
    private static VmqApplication sInstance;

    /**
     * 获取 Application 实例
     *
     * @return Application 实例
     */
    public static VmqApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        Log.d(TAG, "Application 初始化");

        // 设置全局异常捕获，防止启动崩溃
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e(TAG, "未捕获的异常：" + ex.getMessage(), ex);
                // 记录异常但不退出应用（可选）
                // android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        // 创建通知渠道（Android 8.0+）
        createNotificationChannels();
    }

    /**
     * 创建所有需要的通知渠道
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "通知管理器不可用");
                return;
            }

            // 1. 核心服务通知渠道
            NotificationChannel coreServiceChannel = new NotificationChannel(
                    "vmq_core_service",
                    "V 免签监控端_Pro 核心服务",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            coreServiceChannel.setDescription("用于显示核心服务运行状态");
            coreServiceChannel.enableLights(true);
            coreServiceChannel.setLightColor(Color.GREEN);
            coreServiceChannel.setShowBadge(true);

            // 2. 守护服务通知渠道
            NotificationChannel daemonChannel = new NotificationChannel(
                    "daemon_service_channel",
                    "守护服务通知",
                    NotificationManager.IMPORTANCE_LOW
            );
            daemonChannel.setDescription("用于保持应用后台运行");
            daemonChannel.enableLights(false);
            daemonChannel.enableVibration(false);
            daemonChannel.setShowBadge(false);

            // 3. 心跳通知渠道（如果需要）
            NotificationChannel heartbeatChannel = new NotificationChannel(
                    AppConstants.NOTIFICATION_CHANNEL_ID,
                    "心跳通知",
                    NotificationManager.IMPORTANCE_LOW
            );
            heartbeatChannel.setDescription("用于心跳保活");
            heartbeatChannel.enableLights(false);
            heartbeatChannel.enableVibration(false);
            heartbeatChannel.setShowBadge(false);

            // 注册所有渠道
            notificationManager.createNotificationChannel(coreServiceChannel);
            notificationManager.createNotificationChannel(daemonChannel);
            notificationManager.createNotificationChannel(heartbeatChannel);

            Log.d(TAG, "通知渠道创建完成");
        }
    }
}

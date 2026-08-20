package com.shinian.pay.helper;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.shinian.pay.R;
import com.shinian.pay.service.PayNotificationListenerService;

import java.util.Set;

public class NotificationHelper {

    private static final String CHANNEL_ID = "vmq_test_channel";
    private static final String CHANNEL_NAME = "V免签测试通知";
    private static final int MAX_ID = 1000;

    private final Context context;
    private int nextId = 0;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
    }


    /**
     * 创建通知渠道（Android 8+ 必须，低版本自动忽略）
     * 内部做了重复创建保护，可安全多次调用
     */
    public void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getManager();
            // ✅ 避免重复创建导致用户自定义设置被重置
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setDescription("用于测试推送及收款监听状态提示");
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 发送测试通知（兼容 Android 7 ~ 16）
     *
     * @return true=发送成功, false=权限不足或发送失败
     */
    public boolean sendTestNotification() {
        // ① Android 13+ (API 33) 运行时通知权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "未授予通知权限，请在设置中开启", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // 确保渠道存在
        ensureChannel();

        try {
            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("V免签测试推送")
                    .setContentText("如果看到此通知，说明监听权限正常")
                    .setTicker("测试推送信息")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .build();

            getManager().notify(nextId++ % MAX_ID, notification);
            return true;

        } catch (Exception e) {
            Toast.makeText(context, "通知发送失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    /**
     * 检查本 App 的通知监听服务是否已授权
     */
    public boolean isListenerEnabled() {
        Set<String> packages = NotificationManagerCompat.getEnabledListenerPackages(context);
        return packages.contains(context.getPackageName());
    }

    /**
     * 切换监听服务组件状态（触发系统重新绑定）
     * 仅在用户已在设置页授权后调用才有效
     */
    public void toggleListenerService() {
        PackageManager pm = context.getPackageManager();
        ComponentName cn = new ComponentName(context, PayNotificationListenerService.class);

        // 1. 检查当前组件状态，如果已经是 ENABLED 且服务正在运行，跳过
        int currentState = pm.getComponentEnabledSetting(cn);
        if (currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

                || currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            // 组件已启用，无需重复 toggle
            Log.d("NotificationHelper", "NLS 已处于启用状态，跳过 toggle");
            return;
        }

        // 2. 只在组件确实被禁用时才执行 disable→enable
        Log.d("NotificationHelper", "NLS 未启用，执行 toggle");
        pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * 打开通知监听器授权页面
     * Android 11+ 支持直接跳转到本 App 的监听设置页
     */
    public void openListenerSettings(Activity activity) {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        new ComponentName(context, PayNotificationListenerService.class).flattenToString());
            } else {
                // Android 7-10 只能打开通用列表页
                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(fallback);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(activity, "您的手机暂不支持此设置，请手动在「设置→通知权限」中查找", Toast.LENGTH_LONG).show();
            }
        }
    }

    private NotificationManager getManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
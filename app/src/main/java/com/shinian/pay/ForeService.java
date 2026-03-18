package com.shinian.pay;
import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.os.Binder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.BitmapFactory;
import android.app.NotificationManager;
import android.os.Build;
import android.app.NotificationChannel;
import android.graphics.Color;
import androidx.annotation.Nullable;
import android.content.Context;


public class ForeService extends Service {

/**
 * 通知栏常驻类
 */

    public void setNotification() {
        Log.d("常驻通知栏", "启动中！");
        Notification mNotification;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //开始执行
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "V免签监控端_Pro核心服务", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);
            Notification.Builder builder = new Notification.Builder(this, "1");
            mNotification = builder
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker("服务正在后台运行中！")
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(false)
                .setWhen(System.currentTimeMillis())//设置推送时间，格式为"小时：分钟"
                .setContentIntent(pendingIntent)
                .setContentText("服务正在后台运行中！")
                .build();
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        } else {
            mNotification = new Notification.Builder(this)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker("服务正在后台运行中！")
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(false)
                .setWhen(System.currentTimeMillis())//设置推送时间，格式为"小时：分钟"
                .setContentIntent(pendingIntent)
                .setContentText("服务正在后台运行中！")
                .build();
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        }
        startForeground(1, mNotification);
    }

    @Override
    public void onCreate() {
        Log.d("NotifiCollectorMonitor", "onCreate() called");
        //开始执行jobservice
        //startService(new Intent(this, JobService.class));
        //设置常驻通知
        setNotification();
        //ensureCollectorRunning();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        startService(new Intent(this, ForeService.class));
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.shinian.pay.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import com.shinian.pay.R;
import com.shinian.pay.manager.AppConstants;

/**前台Service，使用startForeground
 * 这个Service尽量要轻，不要占用过多的系统资源，否则


/**前台Service，使用startForeground
 * 这个Service尽量要轻，不要占用过多的系统资源，否则
 * 系统在资源紧张时，照样会将其杀死
 *
 * Created by jianddongguo on 2017/7/7.
 * http://blog.csdn.net/andrexpert
 */
public class DaemonService extends Service {
    private static final String TAG = "DaemonService";
    public static final int NOTICE_ID = 100;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(AppConstants.DEBUG)
            Log.d(TAG,"DaemonService---->onCreate 被调用，启动前台 service");
        //如果API大于18，需要弹出一个可见通知
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setContentTitle("KeepAppAlive");
            builder.setContentText("DaemonService is runing...");
            startForeground(NOTICE_ID,builder.build());
            // 如果觉得常驻通知栏体验不好
            // 可以通过启动CancelNoticeService，将通知移除，oom_adj值不变
            Intent intent = new Intent(this,CancelNoticeService.class);
            startService(intent);
        }else{
            startForeground(NOTICE_ID,new Notification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 如果Service被终止
        // 当资源允许情况下，重启service
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 如果Service被杀死，干掉通知
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            NotificationManager mManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            mManager.cancel(NOTICE_ID);
        }
        if(AppConstants.DEBUG)
            Log.d(TAG,"DaemonService---->onDestroy，前台 service 被杀死");
        // 重启自己
        Intent intent = new Intent(getApplicationContext(),DaemonService.class);
        startService(intent);
    }
}

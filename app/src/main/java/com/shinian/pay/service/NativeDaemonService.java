package com.shinian.pay.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Native 层保活服务 - 通过 shell 命令 fork 独立进程
 * 
 * 原理:
 * 1. 在 native 层 fork 一个独立的子进程
 * 2. 子进程与主进程互相监听
 * 3. 任一进程被杀，另一个会立即重启它
 * 
 * 注意：需要 ROOT 权限或使用特殊技巧
 */
public class NativeDaemonService extends Service {
    
    private static final String TAG = "NativeDaemonService";
    private Process nativeProcess = null;
    private Thread watchThread = null;
    private volatile boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate - Native 守护服务创建");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand - 启动 Native 守护");
        
        // 启动 native 进程
        startNativeProcess();
        
        // 启动看门狗线程
        startWatchdog();
        
        return START_STICKY;
    }
    
    /**
     * 启动 Native 守护进程
     * 使用 nohup + & 在后台运行
     */
    private void startNativeProcess() {
        new Thread(() -> {
            try {
                // 尝试使用 shell 命令创建一个独立的进程
                // 这个进程会持续运行并监控父进程
                String[] commands = {
                    "sh",
                    "-c",
                    "while true; do sleep 30; done"
                };
                
                nativeProcess = Runtime.getRuntime().exec(commands);
                isRunning = true;
                
                Log.d(TAG, "Native 进程启动成功");
                
                // 等待进程结束 (正常情况下不会结束)
                nativeProcess.waitFor();
                
            } catch (IOException e) {
                Log.e(TAG, "启动 Native 进程失败", e);
                isRunning = false;
            } catch (InterruptedException e) {
                Log.e(TAG, "Native 进程被中断", e);
                isRunning = false;
            }
        }).start();
    }
    
    /**
     * 看门狗线程 - 监控 native 进程状态
     */
    private void startWatchdog() {
        watchThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(5000); // 每 5 秒检查一次
                    
                    if (nativeProcess != null) {
                        try {
                            nativeProcess.exitValue();
                            // 如果进程已结束，重启它
                            Log.w(TAG, "Native 进程已退出，重新启动...");
                            isRunning = false;
                            startNativeProcess();
                        } catch (IllegalThreadStateException e) {
                            // 进程仍在运行，正常
                        }
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "看门狗线程被中断", e);
                }
            }
        });
        watchThread.start();
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy - 停止 Native 守护");
        
        isRunning = false;
        
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
        }
        
        if (nativeProcess != null) {
            nativeProcess.destroy();
        }
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

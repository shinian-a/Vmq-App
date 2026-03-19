package com.shinian.pay.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * 同步适配器服务
 * 负责执行实际的同步任务，配合 Account 实现系统级保活
 */
public class SyncAdapterService extends Service {

    private static final String TAG = "SyncAdapterService";

    // 同步适配器实例
    private static AbstractThreadedSyncAdapter sSyncAdapter = null;

    // ContentProvider 客户端
    private ContentProviderClient mContentProviderClient;

    /**
     * 获取同步适配器
     */
    public static AbstractThreadedSyncAdapter getSyncAdapter() {
        return sSyncAdapter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化同步适配器
        if (sSyncAdapter == null) {
            sSyncAdapter = createSyncAdapter(this);
        }
        
        Log.i(TAG, "SyncAdapter 服务已创建");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 返回同步适配器的 Binder
        return sSyncAdapter.getSyncAdapterBinder();
    }

    /**
     * 创建同步适配器
     */
    private static SyncAdapterImpl createSyncAdapter(Context context) {
        return new SyncAdapterImpl(context, true);
    }

    /**
     * 同步适配器实现类
     */
    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

        public SyncAdapterImpl(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
            Log.d(TAG, "SyncAdapter 初始化完成");
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, 
                                 String authority, ContentProviderClient provider, 
                                 SyncResult syncResult) {
            // 执行同步任务
            // 这里不需要实际同步数据，只需要保持应用存活
            Log.d(TAG, "执行同步任务 - 账户：" + account.name + ", 授权：" + authority);
            
            // 模拟轻量级同步操作（不消耗资源）
            try {
                // 短暂休眠，模拟同步过程
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.w(TAG, "同步任务被中断", e);
            }
            
            // 同步成功
            syncResult.stats.numEntries++;
            Log.d(TAG, "同步任务完成");
        }
    }
}

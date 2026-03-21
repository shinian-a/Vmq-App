package com.shinian.pay.service;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    public SyncAdapter(Context context, boolean autoInitialize) {
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

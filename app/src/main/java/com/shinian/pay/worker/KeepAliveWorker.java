package com.shinian.pay.worker;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * 实际执行的 Worker（同步版本）
 * 继承 Worker 适用于简单同步任务
 * 如需异步操作，可继承 ListenableWorker 或使用 SettableFuture
 */
public class KeepAliveWorker extends Worker {

    private static final String TAG = "KeepAliveWorker";

    public KeepAliveWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "▶ 周期任务开始执行，attempt=" + getRunAttemptCount());

            // ========== 在此处写入你的业务逻辑 ==========
            performKeepAliveTask();
            // =============================================

            Log.d(TAG, "✓ 周期任务执行成功");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "✗ 周期任务执行失败", e);

            // 重试策略：最多重试3次
            if (getRunAttemptCount() < 3) {
                Log.w(TAG, "将进行第 " + (getRunAttemptCount() + 1) + " 次重试");
                return Result.retry();
            } else {
                return Result.failure();
            }
        }
    }

    /**
     * 你的实际业务逻辑
     */
    private void performKeepAliveTask() {
        // 示例1：检查并重启前台服务
        // if (!ServiceUtils.isServiceRunning(getApplicationContext(), MyForegroundService.class)) {
        //     ServiceUtils.startForegroundService(getApplicationContext());
        // }

        // 示例2：心跳上报
        // HeartbeatApi.sendHeartbeat();

        // 示例3：数据同步
        // SyncRepository.syncData(getApplicationContext());

        // 示例4：检查进程状态
        // ProcessChecker.checkAndRestart(getApplicationContext());

        Log.d(TAG, "业务逻辑执行中...");
    }
}
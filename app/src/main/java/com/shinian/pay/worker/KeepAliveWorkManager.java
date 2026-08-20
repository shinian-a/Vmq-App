package com.shinian.pay.worker;


import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager 周期任务管理器（Java 版）
 * 兼容 Android 7.0 (API 24) ~ Android 16 (API 36)
 *
 * 用法：
 *   KeepAliveWorkManager.init(context);
 *   KeepAliveWorkManager.schedule(context);
 *   KeepAliveWorkManager.cancel(context);
 */
public final class KeepAliveWorkManager {

    private static final String TAG = "KeepAliveWork";
    private static final String UNIQUE_WORK_NAME = "keep_alive_periodic_work";
    private static final long DEFAULT_INTERVAL_MINUTES = 15L;

    // 禁止实例化
    private KeepAliveWorkManager() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    // ==================== 初始化 ====================

    /**
     * 在 Application.onCreate() 中调用
     * 自定义 WorkManager 配置
     */
    public static void init(@NonNull Context context) {
        try {
            androidx.work.Configuration config = new androidx.work.Configuration.Builder()
                    .setMinimumLoggingLevel(Log.INFO)
                    .build();
            WorkManager.initialize(context, config);
            Log.d(TAG, "WorkManager 初始化完成");
        } catch (IllegalStateException e) {
            // 已经初始化过，忽略
            Log.w(TAG, "WorkManager 已初始化，跳过", e);
        }
    }

    // ==================== 调度任务 ====================

    /**
     * 启动周期性保活任务（使用默认参数）
     */
    public static void schedule(@NonNull Context context) {
        schedule(context, DEFAULT_INTERVAL_MINUTES, false, false, false);
    }

    /**
     * 启动周期性保活任务
     *
     * @param context         上下文
     * @param intervalMinutes 执行间隔（分钟），最小15分钟
     * @param requireNetwork  是否需要网络
     * @param requireCharging 是否需要充电中
     * @param requireIdle     是否需要设备空闲
     */
    public static void schedule(
            @NonNull Context context,
            long intervalMinutes,
            boolean requireNetwork,
            boolean requireCharging,
            boolean requireIdle
    ) {
        // 系统强制最小15分钟
        long safeInterval = Math.max(intervalMinutes, 15L);

        // 构建约束条件
        Constraints constraints = buildConstraints(requireNetwork, requireCharging, requireIdle);

        // 构建周期性任务
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                KeepAliveWorker.class,
                safeInterval,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                )
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag(TAG)
                .build();

        // 入队（唯一名称，已存在则保留不重复创建）
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
        );

        Log.d(TAG, "周期任务已调度，间隔=" + safeInterval + "分钟");
    }

    // ==================== 取消任务 ====================

    /**
     * 取消周期任务
     */
    public static void cancel(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
        Log.d(TAG, "周期任务已取消");
    }

    /**
     * 取消所有 WorkManager 任务（慎用）
     */
    public static void cancelAll(@NonNull Context context) {
        WorkManager.getInstance(context).cancelAllWork();
        Log.w(TAG, "所有 WorkManager 任务已取消");
    }

    // ==================== 查询状态 ====================

    /**
     * 获取任务运行状态（LiveData，用于Activity/Fragment观察）
     */
    public static LiveData<List<WorkInfo>> observeStatus(@NonNull Context context) {
        return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME);
    }

    /**
     * 同步获取当前任务状态（子线程调用）
     */
    @Nullable
    public static List<WorkInfo> getStatusSync(@NonNull Context context) {
        try {
            return WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME)
                    .get();
        } catch (Exception e) {
            Log.e(TAG, "获取任务状态失败", e);
            return null;
        }
    }

    /**
     * 判断任务是否已调度
     */
    public static boolean isScheduled(@NonNull Context context) {
        try {
            List<WorkInfo> workInfos = WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME)
                    .get();
            if (workInfos != null && !workInfos.isEmpty()) {
                WorkInfo.State state = workInfos.get(0).getState();
                return state == WorkInfo.State.ENQUEUED

                        || state == WorkInfo.State.RUNNING
                        || state == WorkInfo.State.BLOCKED;
            }
        } catch (Exception e) {
            Log.e(TAG, "查询任务状态异常", e);
        }
        return false;
    }

    // ==================== 内部方法 ====================

    private static Constraints buildConstraints(
            boolean requireNetwork,
            boolean requireCharging,
            boolean requireIdle
    ) {
        Constraints.Builder builder = new Constraints.Builder();

        // 网络要求
        if (requireNetwork) {
            builder.setRequiredNetworkType(NetworkType.CONNECTED);
        } else {
            builder.setRequiredNetworkType(NetworkType.NOT_REQUIRED);
        }

        // 充电要求
        builder.setRequiresCharging(requireCharging);

        // 设备空闲要求（Android 6.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setRequiresDeviceIdle(requireIdle);
        }

        // 存储空间充足
        builder.setRequiresStorageNotLow(true);

        // 电量不低（保活场景建议false，避免低电量时不执行）
        builder.setRequiresBatteryNotLow(false);

        return builder.build();
    }
}
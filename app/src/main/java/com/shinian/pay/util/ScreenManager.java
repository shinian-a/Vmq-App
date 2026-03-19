package com.shinian.pay.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.shinian.pay.ui.SinglePixelActivity;
import com.shinian.pay.manager.AppConstants;

import java.lang.ref.WeakReference;

/**
 * 1 像素保活管理类
 *
 * 功能说明:
 * 1. 管理 1 像素透明 Activity 的生命周期
 * 2. 在锁屏时启动 1 像素 Activity 提升进程优先级
 * 3. 在亮屏时销毁 1 像素 Activity 避免用户感知
 * 4. 使用单例模式确保全局唯一实例
 * 5. 使用弱引用持有 Activity，防止内存泄漏
 *
 * 使用场景:
 * - 监听系统锁屏广播时调用
 * - 需要在后台保持应用活跃状态时
 *
 * 注意事项:
 * - 必须在 Application 或 Activity 中初始化
 * - 需要配合 ScreenReceiverUtil 使用
 * - SinglePixelActivity 需在 AndroidManifest 中配置透明主题
 *
 * @author jianddongguo
 * @date 2017/7/8
 * @see SinglePixelActivity
 * @see ScreenReceiverUtil
 */
public class ScreenManager {
    /** 日志标签 */
    private static final String TAG = "ScreenManager";

    /** 上下文对象 */
    private Context mContext;

    /** 单例实例 */
    private static ScreenManager mSreenManager;

    /** Activity 的弱引用，防止内存泄漏 */
    private WeakReference<Activity> mActivityRef;

    /**
     * 私有构造函数
     *
     * @param context 上下文对象，建议使用 ApplicationContext
     */
    private ScreenManager(Context mContext){
        this.mContext = mContext;
    }

    /**
     * 获取 ScreenManager 单例实例
     *
     * 采用懒汉式单例模式，线程不安全但性能较好。
     * 如需线程安全可改为双重检查锁定 (DCL) 模式。
     *
     * @param context 上下文对象
     * @return ScreenManager 单例实例
     */
    public static ScreenManager getScreenManagerInstance(Context context){
        if(mSreenManager == null){
            mSreenManager = new ScreenManager(context);
        }
        return mSreenManager;
    }

    /**
     * 设置 SinglePixelActivity 的引用
     *
     * 当 SinglePixelActivity 创建时调用此方法，将自身引用传递给 ScreenManager。
     * 使用弱引用包装，避免强引用导致的内存泄漏问题。
     *
     * @param mActivity SinglePixelActivity 实例，不能为 null
     *
     * 调用时机:
     * - 在 SinglePixelActivity.onCreate() 中调用
     *
     * 示例:
     * <pre>
     * {@code
     * @Override
     * protected void onCreate(Bundle savedInstanceState) {
     *     super.onCreate(savedInstanceState);
     *     ScreenManager.getScreenManagerInstance(this).setSingleActivity(this);
     * }
     * }
     * </pre>
     */
    public void setSingleActivity(Activity mActivity){
        mActivityRef = new WeakReference<>(mActivity);
    }

    /**
     * 启动 SinglePixelActivity
     *
     * 创建并启动 1 像素透明 Activity，用于在锁屏时保持应用在前台。
     * 该方法会添加 Intent.FLAG_ACTIVITY_NEW_TASK 标志，确保在新任务栈中启动。
     *
     * 使用场景:
     * - 系统锁屏时调用，将应用置于前台
     * - 需要提升进程优先级时
     *
     * 注意事项:
     * - 仅在 DEBUG 模式下输出日志
     * - 需要确保 mContext 不为 null
     * - SinglePixelActivity 必须配置透明主题，避免用户看到闪烁
     *
     * 调用时机:
     * - 在 BroadcastReceiver 收到 ACTION_SCREEN_OFF 广播后调用
     *
     * @see Intent#FLAG_ACTIVITY_NEW_TASK
     */
    public void startActivity(){
        if(AppConstants.DEBUG)
            Log.d(TAG,"准备启动 SinglePixelActivity...");
        Intent intent = new Intent(mContext,SinglePixelActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 结束 SinglePixelActivity
     *
     * 通过弱引用获取 SinglePixelActivity 实例并调用 finish() 方法关闭它。
     * 如果 Activity 已经被回收或为 null，则不执行任何操作。
     *
     * 使用场景:
     * - 系统亮屏时调用，移除 1 像素界面
     * - 不再需要保活时清理资源
     *
     * 注意事项:
     * - 使用弱引用避免空指针异常
     * - 多次调用不会产生副作用
     * - 仅在 DEBUG 模式下输出日志
     *
     * 调用时机:
     * - 在 BroadcastReceiver 收到 ACTION_SCREEN_ON 广播后调用
     * - 在应用退出或清理保活服务时调用
     */
    public void finishActivity(){
        if(AppConstants.DEBUG)
            Log.d(TAG,"准备结束 SinglePixelActivity...");
        if(mActivityRef != null){
            Activity mActivity = mActivityRef.get();
            if(mActivity != null){
                mActivity.finish();
            }
        }
    }
}

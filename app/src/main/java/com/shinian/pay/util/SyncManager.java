package com.shinian.pay.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * 同步管理器
 * 负责管理 Account 和 SyncAdapter，实现系统级保活
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    // 账户类型（必须与 authenticator.xml 中的一致）
    public static final String ACCOUNT_TYPE = "com.shinian.pay.account";

    // 账户名称
    public static final String ACCOUNT_NAME = "VmqAppSyncAccount";

    // ContentProvider 的 authority（必须与 syncadapter.xml 中的一致）
    public static final String AUTHORITY = "com.shinian.pay.provider";

    // 同步间隔（15 分钟）
    private static final long SYNC_INTERVAL = 15 * 60; // 900 秒

    // 单例实例
    private static SyncManager sInstance;

    private Context mContext;
    private Account mAccount;
    private AccountManager mAccountManager;
    private ContentResolver mContentResolver;

    /**
     * 私有构造函数
     */
    private SyncManager(Context context) {
        mContext = context.getApplicationContext();
        mAccountManager = AccountManager.get(mContext);
        mContentResolver = mContext.getContentResolver();
        
        // 创建账户对象
        mAccount = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
    }

    /**
     * 获取单例实例
     */
    public static SyncManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SyncManager.class) {
                if (sInstance == null) {
                    sInstance = new SyncManager(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 创建并注册账户到系统
     * @return true 表示成功，false 表示失败
     */
    public boolean createAndRegisterAccount() {
        try {
            // 检查账户是否已存在
            Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);
            
            if (accounts.length > 0) {
                Log.d(TAG, "账户已存在，无需重复创建");
                mAccount = accounts[0];
                return true;
            }

            // 创建新账户并添加到系统
            if (mAccountManager.addAccountExplicitly(mAccount, null, null)) {
                Log.i(TAG, "账户创建成功：" + ACCOUNT_NAME);
                
                // 设置自动同步
                ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
                
                return true;
            } else {
                Log.e(TAG, "账户创建失败");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "创建账户时发生异常", e);
            return false;
        }
    }

    /**
     * 启动周期性同步
     * 系统会每 15 分钟自动唤醒应用一次
     */
    public void startPeriodicSync() {
        try {
            // 确保账户已创建
            if (!createAndRegisterAccount()) {
                Log.e(TAG, "账户未创建，无法启动同步");
                return;
            }

            // 取消现有的同步请求
            cancelPeriodicSync();

            // 添加周期性同步请求
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);

            // 设置同步参数
            ContentResolver.addPeriodicSync(
                mAccount,
                AUTHORITY,
                extras,
                SYNC_INTERVAL // 同步间隔（秒）
            );

            Log.i(TAG, "周期性同步已启动，间隔：" + SYNC_INTERVAL + "秒");

            // 立即执行一次同步测试
            requestImmediateSync();

        } catch (Exception e) {
            Log.e(TAG, "启动周期性同步失败", e);
        }
    }

    /**
     * 取消周期性同步
     */
    public void cancelPeriodicSync() {
        try {
            if (createAndRegisterAccount()) {
                ContentResolver.removePeriodicSync(
                    mAccount,
                    AUTHORITY,
                    new Bundle()
                );
                Log.d(TAG, "周期性同步已取消");
            }
        } catch (Exception e) {
            Log.e(TAG, "取消周期性同步失败", e);
        }
    }

    /**
     * 请求立即同步
     */
    public void requestImmediateSync() {
        try {
            if (createAndRegisterAccount()) {
                Bundle extras = new Bundle();
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, true);

                ContentResolver.requestSync(mAccount, AUTHORITY, extras);
                Log.d(TAG, "已请求立即同步");
            }
        } catch (Exception e) {
            Log.e(TAG, "请求立即同步失败", e);
        }
    }

    /**
     * 检查同步是否已启用
     * @return true 表示已启用，false 表示未启用
     */
    public boolean isSyncEnabled() {
        try {
            if (createAndRegisterAccount()) {
                return ContentResolver.isSyncActive(mAccount, AUTHORITY) ||
                       ContentResolver.isSyncPending(mAccount, AUTHORITY);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "检查同步状态失败", e);
            return false;
        }
    }

    /**
     * 验证账户和同步状态
     * @return true 表示正常，false 表示异常
     */
    public boolean verifyAccountAndSync() {
        try {
            // 验证账户是否存在
            Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);
            if (accounts.length == 0) {
                Log.w(TAG, "账户不存在");
                return false;
            }

            // 验证同步是否启用
            boolean isSyncable = ContentResolver.getSyncAutomatically(mAccount, AUTHORITY);
            Log.d(TAG, "同步自动化状态：" + isSyncable);

            // 验证 master sync 是否启用
            boolean masterSync = ContentResolver.getMasterSyncAutomatically();
            Log.d(TAG, "主同步开关：" + masterSync);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "验证账户和同步状态失败", e);
            return false;
        }
    }

    /**
     * 获取当前账户
     */
    public Account getAccount() {
        return mAccount;
    }

    /**
     * 获取账户管理器
     */
    public AccountManager getAccountManager() {
        return mAccountManager;
    }
}

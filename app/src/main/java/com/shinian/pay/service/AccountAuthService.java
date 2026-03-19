package com.shinian.pay.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

/**
 * Account 认证服务
 * 用于在系统中创建和管理账户，配合 SyncAdapter 实现系统级保活
 */
public class AccountAuthService extends AbstractAccountAuthenticator {

    private static final String TAG = "AccountAuthService";

    public AccountAuthService(Context context) {
        super(context);
    }

    /**
     * 添加账户时调用
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, 
                            String authTokenType, String[] requiredFeatures, Bundle options) 
                            throws NetworkErrorException {
        // 不支持在线添加账户，返回 null
        return null;
    }

    /**
     * 获取 authToken（本应用不需要）
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, 
                              String authTokenType, Bundle options) throws NetworkErrorException {
        // 本应用不需要 authToken
        return null;
    }

    /**
     * 更新账户凭证（本应用不需要）
     */
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, 
                                   String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    /**
     * 确认 authToken（本应用不需要）
     */
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, 
                             String[] features) throws NetworkErrorException {
        return null;
    }

    /**
     * 编辑账户属性（本应用不需要）
     */
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    /**
     * 验证账户名称和密码（本应用不需要）
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, 
                                    Bundle options) throws NetworkErrorException {
        return null;
    }

    /**
     * 获取 authToken 标签（本应用不需要）
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }
}

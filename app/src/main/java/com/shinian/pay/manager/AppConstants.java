package com.shinian.pay.manager;

/**
 * 全局常量管理
 */
public class AppConstants {
    
    // 基础配置
    public static final boolean DEBUG = true;
    public static final String PACKAGE_NAME = "com.shinian.pay";
    
    // 请求码
    public static final int REQ_QR_CODE = 11002;      // 打开扫描界面请求码
    public static final int REQ_PERM_CAMERA = 11003;  // 打开摄像头
    public static final int REQ_PERM_EXTERNAL_STORAGE = 11004; // 读写文件
    
    // Intent 参数
    public static final String INTENT_EXTRA_KEY_QR_SCAN = "qr_scan_result";
    
    // SharedPreferences 名称
    public static final String SP_NAME_CONFIG = "shinian";
    public static final String SP_NAME_LOGS = "items";
    
    // SharedPreferences Key
    public static final String SP_KEY_HOST = "host";
    public static final String SP_KEY_KEY = "key";
    public static final String SP_KEY_LOGS_STR = "logsStr";
    
    // 支付类型
    public static final int PAY_TYPE_WECHAT = 1;
    public static final int PAY_TYPE_ALIPAY = 2;
    
    // 通知渠道 ID
    public static final String NOTIFICATION_CHANNEL_ID = "1";
    
    // 心跳间隔 (毫秒)
    public static final long HEARTBEAT_INTERVAL = 30 * 1000;
    
    // 回调补单延迟 (毫秒)
    public static final long CALLBACK_RETRY_DELAY = 1000;
    
    // 日志最大条数
    public static final int MAX_LOG_ENTRIES = 20;
}

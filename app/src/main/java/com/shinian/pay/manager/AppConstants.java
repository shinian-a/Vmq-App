package com.shinian.pay.manager;

/**
 * 全局常量统一管理
 * ⚠️ 项目中不应再出现其他常量类，新增常量请在此文件中按分区添加
 */
public final class AppConstants {

    private AppConstants() {} // 禁止实例化

    // ==================== 基础配置 ====================
    // ==================== 请求码 ====================
    public static final int REQ_QR_CODE = 11002;
    public static final int REQ_PERM_CAMERA = 11003;


    // ==================== Intent 参数 ====================
    public static final String INTENT_EXTRA_KEY_QR_SCAN = "qr_scan_result";

    // ==================== SharedPreferences ====================
    public static final String SP_NAME_CONFIG = "shinian";
    public static final String SP_NAME_STATE = "state_switch";
    public static final String SP_NAME_LOGS = "items";

    public static final String SP_KEY_HOST = "host";
    public static final String SP_KEY_KEY = "key";
    public static final String SP_KEY_LOGS_STR = "logsStr";
    public static final String SP_KEY_STATE_SWITCH = "state_switch";

    // ==================== 通知渠道 ====================
    public static final int REQ_PERM_NOTIFICATION = 1003;
    public static final String NOTIFICATION_CHANNEL_ID = "1";

    // ==================== 网络 & 服务器 ====================
    public static final String UPDATE_CHECK_URL = "http://w.t3yanzheng.com/A729B02347E855EC";

    // ==================== 第三方包名 & 标识 ====================
    public static final String PKG_WECHAT = "com.tencent.mm";
    public static final String PKG_ALIPAY = "com.eg.android.AlipayGphone";
    public static final String AUTHOR_QQ = "1614790395";
    public static final String AUTHOR_QQ_GROUP_URL = "https://qm.qq.com/q/ESWRuTY6uk";
    public static final String AUTHOR_WEBSITE = "https://shinian-a.github.io/";

    // ==================== 时间间隔 (毫秒) ====================

    public static final long BATTERY_UI_UPDATE_INTERVAL = 60_000L;
    public static final long FORE_SERVICE_START_DELAY = 1_000L;
    public static final long EXIT_DELAY = 800L;
    public static final long SCREEN_DIM_DELAY = 3_000L;

    // ==================== UI / 业务参数 ====================
    public static final int MAX_LOG_ENTRIES = 50;
    // ==================== 聚合 ====================
    public static final String AD_REWARD_ADPID = "1400213957";
}
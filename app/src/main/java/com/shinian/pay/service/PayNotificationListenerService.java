package com.shinian.pay.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.shinian.pay.manager.AppConstants;
import com.shinian.pay.manager.MonitorLogManager;

import com.shinian.pay.utils.TimeUtils;
import okhttp3.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "PayNotService";

    // 支付平台包名常量
    private static final String PACKAGE_WECHAT = "com.tencent.mm";
    private static final String PACKAGE_WECHAT_WORK = "com.tencent.wework";
    private static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";
    private static final String PACKAGE_SELF = "com.shinian.pay";

    // 微信支付标题关键字
    private static final String[] WECHAT_PAY_TITLES = {
            "微信支付", "微信收款助手", "微信收款商业版", "对外收款", "企业微信", "Weixin Cashier Assistant"
    };

    // 金额提取正则（预编译）
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?<![.\\d])\\d+(?:\\.\\d{1,2})?(?=\\s*(?:元|人民币))");

    // ========== 实例变量 ==========
    private volatile boolean isHeartRunning = false;       // 心跳线程退出标志
    private Thread heartThread;
    private PowerManager.WakeLock wakeLock;
    private OkHttpClient okHttpClient;
    private Handler mainHandler;
    private final ExecutorService retryExecutor = Executors.newSingleThreadExecutor(); // 重试用线程池

    // 心跳防抖（带锁保护）
    private final Object hbLock = new Object();
    private long lastHbErrElapsed = 0L;

    // ==================== 生命周期 ====================

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        initOkHttp();
        startHeartbeat();

        //MonitorLogManager.appendLog(getApplication(),TimeUtils.now() + "\r\r\r\r监听服务开启成功！");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopHeartbeat();           // 停止心跳线程
        releaseWakeLock();         // 释放电源锁
        retryExecutor.shutdownNow(); // 关闭重试线程池
        Log.d(TAG, "Service destroyed, resources released");
    }

    // ==================== 心跳管理 ====================

    private void initOkHttp() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
    }

    private synchronized void startHeartbeat() {
        if (isHeartRunning) return;
        isHeartRunning = true;

        heartThread = new Thread(() -> {
            Log.d(TAG, "心跳线程启动");
            acquireWakeLock();

            while (isHeartRunning) {
                try {
                    doHeartbeatRequest();
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "心跳异常", e);
                }
            }
            Log.d(TAG, "心跳线程已退出");
        }, "HeartbeatThread");
        heartThread.setDaemon(true);
        heartThread.start();
    }

    private synchronized void stopHeartbeat() {
        isHeartRunning = false;
        if (heartThread != null) {
            heartThread.interrupt();
            heartThread = null;
        }
    }

    private void doHeartbeatRequest() {
        SharedPreferences sp = getSharedPreferences(AppConstants.SP_NAME_CONFIG, MODE_PRIVATE);
        String host = sp.getString(AppConstants.SP_KEY_HOST, "");
        String key = sp.getString(AppConstants.SP_KEY_KEY, "");

        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(key)) return;

        String t = String.valueOf(System.currentTimeMillis());
        String sign = md5(t + key);

        Request request = new Request.Builder()
                .url("http://" + host + "/appHeart?t=" + t + "&sign=" + sign)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 线程安全的防抖：synchronized + elapsedRealtime
                synchronized (hbLock) {
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastHbErrElapsed > 120_000) {
                        lastHbErrElapsed = now;
                        String error = e.getMessage() != null ? e.getMessage() : "Unknown";
                        String logMsg = TimeUtils.now()  + "\r\r\r\r心跳状态错误，请重新配置或切换网络环境!\n错误详情：" + error;
                        MonitorLogManager.appendLog(getApplication(),TimeUtils.now() + "\r\r\r\r" + logMsg);
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseToClose = response) {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "心跳响应 HTTP " + response.code());
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "心跳响应: " + body);
                }
            }
        });
    }

    // ==================== 通知监听 ====================

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        String pkg = sbn.getPackageName();
        String title = notification.extras.getString(NotificationCompat.EXTRA_TITLE, "");
        String content = notification.extras.getString(NotificationCompat.EXTRA_TEXT, "");

        if (PACKAGE_SELF.equals(pkg) && title.equals("V免签测试推送")) {
            String msg = TimeUtils.now() + "\r\r\r\r" + "✅ 通知监听权限正常";
            Log.i(TAG, msg);
            MonitorLogManager.appendLog(getApplication(), msg);
        }

        if (PACKAGE_WECHAT.equals(pkg) || PACKAGE_WECHAT_WORK.equals(pkg)) {
            handleWechatNotification(title, content);
        } else if (PACKAGE_ALIPAY.equals(pkg)) {
            handleAlipayNotification(title, content);
        } else if (PACKAGE_SELF.equals(pkg)) {
            handleSelfTestNotification(content);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // no-op
    }

    // ==================== 微信处理 ====================

    private void handleWechatNotification(String title, String content) {
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) return;

        // 检查标题是否匹配
        boolean isPayTitle = false;
        for (String payTitle : WECHAT_PAY_TITLES) {
            if (payTitle.equals(title)) {
                isPayTitle = true;
                break;
            }
        }
        if (!isPayTitle) return;

        // 过滤非收款消息
        if (!content.contains("收款") || content.contains("已支付")) return;

        processPayment("微信", 1, content, title);
    }

    // ==================== 支付宝处理 ====================

    private void handleAlipayNotification(String title, String content) {
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) return;

        boolean isNormalPay = (title.contains("成功收款") && content.contains("已转入余额"))

                || content.contains("通过扫码向你付款");
        boolean isStaffPay = title.contains("店员通") || content.contains("支付宝成功收款");

        if (!isNormalPay && !isStaffPay) return;

        String platform = isStaffPay ? "支付宝店员" : "支付宝";
        // 优先从更可能包含金额的字段提取，失败再尝试另一个
        String primarySource = isStaffPay ? content : title;
        String fallbackSource = isStaffPay ? title : content;

        String money = getMoney(primarySource);
        if (money == null) {
            money = getMoney(fallbackSource);
        }

        if (money != null) {
            try {
                double amount = Double.parseDouble(money);
                showToast("匹配成功：" + platform + "到账" + money + "元");
                Log.d(TAG, "匹配成功：" + platform + "到账 " + money + "元");
                appPush(2, amount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "解析" + platform + "金额失败：" + money, e);
                showMoneyParseErrorToast(platform);
            }
        } else {
            showMoneyParseErrorToast(platform);
        }
    }

    // ==================== 通用支付处理 ====================

    /**
     * 统一的支付处理入口（消除微信/支付宝重复代码）
     */
    private void processPayment(String platform, int type, String content, String title) {
        String money = getMoney(content);
        if (money == null) {
            money = getMoney(title); // 真正的fallback：换字段重试
        }

        if (money != null) {
            try {
                double amount = Double.parseDouble(money);
                showToast("匹配成功：" + platform + "到账" + money + "元");
                Log.d(TAG, "匹配成功：" + platform + "到账 " + money + "元");
                appPush(type, amount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "解析" + platform + "金额失败：" + money, e);
                showMoneyParseErrorToast(platform);
            }
        } else {
            showMoneyParseErrorToast(platform);
        }
    }

    private void handleSelfTestNotification(String content) {
        if ("测试推送通知，如果程序正常，则会提示监听权限正常".equals(content)) {
            MonitorLogManager.appendLog(getApplication(),TimeUtils.now()+ "\r\r\r\r测试收款监听权限正常！");
        }
    }

    // ==================== 金额提取 ====================

    public static String getMoney(String content) {
        if (content == null || content.isEmpty()) return null;

        int idx = content.indexOf("收款");
        if (idx < 0) return null;

        String afterShoukuan = content.substring(idx);
        Matcher matcher = MONEY_PATTERN.matcher(afterShoukuan);

        while (matcher.find()) {
            String matched = matcher.group();
            if (isValidNumber(matched)) {
                try {
                    double amount = Double.parseDouble(matched);
                    if (amount >= 0.01 && amount <= 999999.99) {
                        return matched; // 找到第一个有效金额立即返回
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private static boolean isValidNumber(String str) {
        if (str == null || str.isEmpty()) return false;
        int dotCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '.') {
                dotCount++;
                if (dotCount > 1 || i == 0 || i == str.length() - 1) return false;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    // ==================== 推送回调 ====================

    public void appPush(final int type, final double price) {
        SharedPreferences sp = getSharedPreferences(AppConstants.SP_NAME_CONFIG, MODE_PRIVATE);
        String host = sp.getString(AppConstants.SP_KEY_HOST, "");
        String key = sp.getString(AppConstants.SP_KEY_KEY, "");

        String priceStr = String.format(Locale.US, "%.2f", price); // 指定Locale避免逗号
        String t = String.valueOf(System.currentTimeMillis());
        String sign = md5(type + priceStr + t + key);
        String url = "http://" + host + "/appPush?t=" + t + "&type=" + type
                + "&price=" + priceStr + "&sign=" + sign;

        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String error = e.getMessage() != null ? e.getMessage() : "Unknown";
                Log.e(TAG, "appPush失败: " + error);
                MonitorLogManager.appendLog(getApplication(),TimeUtils.now()+ "\r\r\r\r通知回调失败：" + error);
                scheduleRetry(type, price, priceStr, host, key);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseToClose = response) {
                    if (!response.isSuccessful()) {
                        String error = "HTTP " + response.code();
                        Log.e(TAG, "appPush失败: " + error);
                        MonitorLogManager.appendLog(getApplication(),
                                TimeUtils.now()+ "\r\r\r\r通知回调失败：" + error);
                        scheduleRetry(type, price, priceStr, host, key);
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "{}";
                    JSONObject result = new JSONObject(body);
                    String msg = result.optString("msg", "");

                    logPushResult(type, priceStr, msg, body, false);

                    if ("成功".equals(msg)) {
                        cancelPayNotification(type);
                    }
                } catch (JSONException e) {
                    String error = e.getMessage() != null ? e.getMessage() : "JSON parse error";
                    Log.e(TAG, "appPush JSON解析失败: " + error);
                    MonitorLogManager.appendLog(getApplication(),TimeUtils.now()+ "\r\r\r\r通知回调失败：" + error);
                    scheduleRetry(type, price, priceStr, host, key);
                }
            }
        });
    }

    /**
     * 重试移到独立线程池，不再占用主线程Handler
     */
    private void scheduleRetry(int type, double price, String priceStr, String host, String key) {
        retryExecutor.execute(() -> {
            try {
                Thread.sleep(1000);
                String t = String.valueOf(System.currentTimeMillis());
                String sign = md5(type + priceStr + t + key);
                String url = "http://" + host + "/appPush?t=" + t + "&type=" + type
                        + "&price=" + priceStr + "&sign=" + sign;

                String data = getHtml(url);
                JSONObject json = new JSONObject(data);
                String message = json.optString("msg", "");

                if (json.optInt("code") == 1 && "成功".equals(message)) {
                    logPushResult(type, priceStr, message, data, true);
                    showToast("补通知回调成功：" + data);
                } else {
                    Log.w(TAG, "补回调失败 - code:" + json.optInt("code") + ", msg:" + message);
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Unknown";
                Log.e(TAG, "补回调异常: " + error);
                showToast("自动补单回调失败！联系作者反馈\n错误详情：" + error);
            }
        });
    }

    // ==================== 辅助方法 ====================

    private void cancelPayNotification(int type) {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            String targetPkg = (type == 1) ? PACKAGE_WECHAT : PACKAGE_ALIPAY;
            for (StatusBarNotification sbn : active) {
                if (sbn != null && targetPkg.equals(sbn.getPackageName())) {
                    cancelNotification(sbn.getKey());
                    Log.d(TAG, "已清除" + targetPkg + "通知");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "清除通知失败", e);
        }
    }

    private void logPushResult(int type, String priceStr, String msg, String data, boolean isRetry) {
        String prefix = isRetry ? "自动补回调：" : "";
        String payType = (type == 1) ? "微信支付" : "支付宝";
        String logContent = TimeUtils.now()  + "\r\r\r\r" + prefix
                + "监听到" + payType + "收款" + priceStr + "元"
                + "\t通知回调状态：" + msg
                + "\n通知回调信息：" + data;
        MonitorLogManager.appendLog(this,TimeUtils.now() + logContent);
    }

    private void showMoneyParseErrorToast(String platform) {
        showToast("监听到" + platform + "收款消息但未匹配到金额！");
    }

    private void showToast(String msg) {
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() ->
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }


    // ==================== MD5 ====================

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(string.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2); // StringBuilder替代+=
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5算法不可用", e);
            return "";
        }
    }

    // ==================== WakeLock ====================

    @SuppressLint("WakelockTimeout")
    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                // 添加超时自动释放，防止永久持锁
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PayNotService:Heartbeat");
                wakeLock.acquire(70 * 60 * 1000L); // 70分钟超时（略大于心跳周期）
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    // ==================== 日志 & 网络工具 ====================

    public String getHtml(String path) throws IOException {
        Request request = new Request.Builder().url(path).get().build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}
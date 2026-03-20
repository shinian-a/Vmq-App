package com.shinian.pay.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.*;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.shinian.pay.ui.MainActivity;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class PayNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "PayNotService";
    private String host = "";
    private String key = "";
    private Thread newThread = null;
    private PowerManager.WakeLock mWakeLock = null;
    private OkHttpClient okHttpClient; // 复用 OkHttpClient 实例
    private Handler mainHandler; // 复用主线程 Handler

    //释放设备电源锁
    public void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    //心跳进程
    public void initAppHeart() {
        Log.d(TAG, "开始启动心跳线程");
        if (newThread != null) {
            return;
        }
        acquireWakeLock(this);

        // 初始化复用的 OkHttpClient
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }

        newThread = new Thread(() -> {
            Log.d(TAG, "心跳线程启动！");
            while (true) {
                SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
                host = read.getString("host", "");
                key = read.getString("key", "");

                String t = String.valueOf(new Date().getTime());
                String sign = md5(t + key);
                Request request = new Request.Builder()
                        .url("http://" + host + "/appHeart?t=" + t + "&sign=" + sign)
                        .method("GET", null)
                        .build();

                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        final String error = e.getMessage();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                if (MainActivity.LogsTextView != null && !MainActivity.LogsTextView.getText().toString().contains("心跳状态错误")) {
                                    //发送监听日志
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
                                                + "\r\r\r\r" + "心跳状态错误，请重新配置或切换网络环境!\n错误详情：" + error);
                                    }
                                    Toast.makeText(getApplicationContext(), "心跳状态错误，请重新配置或切换网络!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "onResponse heard: " + response.body().string());
                    }
                });

                try {
                    Thread.sleep(50 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        newThread.start();
    }

    //申请设备电源锁
    @SuppressLint("InvalidWakeLockTag")
    public void acquireWakeLock(Context context) {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLock");
            if (null != mWakeLock) {
                mWakeLock.acquire();
            }
        }
    }


    // 支付平台包名常量
    private static final String PACKAGE_WECHAT = "com.tencent.mm";
    private static final String PACKAGE_WECHAT_WORK = "com.tencent.wework";
    private static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";
    private static final String PACKAGE_SELF = "com.shinian.pay";
    
    // 微信支付标题关键字
    private static final String[] WECHAT_PAY_TITLES = {
        "微信支付", "微信收款助手", "微信收款商业版", "对外收款", "企业微信","Weixin Cashier Assistant"
    };
    
    // 金额提取正则表达式（预编译提升性能）
    private static final java.util.regex.Pattern MONEY_PATTERN = 
            java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)?");

    /**
     * 当收到一条消息的时候回调，sbn 即收到的消息
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // 获取配置（只在需要时读取）
        SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");
        
        // 获取通知对象和包名（只获取一次）
        Notification notification = sbn.getNotification();
        String pkg = sbn.getPackageName();
        
        if (notification == null) {
            return;
        }
        
        Bundle extras = notification.extras;
        if (extras == null) {
            return;
        }
        
        String title = extras.getString(NotificationCompat.EXTRA_TITLE, "");
        String content = extras.getString(NotificationCompat.EXTRA_TEXT, "");
        Log.d(TAG, "包名: " + pkg);
        // 根据包名分发处理逻辑
        if (PACKAGE_WECHAT.equals(pkg) || PACKAGE_WECHAT_WORK.equals(pkg)) {
            handleWechatNotification(title, content);
        } else if (PACKAGE_ALIPAY.equals(pkg)) {
            handleAlipayNotification(title, content);
        } else if (PACKAGE_SELF.equals(pkg)) {
            handleSelfTestNotification(content);
        }
    }
    
    /**
     * 处理微信收款通知
     */
    private void handleWechatNotification(String title, String content) {
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
            return;
        }
        
        // 检查是否为支付相关标题
        boolean isPayTitle = false;
        for (String payTitle : WECHAT_PAY_TITLES) {
            if (payTitle.equals(title)) {
                isPayTitle = true;
                break;
            }
        }
        
        if (!isPayTitle) {
            return;
        }
        
        // 只处理收款消息，忽略支付成功消息
        if (!content.contains("收款") || content.contains("已支付")) {
            return;
        }
        
        // 尝试获取金额（重试一次避免掉单）
        String money = getMoney(content);
        if (money == null || money.isEmpty()) {
            money = getMoney(content);
        }
        
        if (money != null && !money.isEmpty()) {
            try {
                double amount = Double.parseDouble(money);
                Toast.makeText(this, "匹配成功：微信到账" + money + "元", Toast.LENGTH_LONG).show();
                Log.d(TAG, "onAccessibilityEvent: 匹配成功：微信到账 " + money + "元");
                appPush(1, amount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "解析微信金额失败：" + money, e);
                showMoneyParseErrorToast("微信");
            }
        } else {
            showMoneyParseErrorToast("微信");
        }
    }
    
    /**
     * 处理支付宝收款通知
     */
    private void handleAlipayNotification(String title, String content) {
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
            return;
        }
        
        String money = null;
        String platform = "";
        int platformType = 2;
        
        // 判断是普通收款还是店员通收款（添加括号明确优先级）
        boolean isNormalPay = (title.contains("成功收款") && content.contains("已转入余额")) 
                             || content.contains("通过扫码向你付款");
        boolean isStaffPay = title.contains("店员通") || content.contains("支付宝成功收款");
        
        if (isNormalPay) {
            // 普通收款：优先从标题获取金额
            platform = "支付宝";
            money = getMoney(title);
            if (money == null || money.isEmpty()) {
                money = getMoney(content);
            }
        } else if (isStaffPay) {
            // 店员通收款：优先从内容获取金额
            platform = "支付宝店员";
            platformType = 2; // 保持和普通支付宝一致
            money = getMoney(content);
            if (money == null || money.isEmpty()) {
                money = getMoney(title);
            }
        } else {
            return; // 不匹配的收款类型，直接返回
        }
        
        // 重试一次避免掉单
        if (money == null || money.isEmpty()) {
            if (isNormalPay) {
                money = getMoney(content);
            } else {
                money = getMoney(title);
            }
        }
        
        if (money != null && !money.isEmpty()) {
            try {
                double amount = Double.parseDouble(money);
                Toast.makeText(this, "匹配成功：" + platform + "到账" + money + "元", Toast.LENGTH_LONG).show();
                Log.d(TAG, "onAccessibilityEvent: 匹配成功：" + platform + "到账 " + money + "元");
                appPush(platformType, amount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "解析" + platform + "金额失败：" + money, e);
                showMoneyParseErrorToast(platform);
            }
        } else {
            showMoneyParseErrorToast(platform);
        }
    }
    
    /**
     * 处理自检测试通知
     */
    private void handleSelfTestNotification(String content) {
        if ("测试推送通知，如果程序正常，则会提示监听权限正常".equals(content)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
                               + "\r\r\r\r" + "测试收款监听权限正常！");
            }
        }
    }
    
    /**
     * 显示金额解析错误提示（避免频繁弹出 Toast）
     */
    private void showMoneyParseErrorToast(String platform) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        
        mainHandler.post(() -> 
            Toast.makeText(getApplicationContext(), 
                "监听到" + platform + "收款消息但未匹配到金额！", 
                Toast.LENGTH_SHORT).show()
        );
    }

    //当移除一条消息的时候回调，sbn是被移除的消息
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    @Override
    public void onListenerConnected() {
        // 初始化主线程 Handler（只初始化一次）
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        
        //开启心跳线程
        initAppHeart();
        
        //延迟发送监听日志
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
                                   + "\r\r\r\r" + "监听服务开启成功！");
                }
            }
        }, 1000);
    }

    /**
     * 推送收款通知到服务器
     * @param type 支付类型：1-微信，2-支付宝
     * @param price 收款金额
     */
    public void appPush(final int type, final double price) {
        SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");
    
        // 格式化价格，避免精度问题（例如：0.1 变成 0.10000000000000000555）
        String priceStr = String.format("%.2f", price);
            
        Log.d(TAG, "appPush: 开始 - 类型:" + type + ", 金额:" + priceStr);
    
        // 构建请求 URL
        String t = String.valueOf(new Date().getTime());
        String sign = md5(type + priceStr + t + key);
        String url = buildPushUrl(host, type, priceStr, t, sign);
            
        Log.d(TAG, "appPush: URL:" + url);
    
        // 使用复用的 OkHttpClient 实例
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
    
        Request request = new Request.Builder().url(url).method("GET", null).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String error = e.getMessage();
                Log.e(TAG, "appPush: 请求失败 - " + error);
                    
                // 发送失败日志
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
                            + "\r\r\r\r" + "通知回调失败：" + error);
                }
                    
                // 请求失败后延迟 1 秒自动补回调
                scheduleRetryCallback(type, price, priceStr);
            }
    
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String str = response.body().string();
                    JSONObject result = new JSONObject(str);
                    String msg = result.getString("msg");
                        
                    // 根据支付类型记录日志
                    logPushResult(type, priceStr, msg, str, false);
                        
                    // 通知回调成功后，清除对应的通知消息（防止通知栏堆积）
                    if ("成功".equals(msg)) {
                        cancelNotification(type);
                    }
                        
                } catch (JSONException e) {
                    String error = e.getMessage();
                    Log.e(TAG, "appPush: JSON解析失败 - " + error);
                        
                    // 发送失败日志
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
                                + "\r\r\r\r" + "通知回调失败：" + error);
                    }
                        
                    // JSON解析失败后延迟 1 秒自动补回调
                    scheduleRetryCallback(type, price, priceStr);
                }
            }
        });

    }

    /**
     * 清除支付通知（防止通知栏堆积）
     * @param type 支付类型：1-微信，2-支付宝
     */
    private void cancelNotification(int type) {
        try {
            // 获取所有活跃的通知
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            if (activeNotifications == null || activeNotifications.length == 0) {
                return;
            }

            // 根据支付类型确定要清除的通知包名
            String targetPackage = (type == 1) ? PACKAGE_WECHAT : PACKAGE_ALIPAY;

            // 遍历并清除指定包名的通知
            for (StatusBarNotification sbn : activeNotifications) {
                if (sbn != null && targetPackage.equals(sbn.getPackageName())) {
                    // 取消该通知
                    cancelNotification(sbn.getKey());
                    Log.d(TAG, "cancelNotification: 已清除" + (type == 1 ? "微信" : "支付宝") +
                            "通知 - " + sbn.getPackageName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "cancelNotification: 清除通知失败 - " + e.getMessage());
        }
    }

    /**
     * 构建推送 URL（消除重复代码）
     */
    private String buildPushUrl(String host, int type, String priceStr, String t, String sign) {
        return "http://" +
                host +
                "/appPush?t=" +
                t +
                "&type=" +
                type +
                "&price=" +
                priceStr +
                "&sign=" +
                sign;
    }
        
    /**
     * 记录推送结果日志（消除重复代码）
     */
    private void logPushResult(int type, String priceStr, String msg, String data, boolean isRetry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String prefix = isRetry ? "自动补回调：" : "";
            String payType = (type == 1) ? "微信支付" : "支付宝";
            String logContent = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                    + "\r\r\r\r"
                    + prefix + "监听到" + payType + "收款" + priceStr + "元"
                    + "\t" + "通知回调状态：" + msg
                    + "\n" + "通知回调信息：" + data;
            sendMonitorLogs(logContent);
        }
    }
        
    /**
     * 延迟重试回调（消除重复代码）
     */
    private void scheduleRetryCallback(final int type, final double price, final String priceStr) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String t = String.valueOf(new Date().getTime());
                    String sign = md5(type + priceStr + t + key);
                    String url = buildPushUrl(host, type, priceStr, t, sign);
                        
                    // 同步请求（在子线程中执行）
                    String data = getHtml(url);
                        
                    // 解析响应
                    JSONObject jsonObject = new JSONObject(data);
                    int code = jsonObject.getInt("code");
                    String message = jsonObject.getString("msg");
                        
                    if (code == 1 && "成功".equals(message)) {
                        // 记录补回调日志
                        logPushResult(type, priceStr, message, data, true);
                            
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), 
                                        "补通知回调成功：" + data, 
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Log.w(TAG, "appPush: 补回调失败 - code:" + code + ", msg:" + message);
                    }
                } catch (Exception e) {
                    String error = e.getMessage();
                    Log.e(TAG, "appPush: 补回调异常 - " + error);
                        
                    final String finalError = error;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplication(), 
                                    "自动补单回调失败！联系作者反馈\n错误详情：" + finalError, 
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }, 1000); // 延迟 1 秒重试
    }
    /**
     * 从文本内容中提取金额数字（使用预编译的正则表达式）
     * 例如："收款 10.50 元" → "10.50"
     * 
     * @param content 包含金额的文本内容
     * @return 提取到的金额字符串，如果未找到则返回 null
     */
    public static String getMoney(String content) {
        // 空值检查
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // 使用预编译的正则表达式匹配所有数字和小数点组合
        java.util.regex.Matcher matcher = MONEY_PATTERN.matcher(content);
        
        // 收集所有匹配到的数字
        List<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            String matched = matcher.group();
            // 验证是否为合法的数字格式（避免 "1.2.3" 这样的情况）
            if (isValidNumber(matched)) {
                numbers.add(matched);
            }
        }
        
        // 如果没有找到任何合法数字，返回 null
        if (numbers.isEmpty()) {
            return null;
        }
        
        // 返回最后一个匹配到的数字（通常是金额）
        return numbers.get(numbers.size() - 1);
    }
    
    /**
     * 验证字符串是否为合法的数字格式
     * @param str 待验证的字符串
     * @return true 表示合法，false 表示不合法
     */
    private static boolean isValidNumber(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // 不能以小数点开头或结尾，且只能包含一个小数点
        int dotCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '.') {
                dotCount++;
                // 不能有超过一个小数点，或者小数点在开头/结尾
                if (dotCount > 1 || i == 0 || i == str.length() - 1) {
                    return false;
                }
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    // 监听日志
    private void sendMonitorLogs(String msgStr) {
        SharedPreferences read = getSharedPreferences("items", MODE_PRIVATE);
        String logsStr = read.getString("logsStr", "");
            
        // 计算当前日志行数，只保留最近 20 条
        int lineCount = 0;
        int lastIndex = -1;
        for (int i = 0; i < logsStr.length(); i++) {
            if (logsStr.charAt(i) == '\n') {
                lineCount++;
                if (lineCount >= 20) {
                    lastIndex = i;
                    break;
                }
            }
        }
            
        // 如果超过 20 行，截取最后一部分
        if (lastIndex >= 0 && lastIndex < logsStr.length() - 1) {
            logsStr = logsStr.substring(lastIndex + 1);
        }
            
        // 追加新日志
        logsStr = msgStr + "\n" + logsStr;
            
        SharedPreferences.Editor editor = getSharedPreferences("items", MODE_PRIVATE).edit();
        editor.putString("logsStr", logsStr);
        editor.commit();
    
        //创建消息对象并发送
        Message msg = new Message();
        msg.what = 0;
        Bundle bundle = new Bundle();
        bundle.putString("logsStr", logsStr);
        msg.setData(bundle);
        MainActivity.monitorLogHandler.sendMessage(msg);
    }

    // 获取 HTML（确保资源正确关闭）
    public String getHtml(String path) throws Exception {
        HttpURLConnection conn = null;
        InputStream inStream = null;
        try {
            URL url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8 * 1000);
                
            //通过输入流获取 html 数据
            inStream = conn.getInputStream();
            //获取 html 的二进制数组
            byte[] data = readInputStream(inStream);
            //获取指定字符集解码指定的字节数组构造一个新的字符串
            return new String(data, StandardCharsets.UTF_8);
        } finally {
            // 关闭输入流
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭输入流失败", e);
                }
            }
            // 断开连接
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // 读取输入流 获取 HTML 二进制数组（调用后需关闭输入流）
    public byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        return outStream.toByteArray();
    }


}

package com.shinian.pay.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.shinian.pay.callback.PaymentCallback;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 支付通知管理器
 * 负责处理支付通知的监听、回调和心跳
 */
public class PaymentManager {
    
    private static final String TAG = "PaymentManager";
    private static PaymentManager instance;
    
    private Context context;
    private OkHttpClient okHttpClient;
    private Handler mainHandler;
    private PaymentCallback callback;
    
    // 配置信息
    private String host;
    private String key;
    
    private PaymentManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        loadConfig();
    }
    
    public static synchronized PaymentManager getInstance(Context context) {
        if (instance == null) {
            instance = new PaymentManager(context);
        }
        return instance;
    }
    
    /**
     * 加载配置信息
     */
    private void loadConfig() {
        SharedPreferences read = context.getSharedPreferences(AppConstants.SP_NAME_CONFIG, Context.MODE_PRIVATE);
        this.host = read.getString(AppConstants.SP_KEY_HOST, "");
        this.key = read.getString(AppConstants.SP_KEY_KEY, "");
    }
    
    /**
     * 设置回调监听
     */
    public void setCallback(PaymentCallback callback) {
        this.callback = callback;
        loadConfig(); // 重新加载配置
    }
    
    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        if (host.isEmpty()) {
            Log.w(TAG, "Host is empty, skip heartbeat");
            return;
        }
        
        String t = String.valueOf(new Date().getTime());
        String sign = md5(t + key);
        String url = "http://" + host + "/appHeart?t=" + t + "&sign=" + sign;
        
        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .build();
        
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final String error = e.getMessage();
                Log.e(TAG, "Heartbeat failed: " + error);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onHeartbeatResponse(false, error);
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Heartbeat response: " + responseBody);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onHeartbeatResponse(true, responseBody);
                    }
                });
            }
        });
    }
    
    /**
     * 发送支付回调
     * @param type 支付类型 1=微信 2=支付宝
     * @param amount 金额
     */
    public void sendPaymentCallback(int type, double amount) {
        if (host.isEmpty()) {
            Log.w(TAG, "Host is empty, skip payment callback");
            return;
        }
        
        String t = String.valueOf(new Date().getTime());
        String sign = md5(type + "" + amount + t + key);
        String url = "http://" + host + "/appPush?t=" + t + "&type=" + type + "&price=" + amount + "&sign=" + sign;
        
        Log.d(TAG, "Sending payment callback: type=" + type + ", amount=" + amount + ", url=" + url);
        
        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .build();
        
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String error = e.getMessage();
                Log.e(TAG, "Payment callback failed: " + error);
                
                // 失败后重试
                mainHandler.postDelayed(() -> retryPaymentCallback(type, amount), AppConstants.CALLBACK_RETRY_DELAY);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Payment callback response: " + responseBody);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onPaymentReceived(type, amount);
                    }
                    
                    Toast.makeText(context, "通知回调成功：" + responseBody, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * 重试支付回调
     */
    private void retryPaymentCallback(int type, double amount) {
        if (host.isEmpty()) {
            return;
        }
        
        String t = String.valueOf(new Date().getTime());
        String sign = md5(type + "" + amount + t + key);
        String url = "http://" + host + "/appPush?t=" + t + "&type=" + type + "&price=" + amount + "&sign=" + sign;
        
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .method("GET", null)
                    .build();
            
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();
            
            Log.d(TAG, "Retry payment callback response: " + responseBody);
            
            mainHandler.post(() -> 
                Toast.makeText(context, "补通知回调成功：" + responseBody, Toast.LENGTH_LONG).show()
            );
        } catch (IOException e) {
            Log.e(TAG, "Retry payment callback failed: " + e.getMessage());
            mainHandler.post(() -> 
                Toast.makeText(context, "自动补单回调失败！" + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }
    }
    
    /**
     * 添加日志
     */
    public void addLog(String log) {
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.SP_NAME_LOGS, Context.MODE_PRIVATE);
        String logsStr = prefs.getString(AppConstants.SP_KEY_LOGS_STR, "");
        String[] logsStrs = logsStr.split("\n");
        
        // 限制日志条数
        if (logsStrs.length > AppConstants.MAX_LOG_ENTRIES) {
            int lastNewlineIndex = getNthNewlineIndex(logsStr, AppConstants.MAX_LOG_ENTRIES);
            logsStr = logsStr.substring(0, lastNewlineIndex);
        }
        
        // 添加新日志
        SharedPreferences.Editor editor = prefs.edit();
        logsStr = log + "\n" + logsStr;
        editor.putString(AppConstants.SP_KEY_LOGS_STR, logsStr);
        editor.apply();
        
        // 通知回调更新日志
        if (callback != null) {
            callback.onLogUpdated(logsStr);
        }
    }
    
    /**
     * 获取第 N 个换行符的位置
     */
    private int getNthNewlineIndex(String str, int n) {
        int index = -1;
        for (int i = 0; i < n; i++) {
            index = str.indexOf("\n", index + 1);
            if (index == -1) break;
        }
        return index == -1 ? str.length() : index;
    }
    
    /**
     * MD5 加密
     */
    public static String md5(String string) {
        if (android.text.TextUtils.isEmpty(string)) {
            return "";
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * 从文本中提取金额
     */
    public static String extractAmount(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        java.util.List<String> numbers = new java.util.ArrayList<>();
        for (String sss : content.replaceAll("[^0-9.]", ",").split(",")) {
            if (!sss.isEmpty()) {
                numbers.add(sss);
            }
        }
        
        if (numbers.isEmpty()) {
            return null;
        }
        
        return numbers.get(numbers.size() - 1);
    }
    
    /**
     * 格式化时间戳
     */
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}

package com.shinian.pay;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.*;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.shinian.pay.MainActivity;


public class PayNotificationListenerService extends NotificationListenerService {

    private String TAG = "NotificationListenerService";
    private String host = "";
    private String key = "";
    private Thread newThread = null;
    private PowerManager.WakeLock mWakeLock = null;
    private OkHttpClient okHttpClient; // 复用 OkHttpClient 实例

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
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {

                                if (MainActivity.LogsTextView != null && !MainActivity.LogsTextView.getText().toString().contains("心跳状态错误")) {
                                    //发送监听日志
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "心跳状态错误，请重新配置或切换网络环境!\n错误详情：" + error);
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


    /**
     * 当收到一条消息的时候回调，sbn即收到的消息
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");
        //获取通知对象
        Notification notification = sbn.getNotification();
        //获取对象包名
        String pkg = sbn.getPackageName();
        pkg = sbn.getPackageName();
        if (notification != null) {
            Bundle extras = notification.extras;
            if (extras != null) {
                String title = extras.getString(NotificationCompat.EXTRA_TITLE, "");
                String content = extras.getString(NotificationCompat.EXTRA_TEXT, "");
                /*
                 Log.d(TAG, "**********************");
                 Log.d(TAG, "包名:" + pkg);
                 Log.d(TAG, "标题:" + title);
                 Log.d(TAG, "内容:" + content);
                 Log.d(TAG, "**********************");
                 */
                if (pkg.equals("com.tencent.mm") || pkg.equals("com.tencent.wework")) {
                    if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(content)) {
                        if (title.equals("微信支付") || title.equals("微信收款助手") || title.equals("微信收款商业版") || title.equals("对外收款") || title.equals("企业微信")) {
                            if (content.contains("收款") && !content.contains("已支付")) {
                                String money = getMoney(content);
                                //加一层如果获取金额失败再次获取避免掉单
                                if (money == null || money.equals("")) {
                                    money = getMoney(content);
                                }
                                if (money != null || !money.equals("")) {
                                    Toast.makeText(this, "匹配成功： 微信到账" + money + "元", Toast.LENGTH_LONG).show();
                                    Log.d(TAG, "onAccessibilityEvent: 匹配成功： 微信到账 " + money + "元");
                                    appPush(1, Double.valueOf(money));
                                    //appPush2(1, Double.valueOf(money));//再次发送回调数据
                                } else {
                                    Handler handlerThree = new Handler(Looper.getMainLooper());
                                    handlerThree.post(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "监听到微信收款消息但未匹配到金额！", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                //这里处理非付款的逻辑
                            }
                        }
                    }
                } else if (pkg.equals("com.eg.android.AlipayGphone")) {
                    if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(content)) {
                        //通知消息匹配 contains 方法匹配包含关键字
                        if (title.contains("成功收款") && content.contains("已转入余额") || content.contains("通过扫码向你付款")) {
                            //获取标题具体收款金额
                            String money = getMoney(title);
                            //如果获取标题金额失败再获取一次内容金额
                            if (money == null || money.equals("")) {
                                money = getMoney(content);
                            }
                            if (money != null || !money.equals("")) {
                                Toast.makeText(this, "匹配成功： 支付宝到账" + money + "元", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "onAccessibilityEvent: 匹配成功： 支付宝到账 " + money + "元");
                                appPush(2, Double.valueOf(money));
                                //appPush2(2, Double.valueOf(money));//再次发送回调数据
                            } else {
                                Handler handlerThree = new Handler(Looper.getMainLooper());
                                handlerThree.post(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "监听到支付宝收款消息但未匹配到金额！", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }else if (title.contains("店员通") || content.contains("支付宝成功收款")) {
                            String money = getMoney(content);
                            if (money == null || !money.equals("")) {
                                money = getMoney(title);
                            }
                            if (money != null || !money.equals("")) {
                                Toast.makeText(this, "匹配成功： 支付宝店员到账" + money + "元", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "onAccessibilityEvent: 匹配成功： 支付宝店员到账 " + money + "元");
                                appPush(2, Double.valueOf(money));
                                //appPush2(2, Double.valueOf(money));//再次发送回调数据
                            } else {
                                Handler handlerThree = new Handler(Looper.getMainLooper());
                                handlerThree.post(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "监听到支付宝店员收款消息但未匹配到金额！", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                } else if (pkg.equals("com.shinian.pay")) {
                    if ("测试推送通知，如果程序正常，则会提示监听权限正常".equals(content)) {
                        //发送监听日志
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "测试收款监听权限正常！");
                        }
//                        Handler handlerThree = new Handler(Looper.getMainLooper());
//                        handlerThree.post(new Runnable() {
//                            public void run() {
//                                //Toast.makeText(getApplicationContext(), "监听权限正常!", Toast.LENGTH_SHORT).show();
//                            }
//                        });
                    }
                }
            }
        }
    }

    //当移除一条消息的时候回调，sbn是被移除的消息
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    //当连接成功时调用，一般在开启监听后会回调一次该方法
    @Override
    public void onListenerConnected() {
        //开启心跳线程
        initAppHeart();
        //发送监听日志
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "监听服务开启成功！");
                }
            }
        }, 1000);
        Handler handlerThree = new Handler(Looper.getMainLooper());
        handlerThree.post(new Runnable() {
            public void run() {
                //Toast.makeText(getApplicationContext(), "监听服务开启成功!", Toast.LENGTH_SHORT).show();
            }
        });


    }

    //通知回调过程
    public void appPush(final int type, final double price) {
        SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");

        Log.d(TAG, "onResponse  push: 开始:" + type + "  " + price);

        String t = String.valueOf(new Date().getTime());
        String sign = md5(type + "" + price + t + key);
        String url = "http://" + host + "/appPush?t=" + t + "&type=" + type + "&price=" + price + "&sign=" + sign;
        Log.d(TAG, "onResponse  push: 开始:" + url);

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).method("GET", null).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                String error = e.getMessage();
                //发送失败后监听日志
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "失败时-通知回调失败：" + error);
                }
                Log.d(TAG, "push: 请求失败");

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Looper.prepare();
                try {
                    String str = response.body().string();
                    JSONObject result = new JSONObject(str);
                    String msg = result.getString("msg");
                    //String[] pay_type = new String[2];
                    if (type == 1) {
                        //发送监听日志
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "监听到微信支付收款" + price + "元" + "\t" + "通知回调状态：" + msg + "\n" + "通知回调信息：" + str);
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "监听到支付宝收款" + price + "元" + "\t" + "通知回调状态：" + msg + "\n" + "通知回调信息：" + str);
                        }
                    }
                    //Toast.makeText(getApplicationContext(), "通知回调成功：" + str, Toast.LENGTH_LONG).show();
                } catch (JSONException e) {//创建JSONobject需抛JSON异常
                    String error = e.getMessage();
                    //发送失败监听日志
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "通知回调失败：" + error);
                    }
                    //Toast.makeText(getApplicationContext(), "通知回调失败！ " + error, Toast.LENGTH_LONG).show();
                    //如果通知回调失败则1秒后执行补回调
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                                    /*
                                     //Thread state = new Thread(new Runnable(){

                                     /* @Override
                                     public void run(){*/
                            try {
                                String t = String.valueOf(new Date().getTime());
                                String sign = md5(type + "" + price + t + key);
                                String url = "http://" + host + "/appPush?t=" + t + "&type=" + type + "&price=" + price + "&sign=" + sign;
                                String data = getHtml(url);//请求
                                //创建JSON解析对象
                                JSONObject jsonObject = new JSONObject(data);
                                //获取状态码
                                int code = jsonObject.getInt("code");
                                //获取msg所有内容
                                String Message = jsonObject.getString("msg");
                                if (code == 1 && Message.equals("成功")) {
                                    if (type == 1) {
                                        //发送监听日志
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "自动补回调：" + "监听到微信支付收款" + price + "元" + "\t" + "通知回调状态：" + Message + "\n" + "通知回调信息：" + data);
                                        }
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "自动补回调：" + "监听到支付宝收款" + price + "元" + "\t" + "通知回调状态：" + Message + "\n" + "通知回调信息：" + data);
                                        }
                                    }
                                    Toast.makeText(getApplicationContext(), "补通知回调成功：" + data, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                String error = e.getMessage();
                                Toast.makeText(getApplication(), "自动补单回调失败！联系作者反馈" + "\n错误详情：" + error, Toast.LENGTH_LONG).show();
                            }
                                    /*}
                                     });state.start();*/
                        }
                    }, 1000);
                }
                Looper.loop();
            }
        });
    }


    //通知服务器回调
    public void appPush2(int type, double price) {
        SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");

        Log.d(TAG, "onResponse  push: 开始:" + type + "  " + price);

        String t = String.valueOf(new Date().getTime());
        String sign = md5(type + "" + price + t + key);
        String url = "http://" + host + "/appPush?t=" + t + "&type=" + type + "&price=" + price + "&sign=" + sign;
        Log.d(TAG, "onResponse  push: 开始:" + url);

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).method("GET", null).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onResponse  push: 请求失败");

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d(TAG, "onResponse  push: " + response.body().string());

            }
        });
    }


    public static String getMoney(String content) {

        List<String> ss = new ArrayList<String>();
        for (String sss : content.replaceAll("[^0-9.]", ",").split(",")) {
            if (sss.length() > 0)
                ss.add(sss);
        }
        if (ss.size() < 1) {
            return null;
        } else {
            return ss.get(ss.size() - 1);
        }

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

    //监听日志
    private void sendMonitorLogs(String msgStr) {
        /**
         * 先读取logs日志存储数据
         */
        SharedPreferences read = getSharedPreferences("items", MODE_PRIVATE);
        String logsStr = read.getString("logsStr", "");
        String[] logsStrs = logsStr.split("\n");
        if (logsStrs.length > 20) {
            logsStr = logsStr.substring(0, logsStr.lastIndexOf("\n"));
            logsStr = logsStr.substring(0, logsStr.lastIndexOf("\n"));
        }
        /**
         * 存储起来，在监听界面会用到
         */
        SharedPreferences.Editor editor = getSharedPreferences("items", MODE_PRIVATE).edit();
        logsStr = msgStr + "\n" + logsStr;
        editor.putString("logsStr", logsStr);
        editor.commit();

        //创建消息对象
        Message msg = new Message();
        msg.what = 0;
        Bundle bundle = new Bundle();
        bundle.putString("logsStr", logsStr);
        msg.setData(bundle);
        MainActivity.monitorLogHandler.sendMessage(msg);
    }

    //获取HTML
    public String getHtml(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8 * 1000);
        //通过输入流获取html数据
        InputStream inStream = conn.getInputStream();
        //获取html的二进制数组
        byte[] data = readInputStream(inStream);
        //获取指定字符集解码指定的字节数组构造一个新的字符串
        String html = new String(data, "UTF-8");
        return html;
    }

    //读取输入流 获取HTML二进制数组
    public byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }


}

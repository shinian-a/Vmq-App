package com.shinian.pay.ui;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.zxing.activity.CaptureActivity;
import com.shinian.pay.R;
import com.shinian.pay.manager.AppConstants;
import com.shinian.pay.service.ForeService;
import com.shinian.pay.service.PayNotificationListenerService;
import com.shinian.pay.util.SaveImageUtils;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 email：shiniana@qq.com
 qq：1614790395
 GitHub：https://github.com/shinian-a
 @©️版权所有
 */

public class MainActivity extends AppCompatActivity implements OnLongClickListener {

    private TextView txthost;
    private TextView txtkey;
    private boolean isOk = false;
    private static String TAG = "MainActivity";
    private static String host;
    private static String key;
    public static TextView LogsTextView;
    private static LinearLayout logs_linear_layout;
    private int id = 0;
    //定义 Bitmap变量
    private Bitmap bitmap_image;
    private int code;
    private String ver;
    private int version;
    private String uplog;
    private String upurl;
    private int Version;
    private String state_swich;
    private TextView sj_dl;//当前电量Text
    private int capacity;
    private int ld = 5;//亮度值0~255
    //当前版本

    private static final String ALIPAY_PERSON = "https://qr.alipay.com/fkx12542rpb5fljmhxlal35";

    private Thread dlThread;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // 自动适配屏幕
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.activity_main);


        //查找组件
        txthost = (TextView) findViewById(R.id.txt_host);
        txtkey = (TextView) findViewById(R.id.txt_key);
        LogsTextView = (TextView) findViewById(R.id.state_logs);
        logs_linear_layout = (LinearLayout) findViewById(R.id.logs_linear_layout);
        LogsTextView.setOnLongClickListener((OnLongClickListener) this);//长按
        sj_dl = (TextView) findViewById(R.id.sj_dl);

        // 设置底部版权信息文本的下划线
        TextView bqTextView = (TextView) findViewById(R.id.bq);
        if (bqTextView != null) {
            bqTextView.setPaintFlags(bqTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        // 开始前台服务
        startService(new Intent(MainActivity.this, ForeService.class));



        //调用 App 方法检查更新（在后台静默检查，不显示结果）
        App();


        //接收并设置日志信息
        SharedPreferences read1 = getSharedPreferences("items", MODE_PRIVATE);
        String logsStr = read1.getString("logsStr", "");
        if (logsStr.equals("")) {
            LogsTextView.setText("日志：null");
        } else {
            LogsTextView.setText(logsStr);
        }


        if (!checkQQInstalled(this, "com.tencent.mm")) {
            Toast.makeText(getApplication(), "提示：无法检测到微信包名！\n可能无法正常监听", Toast.LENGTH_LONG).show();
        }
        //检测微信支付宝包名 写在前面
        if (!checkQQInstalled(this, "com.eg.android.AlipayGphone")) {
            Toast.makeText(getApplication(), "提示：无法检测到支付宝包名！\n可能无法正常监听", Toast.LENGTH_LONG).show();
        }

        // 获取状态
        SharedPreferences state = getSharedPreferences("state_switch", MODE_PRIVATE);
        state_swich = state.getString("state_switch", "");
        //屏幕是否常亮
        if (state_swich.equals("no")) {
            Toast.makeText(getApplication(), "当前处于屏幕常亮模式" + "\n当前界面亮度值：" + ld, Toast.LENGTH_LONG).show();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//开启常亮
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            initAppHeart();//电量线程
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                @Override
                public void run() {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = Float.valueOf(ld) * (1f / 255f);//亮度值0~255
                    getWindow().setAttributes(lp);
                }
            }, 3000);
        }

        //检测通知使用权是否启用
        if (!isNotificationListenersEnabled()) {

            //增加一层权限判断 以防部分云手机获取不到是否开启
            if (!isNLServiceEnabled()) {

                //创建弹窗对象
                AlertDialog.Builder qx = new AlertDialog.Builder(MainActivity.this);
                //alert = builder.setIcon(R.mipmap.ic_icon_fish)
                qx.setIcon(R.drawable.menu_gy);
                qx.setTitle("温馨提示：");
                qx.setMessage("当前你未授权收款插件读取通知栏权限，请前往授权后再继续操作");
                qx.setCancelable(false); //设置是否可以点击对话框外部取消
                qx.setPositiveButton("前往授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //跳转到通知使用权页面
                        gotoNotificationAccessSetting();
                    }
                });
                qx.setNeutralButton("暂不授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplication(), "请给予监听权限！否则无法正常运行", Toast.LENGTH_LONG).show();
                    }
                });
                qx.show();//显示
            }
        } else {
            //重启监听服务
            toggleNotificationListenerService(this);
        }

        //读入保存的配置数据并显示
        SharedPreferences read = getSharedPreferences("shinian", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");

        if (host != null && key != null && host != "" && key != "") {
            txthost.setText(" 通知地址：" + host);
            txtkey.setText(" 通讯密钥：" + key);
            isOk = true;
        }

    }

    /**
     * 退出应用，停止所有服务和清理资源
     */
    private void exitApp() {
        try {
            // 1. 停止心跳线程（如果存在）
            if (dlThread != null) {
                dlThread.interrupt();
                dlThread = null;
                Log.d(TAG, "已停止电量线程");
            }

            // 2. 停止前台服务（监听服务）
            try {
                stopService(new Intent(MainActivity.this, ForeService.class));
                Log.d(TAG, "已停止前台监听服务");
            } catch (Exception e) {
                Log.e(TAG, "停止前台服务失败", e);
            }

            // 3. 重启通知监听服务（禁用再启用以清理状态）
            try {
                toggleNotificationListenerService(this);
                Log.d(TAG, "已重启通知监听服务");
            } catch (Exception e) {
                Log.e(TAG, "重启通知监听服务失败", e);
            }

            // 4. 清除所有通知
            try {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.cancelAll();
                    Log.d(TAG, "已清除所有通知");
                }
            } catch (Exception e) {
                Log.e(TAG, "清除通知失败", e);
            }

            // 5. 显示退出提示
            Toast.makeText(this, "正在退出监控服务...", Toast.LENGTH_SHORT).show();

            // 6. 延迟退出，确保所有服务已停止
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 7. 终止进程
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    } catch (Exception e) {
                        Log.e(TAG, "终止进程失败", e);
                        // 强制终止
                        Runtime.getRuntime().exit(0);
                    }
                }
            }, 800); // 延迟 800 毫秒，确保服务完全停止

        } catch (Exception e) {
            Log.e(TAG, "退出应用时发生错误", e);
            // 如果清理过程失败，直接退出
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception ex) {
                Runtime.getRuntime().exit(0);
            }
        }
    }


    //监听日志接收参数
    public static Handler monitorLogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (LogsTextView != null) {
                LogsTextView.setText(msg.getData().getString("logsStr"));
            }
        }
    };

    /**
     * 获取当前电量百分比
     *
     * @param context
     * @return
     */
    public int getBatteryCurrent(Context context) {
        int capacity = 0;
        try {
            BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            capacity = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);//当前电量剩余百分比
        } catch (Exception e) {

        }
        return capacity;
    }


    // 电池电量线程
    public void initAppHeart() {

        dlThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            sj_dl.setText("当前电量：" + getBatteryCurrent(MainActivity.this) + "%");
                        }
                    }, 1000);

                    try {
                        Thread.sleep(60 * 1000);//一分钟休眠
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        dlThread.start(); //启动线程
    }


    /**
     * @param string
     * @return 转换之后的内容
     * @Title: unicodeDecode
     * @Description: unicode解码 将Unicode的编码转换为中文
     */
    public String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
    }

    //检测更新
    /*public void App() {
        //子线程
        Thread thread=new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //调用getHtml方法请求接口
                        html = getHtml("http://www.cute521.cn/api.php?api=ini&app=15204");
                        //创建JSON解析对象
                        JSONObject jsonObject = new JSONObject(html);
                        //获取状态码
                        code = jsonObject.getInt("code");
                        //获取msg所有内容
                        String Message=jsonObject.getString("msg");
                        //创建JSON解析msg所有内容
                        JSONObject json = new JSONObject(Message);
                        //获取msg中的应用版本
                        ver = json.optString("version");
                        //获取版本信息
                        info = json.optString("version_info");
                        //获取更新内容
                        show = json.optString("app_update_show");
                        //获取更新地址
                        url = json.optString("app_update_url");
                        //是否强制更新
                        update = json.optString("app_update_must");
						//主题 0为灰色1为正常
						if (info.equals("0")) {
							setGraySheme(0);
							//Toast.makeText(MainActivity.this,info, Toast.LENGTH_SHORT).show();
						} else {
							//取消灰色
							setGraySheme(1);
						}

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //UI子线程 实现UI控件操作
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (code == 200) {
                                    if (!ver.equals(Constant.Ver)) {
                                        if (update.equals("y")) {
                                            AlertDialog dialog=new AlertDialog.Builder(MainActivity.this)
                                                .setTitle("发现新版本~")
                                                .setMessage(show)
                                                .setIcon(R.drawable.app_gx)
                                                .setCancelable(true) //设置是否可以点击对话框外部取消
                                                .setPositiveButton("强制更新", new DialogInterface.OnClickListener() {

                                                    @Override
                                                    public void onClick(DialogInterface dia, int which) {
                                                        Intent intent_d = new Intent();
                                                        intent_d.setAction("android.intent.action.VIEW");
                                                        Uri content_url = Uri.parse(url);
                                                        intent_d.setData(content_url);
                                                        startActivity(intent_d);
                                                    }
                                                })
                                                .create();
                                            dialog.show();
                                        } else {
                                            AlertDialog dialog=new AlertDialog.Builder(MainActivity.this)
                                                .setTitle("发现新版本~")
                                                .setMessage(show)
                                                .setIcon(R.drawable.app_gx)
                                                .setCancelable(false) //设置是否可以点击对话框外部取消
                                                .setPositiveButton("立即更新", new DialogInterface.OnClickListener() {

                                                    @Override
                                                    public void onClick(DialogInterface dia, int which) {
                                                        Intent intent_d = new Intent();
                                                        intent_d.setAction("android.intent.action.VIEW");
                                                        Uri content_url = Uri.parse(url);
                                                        intent_d.setData(content_url);
                                                        startActivity(intent_d);
                                                    }
                                                })
                                                .setNeutralButton("忽略更新", null)
                                                .create();
                                            dialog.show();
                                        }
                                    } else {
                                        //Toast.makeText(MainActivity.this, "已是最新版本~", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    //Toast.makeText(MainActivity.this, "检测更新失败", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                }
            });
        thread.start();//启动线程
    }*/

    /**
     * 检查应用版本更新
     * @return true 表示有新版本，false 表示已是最新版本或检查失败
     */
    public boolean App() {
        HttpURLConnection httpConn = null;
        DataOutputStream dos = null;
        BufferedReader responseReader = null;
            
        try {
            //检查版本号是否最新接口
            String urlPath = "http://w.t3yanzheng.com/A729B02347E855EC";
    
            //当前软件版本号
            Version = Integer.parseInt(getAppVersionCode());
            String param = "ver=" + URLEncoder.encode(getAppVersionCode(), "UTF-8");
    
            //建立连接
            httpConn = getHttpURLConnection(urlPath);
    
            //建立输入流，向指向的 URL 传入参数
            dos = new DataOutputStream(httpConn.getOutputStream());
            dos.writeBytes(param);
            dos.flush();
    
            //获得响应状态
            int resultCode = httpConn.getResponseCode();
            if (HttpURLConnection.HTTP_OK == resultCode) {
                StringBuilder sb = new StringBuilder();
                String readLine;
                responseReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF-8"));
                while ((readLine = responseReader.readLine()) != null) {
                    sb.append(readLine).append("\n");
                }
                    
                JSONObject data = new JSONObject(sb.toString().trim());
                    
                // 验证必要字段是否存在
                if (!data.has("code")) {
                    Log.e(TAG, "检查更新失败：缺少 code 字段");
                    return false;
                }
                    
                code = data.getInt("code");
                    
                // 只有在 code==200 时才尝试获取其他字段
                if (code == 200) {
                    // 安全地获取可选字段，避免崩溃
                    ver = data.optString("ver", "");
                    version = data.optInt("version", 0);
                    uplog = data.optString("uplog", "");
                    upurl = data.optString("upurl", "");

                    Log.i(TAG, "检查更新成功："+ data.toString() + version + "--" + Version);
                    // 验证必要字段
                    if (version > 0 && version > Version) {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("发现新版本！")
                                .setMessage(uplog)
                                .setIcon(R.drawable.app_gx)
                                .setCancelable(false)
                                .setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dia, int which) {
                                        Intent intent_d = new Intent();
                                        intent_d.setAction("android.intent.action.VIEW");
                                        Uri content_url = Uri.parse(upurl);
                                        intent_d.setData(content_url);
                                        startActivity(intent_d);
                                    }
                                })
                                .setNeutralButton("忽略更新", null)
                                .create();
                        dialog.show();
                        return true;
                    }
                } else {
                    Log.w(TAG, "检查更新返回错误码：" + code);
                }
            } else {
                Log.e(TAG, "检查更新请求失败，响应码：" + resultCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "检查更新发生异常", e);
        } finally {
            // 关闭资源
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭 DataOutputStream 失败", e);
                }
            }
            if (responseReader != null) {
                try {
                    responseReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭 BufferedReader 失败", e);
                }
            }
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return false;
    }

    @NonNull
    static HttpURLConnection getHttpURLConnection(String urlPath) throws IOException {
        URL url = new URL(urlPath);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        //设置参数
        httpConn.setDoOutput(true);     //需要输出
        httpConn.setDoInput(true);      //需要输入
        httpConn.setUseCaches(false);   //不允许缓存
        httpConn.setRequestMethod("POST");      //设置POST方式连接

        //设置请求属性
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpConn.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
        httpConn.setRequestProperty("Charset", "UTF-8");

        //连接,也可以不用明文connect，使用下面的httpConn.getOutputStream()会自动connect
        httpConn.connect();
        return httpConn;
    }

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


    //QQ群 key：qun.qq.com
    public boolean joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(
                "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D"
                        + key));
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "未安装或版本不支持", Toast.LENGTH_SHORT).show();
            // 未安装或版本不支持
            return false;
        }
    }

    public boolean isIgnoringBatteryOptimizations(Context context) {
        if (context == null) {
            return false;
        }
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return isIgnoring;
    }

    /**
     * 忽略电池优化
     */

    public void ignoreBatteryOptimization(Activity activity) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean hasIgnored = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
            //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if (!hasIgnored) {
                try {//先调用系统显示 电池优化权限
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {//如果失败了则引导用户到电池优化界面
                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN);

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        intent.addCategory(Intent.CATEGORY_LAUNCHER);

                        ComponentName cn = ComponentName.unflattenFromString("com.android.settings/.Settings$HighPowerApplicationsActivity");

                        intent.setComponent(cn);

                        startActivity(intent);
                    } catch (Exception ex) {//如果全部失败则说明没有电池优化功能

                    }
                }

            }
        }


    }

    //检测电池白名单权限
    private boolean isIgnoringBatteryOptimizations() {

        boolean isIgnoring = false;

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            }
        }
        return isIgnoring;
    }

    //打开电池白名单设置
    public void requestIgnoreBatteryOptimizations() {

        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //点击两次退出
    static boolean isExit;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                //isExit = true;
                AlertDialog.Builder exit = new AlertDialog.Builder(this);
                exit.setTitle("温馨提示：");
                exit.setIcon(R.drawable.menu_exit);
                exit.setMessage("确定退出程序吗？退出将无法正常监听!");
                exit.setCancelable(false); //设置是否可以点击对话框外部取消
                exit.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dia, int which) {
                                exitApp();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                exit.show();
				/*Toast.makeText(getApplicationContext(), "再按一次退出程序!", Toast.LENGTH_SHORT).show();
                 new Thread(new Runnable() {
                 @Override
                 public void run() {
                 try {
                 Thread.sleep(3000);
                 isExit = false;
                 } catch (InterruptedException e) {
                 e.printStackTrace();
                 }
                 }
                 }).start();
                 } else {*/
                //android.os.Process.killProcess(android.os.Process.myPid());
            }
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    // 创建并显示 Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
            
        // 尝试显示菜单图标（兼容华为 EMUI/Magic UI）
        enableMenuIcons(menu);
        return true;
    }

    // 在选项菜单打开以后会调用这个方法，设置 menu 图标显示（icon）
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        // 通过反射让菜单显示图标
        enableMenuIcons(menu);
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * 通过反射启用菜单图标显示
     * Android 默认隐藏菜单图标，需要通过反射调用 setOptionalIconsVisible
     * 兼容华为 EMUI/Magic UI 系统
     */
    private void enableMenuIcons(Menu menu) {
        if (menu == null) {
            return;
        }

        // 方法 1：尝试使用 MenuBuilder 的 setOptionalIconsVisible 方法
        try {
            String className = menu.getClass().getSimpleName();
            if ("MenuBuilder".equals(className)) {
                Method method = menu.getClass().getDeclaredMethod(
                        "setOptionalIconsVisible", Boolean.TYPE);
                method.setAccessible(true);
                method.invoke(menu, true);
                Log.d(TAG, "enableMenuIcons: 方法 1 成功 - MenuBuilder");
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "enableMenuIcons: 方法 1 失败", e);
        }

        // 方法 2：尝试直接设置每个菜单项的图标可见性（兼容华为）
        try {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                // 请求显示图标
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            Log.d(TAG, "enableMenuIcons: 方法 2 成功 - setShowAsAction");
        } catch (Exception e) {
            Log.w(TAG, "enableMenuIcons: 方法 2 失败", e);
        }

        // 方法 3：尝试使用 setGroupCheckable（备用方案）
        try {
            Method method = menu.getClass().getMethod("setGroupCheckable", int.class, boolean.class, boolean.class);
            method.invoke(menu, 0, true, false);
            Log.d(TAG, "enableMenuIcons: 方法 3 成功 - setGroupCheckable");
        } catch (Exception e) {
            Log.w(TAG, "enableMenuIcons: 方法 3 失败", e);
        }
    }

    //菜单监听item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        //群聊
        if (itemId == R.id.qun) {
            if (joinQQGroup("yy-t5uc2_M6gq66cqFFRDHR4LqQLPCAi")) {
                Toast.makeText(this, "正在跳转至反馈群...", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        //分享软件
        else if (itemId == R.id.share) {
            try {
                // 尝试获取当前应用的 APK 文件路径（兼容不同厂商）
                String apkPath = getApplicationInfo().sourceDir;
                
                // 如果 sourceDir 为空，尝试使用 packageCodePath
                if (apkPath == null || apkPath.isEmpty()) {
                    apkPath = getApplicationContext().getPackageCodePath();
                }
                
                // 检查路径是否有效
                boolean canShareApk = true;
                if (apkPath == null || apkPath.isEmpty() || apkPath.contains("null")) {
                    Log.w(TAG, "APK 路径无效，将使用链接分享：" + apkPath);
                    canShareApk = false;
                } else {
                    File apkFile = new File(apkPath);
                    // 检查文件是否存在且可读
                    if (!apkFile.exists() || !apkFile.canRead()) {
                        Log.w(TAG, "APK 文件不可访问，将使用链接分享：" + apkPath);
                        canShareApk = false;
                    } else {
                        // 尝试分享 APK 文件
                        try {
                            // 创建分享意图
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("application/vnd.android.package-archive");
                            
                            // 使用 FileProvider 分享 APK（Android 7.0+ 要求）
                            Uri apkUri = FileProvider.getUriForFile(
                                    this,
                                    getPackageName() + ".fileprovider",
                                    apkFile
                            );
                            
                            shareIntent.putExtra(Intent.EXTRA_STREAM, apkUri);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                                    getString(R.string.share_content, "https://shinian-a.github.io/"));
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            
                            // 创建选择器并启动
                            Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_content));
                            if (chooser != null) {
                                startActivity(chooser);
                                Toast.makeText(this, "请选择分享方式", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        } catch (Exception apkException) {
                            Log.e(TAG, "分享 APK 失败，将使用链接分享", apkException);
                            canShareApk = false;
                        }
                    }
                }
                
                // 如果无法分享 APK，则降级为分享下载链接
                if (!canShareApk) {
                    shareDownloadLink();
                }
            } catch (Exception e) {
                Log.e(TAG, "分享功能异常", e);
                Toast.makeText(this, "分享失败，请检查是否安装了社交应用", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        //打赏作者
        else if (itemId == R.id.support) {
            /*
             Intent intent2 = new Intent();
             intent2.setClass(MainActivity.this, Pay.class);
             startActivity(intent2);
             */
            final String[] fruits = new String[]{"微信打赏", "支付宝打赏"};

            AlertDialog.Builder pay = new AlertDialog.Builder(this);
            pay.setIcon(R.drawable.pay);
            pay.setTitle("请选择打赏方式：");
            pay.setCancelable(true); //设置是否可以点击对话框外部取消
            pay.setItems(fruits, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String fs = fruits[which];
                    if (fs.equals("微信打赏")) {
                        //requestMyPermissions();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            //判断读写权限
                            if (pdPermissions()) {
                                //获取本地图片bitmap
                                bitmap_image = BitmapFactory.decodeResource(getResources(), R.drawable.wxpay);
                                SaveImageUtils.fileSaveToPublic(MainActivity.this, "微信赞赏码", bitmap_image);
                                //打开微信扫一扫
                                openWeixinToQE_Code(MainActivity.this);
                                Toast.makeText(MainActivity.this, "收款码已保存至相册,请选择相册收款码打赏~", Toast.LENGTH_LONG).show();
                            } else {
                                //申请读写权限
                                requestMyPermissions();
                            }

                        } else {
                            Toast.makeText(MainActivity.this, "无法打赏", Toast.LENGTH_SHORT).show();

                        }
                            /*} else if (fs.equals("支付宝打赏")) {
                             openAliPayPay(ALIPAY_PERSON);
                             /*
                             Toast.makeText(MainActivity.this, "正在打开支付宝...", Toast.LENGTH_SHORT).show();
                             //打开支付宝付款
                             Uri uri = Uri.parse("https://qr.alipay.com/fkx12542rpb5fljmhxlal35");
                             Intent intent = new Intent();
                             intent.setAction("android.intent.action.VIEW");
                             intent.setData(uri);
                             startActivity(intent);
                             */
                    } else if (fs.equals("支付宝打赏")) {
                        openAliPayPay(ALIPAY_PERSON);
                            /*
                             Toast.makeText(MainActivity.this, "正在打开支付宝...", Toast.LENGTH_SHORT).show();
                             //打开支付宝付款
                             Uri uri = Uri.parse("https://qr.alipay.com/fkx12542rpb5fljmhxlal35");
                             Intent intent = new Intent();
                             intent.setAction("android.intent.action.VIEW");
                             intent.setData(uri);
                             startActivity(intent);
                             */
                    }
                }
            }).create();//创建
            pay.show();
            return true;
        }
        //关于
        else if (itemId == R.id.about) {
            AlertDialog.Builder gy = new AlertDialog.Builder(this);
            gy.setIcon(R.drawable.menu_gy);
            gy.setTitle("关于本软件：");
            gy.setCancelable(false); //设置是否可以点击对话框外部取消
            gy.setMessage("简介：\n这是一款基于V免签开发的免签支付接口App监控端\n修复了原版监控支付宝不回调等BUG长期维护并提供个人免费使用\n在您使用本软件前请注意：\n该软件版权归作者所有请勿破解倒卖本软件\n部分申请的权限是必要的拒绝将导致监控功能失效\n喜欢本项目就赞助一下开发者吧~\nPS：使用本软件建议开启软件自启动权限！各大厂商手机自行百度寻找答案\n\nApp使用协议：\n\n说明：\n该软件由作者十年开发并提供技术服务支持并发布免费使用\n1、所有用户在下载并浏览V免签监控端_Pro时均被视为已经仔细阅读本条款并完全同意。\n2、软件完全免费可自由使用学习并分享，您可以将它分享给您的朋友们使用。\n3、使用该软件应当遵守法律法规若侵犯了第三方知识产权或其他权益需本人承担全部责任。\n作者对此不承担任何责任。\n4、V免签监控端_Pro需要获取一定的应用权限例如内存读写，通知监听等请务必开启权限否则软件无法正常运行。\n5、如果您使用的是盗版软件，出现的一切风险作者对此不承担任何责任。\n6、用户明确并同意因其使用本App而产生的一切后果由其本人承担，作者对此不承担任何责任。\n7、我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。\n\n\"确保您已同意以上协议否则请卸载本软件！\"\n\n联系作者反馈请到设置项~")

                    .setNegativeButton("不同意协议", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                            //Toast.makeText(MainActivity.this, "你点击了取消按钮~", Toast.LENGTH_SHORT).show();
                        }
                    })

                    .setPositiveButton("已阅读并同意", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(MainActivity.this, "你点击了确定按钮~", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("配置文档", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(MainActivity.this, "你点击了中立按钮~", Toast.LENGTH_SHORT).show();
                            Intent help = new Intent();
                            help.setClass(MainActivity.this, HelpActivity.class);
                            startActivity(help);

                        /*
                         Intent intent_d = new Intent();
                         intent_d.setAction("android.intent.action.VIEW");
                         Uri content_url = Uri.parse("https://github.com/szvone/Vmq");
                         intent_d.setData(content_url);
                         startActivity(intent_d);
                         */
                        }
                    }).create(); //创建AlertDialog对象
            gy.show(); //显示对话框
            return true;
        }
        //设置
        else if (itemId == R.id.setting) {
            Intent set = new Intent();
            set.setClass(MainActivity.this, SettingActivity.class);
            startActivity(set);
            return true;
        }
        //退出程序
        else if (itemId == R.id.exit) {
            exitApp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 分享下载链接（降级方案）
     * 当无法直接分享 APK 文件时使用此方法
     */
    private void shareDownloadLink() {
        try {
            // 创建纯文本分享意图
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            // 设置分享内容（包含下载链接）
            String shareText = getString(R.string.share_content, "https://shinian-a.github.io/");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            // 创建选择器并启动
            Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_content));
            if (chooser != null) {
                startActivity(chooser);
                Toast.makeText(this, "将分享下载链接", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "分享链接失败", e);
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 支付宝支付 打赏功能
     *
     * @param qrCode
     */
    private void openAliPayPay(String qrCode) {
        if (openAlipayPayPage(this, qrCode)) {
            Toast.makeText(this, "正在打开...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "支付宝打开失败，请检查是否安装支付宝！", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean openAlipayPayPage(Context context, String qrcode) {
        try {
            qrcode = URLEncoder.encode(qrcode, "utf-8");
        } catch (Exception e) {
        }
        try {
            final String alipayqr = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + qrcode;
            openUri(context, alipayqr + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送一个intent
     *
     * @param context
     * @param s
     */
    private static void openUri(Context context, String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        context.startActivity(intent);
    }

    //打赏功能**
    //fileName 为文件路径名称 返回true为存在
    public boolean fileIsExists(String fileName) {
        try {
            File f = new File(fileName);
            if (f.exists()) {
                Log.i("测试", "有这个文件");
                return true;
            } else {
                Log.i("测试", "没有这个文件");
                return false;
            }
        } catch (Exception e) {
            Log.i("测试", "崩溃");
            return false;
        }
    }


    /**
     * 保存图片到本地
     * 仅支持Android10以下
     *
     * @param name   图片的名字，比如传入“123”，最终保存的图片为“123.jpg”
     * @param bitmap 本地图片或者网络图片转成的Bitmap格式的文件
     * @return
     */
    public void saveImage(String name, Bitmap bitmap) {
        File pathFile = new File(Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_PICTURES + File.separator);
        if (!pathFile.exists()) {
            pathFile.mkdir();
        }
        File file = new File(pathFile, name + ".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            // 最后通知图库更新
            //ToastsUtils.centerToast(this, "保存成功");
            Uri localUri = Uri.fromFile(file);
            Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
            sendBroadcast(localIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //打开支付宝扫一扫
    public void AliPay(Context context) {
        try {
            Uri uri = Uri.parse("alipayqr://platformapi/startapp?saId=10000007");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法跳转到支付宝，请检查是否安装了支付宝", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 打开微信并跳入到二维码扫描页面
     * 方法一
     */
    public void openWeixinToQE_Code(Context context) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
            intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
            intent.setAction("android.intent.action.VIEW");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法跳转到微信，请检查是否安装了微信", Toast.LENGTH_LONG).show();
        }
    }

    //打开微信扫一扫方法二
    public void toWeChatScanDirect(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
            intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
            intent.setFlags(335544320);
            intent.setAction("android.intent.action.VIEW");
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法跳转到微信，请检查是否安装了微信", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 判断 用户是否安装QQ客户端
     */
    public boolean isQQClientAvailable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                if (pn.equalsIgnoreCase("com.tencent.qqlite") || pn.equalsIgnoreCase("com.tencent.mobileqq")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断 Uri是否有效 方法
     */
    public boolean isValidIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        return !activities.isEmpty();
    }

    /**
     * 复制内容到剪切板
     *
     * @param copyStr
     * @return
     */
    private boolean copyStr(String copyStr) {
        try {
            //获取剪贴板管理器
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", copyStr);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**************************************************************************/
    //后台管理
    public void admin_url(View v) {
        String url = txthost.getText().toString();
        if (url.equals("通知地址：请手动配置")) {
            Toast.makeText(this, "请先配置数据！", Toast.LENGTH_LONG).show();
        } else {
            String Url = url.substring(6);
            Intent intent_d = new Intent();
            intent_d.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse("http://" + Url);
            intent_d.setData(content_url);
            startActivity(intent_d);
        }

    }

    //扫码配置
    public void startQrCode(View v) {
        // 申请相机权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, AppConstants.REQ_PERM_CAMERA);
            return;
        }
        // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AppConstants.REQ_PERM_EXTERNAL_STORAGE);
            return;
        }
        // 二维码扫码
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, AppConstants.REQ_QR_CODE);
    }

    //手动配置
    public void doInput(View v) {
        final EditText inputServer = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入配置数据").setView(inputServer);
        builder.setIcon(R.drawable.icon_pzsj);
        //builder.setMessage("PS：请输入网站后台显示的配置数据！");
        builder.setCancelable(false); //设置是否可以点击对话框外部取消

        builder.setNeutralButton("如何配置?", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //配置文档activity
                Intent help = new Intent();
                help.setClass(MainActivity.this, HelpActivity.class);
                startActivity(help);
            }
        });

        builder.setNegativeButton("取消", null);

        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                String scanResult = inputServer.getText().toString();
                String[] tmp = scanResult.split("/");
                if (tmp.length != 2) {
                    Toast.makeText(MainActivity.this, "数据不能为空或数据错误!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String t = String.valueOf(new Date().getTime());
                String sign = md5(t + tmp[1]);

                OkHttpClient okHttpClient = new OkHttpClient();
                Request request = new Request.Builder().url("http://" + tmp[0] + "/appHeart?t=" + t + "&sign=" + sign)
                        .method("GET", null).build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "onResponse: " + response.body().string());
                        isOk = true;

                    }
                });

                if (tmp[0].indexOf("localhost") >= 0) {
                    Toast.makeText(MainActivity.this, "配置信息错误，本机调试请访问 本机局域网IP:8080(如192.168.1.101:8080) 获取配置信息进行配置!",
                            Toast.LENGTH_LONG).show();

                    return;
                }
                //检查电池白名单权限
                ignoreBatteryOptimization(MainActivity.this);
                //将配置的信息显示出来
                txthost.setText(" 通知地址：" + tmp[0]);
                txtkey.setText(" 通讯密钥：" + tmp[1]);
                host = tmp[0];
                key = tmp[1];

                SharedPreferences.Editor editor = getSharedPreferences("shinian", MODE_PRIVATE).edit();
                editor.putString("host", host);
                editor.putString("key", key);
                editor.commit();

            }
        });
        builder.show();
    }

    //检测心跳
    public void doStart(View view) {
        if (isOk == false) {
            Toast.makeText(MainActivity.this, "请您先配置!", Toast.LENGTH_SHORT).show();
            return;
        }

        String t = String.valueOf(new Date().getTime());
        String sign = md5(t + key);

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url("http://" + host + "/appHeart?t=" + t + "&sign=" + sign)
                .method("GET", null).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Looper.prepare();
                Toast.makeText(MainActivity.this, "心跳状态错误，请检查配置是否正确!", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Looper.prepare();
                try {
                    //解析JSON内容
                    String str = response.body().string();
                    JSONObject result = new JSONObject(str);
                    int code = result.getInt("code");
                    String msg = result.getString("msg");
                    if (code == 1 && msg.equals("成功")) {
                        //发送心跳监听日志
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "心跳返回：" + msg);
                        }
                        //注意：响应内容在经过JSON和抛异常时需要使用新变量去接收信息进行Toast str
                        //Toast.makeText(MainActivity.this, "心跳返回：" + str, Toast.LENGTH_LONG).show();
                    } else {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "心跳返回错误：" + msg);
                        }
                        //Toast.makeText(MainActivity.this, "心跳返回错误！", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {//抛IO流异常 创建JSONobject需抛JSON异常
                    String error = e.getMessage();
                    //发送监听日志
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sendMonitorLogs(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\r\r\r\r" + "心跳错误：" + error);
                    }
                }
                Looper.loop();
            }
        });
    }

    // 检测监听
    // 通知渠道常量
    private static final String NOTIFICATION_CHANNEL_ID = "vmq_test_channel";
    private static final String NOTIFICATION_CHANNEL_NAME = "V免签测试通知";
    private static final int MAX_NOTIFICATION_ID = 1000; // 防止 ID 无限增长

    /**
     * 发送测试推送通知
     *
     * @param v 触发视图
     */
    public void checkPush(View v) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Toast.makeText(this, "通知服务不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建通知渠道（Android O 及以上）
        createNotificationChannel(notificationManager);

        // 构建通知
        Notification notification = buildTestNotification();

        // 发送通知，ID 循环使用防止溢出
        int notificationId = id++;
        if (notificationId > MAX_NOTIFICATION_ID) {
            id = 0; // 重置 ID 计数器
        }
        notificationManager.notify(notificationId, notification);
        
        // 注意：不要在这里直接发送日志，等待 NotificationListenerService 回调处理
        // 避免日志重复显示
    }

    /**
     * 创建通知渠道（Android O 及以上必需）
     *
     * @param notificationManager 通知管理器
     */
    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建测试通知
     *
     * @return 通知对象
     */
    private Notification buildTestNotification() {
        String title = "V免签测试推送";
        String content = "测试推送通知，如果程序正常，则会提示监听权限正常";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(content);
            // Android 10+ setTicker 已废弃，但为了兼容保留
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                builder.setTicker("测试推送信息，如果程序正常，则会提示监听权限正常");
            }
            return builder.build();
        } else {
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setTicker("测试推送信息，如果程序正常，则会提示监听权限正常");
            return builder.build();
        }
    }

    //清除所有日志
    public void Logs(View v) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                SharedPreferences.Editor editor = getSharedPreferences("items", MODE_PRIVATE).edit();
                editor.putString("logsStr", "");
                editor.commit();
                LogsTextView.setText("日志：null");
                    /*
                     runOnUiThread(new Runnable() {
                     @Override
                     public void run() {                         
                     //清空TextView当前内容
                     LogsTextView.setText("日志：null");
                     }                     
                     });
                     */
            }
        });
    }

    //获取电量onClick
    public void dl(View v) {
        //电量获取
        BatteryManager manager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        capacity = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);//当前电量剩余百分比
        sj_dl.setText("当前电量：" + capacity + "%");
    }

    //联系作者
    public void author(View v) {
        // 跳转之前，可以先判断手机是否安装 QQ
        if (isQQClientAvailable(this)) {
            // 跳转到客服的 QQ
            String url = "mqqwpa://im/chat?chat_type=wpa&uin=1614790395";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // 跳转前先判断 Uri 是否存在，如果打开一个不存在的 Uri，App 可能会崩溃
            if (isValidIntent(this, intent)) {
                startActivity(intent);
            }
        }
    }

    //打开作者网站
    public void openAuthorWebsite(View v) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://shinian-a.github.io/"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开网页，请检查浏览器设置", Toast.LENGTH_SHORT).show();
        }
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


    //复制监听日志内容到剪贴板
    @Override
    public boolean onLongClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.state_logs) {
            String data = LogsTextView.getText().toString();
            copyStr(data);
            Toast.makeText(getApplication(), "Log日志已复制到剪贴板！", Toast.LENGTH_LONG).show();
        }

        //当返回true时，将不会产生连带触发，如点击事件
        return true;
    }


    //MD5取值
    public String md5(String string) {
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

    //判断读写权限
    private boolean pdPermissions() {
        boolean qx = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            qx = false;
        } else {
            qx = true;
        }
        return qx;
    }

    //动态申请读写权限
    private void requestMyPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有写SD权限");
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有写SD权限");
        }

    }

    //重启监听服务
    private void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, PayNotificationListenerService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(context, PayNotificationListenerService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        //Toast.makeText(MainActivity.this, "监听服务启动中...", Toast.LENGTH_SHORT).show();
    }

    //是否获取通知监听权限
    public boolean isNotificationListenersEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 是否启用通知监听服务
     *
     * @return
     */
    public boolean isNLServiceEnabled() {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (packageNames.contains(getPackageName())) {
            return true;
        }
        return false;
    }


    //跳转到通知监听设置页面
    protected boolean gotoNotificationAccessSetting() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;


        } catch (ActivityNotFoundException e) {//普通情况下找不到的时候需要再特殊处理找一次
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName("com.android.settings",
                        "com.android.settings.Settings$NotificationAccessSettingsActivity");
                intent.setComponent(cn);
                intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
                startActivity(intent);
                return true;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Toast.makeText(this, "对不起，您的手机暂不支持", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (requestCode == AppConstants.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(AppConstants.INTENT_EXTRA_KEY_QR_SCAN);

            String[] tmp = scanResult.split("/");
            if (tmp.length != 2) {
                Toast.makeText(MainActivity.this, "二维码错误，请您扫描网站上显示的二维码!", Toast.LENGTH_SHORT).show();
                return;
            }

            String t = String.valueOf(new Date().getTime());
            String sign = md5(t + tmp[1]);


            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url("http://" + tmp[0] + "/appHeart?t=" + t + "&sign=" + sign).method("GET", null).build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(TAG, "onResponse: " + response.body().string());
                    isOk = true;

                }
            });

            //检查电池白名单权限
            ignoreBatteryOptimization(this);
            //将扫描出的信息显示出来
            txthost.setText(" 通知地址：" + tmp[0]);
            txtkey.setText(" 通讯密钥：" + tmp[1]);
            host = tmp[0];
            key = tmp[1];

            SharedPreferences.Editor editor = getSharedPreferences("shinian", MODE_PRIVATE).edit();
            editor.putString("host", host);
            editor.putString("key", key);
            editor.commit();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case AppConstants.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode(null);
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
                }
                break;
            case AppConstants.REQ_PERM_EXTERNAL_STORAGE:
                // 文件读写权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode(null);
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的文件读写权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //获取当前程序版本号
    public String getAppVersionCode() {
        String versioncode = "";
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            versioncode = String.valueOf(pi.versionCode);
            if (versioncode == null || versioncode.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versioncode;
    }

    //获取当前应用的版本名(展示给消费者的版本号)
    private String getAppVersionName() {
        // 获取packagemanager的实例
        PackageManager packageManager = getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = packInfo.versionName;
        return version;
    }

    //获取
    private boolean checkQQInstalled(Context context, String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (Exception x) {
            return false;
        }
        return true;
    }


    //主题
    private void setGraySheme(int gray) {
        View decorView = getWindow().getDecorView();
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(gray);//灰度效果 取值gray（0 - 1）  0是灰色，1取消灰色
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }
}

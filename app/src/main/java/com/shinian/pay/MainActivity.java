package com.shinian.pay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.os.PowerManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;


import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.activity.CaptureActivity;
import com.shinian.pay.manager.AppConstants;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static TextView LogsTextView;
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
        //实际分辨率
        WindowManager windowManager = getWindow().getWindowManager();
        Point point = new Point();
        windowManager.getDefaultDisplay().getRealSize(point);
        //屏幕实际宽度（像素个数）
        int width = point.x;
        //屏幕实际高度（像素个数）
        int height = point.y;

        //实际分辨率2
        WindowManager windowManager2 = getWindow().getWindowManager();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        //屏幕实际宽度（像素个数）
        int width2 = metrics.widthPixels;
        //屏幕实际高度（像素个数）
        int height2 = metrics.heightPixels;

        // 锁定屏幕
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);
//        try {
//            if (height >= 2400) {
//                setContentView(R.layout.activity_main);
//                //Toast.makeText(getApplication(), "当前分辨率" + height + "x" + width, Toast.LENGTH_SHORT).show();
//            } else if (height <= 1920 && height > 1600) {
//                setContentView(R.layout.activity_main_xdpi);
//            } else if (height <= 1280 && height > 960) {
//                setContentView(R.layout.activity_main_xdpi);
//            } else {
//                setContentView(R.layout.activity_main);
//                //Toast.makeText(getApplication(), "暂未适配1280×720分辨率以下屏幕\n获取分辨率失败", Toast.LENGTH_LONG).show();
//            }
//        } catch (Exception e) {
//            String error = e.getMessage();
//            Toast.makeText(getApplication(), error , Toast.LENGTH_SHORT).show();
//        }

        //沉浸式状态栏
        /*
         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
         Window window = ge"tWiIt's not that simple now. None ndow();
         window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
         }*/
		//查找组件
		txthost = (TextView) findViewById(R.id.txt_host);
		txtkey = (TextView) findViewById(R.id.txt_key);
        LogsTextView = (TextView) findViewById(R.id.state_logs);
        logs_linear_layout = (LinearLayout) findViewById(R.id.logs_linear_layout);
        LogsTextView.setOnLongClickListener((OnLongClickListener) this);//长按
        startService(new Intent(MainActivity.this, ForeService.class)); // 开始前台服务
        sj_dl = (TextView) findViewById(R.id.sj_dl);


        //判断签名校验SHA-1 --检测是否重新签名
        //SignCheck signCheck = new SignCheck(this, "0F:85:43:C2:2E:8E:58:1B:96:88:C6:7C:1E:97:1E:0A:1C:55:D2:5F");
        //if (signCheck.check()) {
            //调用App方法检查更新
            try{
            App();
            }catch(Exception e){
                
            }
            //申请读写权限
            //requestMyPermissions();

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

            SharedPreferences state = getSharedPreferences("state_switch", MODE_PRIVATE);
            state_swich = state.getString("state_switch", "");
            //屏幕是否常亮
            if (state_swich.equals("no")) {
                Toast.makeText(getApplication(), "当前处于屏幕常亮模式" + "\n当前界面亮度值：" + ld, Toast.LENGTH_LONG).show();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//开启常亮
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
                initAppHeart();//电量线程
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){

                        @Override
                        public void run() {                    
                            WindowManager.LayoutParams lp = getWindow().getAttributes();
                            lp.screenBrightness = Float.valueOf(ld) * (1f / 255f);//亮度值0~255
                            getWindow().setAttributes(lp);                     
                        }
                    }, 3000);
            }



            //检测支付宝是否安装
            /*
             if (checkQQInstalled(this, "com.eg.android.AlipayGphone")) {              
             } else {
             AlertDialog.Builder alidialog = new AlertDialog.Builder(this);
             alidialog.setCancelable(false); //设置是否可以点击对话框外部取消
             alidialog.setTitle("温馨提示");
             alidialog.setMessage("当前您未安装支付宝！请前往支付宝官网或点击下方按钮下载最新版本！");
             alidialog.setPositiveButton("下载支付宝", new DialogInterface.OnClickListener() {

             @Override
             public void onClick(DialogInterface dia, int which) {
             Intent intent_d = new Intent();
             intent_d.setAction("android.intent.action.VIEW");
             Uri content_url = Uri.parse("https://ds.alipay.com/");
             intent_d.setData(content_url);
             startActivity(intent_d);
             }
             })
             .setNegativeButton("关闭提示", new DialogInterface.OnClickListener() {

             @Override
             public void onClick(DialogInterface dia, int which) {
             Toast.makeText(getApplication(), "请安装支付宝！", Toast.LENGTH_SHORT).show();
             }
             })
             .create();
             alidialog.show();
             }

             //判断微信是否安装
             if (checkQQInstalled(this, "com.tencent.mm")) {              
             } else {
             AlertDialog.Builder wxdialog = new AlertDialog.Builder(this);
             wxdialog.setCancelable(false); //设置是否可以点击对话框外部取消
             wxdialog.setTitle("温馨提示");
             wxdialog.setMessage("当前您未安装微信！请前往微信官网或点击下方按钮下载最新版本！");
             wxdialog.setPositiveButton("下载微信", new DialogInterface.OnClickListener() {

             @Override
             public void onClick(DialogInterface dia, int which) {
             Intent intent_d = new Intent();
             intent_d.setAction("android.intent.action.VIEW");
             Uri content_url = Uri.parse("https://weixin.qq.com/");
             intent_d.setData(content_url);
             startActivity(intent_d);
             }
             })
             .setNegativeButton("关闭提示", new DialogInterface.OnClickListener() {

             @Override
             public void onClick(DialogInterface dia, int which) {
             Toast.makeText(MainActivity.this, "请安装微信！", Toast.LENGTH_SHORT).show();
             }
             })
             .create();
             wxdialog.show();
             }
             */

            /*
             //检测电池白名单是否启用
             if (!isIgnoringBatteryOptimizations()) {
             requestIgnoreBatteryOptimizations();
             }
             */
            //检测通知使用权是否启用
            if (!isNotificationListenersEnabled()) {

                //增加一层权限判断 以防部分云手机获取不到是否开启
                if (!isNLServiceEnabled()) {

                    //创建弹窗对象
                    AlertDialog.Builder qx = new AlertDialog.Builder(MainActivity.this);
                    //alert = builder.setIcon(R.mipmap.ic_icon_fish)
                    qx.setIcon(R.drawable.gy);
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
            //签名错误
//        } else {
//            //TODO 签名错误
//            AlertDialog.Builder qmjy = new AlertDialog.Builder(MainActivity.this);
//            qmjy.setIcon(R.drawable.jg);
//            qmjy.setTitle("警告：");
//            qmjy.setMessage("当前您使用的是非官方正版软件！请于官方地址下载正版软件使用");
//            qmjy.setCancelable(false); //设置是否可以点击对话框外部取消
//            qmjy.setPositiveButton("前往下载", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        //跳转浏览器
//                        Intent intent_d = new Intent();
//                        intent_d.setAction("android.intent.action.VIEW");
//                        Uri content_url = Uri.parse("https://github.com/shinian-a/Vmq-App");
//                        intent_d.setData(content_url);
//                        startActivity(intent_d);
//                    }
//                });
//			qmjy.show();//显示
//        }

        //校验是否有去除签名校验hook 
        if (checkApplication()) {
            //Toast.makeText(this, "签名正确", Toast.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder qm = new AlertDialog.Builder(MainActivity.this);
            qm.setIcon(R.drawable.jg);
            qm.setTitle("警告：");
            qm.setMessage("当前您使用的是非官方正版软件！请于官方地址下载正版软件使用");
            qm.setCancelable(false); //设置是否可以点击对话框外部取消
            qm.setPositiveButton("前往下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //跳转浏览器
                        Intent intent_d = new Intent();
                        intent_d.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse("https://github.com/shinian-a/Vmq-App");
                        intent_d.setData(content_url);
                        startActivity(intent_d);
                    }
                });
			qm.show();//显示
        }

        //验签检测动态PM代理
        if (checkPMProxy()) {         
        } else {
            AlertDialog.Builder qm3 = new AlertDialog.Builder(MainActivity.this);
            qm3.setIcon(R.drawable.jg);
            qm3.setTitle("警告：");
            qm3.setMessage("当前您使用的是非官方正版软件！请于官方地址下载正版软件使用");
            qm3.setCancelable(false); //设置是否可以点击对话框外部取消
            qm3.setPositiveButton("前往下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //跳转浏览器
                        Intent intent_d = new Intent();
                        intent_d.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse("https://github.com/shinian-a/Vmq-App");
                        intent_d.setData(content_url);
                        startActivity(intent_d);
                    }
                });
            qm3.show();//显示
        }

	}

    /**************************************主线程结束*****************************************/ 
    /**************************************主线程结束*****************************************/
    /**************************************主线程结束*****************************************/


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


    //电池电量线程
    public void initAppHeart() {

        dlThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){

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
    
    public void App() throws Exception{
        //检查版本号是否最新接口
        String urlPath = new String("http://w.t3yanzheng.com/A729B02347E855EC");  
        //String urlPath = new String("http://localhost:8080/Test1/HelloWorld?name=丁丁".getBytes("UTF-8"));

        
        //当前软件版本号
        Version = Integer.parseInt(getAppVersionCode());
        String param="ver=" + URLEncoder.encode(getAppVersionCode(), "UTF-8");

        //建立连接
        URL url=new URL(urlPath);
        HttpURLConnection httpConn=(HttpURLConnection)url.openConnection();

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

        //建立输入流，向指向的URL传入参数
        DataOutputStream dos=new DataOutputStream(httpConn.getOutputStream());
        dos.writeBytes(param);
        dos.flush();
        dos.close();

        //获得响应状态
        int resultCode=httpConn.getResponseCode();
        if (HttpURLConnection.HTTP_OK == resultCode) {
            StringBuffer sb=new StringBuffer();
            String readLine=new String();
            BufferedReader responseReader=new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF-8"));
            while ((readLine = responseReader.readLine()) != null) {
                sb.append(readLine).append("\n");
            }
            responseReader.close();
            JSONObject data = new JSONObject(sb.toString());
            
            code = data.getInt("code");              
            ver = data.getString("ver");
            version = data.getInt("version");
            uplog = data.getString("uplog");
            upurl = data.getString("upurl");
            
            if(code==200){
              if(version > Version){
                AlertDialog dialog=new AlertDialog.Builder(MainActivity.this)
                    .setTitle("发现新版本！")
                    .setMessage(uplog)
                    .setIcon(R.drawable.app_gx)
                    .setCancelable(false) //设置是否可以点击对话框外部取消
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
             }
           }
        }  
        
    }

    public String getHtml(String path) throws Exception {  
        URL url = new URL(path);  
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();  
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



    /**************************************************************************/

    /**
     - 校验 application
     * nowApplicationName 返回Application
     */
    private boolean checkApplication() {
        Application nowApplication = getApplication();
        String trueApplicationName = "Application";
        String nowApplicationName = nowApplication.getClass().getSimpleName();
        return trueApplicationName.equals(nowApplicationName);
    }
    /**
     * 检测 PM 代理
     */
    @SuppressLint("PrivateApi")
    private boolean checkPMProxy() {
        String truePMName = "android.content.pm.IPackageManager$Stub$Proxy";
        String nowPMName = "";
        try {
            // 被代理的对象是 PackageManager.mPM
            PackageManager packageManager = getPackageManager();
            Field mPMField = packageManager.getClass().getDeclaredField("mPM");
            mPMField.setAccessible(true);
            Object mPM = mPMField.get(packageManager);
            // 取得类名
            nowPMName = mPM.getClass().getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 类名改变说明被代理了
        return truePMName.equals(nowPMName);
    }

    /**************************************************************************/

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
                AlertDialog.Builder exit=new AlertDialog.Builder(this);
                exit.setTitle("温馨提示：");
                exit.setIcon(R.drawable.menu_exit);
                exit.setMessage("确定退出程序吗？退出将无法正常监听!");
                exit.setCancelable(false); //设置是否可以点击对话框外部取消
                exit.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dia, int which) {
                            //System.exit(0);//终止java虚拟机不推荐不容易退掉
                            android.os.Process.killProcess(android.os.Process.myPid());//杀进程ID
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

	//创建并显示Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inlager = getMenuInflater();
		inlager.inflate(R.menu.main_menu, menu);
		return true;
	}

    //在选项菜单打开以后会调用这个方法，设置menu图标显示（icon）
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
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
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            //分享内容
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                                "V免签最新监控端修复原版支付宝-微信不回调bug长期维护项目下载：https://shinianacn.lanzouy.com/b027kqata 密码:vmq");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "分享"));
            return true;
        }
        //打赏作者
        else if (itemId == R.id.support) {
            /*
             Intent intent2 = new Intent();
             intent2.setClass(MainActivity.this, Pay.class);
             startActivity(intent2);
             */
            final String[] fruits = new String[] { "微信打赏", "支付宝打赏" };

            AlertDialog.Builder pay = new AlertDialog.Builder(this);
            pay.setIcon(R.drawable.pay);
            pay.setTitle("请选择打赏方式：");
            pay.setCancelable(false); //设置是否可以点击对话框外部取消
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
                                Intent wxpay = new Intent();
                                wxpay.setClass(MainActivity.this, WxPayActivity.class);
                                startActivity(wxpay);

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
            gy.setIcon(R.drawable.gy);
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
        //结束程序
        else if (itemId == R.id.setting) {
            Intent set = new Intent();
            set.setClass(MainActivity.this, SettingActivity.class);
            startActivity(set);
            return true;
        }
        else if (itemId == R.id.exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            File f=new File(fileName);
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
     * @param name  图片的名字，比如传入“123”，最终保存的图片为“123.jpg”
     * @param bitmap  本地图片或者网络图片转成的Bitmap格式的文件
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
                            Toast.makeText(MainActivity.this, "心跳返回：" + str, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "心跳返回错误！", Toast.LENGTH_LONG).show();                          
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

    //检测监听
    public void checkPush(View v) {

        Notification mNotification;
        NotificationManager mNotificationManager;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "Channel1",
                                                                  NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(this, "1");

            mNotification = builder.setSmallIcon(R.drawable.ic_launcher).setTicker("测试推送信息，如果程序正常，则会提示监听权限正常")
                .setContentTitle("V免签测试推送").setContentText("测试推送通知，如果程序正常，则会提示监听权限正常").build();
        } else {
            mNotification = new Notification.Builder(MainActivity.this).setSmallIcon(R.drawable.ic_launcher)
                .setTicker("测试推送信息，如果程序正常，则会提示监听权限正常").setContentTitle("V免签测试推送")
                .setContentText("测试推送通知，如果程序正常，则会提示监听权限正常").build();
        }
        mNotificationManager.notify(id++, mNotification);
    }
    //清除所有日志
    public void Logs(View v) {
        runOnUiThread(new Runnable(){

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
        BatteryManager manager = (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        capacity = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);//当前电量剩余百分比
        sj_dl.setText("当前电量：" + capacity + "%");   
    }

    //联系作者
    public void author(View v) {
        // 跳转之前，可以先判断手机是否安装QQ
        if (isQQClientAvailable(this)) {
            // 跳转到客服的QQ
            String url = "mqqwpa://im/chat?chat_type=wpa&uin=1614790395";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // 跳转前先判断Uri是否存在，如果打开一个不存在的Uri，App可能会崩溃
            if (isValidIntent(this, intent)) {
                startActivity(intent);
            }
        }
    }


    //监听日志
    private void sendMonitorLogs(String msgStr) {
        /**
         * 先读取logs日志存储数据
         */
        SharedPreferences read = getSharedPreferences("items", MODE_PRIVATE);
        String logsStr = read.getString("logsStr", "");
        String [] logsStrs = logsStr.split("\n");
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

    //获取当前程序版本号(对消费者不可见的版本号)
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

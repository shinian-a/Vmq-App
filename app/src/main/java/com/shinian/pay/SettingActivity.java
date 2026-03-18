package com.shinian.pay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.shinian.pay.receiver.ScreenReceiverUtil;
import com.shinian.pay.service.DaemonService;
import com.shinian.pay.service.PlayerMusicService;
import com.shinian.pay.manager.AppConstants;
import com.shinian.pay.util.ScreenManager;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class SettingActivity extends AppCompatActivity {

	private static final String TAG = "SettingActivity";


    // 动态注册锁屏等广播
    private ScreenReceiverUtil mScreenListener;
    // 1像素Activity管理类
    private ScreenManager mScreenManager;
	private TextView version;
	private int code;
    private String ver;
    private int version1;
    private String uplog;
    private String upurl;
    private int Version;
    private Switch state_switch;
    private String state_swich;

	private ScreenReceiverUtil.SreenStateListener mScreenListenerer = new ScreenReceiverUtil.SreenStateListener() {
        @Override
        public void onSreenOn() {
            // 亮屏，移除"1像素"
            mScreenManager.finishActivity();
        }

        @Override
        public void onSreenOff() {
            // 接到锁屏广播，将SportsActivity切换到可见模式
            // "咕咚"、"乐动力"、"悦动圈"就是这么做滴
//            Intent intent = new Intent(SportsActivity.this,SportsActivity.class);
//            startActivity(intent);
            // 如果你觉得，直接跳出SportActivity很不爽
            // 那么，我们就制造个"1像素"惨案
            mScreenManager.startActivity();
        }

        @Override
        public void onUserPresent() {
            // 解锁，暂不用，保留
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        //setContentView(R.layout.activity_main);

        if (height >= 2400) {
            setContentView(R.layout.activity_setting);
            //Toast.makeText(getApplication(), "当前分辨率" + height + "×" + width, Toast.LENGTH_SHORT).show();
        } else if (height <= 1920 && height > 1600) {
            setContentView(R.layout.activity_setting_xdpi);
        } else if (height <= 1280 && height > 960) {
            setContentView(R.layout.activity_setting_xdpi);
        } else {
            setContentView(R.layout.activity_setting);
            //Toast.makeText(getApplication(), "暂未适配1280×720分辨率以下屏幕", Toast.LENGTH_SHORT).show();
        }

		version = (TextView) findViewById(R.id.version);       
        state_switch = (Switch) findViewById(R.id.state_switch);	
        version.setText("当前软件版本V" + getAppVersionName() + "（仅新版本提示更新）");


        SharedPreferences state = getSharedPreferences("state_switch", MODE_PRIVATE);
        state_swich = state.getString("state_switch", "");

        //设置Switch开关状态
        if (state_swich.equals("no")) {
            state_switch.setChecked(true);
            state_switch.setHint("开启");
        } else if (state_swich.equals("off")) {
            state_switch.setChecked(false);
            state_switch.setHint("关闭");
        }

    }


	private void stopPlayMusicService() {
        Intent intent = new Intent(SettingActivity.this, PlayerMusicService.class);
        stopService(intent);
    }

    private void startPlayMusicService() {
        Intent intent = new Intent(SettingActivity.this, PlayerMusicService.class);
        startService(intent);
    }

    private void startDaemonService() {
        Intent intent = new Intent(SettingActivity.this, DaemonService.class);
        startService(intent);
    }
    //停止service
    private void stopDaemonService() {
        Intent intent = new Intent(SettingActivity.this, DaemonService.class);
        stopService(intent);
    }

	@Override
    protected void onDestroy() {
        super.onDestroy();
        if (AppConstants.DEBUG)
            Log.d(TAG, "--->onDestroy");
        // stopRunTimer();
//        mScreenListener.stopScreenReceiverListener();
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



	//检测更新
	public void ver_sion(View v) {
        try {
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
                version1 = data.getInt("version");
                uplog = data.getString("uplog");
                upurl = data.getString("upurl");

                if (code == 200) {
                    if (version1 > Version) {
                        AlertDialog dialog=new AlertDialog.Builder(SettingActivity.this)
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
        } catch (Exception e) {}
    }
    
    public String getHtml(String path) throws Exception {  
        URL url = new URL(path);  
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();  
        conn.setRequestMethod("POST");  
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


	//反馈
	public void email_fk(View view) {
		Intent fk = new Intent();
		fk.setClass(this, FeedActivity.class);
		startActivity(fk);
	}

	//Docs
	public void help_api(View v) {
		Intent help = new Intent();
		help.setClass(this, HelpActivity.class);
		startActivity(help);
		Toast.makeText(this, "下版本待完善...", Toast.LENGTH_SHORT).show();
	}

	//白名单权限检测
	public void dc_qx(View v) {
		if (!isIgnoringBatteryOptimizations()) {

            Intent i = new Intent();
            //跳转到设置里面的电池优化管理列表
            i.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(i);
            Toast.makeText(getApplication(), "请勾选此应用\"不优化\"", Toast.LENGTH_LONG).show();
            /*弹窗模式
             Intent i = new Intent();
             i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
             //data为应用包名
             i.setData(Uri.parse("package:" + this.getPackageName()));
             startActivity(i);
             */
		} else {
			Toast.makeText(this, "已授权白名单权限!", Toast.LENGTH_SHORT).show();
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

	//保活服务
	public void service_start(View v) {

        // 1. 注册锁屏广播监听器
        mScreenListener = new ScreenReceiverUtil(this);
        mScreenManager = ScreenManager.getScreenManagerInstance(this);
        mScreenListener.setScreenReceiverListener(mScreenListenerer);
        // 2. 启动系统任务
		/*
         mJobManager = JobSchedulerManager.getJobSchedulerInstance(this);
         mJobManager.startJobScheduler();*/

		// 3. 启动前台Service
		startDaemonService();
		// 4. 启动播放音乐Service
		startPlayMusicService();

		Toast.makeText(SettingActivity.this, "服务启动成功!", Toast.LENGTH_SHORT).show();

	}



    //屏幕永亮
    public void state_swit(View v) {
        if (state_switch.isChecked()) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                //.setTitle("功能提示！")
                .setMessage("开启或关闭此功能将会在3秒后重启软件！\n开启之后便会自动调低软件窗口亮度并进入全屏模式（退出即可恢复！）\n重启后请不要关闭软件，保持Log日志面板即可否则无效！\n此功能仅适用于真机独立挂V免签监控的")
                .setCancelable(false)
                .setPositiveButton("我已知晓", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dia, int which) {
                        //创建并写入共享首选项
                        SharedPreferences.Editor editor = getSharedPreferences("state_switch", MODE_PRIVATE).edit();
                        editor.putString("state_switch", "no");
                        editor.commit();
                        state_switch.setHint("开启");
                        Toast.makeText(SettingActivity.this, "屏幕永亮开启成功，3S后重启...", Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){

                                @Override
                                public void run() {
                                    Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    System.exit(0);
                                    finish();

                                }
                            }, 3000);
                    }
                })
                .setNegativeButton("暂不开启", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dia, int which) {
                        state_switch.setChecked(false);//状态开关
                    }
                })
                .create();
            dialog.show();
        } else {
            //更新关闭事件
            SharedPreferences.Editor editor = getSharedPreferences("state_switch", MODE_PRIVATE).edit();
            editor.putString("state_switch", "off");
            editor.commit();
            state_switch.setHint("关闭");
            Toast.makeText(this, "屏幕永亮已关闭，1S后重启...", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){

                    @Override
                    public void run() {
                        Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                        startActivity(intent);
                        System.exit(0);
                        finish();

                    }
                }, 1000);
        }
    }



	public void vmq_Pro_gy(View v) {
		Toast.makeText(this, "下版本待完善...", Toast.LENGTH_SHORT).show();
	}


}

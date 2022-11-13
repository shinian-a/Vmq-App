package com.shinian.pay;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import java.util.List;
import java.net.NetworkInterface;
import java.util.Collections;
import android.annotation.SuppressLint;
import java.lang.reflect.Field;
import android.app.Application;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.io.File;
import android.content.pm.PackageInfo;
import java.security.Signature;
import java.lang.reflect.Method;
import android.widget.ImageView;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.os.Environment;
import java.io.FileOutputStream;
import android.content.pm.ResolveInfo;
import android.content.ClipboardManager;
import android.content.ClipData;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.io.OutputStream;
import java.net.ProtocolException;
import org.json.JSONObject;
import android.view.Window;
import android.view.WindowManager;

/*
  email：shiniana@qq.com
  qq：1614790395
  GitHub：https://github.com/shinian-a
  @©️版权所有
*/

public class MainActivity extends AppCompatActivity {

	private TextView txthost;
	private TextView txtkey;
	private boolean isOk = false;
	private static String TAG = "MainActivity";
	private static String host;
	private static String key;
	private int id = 0;
    //定义 Bitmap变量
    private Bitmap bitmap_image;
    private String html;
    private int code;
    private Double ver;
    private String info;
    private String show;
    private String url;
    //当前版本
    private double Ver = 1.3;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //沉浸式状态栏
        /*
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }*/
		//查找组件
		txthost = (TextView) findViewById(R.id.txt_host);
		txtkey = (TextView) findViewById(R.id.txt_key);


        //判断签名校验SHA-1 --检测是否重新签名
        //SignCheck signCheck = new SignCheck(this, "0F:85:43:C2:2E:8E:58:1B:96:88:C6:7C:1E:97:1E:0A:1C:55:D2:5F");
        //if (signCheck.check()) {
            //调用App方法检查更新
            App();
            //申请读写权限
            requestMyPermissions();

            //检测电池白名单是否启用
            if (!isIgnoringBatteryOptimizations()) {
                requestIgnoreBatteryOptimizations();
            }

            //检测通知使用权是否启用
            if (!isNotificationListenersEnabled()) {            

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
                qx.show();//显示
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
        /*} else {
            //TODO 签名错误
            AlertDialog.Builder qmjy = new AlertDialog.Builder(MainActivity.this);
            qmjy.setIcon(R.drawable.jg);
            qmjy.setTitle("警告：");
            qmjy.setMessage("当前您使用的是非官方正版软件！请于官方地址下载正版软件使用");
            qmjy.setCancelable(false); //设置是否可以点击对话框外部取消
            qmjy.setPositiveButton("前往下载", new DialogInterface.OnClickListener() {
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
			qmjy.show();//显示
        }*/
/*
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
        }*/
	}

    /**********************************主方法线程结束****************************************/ 


    //检测更新
    public void App() { 
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
                        ver = json.optDouble("version");
                        //获取版本信息
                        info = json.optString("version_info");
                        //获取更新内容
                        show = json.optString("app_update_show");
                        //获取更新地址
                        url = json.optString("app_update_url");                       
                        Log.i("jsonData", html);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //UI子线程 通过UI子线程实现线程切换
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (code == 200) {                                  
                                    if (ver > Ver) {                                    
                                        AlertDialog dialog=new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("发现新版本~")
                                            .setMessage(show)
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
                                    } else {
                                        Toast.makeText(MainActivity.this, "已是最新版本~", Toast.LENGTH_LONG).show();
                                    }
                                } else {                            
                                    Toast.makeText(MainActivity.this, "检测更新失败", Toast.LENGTH_LONG).show();                                     
                                }
                            }
                        });
                }
            });
        thread.start();//启动线程 
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

	//QQ群 key：AEdaVAI7Dvijz39bHJzWg7wtZCT7kTW8
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


	//检测电池白名单
	private boolean isIgnoringBatteryOptimizations() {

		boolean isIgnoring = false;

		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (powerManager != null) {
			isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
		}
		return isIgnoring;
	}

	//打开电池白名单
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
                exit.setMessage("确定退出程序吗？");
                exit.setCancelable(false); //设置是否可以点击对话框外部取消
                exit.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dia, int which) {
                            System.exit(0);
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

        switch (item.getItemId()) {
                //群聊
            case R.id.qun:
                if (joinQQGroup("AEdaVAI7Dvijz39bHJzWg7wtZCT7kTW8")) {
                    Toast.makeText(this, "正在跳转至反馈群...", Toast.LENGTH_SHORT).show();
                }
                return true;
                //分享软件             
            case R.id.share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                //分享内容
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                                    "V免签最新监控端修复原版支付宝-微信不回调bug长期维护项目下载：https://shinianacn.lanzouy.com/b027kqata 密码:vmq");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "分享"));
                return true;
                //打赏作者
            case R.id.support:
                /*                 
                 Intent intent2 = new Intent();
                 intent2.setClass(MainActivity.this, Pay.class);
                 startActivity(intent2);
                 */
                final String[] fruits = new String[] { "微信打赏", "支付宝打赏" };

                AlertDialog.Builder pay = new AlertDialog.Builder(this);
                pay.setIcon(R.drawable.pay);//图标
                pay.setTitle("选择您要打赏的方式~");//标题
                pay.setCancelable(true); //设置是否可以点击对话框外部取消
                //设置单选项目
                pay.setSingleChoiceItems(fruits, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String fs = fruits[which];
                            if (fs.equals("微信打赏")) {
                                Toast.makeText(MainActivity.this, "收款码已保存至相册,请选择相册收款码打赏~", Toast.LENGTH_LONG).show();
                                //获取本地图片bitmap
                                bitmap_image = BitmapFactory.decodeResource(getResources(), R.drawable.wxpay);
                                saveImage("微信打赏", bitmap_image);
                                //调用saveImageToGallery方法 传入上下文 bitmap
                                //SaveImageUtils.saveImageToGallery(MainActivity.this, bitmap_image);                                                      
                                //打开微信扫一扫
                                openWeixinToQE_Code(MainActivity.this);
                            } else if (fs.equals("支付宝打赏")) {
                                Toast.makeText(MainActivity.this, "正在打开支付宝...", Toast.LENGTH_SHORT).show();
                                //打开支付宝付款
                                Uri uri = Uri.parse("https://qr.alipay.com/fkx12542rpb5fljmhxlal35");
                                Intent intent = new Intent();
                                intent.setAction("android.intent.action.VIEW");
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        }
                    }).create();
                pay.show();
                return true;
                //关于
            case R.id.about:
                AlertDialog.Builder gy = new AlertDialog.Builder(this);
                gy.setIcon(R.drawable.gy);
                gy.setTitle("关于本软件：");
                gy.setCancelable(false); //设置是否可以点击对话框外部取消
                gy.setMessage("简介：\n这是一款基于V免签开发的免签支付接口App监控端\n修复了原版监控支付宝不回调等bug长期维护目前免费使用\n在您使用本软件前请注意：\n该软件版权归作者所有请勿破解倒卖本软件\n部分申请的权限是必要的拒绝将导致监控功能失效\n喜欢本项目就赞助一下开发者吧~\nPS：使用本软件建议开启自启动权限！\n\nApp使用协议：\n\n说明：\n该软件由十年*开发并提供技术服务支持免费使用\n1、所有用户在下载并浏览V免签监控端_Pro时均被视为已经仔细阅读本条款并完全同意。\n2、软件完全免费可自由使用并分享，您可以将它分享给您的朋友们使用。\n3、使用该软件应当遵守法律法规若侵犯了第三方知识产权或其他权益需本人承担全部责任。\n作者对此不承担任何责任。\n4、V免签监控端_Pro需要获取一定的应用权限例如内存读写，通知监听等请务必开启权限否则软件无法正常运行。\n5、如果您使用的是盗版软件，出现的一切风险作者对此不承担任何责任。\n6、用户明确并同意因其使用本App而产生的一切后果由其本人承担，作者对此不承担任何责任。\n7、我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。\n\n\"确保您已同意以上协议否则请卸载本软件！\"")

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
                            help.setClass(MainActivity.this, Help.class);
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
                //结束程序
            case R.id.exit:
                //finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
        }
        return super.onOptionsItemSelected(item);
	}

    /**
     * 保存图片到本地
     *
     * @param name   图片的名字，比如传入“123”，最终保存的图片为“123.jpg”
     * @param bitmap    本地图片或者网络图片转成的Bitmap格式的文件
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
            //Toast.makeText(this, "保存成功", Toast.LENGTH_LONG).show();
            Uri localUri = Uri.fromFile(file);
            Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
            sendBroadcast(localIntent);
        } catch (IOException e) {
            //获取本地图片bitmap 二次获取
            bitmap_image = BitmapFactory.decodeResource(getResources(), R.drawable.wxpay);
            SaveImageUtils.saveImageToGallery(MainActivity.this, bitmap_image);
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
    public static void openWeixinToQE_Code(Context context) {
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
    public static void toWeChatScanDirect(Context context) {
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
    public static boolean isQQClientAvailable(Context context) {  
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
    public static boolean isValidIntent(Context context, Intent intent) {
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
                    help.setClass(MainActivity.this, Help.class);
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
                    Toast.makeText(MainActivity.this, "*心跳正常*返回：" + response.body().string(), Toast.LENGTH_LONG).show();
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
                .setContentTitle("V免签测试推送").setContentText("测试推送信息，如果程序正常，则会提示监听权限正常").build();
        } else {
            mNotification = new Notification.Builder(MainActivity.this).setSmallIcon(R.drawable.ic_launcher)
                .setTicker("测试推送信息，如果程序正常，则会提示监听权限正常").setContentTitle("V免签测试推送")
                .setContentText("测试推送信息，如果程序正常，则会提示监听权限正常").build();
        }

        //Toast.makeText(MainActivity.this, "已推送信息，如果权限，那么将会有下一条提示！", Toast.LENGTH_SHORT).show();

        mNotificationManager.notify(id++, mNotification);
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

    //MD5取值
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
    //动态申请读写权限
    private void requestMyPermissions() {

        if (ContextCompat.checkSelfPermission(this,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有写SD权限");
        }
        if (ContextCompat.checkSelfPermission(this,
                                              Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有读SD权限");
        }
    }



    //各种权限的判断
    private void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, PayNotificationListenerService.class),
                                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(context, PayNotificationListenerService.class),
                                      PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        //Toast.makeText(MainActivity.this, "监听服务启动中...", Toast.LENGTH_SHORT).show();
    }

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

}

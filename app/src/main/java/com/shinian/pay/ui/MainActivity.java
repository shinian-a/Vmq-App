package com.shinian.pay.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.activity.CaptureActivity;
import com.shinian.pay.R;
import com.shinian.pay.helper.BatteryHelper;
import com.shinian.pay.helper.NotificationHelper;
import com.shinian.pay.helper.PermissionHelper;
import com.shinian.pay.helper.UpdateChecker;
import com.shinian.pay.manager.AppConstants;
import com.shinian.pay.manager.MonitorLogManager;
import com.shinian.pay.service.ForeService;
import com.shinian.pay.utils.ApkShareHelper;
import com.shinian.pay.utils.CryptoUtils;
import com.shinian.pay.utils.TimeUtils;
import io.dcloud.ads.core.entry.DCloudAdSlot;
import io.dcloud.ads.core.v2.reward.DCRewardAd;
import io.dcloud.ads.core.v2.reward.DCRewardAdListener;
import io.dcloud.ads.core.v2.reward.DCRewardAdLoadListener;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * email：shiniana@qq.com
 * qq：1614790395
 * GitHub：https://github.com/shinian-a
 *
 * @©️版权所有
 */

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener {

    private TextView txthost, txtkey, logsTextView, batteryTextView;
    private SharedPreferences configSp, logsSp;
    private BatteryHelper batteryHelper;
    private NotificationHelper notificationHelper;
    private OkHttpClient okHttpClient;
    private boolean rewardAdLoading = false;
    public static Handler monitorLogHandler;
    private static final String TAG = "MainActivity";


    // 注册 Launcher 处理扫码回调函数
    private final ActivityResultLauncher<Intent> qrCodeLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String qrResult = result.getData()
                                    .getStringExtra(AppConstants.INTENT_EXTRA_KEY_QR_SCAN);
                            if (qrResult != null) {
                                parseAndSaveConfig(qrResult);
                            }
                        }
                    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.activity_main);

        initViews();
        initHelpers();
        loadConfig();
        checkDependencies();
        checkNotificationPermission();
        applyScreenSettings();
        restoreLogs();

        monitorLogHandler = new Handler(Looper.getMainLooper(), msg -> {
            if (msg.what == 0 && msg.getData() != null) {
                String str = msg.getData().getString("logsStr", "");
                if (logsTextView != null) logsTextView.setText(str);
            }
            return true;
        });

        UpdateChecker.checkAsync(this, new UpdateChecker.Callback() {
            @Override public void onNewVersion(String ver, int code, String log, String url) {
                showUpdateDialog(log, url);
            }
            @Override public void onAlreadyLatest() { /* 静默 */ }
            @Override public void onError(String msg) { Log.w("Update", msg); }
        });

        Log.d("MainActivity", "========== onCreate 完成 ==========");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(requestCode, perms, grants);

        if (grants.length == 0) return;

        switch (requestCode) {
            case AppConstants.REQ_PERM_CAMERA:
                if (grants[0] == PackageManager.PERMISSION_GRANTED) {
                    startQrCode(null);
                } else {
                    Toast.makeText(this, "相机权限被拒绝，扫码功能不可用", Toast.LENGTH_LONG).show();
                }
                break;

            case AppConstants.REQ_PERM_NOTIFICATION:
                if (grants[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show();
                    initListenerServiceOnce();
                } else {
                    // 用户拒绝了通知权限
                    boolean dontAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.POST_NOTIFICATIONS);

                    if (dontAskAgain) {
                        // 用户勾选了"不再询问"，引导去设置页手动开启
                        new AlertDialog.Builder(this)
                                .setIcon(R.drawable.menu_gy)
                                .setTitle("通知权限被永久拒绝")
                                .setMessage("您已选择不再询问通知权限。\n请前往系统设置手动开启，否则前台服务通知无法显示。")
                                .setPositiveButton("前往设置", (d, w) -> {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                })
                                .setNegativeButton("暂不开启", null)
                                .show();
                    } else {
                        Toast.makeText(this, "通知权限被拒绝，前台服务通知将无法显示", Toast.LENGTH_LONG).show();
                    }
                }
                break;

            default:
                if (grants[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限被拒绝，功能不可用", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        batteryHelper.stop();
        // 清理日志
        if (monitorLogHandler != null) {
            monitorLogHandler.removeCallbacksAndMessages(null);
            monitorLogHandler = null;
        }
    }

    // ========== 初始化 ==========

    private void initViews() {
        txthost = findViewById(R.id.txt_host);
        txtkey = findViewById(R.id.txt_key);
        logsTextView = findViewById(R.id.state_logs);
        batteryTextView = findViewById(R.id.sj_dl);

        logsTextView.setOnLongClickListener(this);

        TextView copyright = findViewById(R.id.bq);
        if (copyright != null) {
            copyright.setPaintFlags(copyright.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    private void initHelpers() {
        configSp = getSharedPreferences(AppConstants.SP_NAME_CONFIG, MODE_PRIVATE);
        logsSp = getSharedPreferences(AppConstants.SP_NAME_LOGS, MODE_PRIVATE);
        batteryHelper = new BatteryHelper(this);
        notificationHelper = new NotificationHelper(this);
        okHttpClient = new OkHttpClient();
        ApkShareHelper.cleanCache(this);
    }

    private void loadConfig() {
        String host = configSp.getString(AppConstants.SP_KEY_HOST, "");
        String key = configSp.getString(AppConstants.SP_KEY_KEY, "");
        if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(key)) {
            txthost.setText(" 通知地址：" + host);
            txtkey.setText(" 通讯密钥：" + key);
        }
    }

    // 启动前台服务
    private void startForegroundServiceDelayed() {
        // 用 WeakReference 防止内存泄漏
        WeakReference<Context> weakRef = new WeakReference<>(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Context context = weakRef.get();
            if (context == null) return; // Activity 已销毁

            // 如果是 Activity，检查是否还存活
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (activity.isFinishing() || activity.isDestroyed()) return;
            }

            try {
                Intent intent = new Intent(context, ForeService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } catch (Exception e) {
                // Android 12+: ForegroundServiceStartNotAllowedException
                // Android 14+: SecurityException (缺少类型权限)
                Log.e("ForeService", "启动失败: " + e.getClass().getSimpleName(), e);
                Toast.makeText(context, "服务启动失败，请检查权限", Toast.LENGTH_SHORT).show();
            }
        }, AppConstants.FORE_SERVICE_START_DELAY);
    }

    private boolean hasCheckedDependencies = false;

    private void checkDependencies() {
        if (hasCheckedDependencies) return;
        hasCheckedDependencies = true;

        List<String> missing = new ArrayList<>();
        if (!PermissionHelper.isAppInstalled(this, AppConstants.PKG_WECHAT)) {
            missing.add("微信");
        }
        if (!PermissionHelper.isAppInstalled(this, AppConstants.PKG_ALIPAY)) {
            missing.add("支付宝");
        }
        if (!missing.isEmpty()) {
            String msg = "未检测到" + TextUtils.join("、", missing) + "，可能无法正常监听收款";
            Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
            Log.w("Package", msg);
        }
    }

    //
    // MainActivity 类成员变量
    private boolean isServiceInitialized = false;

    /**
     * 安全地初始化监听服务，整个生命周期只执行一次
     */
    private void initListenerServiceOnce() {
        if (isServiceInitialized) return;
        isServiceInitialized = true;

        // 启动前台服务
        startForegroundServiceDelayed();

        // 检查并启动 NLS
        if (!notificationHelper.isListenerEnabled()) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.menu_gy)
                    .setTitle("温馨提示")
                    .setMessage("未授权通知读取权限，请前往授权后继续操作")
                    .setCancelable(false)
                    .setPositiveButton("前往授权", (d, w) ->
                            notificationHelper.openListenerSettings(this))
                    .setNeutralButton("暂不授权", (d, w) ->
                            Toast.makeText(this, "请给予监听权限，否则无法运行", Toast.LENGTH_LONG).show())
                    .show();
        } else {
            notificationHelper.toggleListenerService();
        }
    }

    private void checkNotificationPermission() {
        // ========== Android 13+ 运行时通知权限 ==========
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    // 需要向用户解释为什么需要权限
                    new AlertDialog.Builder(this)
                            .setIcon(R.drawable.menu_gy)
                            .setTitle("需要通知权限")
                            .setMessage("本应用需要通过通知来展示前台服务状态和收款监听结果。\n\n" +
                                    "没有此权限，前台服务将无法正常运行。")
                            .setCancelable(false)
                            .setPositiveButton("授予权限", (d, w) ->
                                    ActivityCompat.requestPermissions(this,
                                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                            AppConstants.REQ_PERM_NOTIFICATION))
                            .setNegativeButton("暂不授予", (d, w) ->
                                    Toast.makeText(this, "未授予通知权限，部分功能可能受限", Toast.LENGTH_LONG).show())
                            .show();
                } else {
                    // 首次请求或用户勾选了"不再询问"
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            AppConstants.REQ_PERM_NOTIFICATION);
                }

                // ✅ 关键：权限尚未确认，直接返回，不初始化服务
                return;
            }
        }
        initListenerServiceOnce();
    }

    private void applyScreenSettings() {
        String state = getSharedPreferences(AppConstants.SP_NAME_STATE, MODE_PRIVATE)
                .getString(AppConstants.SP_KEY_STATE_SWITCH, "");
        if ("no".equals(state)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            batteryHelper.startPeriodicUpdate(batteryTextView, AppConstants.BATTERY_UI_UPDATE_INTERVAL);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = 5f / 255f;
                getWindow().setAttributes(lp);
            }, AppConstants.SCREEN_DIM_DELAY);
        }
    }

    private void restoreLogs() {
        String logs = logsSp.getString(AppConstants.SP_KEY_LOGS_STR, "");
        logsTextView.setText(TextUtils.isEmpty(logs) ? "日志：null" : logs);
    }

    // ========== onClick绑定 ==========

    public void admin_url(View v) {
        String text = txthost.getText().toString();
        if (text.startsWith(" 通知地址：") && text.length() > 6) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + text.substring(6))));
        } else {
            Toast.makeText(this, "请先配置数据！", Toast.LENGTH_LONG).show();
        }
    }

    public void doInput(View v) {
        EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("请输入配置数据")
                .setView(input)
                .setIcon(R.drawable.icon_pzsj)
                .setCancelable(false)
                .setNeutralButton("如何配置?", (d, w) ->
                        startActivity(new Intent(this, HelpActivity.class)))
                .setNegativeButton("取消", null)
                .setPositiveButton("确认", (d, w) -> parseAndSaveConfig(input.getText().toString()))
                .show();
    }
    public void startQrCode(View v) {
        if (!PermissionHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, AppConstants.REQ_PERM_CAMERA);
            return;
        }
        qrCodeLauncher.launch(new Intent(this, CaptureActivity.class));
    }

    public void doStart(View view) {
        String host = configSp.getString(AppConstants.SP_KEY_HOST, "");
        String key = configSp.getString(AppConstants.SP_KEY_KEY, "");
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(key)) {
            Toast.makeText(this, "请先配置!", Toast.LENGTH_SHORT).show();
            return;
        }
        String t = String.valueOf(System.currentTimeMillis());
        String sign = CryptoUtils.md5(t + key);
        Request request = new Request.Builder()
                .url("http://" + host + "/appHeart?t=" + t + "&sign=" + sign)
                .get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "心跳失败，请检查配置", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response responseToClose = response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        MonitorLogManager.appendLog(MainActivity.this,
                                TimeUtils.now() + "\r\r\r\r心跳请求失败：HTTP " + response.code());
                        return;
                    }
                    JSONObject result = new JSONObject(response.body().string());
                    int code = result.getInt("code");
                    String msg = result.getString("msg");
                    String ts = TimeUtils.now();
                    MonitorLogManager.appendLog(MainActivity.this,ts + "\r\r\r\r心跳返回：" + msg);
                } catch (JSONException e) {
                    MonitorLogManager.appendLog(MainActivity.this,TimeUtils.now() + "\r\r\r\r心跳解析错误：" + e.getMessage());
                }
            }
        });
    }

    public void checkPush(View v) {
        notificationHelper.sendTestNotification();
    }

    public void Logs(View v) {
        logsSp.edit().putString(AppConstants.SP_KEY_LOGS_STR, "").apply();
        logsTextView.setText("日志：null");
    }

    public void dl(View v) {
        batteryTextView.setText("当前电量：" + batteryHelper.getLevel() + "%");
    }

    public void author() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(AppConstants.AUTHOR_QQ_GROUP_URL));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "未找到可打开QQ群链接的应用", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "打开QQ群失败", e);
            Toast.makeText(this, "QQ群打开失败，请稍后重试", Toast.LENGTH_SHORT).show();
        }
    }

    public void openAuthorWebsite(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.AUTHOR_WEBSITE)));
    }

    private void parseAndSaveConfig(String raw) {
        String[] parts = raw.split("/");
        if (parts.length != 2) {
            Toast.makeText(this, "数据格式错误!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (parts[0].contains("localhost")) {
            Toast.makeText(this, "请使用局域网IP而非localhost", Toast.LENGTH_LONG).show();
            return;
        }
        // 验证连通性
        String t = String.valueOf(System.currentTimeMillis());
        String sign = CryptoUtils.md5(t + parts[1]);
        Request req = new Request.Builder()
                .url("http://" + parts[0] + "/appHeart?t=" + t + "&sign=" + sign)
                .get().build();
        okHttpClient.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                Log.w(TAG, "配置地址连通性检查失败", e);
            }
            @Override public void onResponse(@NonNull Call c, @NonNull Response r) {
                try (Response response = r) {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "配置地址返回 HTTP " + response.code());
                    }
                }
            }
        });

        PermissionHelper.requestIgnoreBatteryOptimization(this);
        txthost.setText(" 通知地址：" + parts[0]);
        txtkey.setText(" 通讯密钥：" + parts[1]);
        configSp.edit()
                .putString(AppConstants.SP_KEY_HOST, parts[0])
                .putString(AppConstants.SP_KEY_KEY, parts[1])
                .apply();
    }


    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.state_logs) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("log", logsTextView.getText()));
            Toast.makeText(this, "日志已复制", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    // ========== 菜单 ==========


    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);


        if (menu instanceof androidx.appcompat.view.menu.MenuBuilder) {
            try {
                java.lang.reflect.Method method = menu.getClass()
                        .getDeclaredMethod("setOptionalIconsVisible", boolean.class);
                method.setAccessible(true);
                method.invoke(menu, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.qun) {
            author();
            return true;
        } else if (id == R.id.share) {
            ApkShareHelper.share(this);
            return true;
        } else if (id == R.id.support) {
            showRewardAd();
            return true;
        } else if (id == R.id.about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.setting) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        } else if (id == R.id.exit) {
            exitApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ========== 返回键 ==========

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this)
                    .setTitle("温馨提示")
                    .setIcon(R.drawable.menu_exit)
                    .setMessage("确定退出？退出将无法正常监听!")
                    .setCancelable(false)
                    .setPositiveButton("确定", (d, w) -> exitApp())
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ========== 私有方法 ==========

    private void showUpdateDialog(String log, String url) {
        new AlertDialog.Builder(this)
                .setTitle("发现新版本！")
                .setMessage(log)
                .setIcon(R.drawable.app_gx)
                .setCancelable(false)
                .setPositiveButton("立即更新", (d, w) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
                .setNeutralButton("忽略", null)
                .show();
    }

    private void showRewardAd() {
        if (rewardAdLoading) {
            Toast.makeText(this, "广告正在加载，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        rewardAdLoading = true;
        DCRewardAd ad = new DCRewardAd(this);
        ad.setRewardAdListener(new DCRewardAdListener() {
            @Override public void onReward(JSONObject j) {
                Log.d(TAG, "Reward ad completed.");
            }
            @Override public void onShow() {}
            @Override public void onClick() {}
            @Override public void onVideoPlayEnd() {}
            @Override public void onSkip() {
                rewardAdLoading = false;
            }
            @Override public void onClose() {
                rewardAdLoading = false;
            }
            @Override public void onShowError(int code, String message) {
                rewardAdLoading = false;
                Log.e(TAG, "Reward ad show failed: code=" + code + ", msg=" + message);
                Toast.makeText(MainActivity.this, "广告暂时无法播放，请稍后再试", Toast.LENGTH_SHORT).show();
            }
        });
        DCloudAdSlot slot = new DCloudAdSlot.Builder().adpid(AppConstants.AD_REWARD_ADPID).build();
        ad.load(slot, new DCRewardAdLoadListener() {
            @Override public void onRewardAdLoad() { ad.show(MainActivity.this); }
            @Override public void onError(int code, String message, @Nullable JSONArray detail) {
                rewardAdLoading = false;
                Log.e(TAG, "Reward ad load failed: code=" + code + ", msg=" + message);
                Toast.makeText(MainActivity.this, "广告暂时无法加载，请稍后再试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.menu_gy)
                .setTitle("关于本软件")
                .setCancelable(false)
                .setMessage("简介：\n这是一款基于V免签开发的免RootApp监控端\n维护支持新API对个人免费使用\n在您使用本软件前请注意：\n该软件版权归作者所有请勿破解倒卖本软件\n部分申请的权限是必要的拒绝将导致监控功能失效\n本软件承诺不会读取并保存您的隐私\nPS：使用本软件建议开启软件自启动权限和电池优化\n\nApp使用协议：\n\n说明：\n软件发布GItHub开源项目Vmq_App\n1、所有用户在下载并浏览本软件时均被视为已经仔细阅读本条款并完全同意。\n2、软件完全免费可自由使用学习修改源代码并分享\n3、使用该软件应当遵守法律法规若侵犯了第三方知识产权或其他权益需本人承担全部责任。\n作者对此不承担任何责任。\n4、V免签监控端_Pro需要获取相应的应用权限进行工作，若不给予权限将不能使用\n5、如果您使用的是盗版软件，出现的一切风险作者对此不承担任何责任\n6、用户明确并同意因其使用本App而产生的一切后果由其本人承担，作者对此不承担任何责任。\n7、我们深知个人隐私对您的重要性，并会尽全力保护您的个人信息安全可靠\n\n\n确保您已同意以上协议否则请卸载本软件！\n\n正版仅在GitHub或博客shinian-a.github.io或群聊发布，其余全部视为盗版\n\n")
                .setNegativeButton("不同意", (d, w) -> finishAffinity())
                .setPositiveButton("已阅读并同意", null)
                .setNeutralButton("配置文档", (d, w) ->
                        startActivity(new Intent(this, HelpActivity.class)))
                .show();
    }

    private void exitApp() {
        stopService(new Intent(this, ForeService.class));
        notificationHelper.toggleListenerService();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancelAll();
        Toast.makeText(this, "正在退出...", Toast.LENGTH_SHORT).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        }, AppConstants.EXIT_DELAY);
    }


}

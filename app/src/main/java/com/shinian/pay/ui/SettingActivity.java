package com.shinian.pay.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkInfo;

import com.shinian.pay.R;
import com.shinian.pay.utils.AutoStartHelper;
import com.shinian.pay.worker.KeepAliveWorkManager;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = "SettingActivity";
    private static final int HTTP_TIMEOUT = 8000;
    private static final int REQUEST_CODE_BATTERY = 1002;

    private TextView versionTv;
    private Switch stateSwitch;
    private AlertDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 标记：电池优化请求返回后是否需要弹自启动引导 */
    private boolean mPendingAutoStartGuide = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_setting);

        versionTv = findViewById(R.id.version);
        stateSwitch = findViewById(R.id.state_switch);
        versionTv.setText("当前软件版本 V" + getVersionName());

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        String state = getSharedPreferences("state_switch", MODE_PRIVATE)
                .getString("state_switch", "");
        boolean enabled = "no".equals(state);
        stateSwitch.setChecked(enabled);
        stateSwitch.setHint(enabled ? "开启" : "关闭");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading();
        mainHandler.removeCallbacksAndMessages(null);
    }

    // ========== 保活服务==========

    public void service_start(View v) {
        // ① WorkManager 调度周期任务
        KeepAliveWorkManager.schedule(this, 15, false, false, false);

        // 观察任务状态
        KeepAliveWorkManager.observeStatus(this).observe(this, workInfos -> {
            if (workInfos != null && !workInfos.isEmpty()) {
                WorkInfo info = workInfos.get(0);
                Log.d(TAG, "任务状态: " + info.getState());
            }
        });

        // ② 分步引导：先电池优化，再自启动
        requestBatteryThenAutoStart();

        Toast.makeText(this, "服务启动成功!", Toast.LENGTH_SHORT).show();
    }

    /**
     * 分步引导：电池优化 → 自启动
     * 避免双弹窗叠加 + 防止Activity销毁崩溃
     */
    private void requestBatteryThenAutoStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_BATTERY);
                    mPendingAutoStartGuide = true;
                    return; // ← 关键：return 等待回调，不立即弹自启动
                } catch (Exception e) {
                    Log.e(TAG, "电池优化请求失败，降级到自启动引导", e);
                }
            }
        }
        // 电池已白名单或不支持 → 直接弹自启动
        showAutoStartGuideSafely();
    }

    /**
     * 电池优化页面返回后的回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BATTERY) {
            mPendingAutoStartGuide = false;
            // 延迟300ms，等系统弹窗完全消失后再弹自启动引导
            mainHandler.postDelayed(() -> showAutoStartGuideSafely(), 300);
        }
    }

    /**
     * onResume 兜底：部分ROM不触发 onActivityResult
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mPendingAutoStartGuide) {
            mPendingAutoStartGuide = false;
            mainHandler.postDelayed(() -> showAutoStartGuideSafely(), 500);
        }
    }

    /**
     * 安全地显示自启动引导弹窗
     */
    private void showAutoStartGuideSafely() {
        if (!isFinishing() && !isDestroyed()) {
            AutoStartHelper.showGuideDialog(this);
        } else {
            Log.w(TAG, "Activity已销毁，跳过自启动引导弹窗");
        }
    }

    // ========== 屏幕永亮开关（不变） ==========

    public void state_swit(View v) {
        if (stateSwitch.isChecked()) {
            new AlertDialog.Builder(this)
                    .setMessage("开启或关闭此功能将会在 2 秒后重启软件！\n" +
                            "开启之后便会自动调低软件窗口亮度并进入全屏模式（退出即可恢复！）\n" +
                            "重启后请不要关闭软件，保持 Log 日志面板即可否则无效！\n" +
                            "此功能仅适用于真机独立挂 V 免签监控的")
                    .setCancelable(false)
                    .setPositiveButton("我已知晓", (d, w) -> saveStateAndRestart("no", "开启", 2000))
                    .setNegativeButton("暂不开启", (d, w) -> stateSwitch.setChecked(false))
                    .show();
        } else {
            saveStateAndRestart("off", "关闭", 1500);
        }
    }

    private void saveStateAndRestart(String value, String hint, int delay) {
        getSharedPreferences("state_switch", MODE_PRIVATE).edit()
                .putString("state_switch", value).apply();
        stateSwitch.setHint(hint);
        Toast.makeText(this, "屏幕永亮" + hint + "成功，" + (delay / 1000) + "S 后重启...",
                Toast.LENGTH_SHORT).show();
        mainHandler.postDelayed(() -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            } else {
                Toast.makeText(this, "重启失败，请手动重启", Toast.LENGTH_LONG).show();
            }
        }, delay);
    }

    // ========== 检查更新（不变） ==========

    public void ver_sion(View v) {
        showLoading();
        new Thread(() -> {
            boolean hasNew = checkUpdate();
            runOnUiThread(() -> {
                dismissLoading();
                if (!hasNew) Toast.makeText(this, "已经是最新版本", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private boolean checkUpdate() {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        BufferedReader reader = null;
        try {
            String verCode = getVersionCode();
            if (verCode.isEmpty()) return false;

            int currentVer = Integer.parseInt(verCode);
            conn = (HttpURLConnection) new URL("http://w.t3yanzheng.com/A729B02347E855EC").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(HTTP_TIMEOUT);
            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setDoOutput(true);

            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes("ver=" + URLEncoder.encode(verCode, "UTF-8"));
            dos.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return false;

            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject data = new JSONObject(sb.toString().trim());
            if (data.optInt("code") != 200) return false;

            int remoteVer = data.optInt("version", 0);
            if (remoteVer > currentVer) {
                String uplog = data.optString("uplog", "");
                String upurl = data.optString("upurl", "");
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("发现新版本！").setMessage(uplog).setIcon(R.drawable.app_gx)
                        .setCancelable(false)
                        .setPositiveButton("立即更新", (d, w) ->
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upurl))))
                        .setNeutralButton("忽略更新", null).show());
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "检查更新异常", e);
        } finally {
            try { if (dos != null) dos.close(); } catch (IOException ignored) {}
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    // ========== 其他UI事件（不变） ==========

    public void email_fk(View v) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/shinian-a/Vmq-App/issues")));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show();
        }
    }

    public void help_api(View v) {
        startActivity(new Intent(this, HelpActivity.class));
    }

    public void dc_qx(View v) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null
                && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Toast.makeText(this, "已授权白名单权限!", Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            Toast.makeText(this, "请勾选此应用\"不优化\"", Toast.LENGTH_LONG).show();
        }
    }

    public void vmq_Pro_gy(View v) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    // ========== 工具方法（不变） ==========

    private String getVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName != null ? info.versionName : "未知版本";
        } catch (PackageManager.NameNotFoundException e) {
            return "未知版本";
        }
    }

    private String getVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return String.valueOf(info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void showLoading() {
        if (loadingDialog == null || !loadingDialog.isShowing()) {
            loadingDialog = new AlertDialog.Builder(this)
                    .setMessage("正在检查更新...").setCancelable(false).create();
            loadingDialog.show();
        }
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}
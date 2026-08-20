package com.shinian.pay.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.shinian.pay.R;
import io.dcloud.ads.core.DCloudAdManager;
import io.dcloud.ads.core.entry.SplashConfig;
import io.dcloud.ads.core.v2.splash.DCSplashAd;
import io.dcloud.ads.core.v2.splash.DCSplashAdListener;
import io.dcloud.ads.core.v2.splash.DCSplashAdLoadListener;
import org.json.JSONArray;

public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";
    private static final String KEY_FROM_MAIN = "fromMain";
    private static final long AD_LOAD_TIMEOUT_MS = 5000L;

    private boolean fromMain = false;
    private boolean hasNavigated = false;

    // ====== 广告点击 & 返回状态管理 ======
    private boolean isAdClicked = false;        // 用户点击了广告（正在外部浏览）
    private boolean adLifecycleEnded = false;   // 广告生命周期已结束（倒计时完/关闭/播完）
    private boolean returnedFromClick = false;  // 用户已从广告点击中返回

    private DCSplashAd splashAd;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        Log.w(TAG, "Ad load timeout, navigating to main.");
        toMain();
    };

    // ==================== 生命周期 ====================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemBars();

        setContentView(R.layout.activity_splash);

        if (savedInstanceState != null) {
            fromMain = savedInstanceState.getBoolean(KEY_FROM_MAIN, false);
        } else {
            Intent intent = getIntent();
            if (intent != null) {
                fromMain = "MAIN".equals(intent.getStringExtra("FROM"));
            }
        }

        initAdSdk();
        loadAndShowAd();
    }


    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();

        if (isAdClicked) {
            // 用户刚从广告落地页/下载页回来
            isAdClicked = false;
            returnedFromClick = true;

            if (adLifecycleEnded) {
                // 场景A：广告在用户离开期间已经结束了 → 跳主页
                Log.d(TAG, "Returned from click, ad already ended → navigate.");
                toMain();
            } else {
                // 场景B：==--
                Log.d(TAG, "Returned from click, ad still showing → stay.");
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FROM_MAIN, fromMain);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (splashAd != null) {
            splashAd.setSplashAdListener(null);
            splashAd = null;
        }
    }

    // ==================== 广告初始化 ====================

    private void initAdSdk() {
        DCloudAdManager.InitConfig config = new DCloudAdManager.InitConfig();
        config.setAppId("NADB05D59").setAdId("124038290811");
        DCloudAdManager.init(this, config);

        DCloudAdManager.setPrivacyConfig(new DCloudAdManager.PrivacyConfig() {
            @Override public boolean isAdult()              { return true; }
            @Override public boolean isCanUsePhoneState()   { return true; }
            @Override public boolean isCanUseStorage()      { return true; }
            @Override public boolean isCanUseLocation()     { return true; }
            @Override public boolean isCanUseWifiState()    { return true; }
            @Override public boolean isCanGetInstallAppList(){ return true; }
            @Override public boolean isCanGetRunningApps()  { return true; }
            @Override public boolean isCanGetMacAddress()   { return true; }
            @Override public boolean isCanGetAndroidId()    { return true; }
            @Override public boolean isCanGetOAID()         { return true; }
            @Override public boolean isCanGetIP()           { return true; }
            @Override public boolean isCanUseSensor()       { return true; }
            @Override public boolean isCanUseSimOperator()  { return true; }
            @Override public boolean isCanUseRecordPermission(){ return true; }
        });
    }

    // ==================== 广告加载与展示 ====================

    private void loadAndShowAd() {
        RelativeLayout container = findViewById(R.id.splashContainer);
        if (container == null) {
            toMain();
            return;
        }

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        splashAd = new DCSplashAd(this);
        SplashConfig config = new SplashConfig.Builder()
                .width(screenWidth)
                .height(screenHeight)
                .build();

        splashAd.setSplashAdListener(new DCSplashAdListener() {
            @Override
            public void onShow() {
                Log.d(TAG, "Ad shown successfully.");
                cancelTimeout();
            }

            @Override
            public void onClick() {
                Log.d(TAG, "Ad clicked.");
                isAdClicked = true;
                returnedFromClick = false; // 重置返回标记
            }

            @Override
            public void onVideoPlayEnd() {
                Log.d(TAG, "Video play end.");
                handleAdEnd();
            }

            @Override
            public void onSkip() {
                Log.d(TAG, "Skip clicked.");
                handleAdEnd();
            }

            @Override
            public void onClose() {
                Log.d(TAG, "Ad closed.");
                handleAdEnd();
            }

            @Override
            public void onShowError(int code, String msg) {
                Log.e(TAG, "Ad show error: code=" + code + ", msg=" + msg);
                handleAdEnd();
            }
        });

        // 启动超时兜底
        timeoutHandler.postDelayed(timeoutRunnable, AD_LOAD_TIMEOUT_MS);

        splashAd.load(config, new DCSplashAdLoadListener() {
            @Override
            public void onSplashAdLoad() {
                cancelTimeout();
                if (splashAd != null && splashAd.isValid()) {
                    splashAd.showIn(container);
                } else {
                    toMain();
                }
            }

            @Override
            public void redBag(@Nullable View view, @Nullable FrameLayout.LayoutParams layoutParams) {}

            @Override
            public void onError(int code, String message, @Nullable JSONArray detail) {
                cancelTimeout();
                Log.e(TAG, "Ad load failed: code=" + code + ", msg=" + message);
                toMain();
            }
        });
    }

    // ==================== 核心跳转逻辑 ====================

    /**
     * 广告结束统一入口（倒计时完/跳过/关闭/播放结束）
     *
     * 判断逻辑：
     * - 如果用户正在外部浏览广告（isAdClicked=true）→ 只标记结束，不跳转
     * - 否则 → 直接跳主页
     */
    private void handleAdEnd() {
        adLifecycleEnded = true; // 无论如何都标记广告已结束

        if (isAdClicked && !returnedFromClick) {
            // 用户还在外部（应用商店/浏览器），不能跳转
            // 等 onResume 中处理
            Log.d(TAG, "Ad ended but user is in external app, defer navigation.");
            return;
        }

        // 正常情况：用户在前台，直接跳转
        toMain();
    }

    private void toMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        cancelTimeout();

        if (isFinishing() || isDestroyed()) return;

        if (!fromMain) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        finish();
        overridePendingTransition(0, 0);
    }

    private void cancelTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }

    // 沉浸全屏
    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            controller.hide(WindowInsetsCompat.Type.systemBars());
        }
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }
}
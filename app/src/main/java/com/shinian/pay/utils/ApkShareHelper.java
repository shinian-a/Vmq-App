package com.shinian.pay.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * APK 分享工具类（兼容 Android 7.0 ~ 16）
 *
 * 使用前提：
 * 1. AndroidManifest.xml 中已配置 FileProvider，authorities = "${applicationId}.fileprovider"
 * 2. res/xml/file_paths.xml 中包含 <cache-path name="apk_cache" path="." />
 *
 * 用法：
 *   ApkShareHelper.share(activity);
 */
public final class ApkShareHelper {

    private static final String TAG = "ApkShareHelper";
    private static final String CACHE_DIR_NAME = "cache";
    private static final String FALLBACK_URL = "https://shinian-a.github.io/";

    private ApkShareHelper() {
    }

    /**
     * 分享自身 APK，失败时自动降级为分享下载链接
     */
    public static void share(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        try {
            shareApkInternal(activity);
        } catch (Exception e) {
            Log.e(TAG, "分享 APK 失败，降级为链接分享", e);
            shareLinkFallback(activity);
        }
    }

    /**
     * 清理分享产生的缓存文件
     */
    public static void cleanCache(Activity activity) {
        if (activity == null) return;
        File dir = getCacheDir(activity);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }


    private static void shareApkInternal(Activity activity) throws Exception {
        // 1. 获取源 APK
        ApplicationInfo appInfo = activity.getApplicationInfo();
        File sourceApk = new File(appInfo.sourceDir);

        if (!sourceApk.exists()) {
            throw new IOException("APK not found: " + appInfo.sourceDir);
        }

        // 2. 复制到缓存目录（规避 /data/app/ 访问限制）
        File cacheDir = getCacheDir(activity);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Failed to create cache dir");
        }

        String versionName = getVersionName(activity);
        File targetApk = new File(cacheDir, "vmq_pro_v" + versionName + ".apk");

        if (!targetApk.exists() || targetApk.length() != sourceApk.length()) {
            copyFile(sourceApk, targetApk);
            Log.d(TAG, "APK copied → " + targetApk.getAbsolutePath()
                    + " (" + targetApk.length() + " bytes)");
        } else {
            Log.d(TAG, "APK cache hit, skip copy.");
        }

        // 3. 生成 content:// URI
        String authority = activity.getPackageName() + ".fileprovider";
        Uri contentUri = FileProvider.getUriForFile(activity, authority, targetApk);

        // 4. 构建分享 Intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/vnd.android.package-archive");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享应用：" + getAppLabel(activity));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 5. 启动选择器
        Intent chooser = Intent.createChooser(shareIntent, "分享应用安装包");
        activity.startActivity(chooser);
    }


    private static void shareLinkFallback(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "下载应用：" + FALLBACK_URL);
            Intent chooser = Intent.createChooser(intent, "分享下载链接");
            activity.startActivity(chooser);
            Toast.makeText(activity, "已切换为分享下载链接", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "链接分享也失败了", e);
            Toast.makeText(activity, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }


    private static File getCacheDir(Activity activity) {
        return new File(activity.getCacheDir(), CACHE_DIR_NAME);
    }

    private static String getVersionName(Activity activity) {
        try {
            PackageInfo info = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            return info.versionName != null ? info.versionName : "unknown";
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    private static String getAppLabel(Activity activity) {
        try {
            PackageManager pm = activity.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(activity.getPackageName(), 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return activity.getPackageName();
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
    }
}
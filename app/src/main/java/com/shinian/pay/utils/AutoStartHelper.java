package com.shinian.pay.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.shinian.pay.ui.HelpActivity;

/**
 * 自启动权限引导工具类
 * 覆盖：小米/红米、华为/荣耀、OPPO/一加/realme、vivo/iQOO、三星、联想/摩托罗拉、魅族、中兴
 * 兼容 Android 7.0 ~ Android 16
 *
 * 用法：
 *   AutoStartHelper.showGuideDialog(activity);
 */
public final class AutoStartHelper {

    private static final String TAG = "AutoStartHelper";

    private AutoStartHelper() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    // ==================== 对外接口 ====================

    /**
     * 显示自启动引导弹窗
     * 自动识别厂商，弹出包含文字操作步骤的引导对话框
     *
     * @param activity Activity 上下文（用于弹窗和跳转）
     */
    public static void showGuideDialog(@NonNull Activity activity) {
        ManufacturerInfo info = getManufacturerInfo();

        if (info == null) {
            showGenericGuideDialog(activity);
            return;
        }

        String guideText = getGuideText(info);

        new AlertDialog.Builder(activity)
                .setTitle("请开启自启动权限")
                .setMessage("为了保证「V免签监控端_Pro」在后台正常接收消息，"
                        + "请允许本应用自启动。\n\n"
                        + "当前设备：" + info.displayName + "\n\n"
                        + guideText)
                .setPositiveButton("去设置", (dialog, which) -> {
                    boolean success = jumpToAutoStartSetting(activity, info);
                    if (!success) {
                        Log.w(TAG, "厂商自启动页面跳转失败，降级到通用设置");
                        jumpToGenericSetting(activity);
                    }
                })
                .setNeutralButton("查看图文教程", (d, w) -> {
                    Intent intent = new Intent(activity, HelpActivity.class);
                    intent.putExtra("page", "autostart_guide");
                    activity.startActivity(intent);
                })
                .setNegativeButton("暂不开启", null)
                .setCancelable(false)
                .show();
    }

    /**
     * 静默尝试跳转自启动设置页（不弹窗）
     *
     * @return true=成功跳转，false=无法识别或跳转失败
     */
    public static boolean tryJumpSilently(@NonNull Context context) {
        ManufacturerInfo info = getManufacturerInfo();
        if (info == null) return false;
        return jumpToAutoStartSetting(context, info);
    }

    /**
     * 判断是否为已知需要自启动设置的厂商
     */
    public static boolean isKnownManufacturer() {
        return getManufacturerInfo() != null;
    }

    // ==================== 厂商识别 ====================

    /**
     * 厂商信息封装
     */
    private static class ManufacturerInfo {
        final String manufacturer;       // Build.MANUFACTURER 原始值（小写）
        final String displayName;        // 用户可读名称
        final ComponentName[] components; // 候选 Intent 目标组件（按优先级排列）

        ManufacturerInfo(String manufacturer, String displayName, ComponentName[] components) {
            this.manufacturer = manufacturer;
            this.displayName = displayName;
            this.components = components;
        }
    }

    /**
     * 根据 Build.MANUFACTURER 识别厂商并返回对应设置页信息
     */
    @Nullable
    private static ManufacturerInfo getManufacturerInfo() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        // ---- 小米 / 红米 ----
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            return new ManufacturerInfo(manufacturer, "小米/红米", new ComponentName[]{
                    new ComponentName("com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                    new ComponentName("com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartActivity"),
            });
        }

        // ---- 华为 / 荣耀 ----
        if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            return new ManufacturerInfo(manufacturer, "华为/荣耀", new ComponentName[]{
                    new ComponentName("com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                    new ComponentName("com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"),
                    new ComponentName("com.huawei.systemmanager",
                            "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            });
        }

        // ---- OPPO / 一加 / realme ----
        if (manufacturer.contains("oppo") || manufacturer.contains("oneplus")

                || manufacturer.contains("realme")) {
            return new ManufacturerInfo(manufacturer, "OPPO/一加/realme", new ComponentName[]{
                    new ComponentName("com.coloros.safecenter",
                            "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                    new ComponentName("com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    new ComponentName("com.oppo.safe",
                            "com.oppo.safe.permission.startup.StartupAppListActivity"),
            });
        }

        // ---- vivo / iQOO ----
        if (manufacturer.contains("vivo") || manufacturer.contains("iqoo")) {
            return new ManufacturerInfo(manufacturer, "vivo/iQOO", new ComponentName[]{
                    new ComponentName("com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                    new ComponentName("com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            });
        }

        // ---- 三星 ----
        if (manufacturer.contains("samsung")) {
            return new ManufacturerInfo(manufacturer, "三星", new ComponentName[]{
                    new ComponentName("com.samsung.android.lool",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"),
                    new ComponentName("com.samsung.android.sm_cn",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"),
            });
        }

        // ---- 魅族 ----
        if (manufacturer.contains("meizu")) {
            return new ManufacturerInfo(manufacturer, "魅族", new ComponentName[]{
                    new ComponentName("com.meizu.safe",
                            "com.meizu.safe.permission.SmartBGActivity"),
            });
        }

        // ---- 联想 / 摩托罗拉 ----
        if (manufacturer.contains("lenovo") || manufacturer.contains("motorola")) {
            return new ManufacturerInfo(manufacturer, "联想/摩托罗拉", new ComponentName[]{
                    new ComponentName("com.lenovo.powersetting",
                            "com.lenovo.powersetting.ui.Settings$HighPowerApplicationsActivity"),
            });
        }

        // ---- 中兴 / nubia ----
        if (manufacturer.contains("zte") || manufacturer.contains("nubia")) {
            return new ManufacturerInfo(manufacturer, "中兴/nubia", new ComponentName[]{
                    new ComponentName("cn.nubia.security2",
                            "cn.nubia.security2.NubiaPermissionActivity"),
            });
        }

        return null;
    }

    // ==================== 文字指引 ====================

    /**
     * 根据厂商返回人工操作步骤文字
     */
    @NonNull
    private static String getGuideText(@NonNull ManufacturerInfo info) {
        String mfr = info.manufacturer;

        if (mfr.contains("xiaomi") || mfr.contains("redmi")) {
            return "【操作步骤】\n"
                    + "1. 打开「手机管家」\n"
                    + "2. 点击「应用管理」→「权限」\n"
                    + "3. 点击「自启动管理」\n"
                    + "4. 找到「V免签监控端_Pro」→ 开启开关\n\n"
                    + "⚠ 建议同时开启「后台自启动」和「锁屏自启动」";
        }

        if (mfr.contains("huawei") || mfr.contains("honor")) {
            return "【操作步骤】\n"
                    + "1. 打开「手机管家」\n"
                    + "2. 点击「应用启动管理」\n"
                    + "3. 找到「V免签监控端_Pro」→ 关闭「自动管理」\n"
                    + "4. 在弹出窗口中勾选：\n"
                    + "   ☑ 允许自启动\n"
                    + "   ☑ 允许关联启动\n"
                    + "   ☑ 允许后台活动\n"
                    + "5. 点击「确定」\n\n"
                    + "⚠ 三个选项必须全部勾选，否则后台会被杀";
        }

        if (mfr.contains("oppo") || mfr.contains("oneplus") || mfr.contains("realme")) {
            return "【操作步骤】\n"
                    + "1. 打开「手机管家」\n"
                    + "2. 点击「权限隐私」→「自启动管理」\n"
                    + "3. 找到「V免签监控端_Pro」→ 开启开关\n\n"
                    + "⚠ 如未找到，也可尝试：\n"
                    + "   设置 → 电池 → 更多电池设置 → 优化电池使用";
        }

        if (mfr.contains("vivo") || mfr.contains("iqoo")) {
            return "【操作步骤】\n"
                    + "1. 打开「i管家」\n"
                    + "2. 点击「应用管理」→「权限管理」\n"
                    + "3. 点击「自启动管理」\n"
                    + "4. 找到「V免签监控端_Pro」→ 开启开关\n\n"
                    + "⚠ 建议同时在「电池」→「后台耗电管理」中允许后台运行";
        }

        if (mfr.contains("samsung")) {
            return "【操作步骤】\n"
                    + "1. 打开「设置」→「电池和设备维护」\n"
                    + "2. 点击「电池」→「后台用量」\n"
                    + "3. 找到「V免签监控端_Pro」\n"
                    + "4. 选择「从不休眠」\n\n"
                    + "⚠ 如未找到，也可尝试：\n"
                    + "   设置 → 应用程序 → 本应用 → 电池 → 不限制";
        }

        if (mfr.contains("meizu")) {
            return "【操作步骤】\n"
                    + "1. 打开「手机管家」\n"
                    + "2. 点击「权限管理」→「后台管理」\n"
                    + "3. 找到「V免签监控端_Pro」→ 允许后台运行\n\n"
                    + "⚠ 建议同时在「安全中心」→「自启动管理」中开启";
        }

        if (mfr.contains("lenovo") || mfr.contains("motorola")) {
            return "【操作步骤】\n"
                    + "1. 打开「手机管家」\n"
                    + "2. 点击「应用管理」→「自启动管理」\n"
                    + "3. 找到「V免签监控端_Pro」→ 开启开关";
        }

        if (mfr.contains("zte") || mfr.contains("nubia")) {
            return "【操作步骤】\n"
                    + "1. 打开「手机管家」\n"
                    + "2. 点击「权限管理」→「自启动管理」\n"
                    + "3. 找到「V免签监控端_Pro」→ 开启开关";
        }

        // fallback
        return "【操作步骤】\n"
                + "1. 打开手机管家 / 安全中心\n"
                + "2. 找到「自启动管理」或「应用启动管理」\n"
                + "3. 找到「V免签监控端_Pro」→ 开启开关";
    }

    // ==================== 跳转逻辑 ====================

    /**
     * 尝试跳转到厂商自启动设置页
     * 按候选组件列表依次尝试，任一成功即返回 true
     */
    private static boolean jumpToAutoStartSetting(@NonNull Context context,
                                                  @NonNull ManufacturerInfo info) {
        for (ComponentName component : info.components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(component);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (context.getPackageManager().resolveActivity(intent,
                        PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    context.startActivity(intent);
                    Log.d(TAG, "成功跳转: " + component.flattenToString());
                    return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "跳转失败: " + component.flattenToString() + " | " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * 降级：跳转到系统通用应用设置页
     */
    private static void jumpToGenericSetting(@NonNull Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "已跳转到通用应用设置页");
        } catch (Exception e) {
            Log.e(TAG, "通用设置页跳转也失败了", e);
        }
    }

    /**
     * 未识别厂商时的通用引导弹窗
     */
    private static void showGenericGuideDialog(@NonNull Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("请开启自启动权限")
                .setMessage("为了保证「V免签监控端_Pro」在后台正常运行，"
                        + "请手动开启自启动权限。\n\n"
                        + "【通用操作步骤】\n"
                        + "1. 打开手机管家 / 安全中心\n"
                        + "2. 找到「自启动管理」或「应用启动管理」\n"
                        + "3. 找到「V免签监控端_Pro」→ 开启开关\n\n"
                        + "不同品牌手机路径不同，常见位置：\n"
                        + "• 手机管家 → 应用启动管理\n"
                        + "• 设置 → 电池 → 后台运行\n"
                        + "• 安全中心 → 权限管理 → 自启动")
                .setPositiveButton("去应用设置", (d, w) -> jumpToGenericSetting(activity))
                .setNeutralButton("查看图文教程", (d, w) -> {
                    Intent intent = new Intent(activity, HelpActivity.class);
                    intent.putExtra("page", "autostart_guide");
                    activity.startActivity(intent);
                })
                .setNegativeButton("我知道了", null)
                .setCancelable(true)
                .show();
    }
}
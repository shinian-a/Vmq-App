package com.shinian.pay.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.shinian.pay.ui.MainActivity;

public class MonitorLogManager {

    /**
     * 追加一条日志（倒序：最新在最上面），自动保留最近 MAX_LOG_ENTRIES 条
     *
     * @param context  上下文（用于获取 SharedPreferences）
     * @param msg      新日志内容
     * @param textView 可选，传入则直接更新UI；传null则仅持久化+通知Handler
     */
    public static synchronized void appendLog(Context context, String msg, @Nullable TextView textView) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(
                AppConstants.SP_NAME_LOGS, Context.MODE_PRIVATE);
        String old = sp.getString(AppConstants.SP_KEY_LOGS_STR, "");

        // ---- 1. 按 \n 分割，统计行数并截断 ----
        String[] lines = old.isEmpty() ? new String[0] : old.split("\n", -1);

        StringBuilder sb = new StringBuilder();
        sb.append(msg);

        int keepCount = Math.min(lines.length, AppConstants.MAX_LOG_ENTRIES - 1);
        for (int i = 0; i < keepCount; i++) {
            sb.append('\n').append(lines[i]);     // 保留旧的最近 N-1 条
        }

        String updated = sb.toString();

        // ---- 2. 持久化 ----
        sp.edit().putString(AppConstants.SP_KEY_LOGS_STR, updated).apply();

        // ---- 3. 更新 UI ----
        if (textView != null) {
            textView.post(() -> textView.setText(updated));
        }

        // 兼容原有 Handler 通知机制
        Handler handler = MainActivity.monitorLogHandler;
        if (handler != null) {
            Message message = handler.obtainMessage(0);
            Bundle bundle = new Bundle();
            bundle.putString("logsStr", updated);
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    /**
     * 便捷重载：不直接绑定 TextView，仅持久化 + Handler 通知
     */
    public static void appendLog(Context context, String msg) {
        appendLog(context, msg, null);
    }
}
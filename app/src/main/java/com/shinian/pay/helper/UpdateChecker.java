package com.shinian.pay.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.shinian.pay.manager.AppConstants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private UpdateChecker() {
    }

    public interface Callback {
        void onNewVersion(String versionName, int versionCode, String updateLog, String downloadUrl);
        void onAlreadyLatest();
        void onError(String message);
    }

    public static void checkAsync(Context context, Callback callback) {
        int currentVersion = getCurrentVersionCode(context);
        Request request = new Request.Builder()
                .url(AppConstants.UPDATE_CHECK_URL)
                .post(new FormBody.Builder()
                        .add("ver", String.valueOf(currentVersion))
                        .build())
                .build();

        HTTP_CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "检查更新异常", e);
                postError(callback, e.getMessage() != null ? e.getMessage() : "网络请求失败");
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response responseToClose = response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        postError(callback, "HTTP " + response.code());
                        return;
                    }

                    JSONObject data = new JSONObject(response.body().string().trim());
                    int code = data.optInt("code", -1);
                    if (code != 200) {
                        postError(callback, "服务器返回错误码: " + code);
                        return;
                    }

                    int remoteVersion = data.optInt("version", 0);
                    if (remoteVersion > currentVersion) {
                        String versionName = data.optString("ver", "");
                        String updateLog = data.optString("uplog", "");
                        String downloadUrl = data.optString("upurl", "");
                        MAIN_HANDLER.post(() -> callback.onNewVersion(
                                versionName, remoteVersion, updateLog, downloadUrl));
                    } else {
                        MAIN_HANDLER.post(callback::onAlreadyLatest);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析更新响应失败", e);
                    postError(callback, e.getMessage() != null ? e.getMessage() : "响应解析失败");
                }
            }
        });
    }

    private static void postError(Callback callback, String message) {
        MAIN_HANDLER.post(() -> callback.onError(message));
    }

    private static int getCurrentVersionCode(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            Log.w(TAG, "读取当前版本号失败", e);
            return 0;
        }
    }
}

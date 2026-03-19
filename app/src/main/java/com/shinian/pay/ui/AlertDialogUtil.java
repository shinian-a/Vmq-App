package com.shinian.pay.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * AlertDialog 工具类
 */
public class AlertDialogUtil {
    
    /**
     * 显示简单的提示对话框
     * 
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     */
    public static void showAlertDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setCancelable(true)
            .show();
    }
    
    /**
     * 显示带取消按钮的对话框
     * 
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param positiveText 确认按钮文字
     * @param negativeText 取消按钮文字
     * @param positiveListener 确认按钮监听器
     */
    public static void showAlertDialog(Context context, String title, String message,
                                      String positiveText, String negativeText,
                                      DialogInterface.OnClickListener positiveListener) {
        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText, positiveListener)
            .setNegativeButton(negativeText, null)
            .setCancelable(true)
            .show();
    }
}

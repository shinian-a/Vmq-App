package com.shinian.pay;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.content.ComponentName;


public class WxPayActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wxpay);
		/*隐藏标题栏
		ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			actionBar.hide();
		}
		*/

    }

    public void wxpay_go(View v) {
        Toast.makeText(this, "请选择相册赞赏码~", Toast.LENGTH_LONG).show();
        openWeixinToQE_Code(WxPayActivity.this);
    }
    

    /**
     * 打开微信并跳入到二维码扫描页面
     * 方法一
     */
    public void openWeixinToQE_Code(Context context) {
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
    public void toWeChatScanDirect(Context context) {
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

}

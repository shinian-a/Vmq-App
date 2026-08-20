package com.shinian.pay.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;
import com.shinian.pay.R;

public class HelpActivity extends AppCompatActivity {

    
    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// 隐藏系统标题栏
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide(); // 隐藏标题栏
		}

		setContentView(R.layout.activity_help);

		String page = getIntent().getStringExtra("page");
		WebView webView = findViewById(R.id.webview);
		ScrollView scrollView = findViewById(R.id.scroll_help);
		if ("autostart_guide".equals(page)) {
			// 显示 WebView，隐藏图文内容
			webView.setVisibility(View.VISIBLE);
			scrollView.setVisibility(View.GONE);

			webView.getSettings().setDomStorageEnabled(true);
			webView.loadUrl("file:///android_asset/autostart_guide.html");
		} else {
			// 显示图文内容，隐藏 WebView
			webView.setVisibility(View.GONE);
			scrollView.setVisibility(View.VISIBLE);
		}
		// 设置返回按钮点击事件
		View btnBack = findViewById(R.id.btn_back);
		if (btnBack != null) {
			btnBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish(); // 返回上一层
				}
			});
		}
    }
    
}

package com.shinian.pay.ui;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.ActionBar;
import java.util.function.BooleanSupplier;
import android.service.controls.actions.BooleanAction; // 这个可以不用管
import com.shinian.pay.R;

public class HelpActivity extends AppCompatActivity {

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		// 隐藏系统标题栏
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide(); // 隐藏标题栏
		}
		
		setContentView(R.layout.activity_help);
		
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

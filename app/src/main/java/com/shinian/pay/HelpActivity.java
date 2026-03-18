package com.shinian.pay;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import java.util.function.BooleanSupplier;
import android.service.controls.actions.BooleanAction; // 这个可以不用管

public class HelpActivity extends AppCompatActivity {

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		//隐藏标题栏
/*		
		ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			actionBar.hide();
		}
		*/
		
    }
    
}

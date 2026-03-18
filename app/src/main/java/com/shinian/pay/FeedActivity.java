package com.shinian.pay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class FeedActivity extends AppCompatActivity {


    
	TextView feedbacktext;
	TextView contact;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
		
		feedbacktext = (TextView) findViewById(R.id.feedbacktext);
		contact = (TextView) findViewById(R.id.contact);
    }
    
	
	
	public void email_go(View v){
		if(feedbacktext.getText().toString().equals("")){
			Toast.makeText(this, "反馈内容不能为空!", Toast.LENGTH_SHORT).show();
		}else if(contact.getText().toString().equals("")){
			Toast.makeText(this, "联系邮箱不能为空!", Toast.LENGTH_SHORT).show();
		}else{
			//发送邮件
			SendmailUtil.getInstance().sendMail("V免签监控端_Pro-用户反馈",feedbacktext.getText().toString() + "\n\n用户邮箱：" +contact.getText().toString());
			Toast.makeText(this, "反馈中...", Toast.LENGTH_SHORT).show();
			new Handler().postDelayed(new Runnable(){

                    @Override
                    public void run() {

                    }
				}, 3000);
			Toast.makeText(this, "反馈成功~感谢您的的支持！", Toast.LENGTH_SHORT).show();
		}
		
	}
}

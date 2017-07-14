package com.quicktip.quick_tip_servant;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.quicktip.quick_tip_servant.R;

public class LoadingActivity extends AppCompatActivity {
    private int loadingTime = 1000;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        thread = new Thread(runable);
        thread.start();
    }

    public Runnable runable = new Runnable() {
        public void run() {
            try {
                Thread.sleep(loadingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                boolean isLogin = hasLoginData(LoadingActivity.this, LoadingActivity.this.getClass().getName());
                if (isLogin) {
                    Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(LoadingActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    };

    private boolean hasLoginData(Context context, String className){
        if(context==null || className==null || "".equalsIgnoreCase(className)) return false;
        String mResultStr = context.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                .getString("login", "");
        if(mResultStr.equalsIgnoreCase("false"))
            return false;
        else
            return true;
    }
}

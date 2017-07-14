package com.quicktip.quick_tip_servant;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class WebsocketService extends Service {
    boolean status=false;
    Handler handler;
    WebSocketClient client;
    NotificationManager mNotificationManager;
    Notification.Builder mBuilder;
    PendingIntent resultPendingIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler=new Handler();

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder = new Notification.Builder(WebsocketService.this)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle("Quick Tip")
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(alarmSound)
                .setContentIntent(resultPendingIntent);
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started",Toast.LENGTH_LONG).show();
        try {
            final SharedPreferences sp = WebsocketService.this.getSharedPreferences("login_data", MODE_PRIVATE);
            client = new WebSocketClient(new URI("ws://crcrcry.com.cn:3001")) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    status = true;
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("service", "started");
                    editor.apply();
                    client.send("Connect:"+sp.getString("token", ""));
                }

                @Override
                public void onWebsocketMessageFragment(WebSocket conn, Framedata frame ) {
                    FramedataImpl1 builder = (FramedataImpl1) frame;
                    builder.setTransferemasked( true );
                }

                @Override
                public void onMessage( String message ) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WebsocketService.this);
                    if (settings.getBoolean("notiSetting", true)) {
                        mBuilder.setContentText(message);
                        if (settings.getBoolean("notiVibrateSetting", true))
                            mBuilder.setVibrate(new long[]{0, 500, 500});
                        mNotificationManager.notify(1, mBuilder.build());
                    }
                }

                @Override
                public void onError(Exception ex) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("service", "false");
                    editor.apply();
                }

                @Override
                public void onClose(int code, String reason, boolean remote){
                    status = false;
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("service", "stopped");
                    editor.apply();
                }
            };
        }catch(Exception e){ }

        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, 1000);
        return START_STICKY;
    }

    private Runnable runnable=new Runnable() {
        @Override
        public void run() {
            client.connect();

            for(int i=0; i<5;i++){
                if(status){
                    break;
                }else{
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!status) {
                Toast.makeText(WebsocketService.this, "连接超时", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences sp = WebsocketService.this.getSharedPreferences("login_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("service", "stopped");
        editor.apply();
        Toast.makeText(WebsocketService.this, "Destoryed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLowMemory() {
        SharedPreferences sp = WebsocketService.this.getSharedPreferences("login_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("service", "stopped");
        editor.apply();
        Toast.makeText(WebsocketService.this, "Destoryed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTrimMemory(int level) {
        SharedPreferences sp = WebsocketService.this.getSharedPreferences("login_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("service", "stopped");
        editor.apply();
        Toast.makeText(WebsocketService.this, "Destoryed", Toast.LENGTH_LONG).show();
    }
}

package com.btapp1.meetingmanage;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;

public class LiveStatusReporterService extends Service {

    String Server_address;
    int Server_port;

    Thread thread ;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Server_address = intent.getStringExtra("SERVERADDR");
        Server_port = intent.getIntExtra("SERVERPORT", 0);
        thread = new Thread(new RUNNER());
        thread.start();
        Log.d("TAG", "INFO !!!!!!!! " + Server_address + ":" + Server_port);
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(thread != null) {
            thread.interrupt();
            thread = null;
        }
        Thread.currentThread().interrupt();
    }

    private class RUNNER extends Thread {
        String s  = "chk";
        @Override
        public void run() {
            boolean run = true;
                while(run)
                {
                    try
                    {
                        Log.d("TAG", Server_address + ":" + Server_port);
                        try {
                            Socket socket = new Socket(Server_address, Server_port);
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            out.write((s).getBytes());
                            socket.close();
                            Log.d("TAG", "REPORTED");
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException e)
                    {
                        run = false;
                        e.printStackTrace();
                    }
            }
        }
    }
}

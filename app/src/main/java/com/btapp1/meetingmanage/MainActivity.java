package com.btapp1.meetingmanage;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    final int ACTIVITYREQUEST_APPSETTINGS = 0xf;
    final int ACTIVITYREQUEST_SCHED = 0xff;
    final int ACTIVITYREQUEST_ROOMRESERV = 0xfff;

    final int PERMISSION_RQST_PHONESTATE = 0xFE;

    TextView tv_LogView;
    Button BtnSettings;
    String Server_address;
    int Server_port;

    String RTSP_Server;

    private Intent serviceintent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this,  Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,  new String[]{Manifest.permission.READ_PHONE_STATE},  PERMISSION_RQST_PHONESTATE);
        }

        tv_LogView = findViewById(R.id.textview_summary);
        BtnSettings = findViewById(R.id.Action1);

        tv_LogView.setMovementMethod(new ScrollingMovementMethod());

        LoadSharedPreferenceData();

        BtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AppSettingsActivity.class);
                intent.putExtra("SERVERADDR", Server_address);
                intent.putExtra("SERVERPORT", Server_port);
                intent.putExtra("RTSPSERVER", RTSP_Server);
                startActivityForResult(intent, ACTIVITYREQUEST_APPSETTINGS);
            }
        });

        tv_LogView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                (new Thread(new SendRunner("RQLOG"))).start();
                LogBringDialogTask task = new LogBringDialogTask("RQLOG");
                task.execute();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serviceintent != null) {
            stopService(serviceintent);
            serviceintent = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_RQST_PHONESTATE)
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTIVITYREQUEST_APPSETTINGS && resultCode == Activity.RESULT_OK)
        {
            String str_addr = data.getStringExtra("SERVERADDR");
            String str_port = data.getStringExtra("SERVERPORT");
            String str_rtsp = data.getStringExtra("RTSPSERVER");
            if(str_addr.isEmpty() || str_port.isEmpty() || str_rtsp.isEmpty())
                Toast.makeText(getApplicationContext(), "Data Error", Toast.LENGTH_SHORT).show();
            else
            {
                Server_address = str_addr;
                Server_port = Integer.parseInt(str_port);
                RTSP_Server = str_rtsp;
                StorSharedPreferenceData();
                Toast.makeText(getApplicationContext(), "Confirmed", Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == ACTIVITYREQUEST_SCHED && resultCode == Activity.RESULT_OK)
        {
            Toast.makeText(getApplicationContext(), "SCHED OK", Toast.LENGTH_SHORT).show();
        } else if(requestCode == ACTIVITYREQUEST_ROOMRESERV && resultCode == Activity.RESULT_OK)
        {
            Toast.makeText(getApplicationContext(), "ROOM OK", Toast.LENGTH_SHORT).show();
        }
    }

    void StorSharedPreferenceData() {
        SharedPreferences SP = getSharedPreferences("AppSetting", MODE_PRIVATE);
        SharedPreferences.Editor editor = SP.edit();
        editor.putString("SERVERADDR", Server_address);
        editor.putString("RTSPSERVER", RTSP_Server);
        editor.putInt("SERVERPORT", Server_port);
        editor.apply();
    }

    void LoadSharedPreferenceData() {
        SharedPreferences SP = getSharedPreferences("AppSetting", MODE_PRIVATE);
        Server_address = SP.getString("SERVERADDR", "0.0.0.0");
        Server_port = SP.getInt("SERVERPORT", 0);
        RTSP_Server = SP.getString("RTSPSERVER", "0.0.0.0:0000");
    }

    public void MainBtnClick(View v)  {
        Intent intent;
        switch(v.getId()) {
            case R.id.Button1:
                intent = new Intent(getApplicationContext(), SchedAddActivity.class);
                intent.putExtra("SERVERADDR", Server_address);
                intent.putExtra("SERVERPORT", Server_port);
                startActivityForResult(intent, ACTIVITYREQUEST_SCHED);
                break;
            case R.id.Button2:
                intent = new Intent(getApplicationContext(), RoomReservationActivity.class);
                intent.putExtra("SERVERADDR", Server_address);
                intent.putExtra("SERVERPORT", Server_port);
                startActivityForResult(intent, ACTIVITYREQUEST_ROOMRESERV);
                break;
            case R.id.Button3:
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            StringBuilder sb = new StringBuilder();
                            byte[] b = new byte[512];
                            try
                            {
                                Socket sock = new Socket(Server_address, Server_port);

                                sock.connect(new InetSocketAddress(Server_address, Server_port));
                                DataOutputStream output = new DataOutputStream(sock.getOutputStream());
                                DataInputStream input = new DataInputStream(sock.getInputStream());

                                output.write(("RQLOG").getBytes());

                                while(input.read(b) > 0)
                                    sb.append(new String(b));

                                input.close();output.close();sock.close();
                                Log.d("TAG", sb.toString());
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                            Log.d("TAG", "Exit");
                        }
                    }).start();
                }
//                intent = new Intent(getApplicationContext(), StreamingActivity.class);
//                intent.putExtra("RTSPSERVER", RTSP_Server);
//                startActivity(intent);
                break;
            case R.id.Button4:
                intent = new Intent(getApplicationContext(), StatusPreviewActivity.class);
                intent.putExtra("SERVERADDR", Server_address);
                intent.putExtra("SERVERPORT", Server_port);
                startActivity(intent);
                break;
        }
    }

    private class LogBringDialogTask extends AsyncTask<Void, Void, Void>  {
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        AlertDialog ad ;
        boolean successed = false;
        String data;
        StringBuilder sb;

        LogBringDialogTask(String d)
        {
            data = d;
        }

        @Override
        protected void onPreExecute() {
//            builder.setMessage("서버와 통신중입니다");
//            ad = builder.create();
//            ad.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            sb = new StringBuilder("");
            byte b[] = new byte[512];

            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(Server_address, Server_port), 1000);
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                DataInputStream input = new DataInputStream(socket.getInputStream());

                output.write(data.getBytes());

                int rlen;
                while((rlen=input.read(b)) > 0)
                    sb.append(new String(b, 0, rlen));
                successed = true;
                socket.close();
                input.close();output.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }


            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(successed)
                    {
                        tv_LogView.setText(sb.toString().trim());
                        int linetop = tv_LogView.getLayout().getLineTop(tv_LogView.getLineCount());
                        int scrollY = linetop - tv_LogView.getHeight();
                        if(scrollY > 0)
                            tv_LogView.scrollTo(0, scrollY);
                        else
                            tv_LogView.scrollTo(0, 0);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Log Receive Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            ad.dismiss();
            super.onPostExecute(aVoid);
        }
    }
}

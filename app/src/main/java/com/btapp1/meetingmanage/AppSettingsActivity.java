package com.btapp1.meetingmanage;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class AppSettingsActivity extends AppCompatActivity {

    EditText edit_addr, edit_port, edit_rtsp;

    String Server_address;
    int Server_port;

    String RTSP_Server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        edit_addr = findViewById(R.id.edit_address);
        edit_port = findViewById(R.id.edit_port);
        edit_rtsp = findViewById(R.id.edit_rtspserver);

        Intent intent = getIntent();
        Server_address = intent.getStringExtra("SERVERADDR");
        Server_port = intent.getIntExtra("SERVERPORT", 0);
        RTSP_Server = intent.getStringExtra("RTSPSERVER");

        edit_rtsp.setText(RTSP_Server);
        edit_addr.setText(Server_address);
        edit_port.setText(String.valueOf(Server_port));
    }

    public void onClickQuit(View v)
    {
        Intent intent = new Intent();
        intent.putExtra("SERVERADDR", edit_addr.getEditableText().toString());
        intent.putExtra("SERVERPORT", edit_port.getEditableText().toString());
        intent.putExtra("RTSPSERVER", edit_rtsp.getEditableText().toString());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}

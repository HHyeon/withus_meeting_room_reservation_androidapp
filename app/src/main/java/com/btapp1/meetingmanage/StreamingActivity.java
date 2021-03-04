package com.btapp1.meetingmanage;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.VideoView;

public class StreamingActivity extends AppCompatActivity {
    VideoView videoview;
    String RTSPServer;

    AlertDialog.Builder builder;
    AlertDialog ad ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        Intent intent = getIntent();
        RTSPServer = intent.getStringExtra("RTSPSERVER");
        videoview = findViewById(R.id.videoview);
        final String uri = RTSPServer;


        builder = new AlertDialog.Builder(StreamingActivity.this);
        builder.setMessage("buffering...");
        ad = builder.create();
        ad.setCancelable(false);


        videoview.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(StreamingActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                finish();
                return false;
            }
        });
        
        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Toast.makeText(StreamingActivity.this, "prepared", Toast.LENGTH_SHORT).show();
            }
        });


        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(StreamingActivity.this, "completed", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        videoview.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch(what)
                {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
//                        Toast.makeText(StreamingActivity.this, "buffering start", Toast.LENGTH_SHORT).show();
                        ad.show();
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
//                        Toast.makeText(StreamingActivity.this, "buffering end", Toast.LENGTH_SHORT).show();
                        ad.dismiss();
                        videoview.start();
                        break;
                }


                return false;
            }
        });
        videoview.setVideoURI(Uri.parse(uri));
        videoview.requestFocus();

    }
}

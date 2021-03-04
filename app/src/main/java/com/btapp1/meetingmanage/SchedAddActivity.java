package com.btapp1.meetingmanage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SchedAddActivity extends AppCompatActivity {

    RadioGroup radiogroup;
    EditText Edit_EtcReason, Edit_Datefrom, Edit_Dateuntil, Edit_Timefrom, Edit_Timeuntil;
    Date startdate, enddate;
    boolean pickerstatus = false;
    int Year=0, Month=0, Day=0;
    String Server_address;
    int Server_port;
    int Hour=0, Minute=0;


    interface ondialogdissmisslistener {
        void onevent(int arg);
    }

    void layout()
    {
        radiogroup = findViewById(R.id.radiogroup);
        Edit_EtcReason = findViewById(R.id.Edit_reason);
        Edit_EtcReason.setVisibility(View.INVISIBLE);
        Edit_Datefrom = findViewById(R.id.Edit_DateFrom);
        Edit_Dateuntil = findViewById(R.id.Edit_DateUntil);
        Edit_Timefrom = findViewById(R.id.Edit_TimeFrom);
        Edit_Timeuntil = findViewById(R.id.Edit_TimeUntil);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sched);
        layout();

        Intent intent = getIntent();
        Server_address = intent.getStringExtra("SERVERADDR");
        Server_port = intent.getIntExtra("SERVERPORT", 0);

        Edit_Datefrom.setOnTouchListener(onTouchListener);
        Edit_Dateuntil.setOnTouchListener(onTouchListener);
        Edit_Timefrom.setOnTouchListener(onTouchListener);
        Edit_Timeuntil.setOnTouchListener(onTouchListener);

        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.radio_reason)
                    Edit_EtcReason.setVisibility(View.VISIBLE);
                else
                    Edit_EtcReason.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void SchedonClickQuit(View v) {
        boolean isvalid = true;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd/HH-mm", Locale.ENGLISH);
        String start = "", end = "";
        String reasontype = "", reasonetc = "";

        if(startdate != null || enddate != null)
        {
            start = sdf.format(startdate);
            end = sdf.format(enddate);
        }
        else
            isvalid = false;


        switch (radiogroup.getCheckedRadioButtonId())
        {
            case R.id.radio_outwork:
                reasontype = "out";
                reasonetc = "";
                break;
            case R.id.radio_vacation:
                reasontype = "vac";
                reasonetc = "";
                break;
            case R.id.radio_reason:
                reasontype = "rea";
                reasonetc = Edit_EtcReason.getEditableText().toString();
                break;
            default:
                isvalid = false;
                break;
        }

        String phnumber;
        try {
            phnumber = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
        } catch (SecurityException e)
        {
            Toast.makeText(this, "전화번호를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            isvalid = false;
            phnumber = "null";
        }

        if(isvalid)
        {
            String params = reasontype + ";" + reasonetc + ";" + start + ";" + end + ";" + phnumber + ";";
            SenderTask task = new SenderTask(params, new ondialogdissmisslistener() {
                @Override
                public void onevent(int arg) {
                    if(arg == 1)
                    {
                        Toast.makeText(getApplicationContext(), "등록되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), "서버와 통신하지 못했습니다", Toast.LENGTH_SHORT).show();
                }
            });
            task.execute();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "정보를 입력하세요", Toast.LENGTH_SHORT).show();
        }
    }

    public Date getDate(int y, int mon, int d, int h, int m) {
        Calendar cal = Calendar.getInstance();
        cal.set(y,mon,d,h,m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    EditText.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            if(pickerstatus) return false;
            if(v.equals(Edit_Datefrom))
            {
                pickerstatus = true;
                PickDateStart();
            }
            else if(v.equals(Edit_Dateuntil))
            {
                pickerstatus = true;
                PickDateEnd();
            }
            else if(v.equals(Edit_Timefrom))
            {
                pickerstatus = true;
                PickTimeStart();
            }
            else if(v.equals(Edit_Timeuntil))
            {
                pickerstatus = true;
                PickTimeEnd();
            }

            return false;
        }
    };

    void PickDateStart()  {
        CustomDialog dialog = new CustomDialog(SchedAddActivity.this, 0);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SchedAddActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enddate = getDate(Year,Month,Day, Hour, Minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        Edit_Datefrom.setText(sdf.format(enddate));
                        PickTimeStart();
                    }
                });
                pickerstatus = false;
            }
        });
        dialog.show();
    }
    void PickDateEnd() {
        CustomDialog dialog = new CustomDialog(SchedAddActivity.this, 0);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SchedAddActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startdate = getDate(Year,Month,Day, Hour, Minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        Edit_Dateuntil.setText(sdf.format(startdate));
                        PickTimeEnd();
                    }
                });
                pickerstatus = false;
            }
        });
        dialog.show();

    }
    void PickTimeStart() {
        CustomDialog dialog = new CustomDialog(SchedAddActivity.this, 1);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SchedAddActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startdate = getDate(Year,Month,Day, Hour, Minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH : mm", Locale.ENGLISH);
                        Edit_Timefrom.setText(sdf.format(startdate));
                    }
                });
                pickerstatus = false;
            }
        });
        dialog.show();

    }
    void PickTimeEnd() {
        CustomDialog dialog = new CustomDialog(SchedAddActivity.this, 1);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SchedAddActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enddate = getDate(Year,Month,Day, Hour, Minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH : mm", Locale.ENGLISH);
                        Edit_Timeuntil.setText(sdf.format(enddate));
                    }
                });
                pickerstatus = false;
            }
        });
        dialog.show();
    }

    private class CustomDialog extends  Dialog  {
        private LinearLayout timepickview, datepickview;
        private TimePicker timepicker;
        private DatePicker datepicker;
        private Button finishbtn;
        private int type;
        private TextView tv_info;

        CustomDialog(Context context, int type) // 0 -> Date , 1 -> Time
        {
            super(context);
            this.type = type;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.datetimedialoglayout);
            tv_info = findViewById(R.id.tv_info);

            if(type == 1)
                tv_info.setText("시간을 선택합니다");
            else
                tv_info.setText("날짜를 선택합니다");

            if(type == 1) {
                timepickview = findViewById(R.id.timepickview);
                finishbtn = findViewById(R.id.btn_timepick);
                timepicker = findViewById(R.id.timepicker);

                finishbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Hour = timepicker.getHour();
                        Minute = timepicker.getMinute();
                        dismiss();
                    }
                });

                timepicker.setMinute(0);
                timepickview.setVisibility(View.VISIBLE);

                timepicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                    @Override
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                        if(minute < 30 )
                            view.setMinute(0);
                        else if(minute > 30)
                            view.setMinute(0);
                    }
                });
            }
            else {
                datepickview = findViewById(R.id.datepickview);
                finishbtn = findViewById(R.id.btn_datepick);
                datepicker = findViewById(R.id.datepicker);

                finishbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Year = datepicker.getYear();
                        Month = datepicker.getMonth();
                        Day = datepicker.getDayOfMonth();
                        dismiss();
                    }
                });
                datepickview.setVisibility(View.VISIBLE);
            }
        }
    }

    private class SenderTask extends AsyncTask<Void, Void, Void> {

        private ondialogdissmisslistener mListener;
        int successed = 0;
//        AlertDialog.Builder builder = new AlertDialog.Builder(SchedAddActivity.this);
//        AlertDialog ad ;
        String data, headdata = "ADDSCH";

        SenderTask(String d, ondialogdissmisslistener listener)  {
            data = d;
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
//            builder.setMessage("서버와 통신중입니다");
//            builder.setCancelable(false);
//            ad = builder.create();
//            ad.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try
            {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(Server_address, Server_port), 3000);
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());


//                BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
//                bwriter.write(headdata);
//                bwriter.flush();
//                bwriter.write(data);
//                bwriter.flush();
//                bwriter.close();

                output.write(headdata.getBytes());
                output.write(data.getBytes());
                output.flush();
                output.close();

                socket.close();
                successed = 1;
            }
            catch (IOException e)
            { e.printStackTrace(); successed = 0;}
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            ad.dismiss();
            mListener.onevent(successed);
            super.onPostExecute(aVoid);
        }
    }
}

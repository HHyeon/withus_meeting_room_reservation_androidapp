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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RoomReservationActivity extends AppCompatActivity {

    EditText Edit_Timefrom, Edit_Timeuntil, Edit_Datefrom, Edit_Reservername, Edit_Purpose;
    Button btnapply, btntimedatepick;
    ArrayAdapter<String> adapter;

    int Year=0, Month=0, Day=0;
    int Hour=0, Minute=0;
    Date YearMonthdate = null;
    Date startdate = null, enddate = null;
    boolean pickerstatus = false;
    Spinner spinner;

    String Server_address;
    int Server_port;

    void layout() {
        Edit_Datefrom = findViewById(R.id.Edit_DateFrom);
        Edit_Timefrom = findViewById(R.id.Edit_TimeFrom);
        Edit_Timeuntil = findViewById(R.id.Edit_TimeUntil);
//        Edit_Reservername =findViewById(R.id.edit_input_reservername);
        Edit_Purpose = findViewById(R.id.edit_input_purpose);
        spinner = findViewById(R.id.spinner_roomlist);
        btnapply = findViewById(R.id.BTNApply);
        btntimedatepick = findViewById(R.id.btn_userchoose);

        btntimedatepick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickDateStart(true);
            }
        });

        Edit_Datefrom.setOnTouchListener(onTouchListener);
        Edit_Timefrom.setOnTouchListener(onTouchListener);
        Edit_Timeuntil.setOnTouchListener(onTouchListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_reservation);
        layout();

        InitCommunTask task = new InitCommunTask("RMRESVRQST");
        task.execute();

        Intent intent = getIntent();
        Server_address = intent.getStringExtra("SERVERADDR");
        Server_port = intent.getIntExtra("SERVERPORT", 0);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public void RoomReservOnclickQuit(View v) {
        boolean isvalid = true;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        String start = "", end = "", date = "", /*rsvname, */rsvpurpose;
        Date datetoday = null;
        String RoomName = adapter.getItem(spinner.getSelectedItemPosition());

//        rsvname = Edit_Reservername.getEditableText().toString();
        rsvpurpose = Edit_Purpose.getEditableText().toString();

        if(RoomName.equals("회의실 아라")) RoomName = "ROOM1";
        else if(RoomName.equals("회의실 가람")) RoomName = "ROOM2";
        else if(RoomName.equals("회의실 마루")) RoomName = "ROOM3";

        if(startdate != null && enddate != null && YearMonthdate != null) {
            start = sdf.format(startdate);
            end = sdf.format(enddate);
            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            date = sdf.format(YearMonthdate);

            datetoday = new Date(System.currentTimeMillis());

            int compare = startdate.compareTo(datetoday);

            if (compare <= 0)
            {
                Toast.makeText(this, "현재 시간 이후를 설정하세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        else  isvalid = false;

        if(/*rsvname.isEmpty() ||*/ rsvpurpose.isEmpty())
            isvalid = false;

        if(isvalid) {
            try {
                TelephonyManager telmanager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

                String params = date + ";" + start + ";" + end + ";" + RoomName + ";" + "tel" + ";" + telmanager.getLine1Number() + ";" + rsvpurpose + ";";

                ResvConfirmTask task = new ResvConfirmTask(params);
                task.execute();
            } catch (SecurityException e)
            {
                Toast.makeText(this, "전화번호를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        else Toast.makeText(this, "정보를 입력하세요", Toast.LENGTH_SHORT).show();
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
                PickDateStart(false);
            }
            else if(v.equals(Edit_Timefrom))
            {
                pickerstatus = true;
                PickTimeStart(false);
            }
            else if(v.equals(Edit_Timeuntil))
            {
                pickerstatus = true;
                PickTimeEnd();
            }

            return false;
        }
    };

    void PickDateStart(final boolean iscontinue)  {
        CustomDialog dialog = new CustomDialog(RoomReservationActivity.this, 0);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                RoomReservationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        YearMonthdate = getDate(Year,Month,Day, Hour, Minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        Edit_Datefrom.setText(sdf.format(YearMonthdate));
                    }
                });
                pickerstatus = false;
                if(iscontinue)
                    PickTimeStart(true);
            }
        });
        dialog.show();
    }
    void PickTimeStart(final boolean iscontinue) {
        CustomDialog dialog = new CustomDialog(RoomReservationActivity.this, 1);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                RoomReservationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startdate = getDate(Year,Month,Day, Hour, Minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH : mm", Locale.ENGLISH);
                        Edit_Timefrom.setText(sdf.format(startdate));
                        if(iscontinue)
                            PickTimeEnd();
                    }
                });
                pickerstatus = false;
            }
        });
        dialog.show();
    }
    void PickTimeEnd() {
        CustomDialog dialog = new CustomDialog(RoomReservationActivity.this, 2);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                RoomReservationActivity.this.runOnUiThread(new Runnable() {
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
                tv_info.setText("시작 시간을 선택합니다");
            else if(type == 0)
                tv_info.setText("날짜를 선택합니다");
            else if(type == 2)
                tv_info.setText("종료 시간을 선택합니다");


            if(type > 0) {
                timepickview = findViewById(R.id.timepickview);
                finishbtn = findViewById(R.id.btn_timepick);
                timepicker = findViewById(R.id.timepicker);


                timepicker.setMinute(0);
                timepicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                    @Override
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                        if(minute < 30 )
                            view.setMinute(0);
                        else if(minute > 30)
                            view.setMinute(0);
                    }
                });

                finishbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Hour = timepicker.getHour();
                        Minute = timepicker.getMinute();
                        dismiss();
                    }
                });

                timepickview.setVisibility(View.VISIBLE);
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











    private class InitCommunTask extends AsyncTask<Void, Void, Void> {
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomReservationActivity.this);
        AlertDialog ad;
        String rqData;
        String rcvData;
        StringBuilder sb;
        InitCommunTask(String rq)
        {
            rqData = rq;
        }
        int success = 0;
        int userquit = 0;

        @Override
        protected void onPreExecute() {
            builder.setMessage("서버와 통신중입니다");
//            builder.setCancelable(false);
            ad = builder.create();
            ad.show();
            ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    userquit = 1;
                }
            });
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            Log.d("TAG", "room rsv begin");
            sb = new StringBuilder("");
            byte[] buffer = new byte[512];
            int rlen = 1;

            int atmpt = 0;
            while(success == 0 && userquit == 0 && atmpt < 5)
            {
                atmpt++;
                try{
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(Server_address, Server_port), 1000);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    DataInputStream input = new DataInputStream(socket.getInputStream());

                    output.write(rqData.getBytes());

//                    while(rlen != -1)
//                    {
//                        rlen = input.read(buffer);
//                        sb.append(new String(buffer));
//                    }

                    // Receive Only 512 byte
                    rlen = input.read(buffer);
                    Log.d("TAG", "rlen : " + rlen);
                    sb.append(new String(buffer));

                    input.close();output.close();socket.close();
                    rcvData = sb.toString().trim();
                    success = 1;
                } catch(IOException e) {
                    e.printStackTrace();
                    success = 0;
                }
            }

            if(success == 0)
                return null;

            final int succ = success;
            RoomReservationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(succ == 1)
                    {
                        try  {
                            adapter.clear();
                            while(!rcvData.isEmpty())
                            {
                                int pos = rcvData.indexOf(';');
                                Log.d("TAG", rcvData.substring(0, pos));
                                if(rcvData.substring(0, pos).equals("ROOM1"))  adapter.add("회의실 아라");
                                else if(rcvData.substring(0, pos).equals("ROOM2")) adapter.add("회의실 가람");
                                else if(rcvData.substring(0, pos).equals("ROOM3")) adapter.add("회의실 마루");
                                rcvData = rcvData.substring(pos+1);
                            }
                        } catch (Exception e)
                        {
                            Toast.makeText(RoomReservationActivity.this, "string parse error", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(RoomReservationActivity.this, "서버와 통신하지 못했습니다", Toast.LENGTH_SHORT).show();
                        Edit_Timeuntil.setEnabled(false);
                        Edit_Timefrom.setEnabled(false);
                        Edit_Datefrom.setEnabled(false);
                        spinner.setEnabled(false);
                        btnapply.setEnabled(false);
                        btntimedatepick.setEnabled(false);
                    }
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ad.dismiss();
            super.onPostExecute(aVoid);
        }
    }

    private class ResvConfirmTask extends  AsyncTask<Void, Void, Void> {
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomReservationActivity.this);
        AlertDialog ad;
        String trData;
        String head = "RMRESVCFM";

        int connsuccess = 0;
        int userquit = 0;

        ResvConfirmTask(String data)  {
            trData = data;
        }

        @Override
        protected void onPreExecute() {
            builder.setMessage("서버와 통신중입니다");
//            builder.setCancelable(false);
            ad = builder.create();
            ad.show();
            ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    userquit = 1;
                }
            });
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            byte b[] = new byte[512];
            int success = 0;

            int atmpt = 0;

            while(connsuccess == 0 && userquit == 0 && atmpt < 5)
            {
                atmpt++;
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(Server_address, Server_port), 500);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    output.write(head.getBytes());
                    try
                    { Thread.sleep(300); }
                    catch(InterruptedException e)
                    { e.printStackTrace(); }
                    output.write(trData.getBytes());

                    input.read(b);

                    String retrieve = new String(b).trim();
                    if(retrieve.equals("confirmed"))
                        success = 1;
                    else if(retrieve.equals("overlaped"))
                        success = 2;
                    else if(retrieve.equals("reachedmax"))
                        success = 3;
                    else if(retrieve.equals("overred"))
                        success = 4;

                    connsuccess = 1;
                } catch (IOException e) {
                    e.printStackTrace();
                    connsuccess = 0;
                }
            }
            if(userquit == 1)
                return null;

            final int succ = success;
            RoomReservationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(succ == 1){
                        Toast.makeText(RoomReservationActivity.this, "등록되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else if (succ == 2)
                        Toast.makeText(RoomReservationActivity.this, "시간이 겹칩니다", Toast.LENGTH_SHORT).show();
                    else if (succ == 3)
                        Toast.makeText(RoomReservationActivity.this, "reachedmax", Toast.LENGTH_SHORT).show();
                    else if(succ == 4)
                        Toast.makeText(RoomReservationActivity.this, "8시 이전 일정으로 등록해주세요", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(RoomReservationActivity.this, "정상적으로 등록되지 않았습니다", Toast.LENGTH_SHORT).show();
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ad.dismiss();
            super.onPostExecute(aVoid);
        }
    }







}

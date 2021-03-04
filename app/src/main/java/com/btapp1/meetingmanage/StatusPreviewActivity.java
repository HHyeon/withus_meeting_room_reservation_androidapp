package com.btapp1.meetingmanage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatusPreviewActivity extends AppCompatActivity {
    String PHONENUMBER;

    TextView tv_count1, tv_count2, tv_statusbymonth;
    ListView PeopleStatusList, RoomStatusList;
    String Server_address;
    int Server_port;

    CompactCalendarView compactCalendarView;

    PersonStatusListAdapter peoplestatusadapter;
    RoomStatusListAdapter roomstatuslistadapter;

    String UserName = null;

    public Date getDate(int y, int mon, int d, int h, int m) {
        Calendar cal = Calendar.getInstance();
        cal.set(y,mon-1,d,h,m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_preview);

        try {
            PHONENUMBER = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
        } catch (SecurityException e) {
            Toast.makeText(this, "전화번호를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        compactCalendarView = findViewById(R.id.compactcalendar_view);
        PeopleStatusList = findViewById(R.id.List_peoplestatus);
        RoomStatusList = findViewById(R.id.List_meetingroomstatus);
        tv_statusbymonth = findViewById(R.id.tv_statusbymonth);
        tv_count1 = findViewById(R.id.tv_count1);
        tv_count2 = findViewById(R.id.tv_count2);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월", Locale.KOREA);
        String comment = "일별 예약 현황 - " + sdf.format((new Date(System.currentTimeMillis())));
        tv_statusbymonth.setText(comment);


        compactCalendarView.setListener(calendarViewListener);

        peoplestatusadapter = new PersonStatusListAdapter();
        roomstatuslistadapter = new RoomStatusListAdapter();

        RoomStatusList.setAdapter(roomstatuslistadapter);
        PeopleStatusList.setAdapter(peoplestatusadapter);

        Intent intent = getIntent();
        Server_address = intent.getStringExtra("SERVERADDR");
        Server_port = intent.getIntExtra("SERVERPORT", 0);

    }


    @Override
    protected void onResume() {
        compactCalendarView.removeAllEvents();

        roomstatuslistadapter.DelAll();

        CommunTask task = new CommunTask();
        task.execute();

        super.onResume();
    }

    CompactCalendarView.CompactCalendarViewListener calendarViewListener = new CompactCalendarView.CompactCalendarViewListener() {
        @Override
        public void onDayClick(Date dateClicked) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            String now = sdf.format((new Date(System.currentTimeMillis())));
            if(now.equals(sdf.format(dateClicked)))
                return;
            List<Event> eventList = compactCalendarView.getEvents(dateClicked);
            if(!eventList.isEmpty())
            {
                Intent intent = new Intent(getApplicationContext(), RoomStatusByDayActivity.class);
                intent.putExtra("SELECTEDDATE", sdf.format(dateClicked));
                intent.putExtra("SERVERADDR", Server_address);
                intent.putExtra("SERVERPORT", Server_port);
                startActivity(intent);
            }
        }

        @Override
        public void onMonthScroll(Date firstDayOfNewMonth) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월", Locale.KOREA);
            String comment = "일별 예약 현황 - " + sdf.format(firstDayOfNewMonth);
            tv_statusbymonth.setText(comment);
        }
    };

    private class PersonStatus {
        String Name, Reason, StartTime, EndTime, Phonenumber;
        PersonStatus(String name, String reason, String start, String end, String phn)  {
            Name = name;
            Reason = reason;
            StartTime = start;
            EndTime = end;
            Phonenumber = phn;
        }
        String getEndTime() {
            return EndTime;
        }
        String getName() {
            return Name;
        }
        String getReason() {
            return Reason;
        }
        String getStartTime() {
            return StartTime;
        }
        String getPhonenumber() {
            return Phonenumber;
        }
    }
    private class PersonStatusListAdapter extends BaseAdapter {
        private ArrayList<PersonStatus> LIST = new ArrayList<>();

        void Add(String name, String reason, String start, String end, String phn)
        {
            PersonStatus ps = new PersonStatus(name, reason, start, end, phn);
            LIST.add(ps);
        }

        void Del(int pos)
        {
            LIST.remove(pos);
        }

        @Override
        public int getCount() {
            return LIST.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Context context = parent.getContext();

            if(convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.peoplestatuslayout, parent, false);
            }

            TextView  tv_name = convertView.findViewById(R.id.peoplestatus_name);
            TextView  tv_reason = convertView.findViewById(R.id.peoplestatus_reason);
            TextView  tv_starttime = convertView.findViewById(R.id.peoplestatus_starttime);
            TextView  tv_endtime = convertView.findViewById(R.id.peoplestatus_endtime);
            TextView  tv_phnum = convertView.findViewById(R.id.peoples_phonenumber);
            Button    button = convertView.findViewById(R.id.BTN_Cancel_people);

            final PersonStatus ps = LIST.get(position);
            tv_name.setText(ps.getName());
            tv_reason.setText(ps.getReason());
            tv_starttime.setText(ps.getStartTime());
            tv_endtime.setText(ps.getEndTime());
            tv_phnum.setText(ps.getPhonenumber());

            if(PHONENUMBER.equals(ps.getPhonenumber()))
            {
                button.setVisibility(View.VISIBLE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("확인");
                        builder.setMessage("스케줄을 삭제합니다");
                        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String data = "people;" + ps.getStartTime() + ";" + ps.getEndTime() + ";" + ps.getPhonenumber() + ";";
                                SenderTask task = new SenderTask(data, new SchedAddActivity.ondialogdissmisslistener() {
                                    @Override
                                    public void onevent(int arg) {
                                        if (arg == 1) {
                                            Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else
                                            Toast.makeText(getApplicationContext(), "서버와 통신하지 못했습니다", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                task.execute();
                            }
                        });
                        builder.show();
                    }
                });
            }


            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return LIST.get(position);
        }
    }
    private class RoomStatus {
        String Name, User, StartTime, EndTime, PhoneNum, ExacDate;

        RoomStatus(String exacdate,String name, String user, String start, String end, String phnum)  {
            ExacDate = exacdate;
            Name = name;
            User = user;
            StartTime = start;
            EndTime = end;
            PhoneNum = phnum;
        }

        String getStartTime() {
            return StartTime;
        }
        String getName() {
            return Name;
        }
        String getEndTime() {
            return EndTime;
        }
        String getUser() {
            return User;
        }
        String getPhoneNum() {
            return PhoneNum;
        };
        String getExacDate() {
            return ExacDate;
        }
    }
    private class RoomStatusListAdapter extends BaseAdapter {
        private ArrayList<RoomStatus> LIST = new ArrayList<>();

        void Add(String exacdate, String name, String user, String start, String end, String phnum)
        {
            RoomStatus ps = new RoomStatus(exacdate, name, user, start, end, phnum);
            LIST.add(ps);
        }

        void Del(int pos)
        {
            LIST.remove(pos);
        }
        void DelAll() {
            LIST.clear();
        }

        @Override
        public int getCount() {
            return LIST.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Context context = parent.getContext();

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.roomstatuslayout, parent, false);
            }

            TextView tv_name = convertView.findViewById(R.id.roomname);
            TextView tv_user = convertView.findViewById(R.id.roomuser);
            TextView tv_starttime = convertView.findViewById(R.id.room_usestart);
            TextView tv_endtime = convertView.findViewById(R.id.room_useend);
            TextView tv_phnum = convertView.findViewById(R.id.room_phonenumber);
            Button button = convertView.findViewById(R.id.Btn_cancel);

            final RoomStatus ps = LIST.get(position);
            tv_name.setText(ps.getName());
            tv_user.setText(ps.getUser());
            tv_starttime.setText(ps.getStartTime());
            tv_endtime.setText(ps.getEndTime());
            tv_phnum.setText(ps.getPhoneNum());

            if (ps.getUser().equals(UserName)) {
                button.setVisibility(View.VISIBLE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("확인");
                        builder.setMessage("예약을 삭제합니다");
                        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String roomname = ps.getName();
                                if(roomname.equals("회의실 아라")) roomname = "ROOM1";
                                else if(roomname.equals("회의실 가람")) roomname = "ROOM2";
                                else if(roomname.equals("회의실 마루")) roomname = "ROOM3";
                                String data = "room;" + roomname + ";" + ps.getStartTime() + ";" + ps.getExacDate() + ";";
                                SenderTask task = new SenderTask(data, new SchedAddActivity.ondialogdissmisslistener() {
                                    @Override
                                    public void onevent(int arg) {
                                        if (arg == 1) {
                                            Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                                                finish();
                                        } else
                                            Toast.makeText(getApplicationContext(), "서버와 통신하지 못했습니다", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                task.execute();
                            }
                        });
                        builder.show();
                    }
                });
            }
            return convertView;
        }
        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public Object getItem(int position) {
            return LIST.get(position);
        }
    }


    private class CommunTask extends AsyncTask<Void, Void, Void> {
        AlertDialog.Builder builder = new AlertDialog.Builder(StatusPreviewActivity.this);
        AlertDialog ad;
        StringBuilder sb = new StringBuilder("");

        String trData1 = "RQSTSTATUS";
        String trData2 = "WHOAMI";
        String data = null;

        int consuccess = 0;
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
                }
            );
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            char[] buf = new char[512];
            int atmpt = 0;
            while(consuccess == 0 && userquit == 0 && atmpt < 5)
            {
                atmpt++;
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(Server_address, Server_port), 500);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                    output.write(trData1.getBytes());

                    int rlen;
                    while((rlen = input.read(buf)) > 0)
                        sb.append(new String(buf, 0, rlen));

                    data = sb.toString();

                    input.close();
                    output.close();
                    socket.close();


                    socket = new Socket();

                    socket.connect(new InetSocketAddress(Server_address, Server_port), 500);
                    output = new DataOutputStream(socket.getOutputStream());
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));


                    output.write(trData2.getBytes());
                    try
                    { Thread.sleep(300); }
                    catch(InterruptedException e)
                    { e.printStackTrace(); }
                    output.write(PHONENUMBER.getBytes());

                    sb = new StringBuilder();

                    while((rlen = input.read(buf)) > 0)
                        sb.append(new String(buf, 0, rlen));

                    UserName = sb.toString();

                    input.close();
                    output.close();
                    socket.close();


                    Log.d("TAG", "You A " + UserName);



                    consuccess = 1;
                } catch(IOException e) {
                    e.printStackTrace();
                    consuccess = 0;
                }






            }

            if(userquit == 1)  return null;

            int n1 = 0,n2 = 0;

            try {
                int pos = data.indexOf(';')+1;
                n1 = Integer.parseInt(data.substring(0, pos-1));
                data = data.substring(pos);

                for(int j=0;j<n1;j++) {
                    String strs[] = new String[5];
                    for (int i = 0; i < 5; i++) {
                        pos = data.indexOf(';') + 1;
                        strs[i] = data.substring(0, pos-1);
                        data = data.substring(pos);
                    }

                    final String fixedstr[] = strs;
                    StatusPreviewActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peoplestatusadapter.Add(fixedstr[0], fixedstr[1], fixedstr[2], fixedstr[3], fixedstr[4]);
                            peoplestatusadapter.notifyDataSetChanged();
                        }
                    });
                }

                pos = data.indexOf(';')+1;
                n2 = Integer.parseInt(data.substring(0, pos-1));
                data = data.substring(pos);

                for(int j=0;j<n2;j++) {
                    String strs[] = new String[6];
                    for (int i = 0; i < 6; i++) {
                        pos = data.indexOf(';') + 1;
                        strs[i] = data.substring(0, pos-1);
                        data = data.substring(pos);
                    }

                    final String fixedstr[] = strs;
                    StatusPreviewActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(fixedstr[0].equals("ROOM1")) fixedstr[0] = "회의실 아라";
                            else if(fixedstr[0].equals("ROOM2")) fixedstr[0] = "회의실 가람";
                            else if(fixedstr[0].equals("ROOM3")) fixedstr[0] = "회의실 마루";
                            roomstatuslistadapter.Add(fixedstr[5], fixedstr[0], fixedstr[1], fixedstr[2], fixedstr[3], fixedstr[4]);
                            roomstatuslistadapter.notifyDataSetChanged();
                        }
                    });
                }

                int num;

                pos = data.indexOf(';')+1;
                num = Integer.parseInt(data.substring(0, pos-1));
                data = data.substring(pos);

//                Log.d("TAG", "NUM : " + num);

                for(int i=0;i<num;i++)
                {
                    String strdate;
                    pos = data.indexOf(';') + 1;
                    strdate = data.substring(0, pos-1);
                    data = data.substring(pos);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    compactCalendarView.addEvent(new Event(Color.RED, sdf.parse(strdate).getTime(), strdate));
                }
            } catch (final Exception e) {
                e.printStackTrace();
                StatusPreviewActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StatusPreviewActivity.this,e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
            }

            final int num1 = n1, num2 = n2;
            StatusPreviewActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_count1.setText(String.format(Locale.KOREA, "%d 건", num1));
                    tv_count2.setText(String.format(Locale.KOREA, "%d 건", num2));
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

    private class SenderTask extends AsyncTask<Void, Void, Void> {

        private SchedAddActivity.ondialogdissmisslistener mListener;
        int successed = 0;
        int userquit = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(StatusPreviewActivity.this);
        AlertDialog ad ;
        String data, headdata = "DELISSUE";


        SenderTask(String d, SchedAddActivity.ondialogdissmisslistener listener)  {
            data = d;
            mListener = listener;
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
            int atmpt = 0;
            byte b[] = new byte[512];
            while(successed == 0 && userquit == 0 && atmpt < 5)
            {
                atmpt++;
                try
                {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(Server_address, Server_port), 500);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    DataInputStream input = new DataInputStream(socket.getInputStream());

                    output.write(headdata.getBytes());

                    try
                    { Thread.sleep(300); }
                    catch(InterruptedException e)
                    { e.printStackTrace(); }

                    output.write(data.getBytes());

                    int rlen = input.read(b); // never receive long data
                    String resp = new String(b, 0, rlen);

                    successed = 0;
                    if(resp.equals("deleted"))
                        successed = 1;

                    output.close();
                    socket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    successed = 0;
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ad.dismiss();
            mListener.onevent(successed);
            super.onPostExecute(aVoid);
        }
    }




}

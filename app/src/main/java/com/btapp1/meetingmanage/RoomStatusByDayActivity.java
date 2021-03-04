package com.btapp1.meetingmanage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class RoomStatusByDayActivity extends AppCompatActivity {
    String PHONENUMBER;

    TextView tv_title, tv_itemcount;
    ListView listview;

    String Server_address;
    int Server_port;

    RoomStatusListAdapter roomstatuslistadapter;

    public interface guiaddrequestlistener { void onIRQ(final String str[]); }

    String UserName = null;


    guiaddrequestlistener listener = new guiaddrequestlistener() {
        @Override
        public void onIRQ(final String strs[]) {
            Log.d("TAG", strs[0] + " " + strs[1] + " " + strs[2] + " " + strs[3] + " " + strs[4] + " " + strs[5] );

            RoomStatusByDayActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(strs[0].equals("ROOM1")) strs[0] = "회의실 아라";
                    else if(strs[0].equals("ROOM2")) strs[0] = "회의실 가람";
                    else if(strs[0].equals("ROOM3")) strs[0] = "회의실 마루";
                    roomstatuslistadapter.Add(strs[5], strs[0], strs[1], strs[2], strs[3], strs[4]);
                    roomstatuslistadapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_status_by_day);

        try {
            PHONENUMBER = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
        } catch (SecurityException e) {
            Toast.makeText(this, "전화번호를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        layout();

        Intent intent = getIntent();
        String SelectedDay = intent.getStringExtra("SELECTEDDATE");
        String title = SelectedDay + " 회의실 현황";
        Server_address = intent.getStringExtra("SERVERADDR");
        Server_port = intent.getIntExtra("SERVERPORT", 0);
        tv_title.setText(title);

        roomstatuslistadapter = new RoomStatusListAdapter();
        listview.setAdapter(roomstatuslistadapter);

        CommunTask task = new CommunTask(SelectedDay);
        task.execute();

    }


    void layout()
    {
        tv_title = findViewById(R.id.tv_title);
        tv_itemcount = findViewById(R.id.tv_itemcount);
        listview = findViewById(R.id.statuslistthatday);
    }


    private class CommunTask extends AsyncTask<Void, Void, Void>  {
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomStatusByDayActivity.this);
        AlertDialog ad;
        String TData, data = null;
        StringBuilder sb;
        String head = "RMSTTBYDAY";

        String trData2 = "WHOAMI";

        int success = 0;
        int userquit = 0;

        CommunTask(String tdata)
        {
            TData = tdata;
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

            char[] buf = new char[512];
            int atmpt=0;
            while(success == 0 && userquit == 0 && atmpt < 5)
            {
                atmpt++;
                try
                {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(Server_address, Server_port), 500);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                    output.write(head.getBytes());
                    try
                    { Thread.sleep(300); }
                    catch(InterruptedException e)
                    { e.printStackTrace(); }
                    output.write(TData.getBytes());

                    int rlen;

                    sb = new StringBuilder();
                    while((rlen = input.read(buf)) > 0)
                        sb.append(new String(buf, 0, rlen));
                    data = sb.toString();

                    Log.d("TAG", "ByDay : " + data);

//                    rlen = input.read(buf);
//                    data = new String(buf, 0, rlen);

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


                    success = 1;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    success = 0;
                }
            }
            if(success == 0)
                return null;

            try
            {
                int pos = data.indexOf(';')+1;
                int num = Integer.parseInt(data.substring(0, pos-1));
                data = data.substring(pos);

                for(int i=0;i<num;i++)
                {
                    String strs[] = new String[6];
                    for (int j = 0; j < 6; j++) {
                        pos = data.indexOf(';') + 1;
                        strs[j] = data.substring(0, pos-1);
                        data = data.substring(pos);
                    }

                    //TODO needs to improve
                    listener.onIRQ(strs);
                }

            }
            catch (final Exception e)
            {
                e.printStackTrace();
                RoomStatusByDayActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RoomStatusByDayActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ad.dismiss();


            super.onPostExecute(aVoid);
        }
    }

    @Override
    protected void onDestroy() {
        roomstatuslistadapter.DelAll();
        super.onDestroy();
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
        void DelAll()
        {
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

    private class SenderTask extends AsyncTask<Void, Void, Void> {

        private SchedAddActivity.ondialogdissmisslistener mListener;
        int successed = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomStatusByDayActivity.this);
        AlertDialog ad ;
        String data, headdata = "DELISSUE";

        int consucccess = 0;
        int userquit = 0;

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
            while(consucccess == 0 && userquit == 0 && atmpt < 5)
            {
                atmpt++;
                try
                {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(Server_address, Server_port), 500);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
//                DataInputStream input = new DataInputStream(socket.getInputStream());
                    output.write(headdata.getBytes());
                    try
                    { Thread.sleep(300); }
                    catch(InterruptedException e)
                    { e.printStackTrace(); }

                    output.write(data.getBytes());
                    output.close();
                    socket.close();
                    consucccess = 1;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    consucccess = 0;
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ad.dismiss();
            mListener.onevent(consucccess);
            super.onPostExecute(aVoid);
        }
    }

}


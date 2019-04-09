package com.example.aymen.androidchat;


import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatBoxActivity extends AppCompatActivity {
    public RecyclerView myRecylerView ;
    public List<Message> MessageList ;
    public ChatBoxAdapter chatBoxAdapter;
    public  EditText messagetxt ;
    public  Button send ;
    //declare socket object
    private Socket socket;
    String server_nickname = "";


    public String Nickname ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);

        Constants.coloursMap = new HashMap<>();

        messagetxt = (EditText) findViewById(R.id.message) ;
        send = (Button)findViewById(R.id.send);
        //get the intent from MainActivity
        Intent intent = getIntent();
        if (intent != null)
        {
            // get the nickame of the user
            Nickname= (String)intent.getExtras().getString(Constants.NICKNAME);
            Constants.name = new String(Nickname);
            //connect you socket client to the server
            try {
                socket = IO.socket(Constants.IP_ADDR);
                socket.connect();
                socket.emit("join", Nickname);
            } catch (URISyntaxException e) {
                e.printStackTrace();

            }
        }

       //setting up recylerview
        MessageList = new ArrayList<>();
        myRecylerView = (RecyclerView) findViewById(R.id.messagelist);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        myRecylerView.setLayoutManager(mLayoutManager);
        myRecylerView.setItemAnimator(new DefaultItemAnimator());



        // message send action
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //retrieve the nickname and the message content and fire the event messagedetection
                if(!messagetxt.getText().toString().isEmpty()){
                    socket.emit("messagedetection",Nickname,messagetxt.getText().toString());

                    messagetxt.setText(" ");
                }


            }
        });

        //implementing socket listeners
        socket.on("userjoinedthechat", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        String data = (String) args[0];
                        JSONObject data = (JSONObject) args[0];
                        try {
                            //extract data from fired event

                            server_nickname = data.getString("senderNickname");
                            String color = data.getString("random_color");
                            Constants.coloursMap.put(server_nickname, color); //create a map colour between username and the colour from server
//                            if(server_nickname.equals(Nickname)){
//                                Constants.color = color.intValue();
//                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(ChatBoxActivity.this,"user " + server_nickname + " joined the chat",Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
        socket.on("userdisconnect", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String data = (String) args[0];

                        Toast.makeText(ChatBoxActivity.this,data,Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            //extract data from fired event

                            String nickname = data.getString("senderNickname");
                            String message = data.getString("message");

                            Message m = new Message(nickname,message);
                            MessageList.add(m);
                            // add the new updated list to the adapter
                            chatBoxAdapter = new ChatBoxAdapter(MessageList);

                            // notify the adapter to update the recycler view
                            chatBoxAdapter.notifyDataSetChanged();

                            //set the adapter for the recycler view
                            myRecylerView.setAdapter(chatBoxAdapter);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });
            }
        });

        socket.on("message_from_past_event", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
//                    public boolean equals(Object obj) {
//                        return ((Message)this).getMessage().equals(((Message)obj).getMessage());
//                    }

                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            //extract data from fired event

                            String nickname = data.getString("senderNickname");
                            /* This is the client who left and came back */
                            String manshow = data.getString("manshow");
                            if(!manshow.equals(Nickname)){
                                return;
                            }
                            String message = data.getString("message_from_past");
                            String color = data.getString("color");



                            if(!Constants.coloursMap.containsKey(nickname))
                                Constants.coloursMap.put(nickname, color); //recreate the mapping

                            Message m = new Message(nickname,message);
                            if(!MessageList.contains(m))
                                MessageList.add(m);
                            // add the new updated list to the adapter
                            chatBoxAdapter = new ChatBoxAdapter(MessageList);

                            // notify the adapter to update the recycler view
                            chatBoxAdapter.notifyDataSetChanged();

                            //set the adapter for the recycler view
                            myRecylerView.setAdapter(chatBoxAdapter);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });
            }
        });

        socket.on("colors_update_event", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            //extract data from fired event
                            String manshow = data.getString("manshow");
                            if(!manshow.equals(Nickname)){
                                return;
                            }
                            String nickname = data.getString("senderNickname");
                            String color = data.getString("colors_update");


                            if(!Constants.coloursMap.containsKey(nickname))
                                Constants.coloursMap.put(nickname, color); //recreate the mapping

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.emit("disconnected",Constants.name);
        socket.disconnect();
  }
}

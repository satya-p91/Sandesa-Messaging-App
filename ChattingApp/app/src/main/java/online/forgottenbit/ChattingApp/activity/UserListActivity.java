package online.forgottenbit.ChattingApp.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.adapter.ChatRoomsAdapter;
import online.forgottenbit.ChattingApp.adapter.UserRoomAdapter;
import online.forgottenbit.ChattingApp.app.Config;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.fcm.NotificationsUtils;
import online.forgottenbit.ChattingApp.helper.SimpleDividerItemDecoration;
import online.forgottenbit.ChattingApp.model.ChatRoom;
import online.forgottenbit.ChattingApp.model.Message;
import online.forgottenbit.ChattingApp.model.UserRoom;

public class UserListActivity extends AppCompatActivity {

    final String TAG = UserListActivity.class.getSimpleName();


    private BroadcastReceiver mrb;
    private ArrayList<UserRoom> userRoomArrayList;
    private UserRoomAdapter mAdapter;
    private RecyclerView rv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);






        userRoomArrayList = new ArrayList<>();
        mAdapter = new UserRoomAdapter(this, userRoomArrayList);


        fetchUsers();


        rv =findViewById(R.id.recycler_view);


        mrb = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.e("test User msg","get typ oncreate");


                if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    handlePushNotification(intent);
                    Log.e("test User msg","get typ");



                }
            }
        };


        LinearLayoutManager layoutManager = new LinearLayoutManager(UserListActivity.this);
        rv.setLayoutManager(layoutManager);

        rv.addItemDecoration(new SimpleDividerItemDecoration(
                getApplicationContext()
        ));

        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(mAdapter);


        rv.addOnItemTouchListener(new UserRoomAdapter.RecyclerTouchListener(getApplicationContext(), rv, new UserRoomAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // when chat is clicked, launch full chat thread activity


                UserRoom userRoom = userRoomArrayList.get(position);


                Intent intent = new Intent(UserListActivity.this, UserRoomActivity.class);
                intent.putExtra("user_room_id", userRoom.getUserId());
                intent.putExtra("name", userRoom.getUserName());

                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {
                //here we can provide setting for that group
            }
        }));


    }


    @Override
    protected void onResume() {
        super.onResume();

        // register FCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mrb,
                new IntentFilter(Config.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mrb,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        // clearing the notification tray
        NotificationsUtils.clearNotifications();
    }




    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mrb);
        super.onPause();
    }



    /**
     * Handles new push notification
     */
    private void handlePushNotification(Intent intent) {
        int type = Integer.parseInt(intent.getStringExtra("type"));
        Log.e("test User msg","get typ");

        if (type == Config.PUSH_TYPE_USER) {
            Message message = (Message) intent.getSerializableExtra("message");
            String userRoomId = intent.getStringExtra("user_id");



            if (message != null && userRoomId != null) {
                Log.e("test User msg","get typ2");

                updateRow(userRoomId, message);
                Log.e("test User msg","get typ3");

            }
        } else if (type == Config.PUSH_TYPE_CHATROOM) {
            Message message = (Message) intent.getSerializableExtra("message");
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
        }


    }

    /**
     * Updates the chat list unread count and the last message
     */
    private void updateRow(String userRoomId, Message message) {
        for (UserRoom ur : userRoomArrayList) {
            if (ur.getUserId().equals(userRoomId)) {
                int index = userRoomArrayList.indexOf(ur);
                ur.setLastMessage(message.getMessage());
                ur.setUnreadCount(ur.getUnreadCount()+1);
                userRoomArrayList.remove(index);
                userRoomArrayList.add(index, ur);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }



    private void fetchUsers(){
        try{

            StringRequest strReq = new StringRequest(Request.Method.GET,
                    EndPoints.USERS_LIST, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.e(TAG, "responseGETCHAt :" + response);

                    try {
                        JSONObject obj = new JSONObject(response);

                        //check for error flag

                        if (obj.getBoolean("error") == false) {
                            JSONArray userRoomArray = obj.getJSONArray("all_users");

                            for (int i = 0; i < userRoomArray.length(); i++) {
                                JSONObject userRoomsObj = (JSONObject) userRoomArray.get(i);

                                if(Integer.parseInt(userRoomsObj.getString("user_id")) != Integer.parseInt(MyApplication.getInstance().getPrefManager().getUser().getId())){

                                    UserRoom ur = new UserRoom();
                                    ur.setUserId(userRoomsObj.getString("user_id"));
                                    ur.setUserName(userRoomsObj.getString("name"));
                                    ur.setLastMessage("");
                                    ur.setUnreadCount(0);
                                    ur.setTimestamp(userRoomsObj.getString("created_at"));
                                    userRoomArrayList.add(ur);

                                }

                            }
                        } else {
                            // error in fetching chat rooms
                            Log.e("1111111111111", obj.getJSONObject("error").toString());
                            Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        Log.e("222222222", "json parsing error: " + e.getMessage());
                        Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    mAdapter.notifyDataSetChanged();


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                    Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });


            //Adding request to request queue
            MyApplication.getInstance().addToRequestQueue(strReq);

        }catch (Exception e){
            Log.e(TAG, "checkMAinActivity101: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "checkMAinActivity101: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }


}

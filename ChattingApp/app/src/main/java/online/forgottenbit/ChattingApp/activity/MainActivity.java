package online.forgottenbit.ChattingApp.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import online.forgottenbit.ChattingApp.MenuItems.About;
import online.forgottenbit.ChattingApp.MenuItems.CreateGroup;
import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.adapter.ChatRoomsAdapter;
import online.forgottenbit.ChattingApp.app.Config;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.fcm.FcmInstanceIdServiceS;
import online.forgottenbit.ChattingApp.fcm.FcmIntentService;
import online.forgottenbit.ChattingApp.fcm.NotificationsUtils;
import online.forgottenbit.ChattingApp.helper.SimpleDividerItemDecoration;
import online.forgottenbit.ChattingApp.model.ChatRoom;
import online.forgottenbit.ChattingApp.model.Message;
import online.forgottenbit.ChattingApp.model.User;

public class MainActivity extends AppCompatActivity {


    private  static  String TAG = MainActivity.class.getSimpleName();

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ArrayList<ChatRoom> chatRoomArrayList;
    private ChatRoomsAdapter mAdapter;
    private RecyclerView recyclerView;

    Button button;



    String token;


    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        /**
         * Check for login session. If not logged in launch
         * login activity
         * */

        if(MyApplication.getInstance().getPrefManager().getUser() == null){
            launchLoginActivity();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //for user message activity
        button = findViewById(R.id.user_message);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UserListActivity.class);
                startActivity(intent);
            }
        });










        FirebaseApp.initializeApp(MainActivity.this);

        registerGCM();

        fetchChatRooms();

        Log.e("tst","fdsa");
        try {

            Intent i = getIntent();

            Bundle b = i.getExtras();
            Log.e("test", b.toString());
            String c = b.toString().replace("data=", "'data':");
            Log.e("message", c);


        }catch (Exception e){
            Log.e("test1",e.getMessage());
        }
        Log.e("tst","fdsa");



        recyclerView =findViewById(R.id.recycler_view);

        /**
         * Broadcast receiver calls in two scenarios
         * 1. gcm registration is completed
         * 2. when new push notification is received
         * */



        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(Config.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    subscribeToGlobalTopic();



                } else if (intent.getAction().equals(Config.SENT_TOKEN_TO_SERVER)) {
                    // gcm registration id is stored in our server's MySQL
                    Log.e(TAG, "GCM registration id is sent to our server");

                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    handlePushNotification(intent);


                }
            }
        };







        chatRoomArrayList = new ArrayList<>();
        mAdapter = new ChatRoomsAdapter(this, chatRoomArrayList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(
                getApplicationContext()
        ));

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);


        recyclerView.addOnItemTouchListener(new ChatRoomsAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new ChatRoomsAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // when chat is clicked, launch full chat thread activity


                ChatRoom chatRoom = chatRoomArrayList.get(position);


                    subscribeToAllTopics(Integer.parseInt(chatRoom.getId()));

                Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
                intent.putExtra("chat_room_id", chatRoom.getId());
                intent.putExtra("name", chatRoom.getName());
                intent.putExtra("admin_name",chatRoom.getAdminName());
                intent.putExtra("admin_email",chatRoom.getAdminEmail());

                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {
                //here we can provide setting for that group
            }
        }));


    }




    /**
     * Handles new push notification
     */
    private void handlePushNotification(Intent intent) {
        int type = Integer.parseInt(intent.getStringExtra("type"));
        // if the push is of chat room message
        // simply update the UI unread messages count
        if (type == Config.PUSH_TYPE_CHATROOM) {
            Message message = (Message) intent.getSerializableExtra("message");
            String chatRoomId = intent.getStringExtra("chat_room_id");

            if (message != null && chatRoomId != null) {
                updateRow(chatRoomId, message);
            }
        } else if (type == Config.PUSH_TYPE_USER) {
            // push belongs to user alone
            // just showing the message in a toast
            Message message = (Message) intent.getSerializableExtra("message");
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
        }


    }

    /**
     * Updates the chat list unread count and the last message
     */
    private void updateRow(String chatRoomId, Message message) {
        for (ChatRoom cr : chatRoomArrayList) {
            if (cr.getId().equals(chatRoomId)) {
                int index = chatRoomArrayList.indexOf(cr);
                cr.setLastMessage(message.getMessage());
                cr.setUnreadCount(cr.getUnreadCount()+1);
                chatRoomArrayList.remove(index);
                chatRoomArrayList.add(index, cr);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }



    /**
     * fetching the chat rooms by making http call
     */


    private void fetchChatRooms(){
        try{

            StringRequest strReq = new StringRequest(Request.Method.GET,
                    EndPoints.CHAT_ROOMS, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.e(TAG, "responseGETCHAt :" + response);

                    try {
                        JSONObject obj = new JSONObject(response);

                        //check for error flag

                        if (obj.getBoolean("error") == false) {
                            JSONArray chatRoomsArray = obj.getJSONArray("chat_rooms");

                            for (int i = 0; i < chatRoomsArray.length(); i++) {
                                JSONObject chatRoomsObj = (JSONObject) chatRoomsArray.get(i);
                                ChatRoom cr = new ChatRoom();
                                cr.setId(chatRoomsObj.getString("chat_room_id"));
                                cr.setName(chatRoomsObj.getString("name"));
                                cr.setLastMessage("");
                                cr.setUnreadCount(0);
                                cr.setTimestamp(chatRoomsObj.getString("created_at"));

                                JSONObject adminObj = chatRoomsObj.getJSONObject("admin");

                                cr.setAdminName(adminObj.getString("name"));
                                cr.setAdminEmail(adminObj.getString("email"));

                                chatRoomArrayList.add(cr);
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

                    // subscribing to all chat room topics
                    //subscribeToAllTopics();

                    //if(MyApplication.getInstance().getPrefManager().getUser() == null)


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



    // subscribing to global topic
    private void subscribeToGlobalTopic() {


        try{


            FirebaseMessaging.getInstance().subscribeToTopic("global").addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> t) {
                    String msg = "Subscribe to topic : ";
                    if (!t.isSuccessful()) {
                        msg = "Error couldn't subscribe to topic ";
                    }
                    Log.d(TAG, msg);
                    Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
                }
            });

        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
    }



    // Subscribing to topic that been clicked
    // each topic name starts with `topic_` followed by the ID of the chat room
    // Ex: topic_1, topic_2


    private void subscribeToAllTopics(final int id){

            try{
                FirebaseMessaging.getInstance().subscribeToTopic("topic_"+id).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> t) {
                        String msg = "Subscribe to topic : topic_"+id;
                        if (!t.isSuccessful()) {
                            msg = "Error couldn't subscribe to topic : topic_"+id;
                        }
                        Log.e(TAG, msg);
                        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
                    }
                });

            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }

    }


    private void launchLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // register FCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        // clearing the notification tray
        NotificationsUtils.clearNotifications();
    }




    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }



    // starting the service to register with GCM
    private void registerGCM() {

         sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        try{

            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                    if(!task.isSuccessful()){
                        Log.e(TAG, "getInstanceId failed", task.getException());
                        return;
                    }
                    token = task.getResult().getToken();

                    Log.e(TAG,"FCM reg token: "+token);

                    //sending reg id to our server
                    sendRegistrationToServer(token);

                }
            });



            sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, true).apply();

        } catch (Exception e){
            Log.e(TAG,"Failed to complete token refresh",e);

            sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, false).apply();
        }

        //notifu ui that registration has completed, so the process indicator can be hidden

        Intent registrationComplete = new Intent(Config.REGISTRATION_COMPLETE);
        registrationComplete.putExtra("token  ",token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);

    }




    private void sendRegistrationToServer( final String token) {

        User user = MyApplication.getInstance().getPrefManager().getUser();

        if (user == null) {
            // TODO
            // user not found, redirecting him to login screen
            return;
        }

        String endPoint = EndPoints.USER.replace("_ID_", user.getId());


        Log.e(TAG, "endpoint: " + endPoint);


        StringRequest strReq = new StringRequest(Request.Method.PUT,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                Log.e("333333333", "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (obj.getBoolean("error") == false) {

                        // broadcasting token sent to server
                        Intent registrationComplete = new Intent(Config.SENT_TOKEN_TO_SERVER);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(registrationComplete);
                    } else {
                        Toast.makeText(getApplicationContext(), "Unable to send gcm registration id to our sever. " + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }


                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("fcm", token);

                Log.e(TAG, "params: " + params.toString());
                return params;
            }
        };

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);

        //will work on it later
    }














    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_logout:
                MyApplication.getInstance().logout();
                break;
            case R.id.about:
                Intent intent = new Intent(MainActivity.this, About.class);
                startActivity(intent);
                break;
            case R.id.user:
                Intent i = new Intent(MainActivity.this, online.forgottenbit.ChattingApp.MenuItems.User.class);
                startActivity(i);
                break;
            case R.id.create_chat_room:
                Intent intentCreateGroup = new Intent(MainActivity.this, CreateGroup.class);
                startActivity(intentCreateGroup);
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }


}

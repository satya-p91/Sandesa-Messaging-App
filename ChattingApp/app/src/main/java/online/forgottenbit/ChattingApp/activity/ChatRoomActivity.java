package online.forgottenbit.ChattingApp.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import online.forgottenbit.ChattingApp.MenuItems.About;
import online.forgottenbit.ChattingApp.MenuItems.GroupInfo;
import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.adapter.ChatRoomThreadAdapter;
import online.forgottenbit.ChattingApp.app.Config;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.fcm.NotificationsUtils;
import online.forgottenbit.ChattingApp.model.Message;
import online.forgottenbit.ChattingApp.model.User;

public class ChatRoomActivity extends AppCompatActivity {


    private String TAG = ChatRoomActivity.class.getSimpleName();


    private String AES = "AES",pass = "test";
    private String chatRoomId;
    private RecyclerView recyclerView;
    private ChatRoomThreadAdapter mAdapter;
    private ArrayList<Message> messageArrayList;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private EditText inputMessage;
    private Button btnSend;
    private String adminEmail,title,adminName;






    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        inputMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btn_send);


        Intent intent = getIntent();
        chatRoomId = intent.getStringExtra("chat_room_id");
        title = intent.getStringExtra("name");

        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        adminName = intent.getStringExtra("admin_name");
        adminEmail = intent.getStringExtra("admin_email");


        if(chatRoomId == null){
            Toast.makeText(getApplicationContext(),"chat room not found",Toast.LENGTH_LONG).show();
            Log.e(TAG,"chat room not found by chatRoomId");
            finish();
        }

        recyclerView = findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();

        // self user id is to identify the message owner
        String selfUserId = MyApplication.getInstance().getPrefManager().getUser().getId();

        mAdapter = new ChatRoomThreadAdapter(this,messageArrayList,selfUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Config.PUSH_NOTIFICATION)){

                    //new push notificaiton received
                    handlePushNotification(intent);

                }
            }
        };

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        fetchChatThread();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // registering the receiver for new notification
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        NotificationsUtils.clearNotifications();
    }


    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    private void handlePushNotification(Intent intent){
        Message message = (Message) intent.getSerializableExtra("message");
        String cid = intent.getStringExtra("chat_room_id");

        if (message != null && cid != null && cid==chatRoomId ) {
            messageArrayList.add(message);
            if(mAdapter.getItemCount()>1){
                recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount()-1);
            }
        }

    }



    /**
     * Posting a new message in chat room
     * will make an http call to our server. Our server again sends the message
     * to all the devices as push notification
     * */


    private void sendMessage(){
        final String message = this.inputMessage.getText().toString().trim();

        if(TextUtils.isEmpty(message)){
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = EndPoints.CHAT_ROOM_MESSAGE.replace("_ID_",chatRoomId);

        Log.e(TAG,"Endpoint : "+endPoint);
        this.inputMessage.setText("");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response of message sent: " + response);

                try {
                    JSONObject Obj = new JSONObject(response);

                    if (Obj.getBoolean("error") == false) {

                        JSONObject commentObj = Obj.getJSONObject("message");

                        String commentId = commentObj.getString("message_id");
                        String commentText = decrypt(commentObj.getString("message"),pass);
                        String createdAt = commentObj.getString("created_at");

                        JSONObject userObj = Obj.getJSONObject("user");
                        String userId = userObj.getString("user_id");
                        String userName = userObj.getString("name");
                        User user = new User(userId, userName, null);

                        Message message = new Message();
                        message.setId(commentId);
                        message.setMessage(commentText);
                        message.setCreatedAt(createdAt);
                        message.setUser(user);


                        messageArrayList.add(message);

                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1) {
                            // scrolling to bottom of the recycler view
                            recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount() - 1);
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "" + Obj.getString("message"), Toast.LENGTH_LONG).show();
                        Log.e(TAG, Obj.getString("message"));
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                }catch (Exception e){
                    Log.e("error while decrypth",e.getMessage());
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    inputMessage.setText(decrypt(message, pass));
                }catch (Exception e){
                    Log.e("error decrypt",e.getMessage());
                }
            }
        }){

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                try {
                    params.put("user_id", MyApplication.getInstance().getPrefManager().getUser().getId());
                    params.put("message", encrypt(message, pass));
                }catch (Exception e){
                    Log.e(TAG, "Encryption error " + params.toString());
                }

                Log.e(TAG, "Params: " + params.toString());

                return params;
            };

        };


        // disabling retry policy so that it won't make
        // multiple http calls


        int socketTimeout = 0;

        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        strReq.setRetryPolicy(policy);

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);


    }




    /**
     * Fetching all the messages of a single chat room
     * */


    private void fetchChatThread(){
        String endPoint  = EndPoints.CHAT_THREAD.replace("_ID_",chatRoomId);

        Log.e(TAG,"Endpoint for fetching all message of chatroom"+endPoint);

        StringRequest strReq = new StringRequest(Request.Method.GET,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (obj.getBoolean("error") == false) {
                        JSONArray commentsObj = obj.getJSONArray("messages");

                        for (int i = 0; i < commentsObj.length(); i++) {
                            JSONObject commentObj = (JSONObject) commentsObj.get(i);

                            String commentId = commentObj.getString("message_id");
                            String commentText = decrypt(commentObj.getString("message"),pass);
                            String createdAt = commentObj.getString("created_at");

                            JSONObject userObj = commentObj.getJSONObject("user");
                            String userId = userObj.getString("user_id");
                            String userName = userObj.getString("username");
                            User user = new User(userId, userName, null);

                            Message message = new Message();
                            message.setId(commentId);
                            message.setMessage(commentText);
                            message.setCreatedAt(createdAt);
                            message.setUser(user);

                            messageArrayList.add(message);
                        }

                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1) {
                            recyclerView.getLayoutManager().scrollToPosition( mAdapter.getItemCount() - 1);
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Log.e(TAG, "decryption error: " + e.getMessage());
                }
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat_room, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.unsubscribe:
                 String s = "topic_"+chatRoomId;
                 unsubscribeToTopic(s);
                 break;
            case R.id.group_info:
                Intent i = new Intent(ChatRoomActivity.this, GroupInfo.class);
                i.putExtra("grp_id",chatRoomId);
                i.putExtra("grp_name",title);
                i.putExtra("admin_name",adminName);
                i.putExtra("admin_email",adminEmail);
                startActivity(i);
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public  void unsubscribeToTopic(final String topic){

            try{
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> t) {
                        String msg = "Unsubscribed to topic :"+topic;
                        if (!t.isSuccessful()) {
                            msg = "Error: couldn't Unsubscribe to topic : "+topic;
                        }
                        Log.e(TAG, msg);
                        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
                    }
                });

            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }

    }


    private String decrypt(String ope, String pass) throws Exception {
        SecretKeySpec key = generateKey(pass);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decode = Base64.decode(ope,Base64.DEFAULT);
        byte[] decodeValue = c.doFinal(decode);
        String a = new String(decodeValue);
        return a;

    }

    private String encrypt(String ip, String pass) throws Exception{
        SecretKeySpec key = generateKey(pass);

        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE,key);
        byte[] eVal = c.doFinal(ip.getBytes());
        String encryptedValue = Base64.encodeToString(eVal,Base64.DEFAULT);
        return encryptedValue;
    }

    private SecretKeySpec generateKey(String pass) throws Exception{
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = pass.getBytes("UTF-8");
        messageDigest.update(bytes,0,bytes.length);
        byte[] key = messageDigest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key,"AES");
        return secretKeySpec;
    }


}

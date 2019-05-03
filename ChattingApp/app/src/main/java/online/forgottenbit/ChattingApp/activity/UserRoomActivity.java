package online.forgottenbit.ChattingApp.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.adapter.ChatRoomThreadAdapter;
import online.forgottenbit.ChattingApp.adapter.UserRoomThreadAdapter;
import online.forgottenbit.ChattingApp.app.Config;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.fcm.NotificationsUtils;
import online.forgottenbit.ChattingApp.model.Message;
import online.forgottenbit.ChattingApp.model.User;
import online.forgottenbit.ChattingApp.model.UserRoom;

public class UserRoomActivity extends AppCompatActivity {


    private String TAG = ChatRoomActivity.class.getSimpleName();


    private String userRoomId,title,AES="AES",pass = "test",commentText,ct;
    private RecyclerView recyclerView;
    private UserRoomThreadAdapter mAdapter;
    private ArrayList<Message> messageArrayList;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private EditText inputMessage;
    private Button btnSend;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_room);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        inputMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btn_send);


        Intent intent = getIntent();
        userRoomId = intent.getStringExtra("user_room_id");
        title = intent.getStringExtra("name");
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(userRoomId == null){
            Toast.makeText(getApplicationContext(),"chat room not found",Toast.LENGTH_LONG).show();
            Log.e(TAG,"chat room not found by chatRoomId");
            finish();
        }

        recyclerView = findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();

        // self user id is to identify the message owner
        String selfUserId = MyApplication.getInstance().getPrefManager().getUser().getId();

        mAdapter = new UserRoomThreadAdapter(this,messageArrayList,selfUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Config.PUSH_NOTIFICATION)){

                    int type = Integer.parseInt(intent.getStringExtra("type"));

                    if( type == Config.PUSH_TYPE_USER){
                        //new push notificaiton received
                        handlePushNotification(intent);
                    }


                }
            }
        };

        btnSend.setOnClickListener(
                new View.OnClickListener() {
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
        String userRoomId = intent.getStringExtra("user_id");

        if (message != null && userRoomId != null) {
            messageArrayList.add(message);
            if(mAdapter.getItemCount()>1){
                recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount()-1);
            }
        }

    }



    /**
     * Posting a new message in user room
     * will make an http call to our server. Our server again sends the message
     * to the receiver device as push notification
     * */


    private void sendMessage(){
        final String message = this.inputMessage.getText().toString().trim();

        if(TextUtils.isEmpty(message)){
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = EndPoints.USER_ROOM_MESSAGE.replace("_ID_",userRoomId);

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
                        if(Integer.parseInt(userRoomId) == 44) {
                            ct = commentObj.getString("message");
                        }else {
                            ct = decrypt(commentObj.getString("message"), pass);
                        }


                        String createdAt = commentObj.getString("created_at");

                        JSONObject userObj = Obj.getJSONObject("user");
                        //String userId = userObj.getString("user_id");
                        //String userName = userObj.getString("name");

                        String userId = MyApplication.getInstance().getPrefManager().getUser().getId();
                        String userName = MyApplication.getInstance().getPrefManager().getUser().getName();

                        User user = new User(userId, userName, null);

                        Message message = new Message();
                        message.setId(commentId);
                        message.setMessage(ct);
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
                    Log.e("error ehile decrypt",e.getMessage());
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                inputMessage.setText(message);
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
     * Fetching all the messages of a single user room
     * */


    private void fetchChatThread(){
        String endPoint  = EndPoints.USER_THREAD;

        Log.e(TAG,"Endpoint for fetching all message of chatroom "+endPoint);

        StringRequest strReq = new StringRequest(Request.Method.POST,
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
                            String createdAt = commentObj.getString("created_at");
                            String msg_sender_id = commentObj.getString("user_id");
                            String msg_receiver_id = commentObj.getString("receiver_id");


                            if(Integer.parseInt(msg_receiver_id) == 44 || Integer.parseInt(msg_sender_id) == 44){
                                commentText = commentObj.getString("message");
                            }else {
                                commentText = decrypt(commentObj.getString("message"),pass);
                            }


                            Message message = new Message();
                            if(msg_sender_id == MyApplication.getInstance().getPrefManager().getUser().getId()){
                                User user = new User(MyApplication.getInstance().getPrefManager().getUser().getId(),title , null);

                                message.setId(commentId);
                                message.setMessage(commentText);
                                message.setCreatedAt(createdAt);
                                message.setUser(user);

                            }else{
                                User user = new User(msg_sender_id,title , null);
                                message.setId(commentId);
                                message.setMessage(commentText);
                                message.setCreatedAt(createdAt);
                                message.setUser(user);
                            }

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
        }){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", MyApplication.getInstance().getPrefManager().getUser().getId());
                params.put("receiver_id", userRoomId);

                Log.e(TAG, "Params: " + params.toString());

                return params;
            };

        };


        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);
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




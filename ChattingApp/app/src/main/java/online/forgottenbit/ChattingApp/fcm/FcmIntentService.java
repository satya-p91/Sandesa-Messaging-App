

package online.forgottenbit.ChattingApp.fcm;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.app.Config;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.model.User;

public class FcmIntentService  extends IntentService  {

    String token;


    private static final String TAG = FcmIntentService.class.getSimpleName();

    public FcmIntentService() {
        super(TAG);
    }




    public static final String KEY = "key";
    public static final String TOPIC = "topic";
    public static final String SUBSCRIBE = "subscribe";
    public static final String UNSUBSCRIBE = "unsubscribe";



    @Override
    protected void onHandleIntent(Intent intent) {
        String key = intent.getStringExtra(KEY);

        switch (key){
            case SUBSCRIBE:
                //subscribe to topic
                String topic = intent.getStringExtra(TOPIC);
                subscribeToTopic(topic);
                break;

            case UNSUBSCRIBE:
                String topic1 = intent.getStringExtra(TOPIC);
                unsubscribeFromTopic(topic1);
                break;

             default:
                 //if key is not specified, register with fcm
                 registerFCM();
        }
    }





    /**
     * Registering with GCM and obtaining the gcm registration id
     */
    private void registerFCM() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        FirebaseApp.initializeApp(getApplicationContext());

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

                Log.e(TAG, "response: " + response);

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
                params.put("fcm_registration_id", token);

                Log.e(TAG, "params: " + params.toString());
                return params;
            }
        };

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);

        //will work on it later
    }


    public void unsubscribeFromTopic(final String topic) {


        try{

            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                    if(!task.isSuccessful()){
                        Log.e(TAG, "getInstanceId failed", task.getException());
                        return;
                    }

                        if(task.getResult().getToken() != null){
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> t) {
                                    String msg = "Subscribe from topic : "+topic;
                                    if (!t.isSuccessful()) {
                                        msg = "Error couldn't unsubscribe from topic "+topic;
                                    }
                                    Log.d(TAG, msg);
                                    Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();

                                }
                            });
                        }

                }
            });

        }catch (Exception e){
            Log.e(TAG,"Error while un-subscribing"+topic+"  Error:  "+e.getMessage());
            Toast.makeText(getApplicationContext(),"Error while un-subscribing"+topic,Toast.LENGTH_LONG).show();
        }




    }

    public void subscribeToTopic(final String topic) {

        try{

            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                    if(!task.isSuccessful()){
                        Log.e(TAG, "getInstanceId failed", task.getException());
                        return;
                    }else{
                        if(task.getResult().getToken() != null){
                            FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> t) {
                                    String msg = "Subscribe to topic : "+topic;
                                    if (!t.isSuccessful()) {
                                        msg = "Error couldn't subscribe to topic "+topic;
                                    }
                                    Log.d(TAG, msg);
                                    Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();

                                }
                            });
                        }

                    }

                }
            });

        }catch (Exception e){
            Log.e(TAG,"Error while subscribing"+topic+"  Error:  "+e.getMessage());
            Toast.makeText(getApplicationContext(),"Error while subscribing"+topic,Toast.LENGTH_LONG).show();
        }


    }
    
}


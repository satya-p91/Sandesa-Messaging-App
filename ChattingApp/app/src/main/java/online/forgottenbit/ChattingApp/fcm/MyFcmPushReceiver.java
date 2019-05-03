package online.forgottenbit.ChattingApp.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.toolbox.StringRequest;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.activity.ChatRoomActivity;
import online.forgottenbit.ChattingApp.activity.LoginActivity;
import online.forgottenbit.ChattingApp.activity.MainActivity;
import online.forgottenbit.ChattingApp.activity.UserRoomActivity;
import online.forgottenbit.ChattingApp.app.Config;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.model.ChatRoom;
import online.forgottenbit.ChattingApp.model.Message;
import online.forgottenbit.ChattingApp.model.User;

public class MyFcmPushReceiver extends FirebaseMessagingService {

    String from;
    String flag;
    String AES = "AES";
    String pass = "test";
    private NotificationsUtils notificationUtils;


    private static final String TAG = MyFcmPushReceiver.class.getSimpleName();


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage == null)
            return;




        //Log.e(TAG, "Notification Body: " + remoteMessage.getData().toString());
        Log.e(TAG, "From: " + remoteMessage.getFrom());

        from = remoteMessage.getFrom();
        Log.e("remoteMessage data",remoteMessage.getData().toString());


        if(MyApplication.getInstance().getPrefManager().getUser() == null){
            // user is not logged in, skipping push notification
            Log.e(TAG, "user is not logged in, skipping push notification");
            return;
        }


        try {

            String b = remoteMessage.getData().toString();
            String c = b.replace("data=","'data':");
            Log.e("message",c);
            JSONObject objt = new JSONObject(c);

            JSONObject obj = objt.getJSONObject("data");

            flag = obj.getString("flag");
            if(flag == null){
                return;
            }

            switch (Integer.parseInt(flag)){
                case Config.PUSH_TYPE_CHATROOM:
                    //push from chatroom
                    handleMessage(obj);
                    break;
                case Config.PUSH_TYPE_USER:
                    Log.e("test1 usr msg",remoteMessage.getData().toString());
                    handleUserMessage(obj);
                    break;
                default:
                    Log.e("error in myfcmpushreceiver on line 98","not going in case 1");

            }


        }catch (Exception e){
            Log.e(TAG,"getting flag from "+e.getMessage());
        }





        //updating on monday


    }



    private void handleMessage(JSONObject data){
        Log.e(TAG, "push json: " + data.toString());
        try {
            JSONObject dobj = data;
            String title = dobj.getString("title");
            String ChatRoomId = dobj.getString("chat_room_id");
            JSONObject mObj = dobj.getJSONObject("message");
            Message message = new Message();


            message.setId(mObj.getString("message_id"));
            message.setCreatedAt(mObj.getString("created_at"));
            JSONObject uObj = dobj.getJSONObject("user");
            // skip the message if the message belongs to same user as
            // the user would be having the same message when he was sending
            // but it might differs in your scenario
            if (uObj.getString("user_id").equals(MyApplication.getInstance().getPrefManager().getUser().getId())) {
                Log.e(TAG, "Skipping the push message as it belongs to same user");
                return;
            }

            if (Integer.parseInt(uObj.getString("user_id")) == 44){
                message.setMessage(mObj.getString("message"));
            }else {
                message.setMessage(decrypt(mObj.getString("message"),pass));
            }

            User user = new User();
            user.setId(uObj.getString("user_id"));
            user.setEmail(uObj.getString("email"));
            user.setName(uObj.getString("name"));
            message.setUser(user);

            // verifying whether the app is in background or foreground
           if (!NotificationsUtils.isAppIsInBackground(getApplicationContext())){
                //app is in foreground, broadcast the push message
                Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
               pushNotification.putExtra("type", "1");
                pushNotification.putExtra("message", message);
                pushNotification.putExtra("chat_room_id", ChatRoomId);
                LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
                // play notification sound
                NotificationsUtils notificationUtils = new NotificationsUtils(getApplicationContext());
                notificationUtils.playNotificationSound();

            }else{
                // app is in background, show the notification in notification tray
                Log.e("test app background","test1");
                Intent resultIntent = new Intent(getApplicationContext(), ChatRoomActivity.class);
                resultIntent.putExtra("chat_room_id", ChatRoomId);
                Log.e("test app background","test2 before");
                showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);

               Log.e("test app background","test3 after");
            }
        }catch (JSONException e){
            Log.e("error in handleMessage method",e.getMessage());
        }catch (Exception e){
            Log.e("error in Decryption ",e.getMessage());

        }

    }






    private void handleUserMessage(JSONObject data){
        Log.e(TAG, "push json: " + data.toString());
        try{
            JSONObject dobj = data;
            String title = dobj.getString("title");
            JSONObject mObj = dobj.getJSONObject("message");
            Message message = new Message();

            message.setId(mObj.getString("message_id"));
            message.setCreatedAt(mObj.getString("created_at"));

            JSONObject uObj = dobj.getJSONObject("user");
            User user = new User();
            user.setId(uObj.getString("user_id"));
            user.setEmail(uObj.getString("email"));
            user.setName(uObj.getString("name"));

            if(Integer.parseInt(user.getId()) == 44){
                message.setMessage(mObj.getString("message"));
            }else {
                message.setMessage(decrypt(mObj.getString("message"),pass));
            }
            message.setUser(user);

            // verifying whether the app is in background or foreground
            if (!NotificationsUtils.isAppIsInBackground(getApplicationContext())){
                //app is in foreground, broadcast the push message
                Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
                pushNotification.putExtra("type", "2");
                pushNotification.putExtra("message", message);
                pushNotification.putExtra("user_id", user.getId());
                LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
                // play notification sound
                NotificationsUtils notificationUtils = new NotificationsUtils(getApplicationContext());
                notificationUtils.playNotificationSound();

            }else{
                // app is in background, show the notification in notification tray
                Log.e("test app background","test1");
                Intent resultIntent = new Intent(getApplicationContext(), UserRoomActivity.class);
                resultIntent.putExtra("chat_room_id", user.getId());
                Log.e("test app background","test2 before");
                showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);

                Log.e("test app background","test3 after");
            }

        }catch (JSONException e){
            Log.e("JSOn error",e.getMessage());
        }catch (Exception e){
            Log.e("Decryption error",e.getMessage());
        }

    }

    /**
     * Showing notification with text and image
     */

    private void showNotificationMessageWithBigImage(Context applicationContext, String title, String message, String timestamp, Intent resultIntent, String image) {


        NotificationsUtils notificationsUtils = new NotificationsUtils(applicationContext);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        notificationsUtils.showNotificationMessage(title, message, timestamp, resultIntent, image);

    }



    /**
     * Showing notification with text only
     */

    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        Log.e("test app background","in func 1");

        notificationUtils = new NotificationsUtils(context);
        Log.e("test app background","in func 2");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Log.e("test app background","in func 3");
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
        Log.e("test app background","in func 4");
    }




    private void sendNotification( String messageBody, String title, String ChatRoomId) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chat_room_id", ChatRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Message",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(Config.NOTIFICATION_ID, notificationBuilder.build());
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

    private SecretKeySpec generateKey(String pass) throws Exception{
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = pass.getBytes("UTF-8");
        messageDigest.update(bytes,0,bytes.length);
        byte[] key = messageDigest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key,"AES");
        return secretKeySpec;
    }




}

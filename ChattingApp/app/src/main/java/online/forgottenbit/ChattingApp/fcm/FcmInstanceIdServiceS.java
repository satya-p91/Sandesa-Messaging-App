package online.forgottenbit.ChattingApp.fcm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessagingService;

import online.forgottenbit.ChattingApp.activity.MainActivity;
import online.forgottenbit.ChattingApp.app.Config;

public class FcmInstanceIdServiceS extends FirebaseMessagingService {

    private static final String TAG = FcmInstanceIdServiceS.class.getSimpleName();


    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);


        Log.e(TAG, "onNewToken");
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, FcmIntentService.class);
        intent.putExtra("key", "register");
        startService(intent);

    }
}

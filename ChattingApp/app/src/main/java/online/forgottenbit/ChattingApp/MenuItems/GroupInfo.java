package online.forgottenbit.ChattingApp.MenuItems;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.activity.MainActivity;

public class GroupInfo extends AppCompatActivity {

    final String TAG = GroupInfo.class.getSimpleName();
    String grpId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);


        TextView grp_name = findViewById(R.id.grp_name);
        TextView admin_name = findViewById(R.id.admin_name);
        TextView admin_email = findViewById(R.id.admin_email);


        Button mute = findViewById(R.id.btn_mute);


        Intent i = getIntent();

        String grpName = i.getStringExtra("grp_name");
        String adminName = i.getStringExtra("admin_name");
        String adminEmail = i.getStringExtra("admin_email");
        grpId ="topic_" + i.getStringExtra("grp_id");



        grp_name.setText(grpName);
        admin_name.setText("Admin Name: "+adminName);
        admin_email.setText("Admin Email: "+adminEmail);


        mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unsubscribeToTopic(grpId);
                Intent i = new Intent(GroupInfo.this, MainActivity.class);
                startActivity(i);
            }
        });



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
}

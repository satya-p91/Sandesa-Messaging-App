package online.forgottenbit.ChattingApp.MenuItems;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.activity.MainActivity;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.model.Message;
import online.forgottenbit.ChattingApp.model.User;

public class CreateGroup extends AppCompatActivity {

    EditText grpName;
    TextView error;
    Button create;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);


        Toolbar t = findViewById(R.id.toolbar);
        setSupportActionBar(t);

        t.setTitle("New Group");

        grpName = findViewById(R.id.input_name);
        error = findViewById(R.id.error_msg);
        create = findViewById(R.id.btn_create);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeGroup();
            }
        });



    }

    private void makeGroup(){
        final String name = this.grpName.getText().toString().trim();

        if(TextUtils.isEmpty(name)){
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = EndPoints.CREATE_NEW_CHAT_ROOM;

        Log.e("Create Group","Endpoint : "+endPoint);
        this.grpName.setText("");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("Create Group", "response of message sent: " + response);

                try {
                    JSONObject Obj = new JSONObject(response);

                    if (Obj.getBoolean("error") == false) {

                        error.setText(Obj.getString("message"));
                        Toast.makeText(getApplicationContext(), "" + Obj.getString("message"), Toast.LENGTH_LONG).show();
                        Log.e("Create Group", Obj.getString("message"));
                    } else {
                        error.setText(Obj.getString("message"));
                        Toast.makeText(getApplicationContext(), "" + Obj.getString("message"), Toast.LENGTH_LONG).show();
                        Log.e("Create Group", Obj.getString("message"));
                    }

                } catch (JSONException e) {
                    Log.e("Create Group", "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e("Create Group", "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }){

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                params.put("user_id", MyApplication.getInstance().getPrefManager().getUser().getId());

                params.put("name", name);


                Log.e("Create Group", "Params: " + params.toString());

                return params;
            };

        };


        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);



    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(CreateGroup.this, MainActivity.class);
        startActivity(i);
    }
}

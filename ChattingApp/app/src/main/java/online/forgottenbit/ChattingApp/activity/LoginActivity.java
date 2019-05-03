package online.forgottenbit.ChattingApp.activity;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;

import android.support.design.widget.*;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.app.EndPoints;
import online.forgottenbit.ChattingApp.app.MyApplication;
import online.forgottenbit.ChattingApp.model.User;

public class LoginActivity extends AppCompatActivity {


    private String TAG = LoginActivity.class.getSimpleName();
    private EditText inputName, inputEmail;



    Response.Listener<String> r;


    Response.ErrorListener re;


    private Button btnEnter;

    private TextView terror;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        /**
         * Check for login session. It user is already logged in
         * redirect him to main activity
         * */


        try {
            if (MyApplication.getInstance().getPrefManager().getUser() != null) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }catch (Exception e){
            Log.e("Check pt11",e.getMessage());

        }

        setContentView(R.layout.activity_login);




        inputName =  findViewById(R.id.input_name);
        inputEmail =  findViewById(R.id.input_email);
        btnEnter = findViewById(R.id.btn_enter);
        terror = findViewById(R.id.error_msg);

        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        inputEmail.addTextChangedListener(new MyTextWatcher(inputEmail));


        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

    }


    /**
     * logging in user. Will make http post request with name, email
     * as parameters
     */



    private void login() {
        if (!validateName()) {
            return;
        }

        if (!validateEmail()) {
            return;
        }

        final String name = inputName.getText().toString();
        final String email = inputEmail.getText().toString();


        try {

             r = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.e(TAG, "response: " + response);

                    try {
                        JSONObject obj = new JSONObject(response);

                        // check for error flag
                        if (obj.getBoolean("error") == false) {
                            // user successfully logged in

                            JSONObject userObj = obj.getJSONObject("user");
                            User user = new User(userObj.getString("user_id"),
                                    userObj.getString("name"),
                                    userObj.getString("email"));

                            // storing user in shared preferences
                            MyApplication.getInstance().getPrefManager().storeUser(user);

                            // start main activity
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();

                        } else {
                            terror.setText("Error while login. Please check you internet and try again");
                            // login error - simply toast the message
                            Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        terror.setText("Error on our server. Please try again in a while");
                        Log.e(TAG, "json parsing error: " + e.getMessage());
                        Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            };




             if(r == null){
                 Log.e("checkresponseLis","r null");
             }

        }catch (Exception e){
            Log.e("checkresponseLis",e.getMessage());
        }




        try {

            re = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;


                    Log.e(TAG, "Volley error: " + error.getMessage() + ",Network Response code: " + networkResponse);
                    Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };


            if(re == null){
                Log.e("checkresponseLis","re null");
            }

        }catch (Exception e){
            Log.e("checkreserr",e.getMessage());
        }


        try {


            StringRequest strReq = new StringRequest((int)Request.Method.POST, EndPoints.LOGIN, r, re){

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("name", name);
                    params.put("email", email);

                    Log.e(TAG, "params: " + params.toString());
                    return params;
                }

            };

            //Adding request to request queue
            MyApplication.getInstance().addToRequestQueue(strReq);


        }catch (Exception e){
            terror.setText("Error while login. Please check you internet and try again");
            Log.e("strreq",e.getMessage());

        }
    }






    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private boolean validateEmail() {
        String email = inputEmail.getText().toString().trim();

        if (email.isEmpty() || !isValidEmail(email)) {
            inputEmail.setError(getString(R.string.err_msg_email));
            requestFocus(inputEmail);
            return false;
        }

        if(email.equalsIgnoreCase("admin@forgottenbit.online")){
            inputEmail.setError("you can't have this email");
            requestFocus(inputEmail);
            return false;
        }

        return true;
    }

    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean validateName() {

        if (inputName.getText().toString().trim().isEmpty()) {
            inputName.setError(getString(R.string.err_msg_name));
            requestFocus(inputName);
            return false;
        }
        if(inputName.getText().toString().trim().equalsIgnoreCase("admin")){
            inputName.setError("you can't have this name");
            requestFocus(inputName);
            return false;
        }

        return true;
    }





    private class MyTextWatcher implements TextWatcher {

        private View view;
        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_name:
                    validateName();
                    break;
                case R.id.input_email:
                    validateEmail();
                    break;
            }
        }
    }


}




package online.forgottenbit.ChattingApp.MenuItems;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.app.MyApplication;

public class User extends AppCompatActivity {


    TextView a,b;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);


        a = findViewById(R.id.user_name);
        b = findViewById(R.id.user_email);


        button = findViewById(R.id.btn_logout);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.getInstance().logout();
            }
        });

        online.forgottenbit.ChattingApp.model.User user = MyApplication.getInstance().getPrefManager().getUser();

        String name = user.getName();
        String email = user.getEmail();


        a.setText("User Name: "+name);
        b.setText("User Email: "+email);



    }
}

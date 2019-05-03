package online.forgottenbit.ChattingApp.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import online.forgottenbit.ChattingApp.model.User;

public class MyPreferenceManager {

    private String TAG = MyPreferenceManager.class.getSimpleName();

    //shared preferences
    SharedPreferences pref;

    // Editor for shared preferences
    SharedPreferences.Editor editor;


    //Context

    Context _context;

    //shared preferences mpde

    int PRIVATE_MODE = 0;

    //shared Pref file name

    private  static  final String PREF_NAME = "forgottenbit_fcm";

    //All shared prefs keys

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_NOTIFICATIONS = "notifications";



    //Constructor

    public MyPreferenceManager(Context context) {


        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);

        editor = pref.edit();


    }


    public void  addNotification(String notification){


        //get old notification
        String oldNotifications = getNotifications();

        if(oldNotifications != null){
            oldNotifications += "|" + notification;
        }else {
            oldNotifications = notification;
        }


        editor.putString(KEY_NOTIFICATIONS,oldNotifications);
        editor.commit();
    }


    public void storeUser(User user) {
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.commit();

        Log.e(TAG, "User is stored in shared preferences. " + user.getName() + ", " + user.getEmail());
    }


    public User getUser() {
        if (pref.getString(KEY_USER_ID, null) != null) {
            String id, name, email;
            id = pref.getString(KEY_USER_ID, null);
            name = pref.getString(KEY_USER_NAME, null);
            email = pref.getString(KEY_USER_EMAIL, null);

            User user = new User(id, name, email);
            return user;
        }
        return null;
    }




    public String getNotifications() {

        return pref.getString(KEY_NOTIFICATIONS,null);
    }

    public void  clear(){
        editor.clear();
        editor.commit();
    }

}

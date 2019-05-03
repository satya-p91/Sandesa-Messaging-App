package online.forgottenbit.ChattingApp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import online.forgottenbit.ChattingApp.R;
import online.forgottenbit.ChattingApp.model.ChatRoom;
import online.forgottenbit.ChattingApp.model.UserRoom;

public class UserRoomAdapter extends RecyclerView.Adapter<UserRoomAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<UserRoom> userRoomArrayList;
    private static String today;


    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView name, message, timestamp, count;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.name);
            message =  view.findViewById(R.id.message);
            timestamp =  view.findViewById(R.id.timestamp);
            count =  view.findViewById(R.id.count);
        }
    }

    public UserRoomAdapter(Context mContext, ArrayList<UserRoom> userRoomArrayList) {
        this.mContext = mContext;
        this.userRoomArrayList = userRoomArrayList;

        Calendar calendar = Calendar.getInstance();
        today = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }


    @Override
    public UserRoomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_rooms_list_row, parent, false);

        return new UserRoomAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(UserRoomAdapter.ViewHolder holder, int position) {
        UserRoom userRoom = userRoomArrayList.get(position);
        holder.name.setText(userRoom.getUserName());
        holder.message.setText(userRoom.getLastMessage());
        if (userRoom.getUnreadCount() > 0) {
            holder.count.setText(String.valueOf(userRoom.getUnreadCount()));
            holder.count.setVisibility(View.VISIBLE);
        } else {
            holder.count.setVisibility(View.GONE);
        }

        holder.timestamp.setText(getTimeStamp(userRoom.getTimestamp()));
    }



    @Override
    public int getItemCount() {
        return userRoomArrayList.size();
    }



    public static String getTimeStamp(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = "";

        today = today.length() < 2 ? "0" + today : today;

        try {
            Date date = format.parse(dateStr);
            SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
            String dateToday = todayFormat.format(date);
            format = dateToday.equals(today) ? new SimpleDateFormat("hh:mm a") : new SimpleDateFormat("dd LLL, hh:mm a");
            String date1 = format.format(date);
            timestamp = date1.toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }




    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{
        private GestureDetector gestureDetector;
        private UserRoomAdapter.ClickListener clickListener;


        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final UserRoomAdapter.ClickListener clickListener){
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }




        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }

    }

}

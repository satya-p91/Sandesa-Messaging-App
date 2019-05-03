package online.forgottenbit.ChattingApp.model;

import java.io.Serializable;

public class UserRoom implements Serializable {


    String userId, userName, lastMessage, timestamp;
    int unreadCount;

    public UserRoom() {

    }

    public UserRoom(String id, String name, String lastMessage, String timestamp, int unreadCount) {
        this.userId = id;
        this.userName = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String id)
    {
        this.userId = id;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String name) {

        this.userName = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


}

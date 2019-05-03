package online.forgottenbit.ChattingApp.app;

public class EndPoints {

    // localhost url -
    public static final String BASE_URL = "http://www.forgottenbit.online/chat/v1/index.php";
    public static final String LOGIN = BASE_URL + "/user/login";
    public static final String USER = BASE_URL + "/user/_ID_";
    public static final String CHAT_ROOMS = BASE_URL + "/chat_rooms";

    public static final String USERS_LIST = BASE_URL + "/all_users";

    public static final String CHAT_THREAD = BASE_URL + "/chat_rooms/_ID_";
    public static final String USER_THREAD = BASE_URL + "/user_room";

    public static final String CHAT_ROOM_MESSAGE = BASE_URL + "/chat_rooms/_ID_/message";

    public static final String USER_ROOM_MESSAGE = BASE_URL + "/users/_ID_/message";

    public static final String CREATE_NEW_CHAT_ROOM = BASE_URL + "/new_chat_room";

}

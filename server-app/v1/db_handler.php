<?php

//this class handle all the DB operations.

//@author satya prakash

class DbHandler{
    
    private $conn;

    function __construct(){
        include_once 'db_connect.php';
        $db = new DbConnect();
        $this->conn = $db->connect();
    }


    public function verifyLoginUser($email, $pass){
        $res = array();

        if($this->verifyEmailAndPassword($email,$pass)){
            // User with same email already existed in the db
            $res["error"] = false;
            $res['message'] = "Login Successfull";
            $res["user"] = $this->getUserByEmail($email);
        }else{
            $res["error"] = true;
            $res["message"] = "Email or password is worng. Try again...";
        }
        return $res;

    }

    //it creates new user

    public function createUser($name, $email, $pass){
        $res = array();

        //first check if usr exist

        if(!$this->isUserExists($email)){
            $stmt = $this->conn->prepare("INSERT INTO users(name, email, password) values(?, ?, ?)");

            $stmt->bind_param("sss",$name,$email,$pass);
            $result = $stmt->execute();
            $stmt->close();

            //check insertion

            if($result){
                    $res['error'] = false;
                    $res['message'] = 'User created';
                    $res['user'] = $this->getUserByEmail($email);
            }else{
                // Failed to create user
                $res["error"] = true;
                $res["message"] = "Oops! An error occurred while registereing. :(";
            }
        } else {
            // User with same email already existed in the db
            $res["error"] = true;
            $res['message'] = "Email already exist. Please Login...";
        }
        return $res;
    }

    // updating user FCM registration ID
    
    public function updateFcmID($user_id, $fcm_registration_id) {
        $res = array();
        $stmt = $this->conn->prepare("UPDATE users SET fcm_registration_id = ? WHERE user_id = ?");
        $stmt->bind_param("si", $fcm_registration_id, $user_id);
 
        if ($stmt->execute()) {
            // User successfully updated
            $res["error"] = false;
            $res["message"] = 'FCM registration ID updated successfully';
        } else {
            // Failed to update user
            $res["error"] = true;
            $res["message"] = "Failed to update FCM registration ID";
            $stmt->error;
        }
        $stmt->close();
 
        return $res;
    }

    //fetch all users

    public function getAllUsers(){
        $stmt = $this->conn->prepare("SELECT user_id, name, email, fcm_registration_id, created_at FROM users");
        $stmt->execute();
        $tasks = $stmt->get_result();
        $stmt->close();
        return $tasks;
    }

    // fetching single user by id
    public function getUser($user_id) {
        $stmt = $this->conn->prepare("SELECT user_id, name, email, fcm_registration_id, created_at FROM users WHERE user_id = ?");
        $stmt->bind_param("s", $user_id);
        if ($stmt->execute()) {
            // $user = $stmt->get_result()->fetch_assoc();
            $stmt->bind_result($user_id, $name, $email, $fcm_registration_id, $created_at);
            $stmt->fetch();
            $user = array();
            $user["user_id"] = $user_id;
            $user["name"] = $name;
            $user["email"] = $email;
            $user["fcm_registration_id"] = $fcm_registration_id;
            $user["created_at"] = $created_at;
            $stmt->close();
            return $user;
        } else {
            return NULL;
        }
    }


    // fetching multiple users by ids
    public function getUsers($user_ids) {
 
        $users = array();
        if (sizeof($user_ids) > 0) {
            $query = "SELECT user_id, name, email, fcm_registration_id, created_at FROM users WHERE user_id IN (";
 
            foreach ($user_ids as $user_id) {
                $query .= $user_id . ',';
            }
 
            $query = substr($query, 0, strlen($query) - 1);
            $query .= ')';
 
            $stmt = $this->conn->prepare($query);
            $stmt->execute();
            $result = $stmt->get_result();
 
            while ($user = $result->fetch_assoc()) {
                $tmp = array();
                $tmp["user_id"] = $user['user_id'];
                $tmp["name"] = $user['name'];
                $tmp["email"] = $user['email'];
                $tmp["fcm_registration_id"] = $user['fcm_registration_id'];
                $tmp["created_at"] = $user['created_at'];
                array_push($users, $tmp);
            }
        }
 
        return $users;
    }


    // messaging in a chat room / to persional message
    public function addMessage($user_id, $chat_room_id, $message) {
        $res = array();
 
        $stmt = $this->conn->prepare("INSERT INTO messages (chat_room_id, user_id, message) values(?, ?, ?)");
        $stmt->bind_param("iis", $chat_room_id, $user_id, $message);
 
        $result = $stmt->execute();
 
        if ($result) {
            $res['error'] = false;
 
            // get the message
            $message_id = $this->conn->insert_id;
            $stmt = $this->conn->prepare("SELECT message_id, user_id, chat_room_id, message, created_at FROM messages WHERE message_id = ?");
            $stmt->bind_param("i", $message_id);
            if ($stmt->execute()) {
                $stmt->bind_result($message_id, $user_id, $chat_room_id, $message, $created_at);
                $stmt->fetch();
                $tmp = array();
                $tmp['message_id'] = $message_id;
                $tmp['chat_room_id'] = $chat_room_id;
                $tmp['message'] = $message;
                $tmp['created_at'] = $created_at;
                $res['message'] = $tmp;
            }
        } else {
            $res['error'] = true;
            $res['message'] = 'Failed send message';
        }
 
        return $res;
    }



    // persional message
    public function addUserMessage($user_id, $message, $receiver_id) {
        $res = array();

        $cht = 9;
        $rec_id = $receiver_id['id'];
 
        $stmt = $this->conn->prepare("INSERT INTO messages (chat_room_id, user_id, message,receiver_id) values(?, ?, ?,?)");
        $stmt->bind_param("iisi",$cht, $user_id, $message,$rec_id);
 
        $result = $stmt->execute();
 
        if ($result) {
            $res['error'] = false;
 
            // get the message
            $message_id = $this->conn->insert_id;
            $stmt = $this->conn->prepare("SELECT message_id, user_id, receiver_id, message, created_at FROM messages WHERE message_id = ?");
            $stmt->bind_param("i", $message_id);
            if ($stmt->execute()) {
                $stmt->bind_result($message_id, $user_id, $receiver_id, $message, $created_at);
                $stmt->fetch();
                $tmp = array();
                $tmp['message_id'] = $message_id;
                $tmp['receiver_id'] = $receiver_id;
                $tmp['message'] = $message;
                $tmp['created_at'] = $created_at;
                $res['message'] = $tmp;
            }
        } else {
            $res['error'] = true;
            $res['cge'] = (int)$receiver_id['id'];
            $res['message'] = 'Failed send message';
        }
 
        return $res;
    }



    // fetching all chat rooms
    public function getAllChatrooms() {
        $stmt = $this->conn->prepare("SELECT * FROM chat_rooms");
        $stmt->execute();
        $tasks = $stmt->get_result();
        $stmt->close();
        return $tasks;
    }
 
    // fetching single chat room by id
    function getChatRoom($chat_room_id) {
        $stmt = $this->conn->prepare("SELECT cr.chat_room_id, cr.name, cr.created_at as chat_room_created_at, u.name as username, c.* FROM chat_rooms cr LEFT JOIN messages c ON c.chat_room_id = cr.chat_room_id LEFT JOIN users u ON u.user_id = c.user_id WHERE cr.chat_room_id = ?");
        $stmt->bind_param("i", $chat_room_id);
        $stmt->execute();
        $tasks = $stmt->get_result();
        $stmt->close();
        return $tasks;
    }


    //fetch all message of user room
    function getUserRoom($user_id, $receiver_id){
        $stmt = $this->conn->prepare("SELECT * FROM messages where user_id = ? AND receiver_id = ?");
        $stmt->bind_param("ii",$user_id,$receiver_id);
        $stmt->execute();
        $result = $stmt->get_result();    

        $messages = array();

        while ($msg = $result->fetch_assoc()) {
            $tmp = array();
            $tmp["message_id"] = $msg['message_id'];
            $tmp["message"] = $msg['message'];
            $tmp["user_id"] = $msg['email'];
            $tmp["receiver_id"] = $msg['receiver_id'];
            $tmp["created_at"] = $msg['created_at'];
            array_push($messages, $tmp);
        }
        $res = array();
        $res = $messages;
        return $res;      
    }


    
     //Checking for duplicate user by email address
     //@param String $email email to check in db
     // @return boolean
     
    private function isUserExists($email){
        $stmt = $this->conn->prepare("SELECT user_id from users WHERE email = ?");
        $stmt->bind_param("s", $email);
        $stmt->execute();
        $stmt->store_result();
        $num_rows = $stmt->num_rows;
        $stmt->close();
        return $num_rows > 0;
    }

    public function verifyEmailAndPassword($email, $pass){
        $stmt = $this->conn->prepare("SELECT user_id from users WHERE email = ? AND password = ?");
        $stmt->bind_param("ss", $email, $pass);
        $stmt->execute();
        $stmt->store_result();
        $num_rows = $stmt->num_rows;
        $stmt->close();
        return $num_rows > 0;
    }

        /**
     * Fetching user by email
     * @param String $email User email id
     */
    public function getUserByEmail($email) {
        $stmt = $this->conn->prepare("SELECT user_id, name, email, created_at, password FROM users WHERE email = ?");
        $stmt->bind_param("s", $email);
        if ($stmt->execute()) {
            // $user = $stmt->get_result()->fetch_assoc();
            $stmt->bind_result($user_id, $name, $email, $created_at, $password);
            $stmt->fetch();
            $user = array();
            $user["user_id"] = $user_id;
            $user["name"] = $name;
            $user["email"] = $email;
            $user["created_at"] = $created_at;
            $user["pass"] = $password;
            $stmt->close();
            return $user;
        } else {
            return NULL;
        }
    }



}
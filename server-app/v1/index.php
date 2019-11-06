<?php

error_reporting(-1);
ini_set('display_errors', 'On');

require 'db_handler.php';
require 'vendor/autoload.php';

$app = new Slim\App();


//user login
$app->post('/user/login', function($request, $response){
    // check for required params
    verifyRequiredParams($request, $response,array('email','pass'));
 
    // reading post params

    $all = $request->getParsedBody();

    $email = $all['email'];
    $pass = $all['pass'];
 
    // validating email address
    validateEmail($email);
 
    $db = new DbHandler();
    $res = $db->verifyLoginUser($email, $pass);
 
    // echo json response

    return $response->withStatus(200)->withJson($res);

});



// User signup
$app->post('/user/signup', function($request, $response){
    // check for required params
    verifyRequiredParams($request, $response,array('name', 'email','pass'));
 
    // reading post params

    $all = $request->getParsedBody();

     $name = $all['name'];
     $email = $all['email'];
     $pass = $all['pass'];
 
    // validating email address
    validateEmail($email);
 
    $db = new DbHandler();
    $res = $db->createUser($name, $email, $pass);
 
    // echo json response

    return $response->withStatus(200)->withJson($res);

});




//for test purpose


$app->post('/users/push_test', function($request, $response){
    verifyRequiredParams($request,$response,array('message','fcm_key','token'));

    $all = $request->getParsedBody();

    $message = $all['message'];
    $fcm_key = $all['fcm_key'];
    $token = $all['token'];
    $image = FALSE;
    //$all['include_image'];

    $data = array();

    $data['title'] = 'messaging app test';
    $data['message'] = $message;
    if($image == 'true'){
        $data['image'] = 'http://api.androidhive.info/gcm/panda.jpg';
    }else{
        $data['image'] = '';
    }
    $data['created_at'] = date('Y-m-d G:i:s');


    $fields = array(
        'to' => $token,
        'data' => $data,
    );


    $url = 'https://fcm.googleapis.com/fcm/send';
 
    $headers = array(
        'Authorization: key=' . $fcm_key,
        'Content-Type: application/json'
    );

    // Open connection
    $ch = curl_init();

    // Set the url, number of POST vars, POST data
    curl_setopt($ch, CURLOPT_URL, $url);

    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

    // Disabling SSL Certificate support temporarly
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));


    $ress = array();
    // Execute post
    $result = curl_exec($ch);
    if ($result === FALSE) {
        $ress['error'] = TRUE;
        $ress['message'] = 'Unable to send test push notification';
        return $response->withStatus(200)->withJson($ress);
    }

    // Close connection
    curl_close($ch);
    $ress['error'] = FALSE;
    $ress['message'] = 'Test push notification sent Successfully';
       
    //return $response->withStatus(200)->withJson($ress); 
    return $response->withStatus(200)->withJson($result);
    
});


/* * *
 * Updating user
 *  we use this url to update user's fcm registration id
 */
$app->put('/user/{id}', function($request,$response,$user_id){
    
    verifyRequiredParams($request,$response,array('fcm'));
    $fcm=array();
    $fcm = $request->getParsedBody();

    $db = new DbHandler();
    $res = $db->updateFcmID($user_id['id'], $fcm['fcm']);
 
    return $response->withStatus(200)->withJson($res);
});


$app->get('/all_users', function($request,$response){
    $res = array();
    $db = new DbHandler();
    //fetch all users
    $result = $db->getAllUsers();
    $res['error'] = false;
    $res['all_users'] = array();

    while($user = $result->fetch_assoc()){
        $tmp = array();
        $tmp["user_id"] = $user["user_id"];
        $tmp["name"] = $user["name"];
        $tmp["created_at"] = $user["created_at"];
        $tmp["email"] = $user["email"];
        $tmp["fcm_id"] = $user["fcm_registration_id"];
        array_push($res["all_users"], $tmp);
    }
    
    return $response->withStatus(200)->withJson($res);
});



/* * *
 * fetching all chat rooms
 */
$app->get('/chat_rooms', function($request,$response) {
    $res = array();
    $db = new DbHandler();
 
    // fetching all user tasks
    $result = $db->getAllChatrooms();
 
    $res["error"] = false;
    $res["chat_rooms"] = array();
 
    // pushing single chat room into array
    while ($chat_room = $result->fetch_assoc()) {
        $tmp = array();
        $tmp["chat_room_id"] = $chat_room["chat_room_id"];
        $tmp["name"] = $chat_room["name"];
        $tmp["created_at"] = $chat_room["created_at"];
        array_push($res["chat_rooms"], $tmp);
    }
    return $response->withStatus(200)->withJson($res);
});
 


/**
 * Messaging in a chat room
 * Will send push notification using Topic Messaging
 *  */
$app->post('/chat_rooms/{id}/message', function($request,$response,$chat_room_id) {
    $db = new DbHandler();
 
    verifyRequiredParams($request,$response, array('user_id', 'message'));
 
    $params = $request->getParsedBody();

    $user_id = $params['user_id'];
    $message = $params['message'];
 
    $res = $db->addMessage($user_id, $chat_room_id['id'], $message);
 
    if ($res['error'] == false) {
        require_once 'libs/fcm/fcm.php';
        require_once 'libs/fcm/push.php';
        $fcm = new FCM();
        $push = new Push();
 
        // get the user using userid
        
        $user = $db->getUser($user_id);
 
        $data = array();
        $data['user'] = $user;
        $data['message'] = $res['message'];
        $data['flag'] = 1;
        $data['chat_room_id'] = $chat_room_id['id'];
        $data['title'] = "chat app";
        $data['flag'] = "1";
        $data['is_background'] = "false";
 
        $push->setTitle("Satya's Messaging App");
        $push->setIsBackground(FALSE);
        $push->setFlag(PUSH_FLAG_CHATROOM);
        $push->setData($data);
        

         
        // echo json_encode($push->getPush());exit;
 
        // sending push message to a topic
        $fcm->sendToTopic('topic_' . $chat_room_id['id'], $push->getPush());
 
        $res['user'] = $user;
        $res['error'] = false;
    }

    return $response->withStatus(200)->withJson($res);
});

/**
 * Sending push notification to a single user
 * We use user's fcm registration id to send the message
 * * */
$app->post('/users/{id}/message', function($request, $response, $to_user_id) {
    $db = new DbHandler();
 
    verifyRequiredParams($request, $response, array('user_id','message'));
 
    $params = $request->getParsedBody();

    $from_user_id = $params['user_id'];
    $message = $params['message'];
 
    $res = $db->addUserMessage($from_user_id,$message, $to_user_id);
 
    if ($res['error'] == false) {
        require_once 'libs/fcm/fcm.php';
        require_once 'libs/fcm/push.php';
        $fcm = new FCM();
        $push = new Push();
 
        $user = $db->getUser($to_user_id['id']);
 
        $data = array();
        $data['user'] = $user;
        $data['message'] = $res['message'];
        $data['image'] = '';
 
        $push->setTitle("Satya's Messaging app");
        $push->setIsBackground(FALSE);
        $push->setFlag(PUSH_FLAG_USER);
        $push->setData($data);
 
        // sending push message to single user
        //$fcm->send($user['fcm_registration_id'], $push->getPush());
 
        $res['user'] = $user;
        $res['error'] = false;
    }
 
    return $response->withStatus(200)->withJson($res);
    
});



/**
 * Sending push notification to multiple users
 * We use fcm registration ids to send notification message
 * At max we can send message to 1000 recipients
 * * */
$app->post('/users/message', function($request, $response){
 
    $res = array();
    verifyRequiredParams($request,$response,array('user_id', 'to', 'message'));
 
    require_once 'libs/fcm/fcm.php';
    require_once 'libs/fcm/push.php';
 
    $db = new DbHandler();
    $params = $request->getParsedBody();
 
    $user_id = $params['user_id'];
    $to_user_ids = array_filter(explode(',', $params['to']));
    $message = $params['message'];


    $user = $db->getUser($user_id);
    $users = $db->getUsers($to_user_ids);
 
    $registration_ids = array();
 
    // preparing fcm registration ids array
    foreach ($users as $u) {
        array_push($registration_ids, $u['fcm_registration_id']);
    }
 
    // insert messages in db
    // send push to multiple users
    $fcm = new FCM();
    $push = new Push();
 
    // creating tmp message, skipping database insertion
    $msg = array();
    $msg['message'] = $message;
    $msg['message_id'] = '';
    $msg['chat_room_id'] = '';
    $msg['created_at'] = date('Y-m-d G:i:s');
 
    $data = array();
    $data['user'] = $user;
    $data['message'] = $msg;
    $data['image'] = '';
 
    $push->setTitle("Satya's Messaging app");
    $push->setIsBackground(FALSE);
    $push->setFlag(PUSH_FLAG_USER);
    $push->setData($data);
 
    // sending push message to multiple users
    $fcm->sendMultiple($registration_ids, $push->getPush());
 
    $res['error'] = false;
    return $response->withStatus(200)->withJson($res);
});


/**
 * to send messages to all users
 * only admin have access to this
 */

$app->post('/users/send_to_all', function($request, $response){
 
    $res = array();
    verifyRequiredParams($request, $response, array('user_id', 'message'));
 
    require_once 'libs/fcm/fcm.php';
    require_once 'libs/fcm/push.php';
 
    $db = new DbHandler();
 
    $params = $request->getParsedBody();

    $user_id = $params['user_id'];
    $message = $params['message'];
 
    $fcm = new FCM();
    $push = new Push();
 
    // get the user using userid
    $user = $db->getUser($user_id);
     
    // creating tmp message, skipping database insertion
    $msg = array();
    $msg['message'] = $message;
    $msg['message_id'] = '';
    $msg['chat_room_id'] = '';
    $msg['created_at'] = date('Y-m-d G:i:s');
 
    $data = array();
    $data['user'] = $user;
    $data['message'] = $msg;
    $data['image'] = '';
 
    $push->setTitle("Satya's Messaging app");
    $push->setIsBackground(FALSE);
    $push->setFlag(PUSH_FLAG_USER);
    $push->setData($data);
 
    // sending message to topic `global`
    // On the device every user should subscribe to `global` topic
    $fcm->sendToTopic('global', $push->getPush());
 
    $res['user'] = $user;
    $res['error'] = false;
 
    return $response->withStatus(200)->withJson($res);
});





/**
 * Fetching single chat room including all the chat messages
 * 
 */
$app->get('/chat_rooms/{id}', function($request, $response, $chat_room_id) {

    $db = new DbHandler();

    $result = $db->getChatRoom($chat_room_id['id']);

    $res["error"] = false;
    $res["messages"] = array();
    $res['chat_room'] = array();

    $i = 0;
    // looping through result and preparing tasks array
    while ($chat_room = $result->fetch_assoc()) {
        // adding chat room node
        if ($i == 0) {
            $tmp = array();
            $tmp["chat_room_id"] = $chat_room_id['id'];
            $tmp["name"] = $chat_room["name"];
            $tmp["created_at"] = $chat_room["chat_room_created_at"];
            $res['chat_room'] = $tmp;
        }

        if ($chat_room['user_id'] != NULL) {
            // message node
            $cmt = array();
            $cmt["message"] = $chat_room["message"];
            $cmt["message_id"] = $chat_room["message_id"];
            $cmt["created_at"] = $chat_room["created_at"];

            // user node
            $user = array();
            $user['user_id'] = $chat_room['user_id'];
            $user['username'] = $chat_room['username'];
            $cmt['user'] = $user;

            array_push($res["messages"], $cmt);
        }
    }


    
    return $response->withStatus( 200)->withJson($res);
});



/**
 * Fetching single user room including all the user's messages
 * 
 */
$app->post('/user_room', function($request, $response) {


    verifyRequiredParams($request,$response,array('user_id','receiver_id'));

    $all = $request->getParsedBody();

    $user_id = $all['user_id'];
    $receiver_id = $all['receiver_id'];


    $db = new DbHandler();

    $result = $db->getUserRoom($user_id,$receiver_id);

    $res["error"] = false;
    $res["messages"] = array();
    $res['receiver'] = $db->getUser($receiver_id);

    
    foreach($result as $msg) {                  
            // message node
            $cmt = array();
            $cmt["message"] = $msg["message"];
            $cmt["message_id"] = $msg["message_id"];
            $cmt["created_at"] = $msg["created_at"];            
            array_push($res["messages"], $cmt);        
    }
    
    return $response->withStatus( 200)->withJson($res);
});






/**
 * Verifying required params posted or not
 */
function verifyRequiredParams($request,$response, $required_fields) {
    $error = false;
    $error_fields = "";
    $request_params = array();
    $request_params = $_REQUEST;
    // Handling PUT request params
    if ($_SERVER['REQUEST_METHOD'] == 'PUT') {
        
        parse_str($request->getBody(), $request_params);
    }
    foreach ($required_fields as $field) {
        if (!isset($request_params[$field]) || strlen(trim($request_params[$field])) <= 0) {
            $error = true;
            $error_fields .= $field . ', ';
        }
    }

    if ($error) {
        // Required field(s) are missing or empty
        // echo error json and stop the app
        $re = array();
        $re["error"] = true;
        $re["message"] = 'Required field(s) ' . substr($error_fields, 0, -2) . ' is missing or empty';
        echo $response->withStatus(400)->withJson($re);
        die();
    }
}


//validate email

function validateEmail($email){    
    if(!filter_var($email, FILTER_VALIDATE_EMAIL)){
        return false;

    }else{
        return true;
    }
}

function IsNullOrEmptyString($str) {
    return (!isset($str) || trim($str) === '');
}


$app->run();
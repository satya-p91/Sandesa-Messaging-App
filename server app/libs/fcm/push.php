<?php

class Push{
    // push message title
    private $title;
     
    // push message payload
    private $body;
     
    // flag indicating background task on push received
    private $is_background;
     
    // flag to indicate the type of notification
    private $flag;

    private $image ="https://api.androidhive.info/images/firebase_logo.png";
     
    function __construct() {
         
    }
     
    public function setTitle($title){
        $this->title = $title;
    }
     
    public function setBody($body){
        $this->body = $body;
    }
     
    public function setIsBackground($is_background){
        $this->is_background = $is_background;
    }
     
    public function setFlag($flag){
        $this->flag = $flag;
    }


     
    public function getPush(){
        $res = array();
        $res['title'] = $this->title;
        $res['is_background'] = $this->is_background;
        $res['flag'] = $this->flag;
        $res['body'] = $this->body;
        $res['message'] = $this->body;  
        $res['image'] = $this->image;
                 
        return $res;
    }
}
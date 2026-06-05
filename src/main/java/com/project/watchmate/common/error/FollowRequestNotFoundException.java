package com.project.watchmate.common.error;

public class FollowRequestNotFoundException extends RuntimeException{
    public FollowRequestNotFoundException(String message){
        super(message);
    }
}


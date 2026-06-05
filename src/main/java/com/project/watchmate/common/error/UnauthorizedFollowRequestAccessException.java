package com.project.watchmate.common.error;

public class UnauthorizedFollowRequestAccessException extends RuntimeException{
    public UnauthorizedFollowRequestAccessException(String message){
        super(message);
    }
}


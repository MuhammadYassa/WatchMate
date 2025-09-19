package com.project.watchmate.Exception;

public class UnauthorizedFollowRequestAccessException extends RuntimeException{
    public UnauthorizedFollowRequestAccessException(String message){
        super(message);
    }
}

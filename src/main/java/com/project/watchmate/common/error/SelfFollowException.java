package com.project.watchmate.common.error;

public class SelfFollowException extends RuntimeException{
    public SelfFollowException(String message){
        super(message);
    }
}


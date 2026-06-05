package com.project.watchmate.common.error;

public class BlockedUserException extends RuntimeException{
    public BlockedUserException(String message){
        super(message);
    }
}


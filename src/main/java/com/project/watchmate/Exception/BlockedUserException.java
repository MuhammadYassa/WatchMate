package com.project.watchmate.Exception;

public class BlockedUserException extends RuntimeException{
    public BlockedUserException(String message){
        super(message);
    }
}

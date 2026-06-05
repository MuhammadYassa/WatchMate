package com.project.watchmate.common.error;

public class UnauthorizedWatchListAccessException extends RuntimeException{
    public UnauthorizedWatchListAccessException(String message){
        super(message);
    }
}


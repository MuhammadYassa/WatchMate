package com.project.watchmate.Exception;

public class UnauthorizedWatchListAccessException extends RuntimeException{
    public UnauthorizedWatchListAccessException(String message){
        super(message);
    }
}

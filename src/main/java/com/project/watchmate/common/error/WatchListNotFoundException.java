package com.project.watchmate.common.error;

public class WatchListNotFoundException extends RuntimeException{
    public WatchListNotFoundException(String message){
        super(message);
    }
}


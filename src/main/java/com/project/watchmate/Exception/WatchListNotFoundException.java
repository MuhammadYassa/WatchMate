package com.project.watchmate.Exception;

public class WatchListNotFoundException extends RuntimeException{
    public WatchListNotFoundException(String message){
        super(message);
    }
}

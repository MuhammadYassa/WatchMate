package com.project.watchmate.Exception;

public class MediaNotFoundException extends RuntimeException{
    public MediaNotFoundException(String message){
        super(message);
    }
}

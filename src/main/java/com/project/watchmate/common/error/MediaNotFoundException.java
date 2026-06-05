package com.project.watchmate.common.error;

public class MediaNotFoundException extends RuntimeException{
    public MediaNotFoundException(String message){
        super(message);
    }
}


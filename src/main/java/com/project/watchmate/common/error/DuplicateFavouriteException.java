package com.project.watchmate.common.error;

public class DuplicateFavouriteException extends RuntimeException{
    public DuplicateFavouriteException(String message){
        super(message);
    }
}


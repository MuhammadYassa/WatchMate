package com.project.watchmate.common.error;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message){
        super(message);
    }
}


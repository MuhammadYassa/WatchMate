package com.project.watchmate.Exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message){
        super(message);
    }
}

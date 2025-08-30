package com.project.watchmate.Exception;

public class UnauthorizedReviewAccessException extends RuntimeException{
    public UnauthorizedReviewAccessException(String message){
        super(message);
    }

}

package com.project.watchmate.common.error;

public class UnauthorizedReviewAccessException extends RuntimeException{
    public UnauthorizedReviewAccessException(String message){
        super(message);
    }

}


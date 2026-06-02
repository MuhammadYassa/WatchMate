package com.project.watchmate.Exception;

public class InvalidEmailVerificationTokenException extends RuntimeException {
    public InvalidEmailVerificationTokenException(String message) {
        super(message);
    }
    
}

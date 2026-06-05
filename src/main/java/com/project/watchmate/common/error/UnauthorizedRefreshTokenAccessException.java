package com.project.watchmate.common.error;

public class UnauthorizedRefreshTokenAccessException extends RuntimeException {
    public UnauthorizedRefreshTokenAccessException(String message) {
        super(message);
    }
}

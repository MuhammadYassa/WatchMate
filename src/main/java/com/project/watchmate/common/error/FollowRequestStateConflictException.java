package com.project.watchmate.common.error;

public class FollowRequestStateConflictException extends RuntimeException {
    public FollowRequestStateConflictException(String message) {
        super(message);
    }
}

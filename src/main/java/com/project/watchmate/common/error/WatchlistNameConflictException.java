package com.project.watchmate.common.error;

public class WatchlistNameConflictException extends RuntimeException {

    public WatchlistNameConflictException(String message) {
        super(message);
    }
}

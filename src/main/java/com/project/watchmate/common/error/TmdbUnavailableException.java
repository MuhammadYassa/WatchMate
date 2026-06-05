package com.project.watchmate.common.error;

public class TmdbUnavailableException extends RuntimeException {

    public TmdbUnavailableException(String message) {
        super(message);
    }

    public TmdbUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}


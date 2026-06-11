package com.project.watchmate.common.error;

import org.springframework.http.HttpStatus;

public class TmdbClientException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public TmdbClientException(String message, HttpStatus status, String code, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}

package com.project.watchmate.common.error;

public class EmailDeliveryUnavailableException extends RuntimeException {
    public EmailDeliveryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

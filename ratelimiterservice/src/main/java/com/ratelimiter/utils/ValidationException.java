package com.ratelimiter.utils;


/**
 * Exception for issues with validating inputs.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(Exception ex) {
        super(ex);
    }

    public ValidationException(String message, Exception ex) {
        super(message, ex);
    }
}

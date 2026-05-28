package com.financialagent.exception;

/**
 * Exception thrown when AngelOne SmartAPI calls fail.
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

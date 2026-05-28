package com.financialagent.exception;

/**
 * Exception thrown for user account related issues.
 */
public class UserAccountException extends RuntimeException {

    public UserAccountException(String message) {
        super(message);
    }

    public UserAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}

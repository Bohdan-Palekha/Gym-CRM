package com.gymcrm.exception;

//the request conflicts with the resource's current state like activating an active account
//AOP logging catches and rethrows all throwables
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

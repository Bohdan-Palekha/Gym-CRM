package com.gymcrm.exception;

//no entity in storage found. AOP logging catches and rethrows all throwables
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

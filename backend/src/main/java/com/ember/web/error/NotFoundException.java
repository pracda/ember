package com.ember.web.error;

/** Thrown when a requested resource (order, menu item) does not exist. Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

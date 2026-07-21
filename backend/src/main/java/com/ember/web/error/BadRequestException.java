package com.ember.web.error;

/** Thrown for semantically invalid requests (unknown size, add-on not offered, etc.). Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

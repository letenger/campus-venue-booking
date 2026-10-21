package com.campus.venue.common;

/** Wrong credentials, or a token that is missing or expired. Maps to 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

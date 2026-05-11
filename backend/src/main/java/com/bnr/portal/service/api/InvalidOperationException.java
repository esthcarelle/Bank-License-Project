package com.bnr.portal.service.api;

/** The request does not fit the business rules */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}

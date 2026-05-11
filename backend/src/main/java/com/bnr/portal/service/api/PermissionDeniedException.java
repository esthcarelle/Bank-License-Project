package com.bnr.portal.service.api;

/** You are signed in, but this operation is not allowed for you  */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}

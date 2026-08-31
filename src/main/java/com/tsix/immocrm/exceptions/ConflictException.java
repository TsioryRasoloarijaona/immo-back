package com.tsix.immocrm.exceptions;

/**
 * Thrown when a well-formed, otherwise-valid request cannot be applied because it collides with
 * existing state (e.g. a username already in use). Translated to a 409
 * {@link org.springframework.http.ProblemDetail} response by
 * {@link com.tsix.immocrm.web.GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

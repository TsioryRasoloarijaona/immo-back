package com.tsix.immocrm.exceptions;

/**
 * Thrown when a resource requested by id (or another unique lookup) does not exist.
 * Translated to a 404 {@link org.springframework.http.ProblemDetail} response
 * by {@link com.tsix.immocrm.web.GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

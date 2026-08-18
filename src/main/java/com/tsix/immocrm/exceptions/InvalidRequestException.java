package com.tsix.immocrm.exceptions;

/**
 * Thrown when a request is well-formed JSON but violates a business rule that isn't a
 * 404 (e.g. references an unknown related resource, or fails a domain-specific check).
 * Translated to a 400 {@link org.springframework.http.ProblemDetail} response
 * by {@link com.tsix.immocrm.web.GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

package com.tsix.immocrm.exceptions;

import java.util.List;

/**
 * Thrown when a Dossier status transition falls outside
 * {@link com.tsix.immocrm.domain.TransitionsValidees}. Translated to a 409 Conflict
 * {@link org.springframework.http.ProblemDetail} by
 * {@link com.tsix.immocrm.web.GlobalExceptionHandler}, with the allowed transitions listed
 * in the response body's {@code transitionsAutorisees} property.
 */
public class InvalidStatutTransitionException extends RuntimeException {

    private final List<String> transitionsAutorisees;

    public InvalidStatutTransitionException(String message, List<String> transitionsAutorisees) {
        super(message);
        this.transitionsAutorisees = transitionsAutorisees;
    }

    public List<String> getTransitionsAutorisees() {
        return transitionsAutorisees;
    }
}

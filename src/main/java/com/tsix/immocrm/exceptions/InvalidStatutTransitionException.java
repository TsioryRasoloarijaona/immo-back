package com.tsix.immocrm.exceptions;

import java.util.List;

/** Transition hors {@link com.tsix.immocrm.domain.TransitionsValidees} → 409 avec {@code transitionsAutorisees}. */
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

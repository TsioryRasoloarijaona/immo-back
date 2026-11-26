package com.tsix.immocrm.exceptions;

/** Requête valide mais en conflit avec l'état existant → 409 ; {@code code} facultatif recopié dans le ProblemDetail. */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

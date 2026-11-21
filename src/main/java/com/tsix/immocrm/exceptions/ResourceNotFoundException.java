package com.tsix.immocrm.exceptions;

/** Ressource introuvable (ou masquée par le périmètre) → 404 ; {@code code} facultatif, recopié dans le ProblemDetail. */
public class ResourceNotFoundException extends RuntimeException {

    private final String code;

    public ResourceNotFoundException(String message) {
        this(message, null);
    }

    public ResourceNotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

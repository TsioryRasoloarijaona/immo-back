package com.tsix.immocrm.exceptions;

/** Ressource introuvable (ou masquée par le périmètre) → 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

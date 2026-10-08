package com.tsix.immocrm.exceptions;

/** Requête valide mais en conflit avec l'état existant → 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

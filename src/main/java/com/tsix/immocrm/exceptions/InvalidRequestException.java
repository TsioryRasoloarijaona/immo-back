package com.tsix.immocrm.exceptions;

/** Règle métier violée par une requête bien formée (hors ressource introuvable) → 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

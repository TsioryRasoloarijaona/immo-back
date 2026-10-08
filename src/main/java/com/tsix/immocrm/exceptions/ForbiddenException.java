package com.tsix.immocrm.exceptions;

/**
 * 403, réservé aux écritures sur une ressource que l'appelant a lui-même nommée (projet hors périmètre). Une lecture
 * hors périmètre est un 404 ({@link ResourceNotFoundException}) : un 403 confirmerait l'existence et les ids.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

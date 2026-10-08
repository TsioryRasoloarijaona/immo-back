package com.tsix.immocrm.exceptions;

/**
 * Échec de connexion → 401. Message volontairement identique (inconnu, mauvais mot de passe, compte désactivé) pour
 * ne pas révéler qu'un identifiant existe.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

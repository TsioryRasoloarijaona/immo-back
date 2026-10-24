package com.tsix.immocrm.services.patrimoine.importation;

/** Classeur au-delà de la taille admise : 413 avant toute lecture. */
public class ClasseurTropGrosException extends RuntimeException {

    public ClasseurTropGrosException(String message) {
        super(message);
    }
}

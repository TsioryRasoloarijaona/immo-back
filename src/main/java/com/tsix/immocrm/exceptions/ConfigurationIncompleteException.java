package com.tsix.immocrm.exceptions;

/** Référentiel du légataire incomplet (statut, catégorie…) : 500 avec un {@code detail} explicite, rien n'est écrit. */
public class ConfigurationIncompleteException extends IllegalStateException {

    public ConfigurationIncompleteException(String message) {
        super(message);
    }
}

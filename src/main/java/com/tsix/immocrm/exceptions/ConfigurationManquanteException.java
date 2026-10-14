package com.tsix.immocrm.exceptions;

/** Ligne de configuration du légataire introuvable : 500 avec un {@code detail} explicite, rien n'est écrit. */
public class ConfigurationManquanteException extends IllegalStateException {
    public ConfigurationManquanteException(String message) {
        super(message);
    }
}

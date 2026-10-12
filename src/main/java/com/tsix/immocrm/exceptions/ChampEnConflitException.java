package com.tsix.immocrm.exceptions;

/** 409 qui nomme le champ du corps en conflit (code déjà pris, champ protégé d'un article affecté). */
public class ChampEnConflitException extends ConflictException {

    private final String champ;

    public ChampEnConflitException(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}

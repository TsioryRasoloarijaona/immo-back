package com.tsix.immocrm.exceptions;

/** 400 qui nomme le champ du corps en faute : le front affiche le message sous ce champ. */
public class ChampInvalideException extends InvalidRequestException {

    private final String champ;

    public ChampInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}

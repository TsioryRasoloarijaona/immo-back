package com.tsix.immocrm.domain;

import java.io.Serial;
import java.util.List;

/**
 * Nom hors catalogue — 400. Porte les noms valides de <strong>son</strong> catalogue, sinon une
 * faute sur une colonne de prospect répondrait la liste des colonnes de prospection.
 */
public class ColonneInconnueException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient String colonne;
    private final transient List<String> nomsValides;

    public ColonneInconnueException(String colonne, List<String> nomsValides) {
        super("Colonne inconnue : " + colonne);
        this.colonne = colonne;
        this.nomsValides = List.copyOf(nomsValides);
    }

    public String colonne() {
        return colonne;
    }

    public List<String> nomsValides() {
        return nomsValides;
    }
}

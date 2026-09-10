package com.tsix.immocrm.domain;

import java.io.Serial;
import java.util.List;

/**
 * Nom hors catalogue passé à {@code ?colonnes=} — traduit en 400 par le
 * {@code GlobalExceptionHandler}.
 *
 * <p><strong>Porte la liste des noms valides de son propre catalogue.</strong> C'est ce qui permet
 * à plusieurs listes filtrables de coexister : sans cela, une faute de frappe sur une colonne de
 * prospect se verrait répondre la liste des colonnes de prospection, ce qui est pire qu'aucune
 * aide — le message désignerait le mauvais écran.
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

    /** Noms acceptés par le catalogue qui a rejeté l'appel. */
    public List<String> nomsValides() {
        return nomsValides;
    }
}

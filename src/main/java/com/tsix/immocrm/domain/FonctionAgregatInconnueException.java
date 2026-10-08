package com.tsix.immocrm.domain;

import java.io.Serial;
import java.util.List;

/**
 * Fonction d'agrégat refusée (inconnue, ou non permise par la colonne) — 400. Distincte de
 * {@link ColonneInconnueException} : ici le nom de colonne est bon, c'est la fonction qu'il faut changer.
 */
public class FonctionAgregatInconnueException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient String jeton;
    private final transient String colonne;
    private final transient List<String> fonctionsValides;

    public FonctionAgregatInconnueException(String jeton, String colonne,
                                            List<FonctionAgregat> fonctionsValides) {
        super(fonctionsValides.isEmpty()
                ? "La colonne « " + colonne + " » n'est pas agrégeable."
                : "Fonction d'agrégat refusée pour la colonne « " + colonne + " » : " + jeton);
        this.jeton = jeton;
        this.colonne = colonne;
        this.fonctionsValides = fonctionsValides.stream().map(Enum::name).toList();
    }

    public String jeton() {
        return jeton;
    }

    public String colonne() {
        return colonne;
    }

    /** Fonctions que cette colonne accepte — vide si elle n'est pas agrégeable du tout. */
    public List<String> fonctionsValides() {
        return fonctionsValides;
    }
}

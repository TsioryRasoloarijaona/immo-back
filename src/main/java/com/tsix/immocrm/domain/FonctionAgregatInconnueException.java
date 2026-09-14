package com.tsix.immocrm.domain;

import java.io.Serial;
import java.util.List;

/**
 * Fonction d'agrégat refusée dans {@code ?agregats=} — traduite en 400 par le
 * {@code GlobalExceptionHandler}.
 *
 * <p>Distincte de {@link ColonneInconnueException} parce que les deux erreurs se corrigent
 * différemment : là, c'est le nom de colonne qu'il faut changer ; ici, la colonne est bonne mais la
 * fonction ne s'y applique pas. Répondre « colonne inconnue » à un {@code dateDocument:SUM} enverrait
 * chercher une faute de frappe dans un nom qui est correct.
 *
 * <p>Deux cas la déclenchent : un nom de fonction qui n'existe pas, et une fonction qui existe mais
 * que la colonne n'autorise pas (sommer une date, prendre le minimum d'une différence). Le message
 * porte les fonctions réellement acceptées <strong>par cette colonne-là</strong>.
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

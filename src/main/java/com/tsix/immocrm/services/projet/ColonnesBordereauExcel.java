package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Lecture des cellules d'un bordereau importé (lot et demande d'achat) ; erreurs au numéro de ligne Excel. */
public final class ColonnesBordereauExcel {

    // En-têtes OMRAN du bordereau du lot (export et import).
    static final String ODT = "O.D.T.";
    static final String CODE = "Code";
    static final String DESCRIPTION = "Description";
    static final String UNITE = "Unité";
    static final String QUANTITE = "Quantité";
    static final String PU_HT = "P.U. HT";
    static final String TVA = "TVA";
    static final String FODEC = "FODEC";
    static final String PU_TTC = "P.U. TTC";
    static final String NOTES = "Notes";
    static final String CODE_GROUPE = "Code groupe";
    static final String UNITE_GROUPE = "GROUPE";

    private ColonnesBordereauExcel() {
    }

    /** Code de la colonne résolu par {@code parCode} ; null si la cellule est vide, 400 si le code est inconnu. */
    public static UUID reference(LigneLue l, String colonne, Map<String, UUID> parCode) {
        String code = texte(l, colonne);
        if (code == null) {
            return null;
        }
        return Optional.ofNullable(parCode.get(code)).orElseThrow(() -> new InvalidRequestException(
                "Ligne %d : %s « %s » inconnu(e)".formatted(l.numero(), colonne.toLowerCase(), code)));
    }

    public static String texte(LigneLue l, String colonne) {
        return Objects.toString(l.valeurs().get(colonne), null);
    }

    /** Code lu comme un code : une cellule numérique 12 donne « 12 », pas « 12.0 ». */
    public static String code(LigneLue l, String colonne) {
        Object v = l.valeurs().get(colonne);
        if (v instanceof BigDecimal n) {
            return n.stripTrailingZeros().toPlainString();
        }
        return v == null ? null : v.toString().strip();
    }

    public static BigDecimal nombre(LigneLue l, String colonne) {
        Object v = l.valeurs().get(colonne);
        if (v == null || v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        throw new InvalidRequestException("Ligne %d : %s doit être un nombre".formatted(l.numero(), colonne));
    }
}

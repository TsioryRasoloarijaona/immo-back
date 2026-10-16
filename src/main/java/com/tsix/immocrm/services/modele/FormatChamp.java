package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.TypeChamp;
import com.tsix.immocrm.services.dossier.document.MontantFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Formats de la génération : ceux de {@link MontantFormatter}, à l'identique des documents codés en dur (un montant
 * absent s'écrit « 0.00 DH », comme dans l'acte actuel). Voir FEATURE-modeles-documents.md §4.
 */
final class FormatChamp {

    private FormatChamp() {
    }

    static String formater(TypeChamp type, Object valeur) {
        return switch (type) {
            case MONTANT -> MontantFormatter.montant((BigDecimal) valeur);
            case POURCENTAGE -> MontantFormatter.pourcentage((BigDecimal) valeur);
            case DATE -> MontantFormatter.date((LocalDate) valeur);
            case BOOLEEN -> booleen(valeur);
            case NOMBRE -> nombre(valeur);
            case TEXTE -> valeur == null ? "" : valeur.toString();
        };
    }

    /** Règle d'un bloc conditionnel sur un champ non booléen : affiché si la valeur est renseignée. */
    static boolean renseigne(Object valeur) {
        if (valeur instanceof String texte) {
            return !texte.isBlank();
        }
        return valeur != null;
    }

    private static String booleen(Object valeur) {
        if (valeur == null) {
            return "";
        }
        return Boolean.TRUE.equals(valeur) ? "Oui" : "Non";
    }

    private static String nombre(Object valeur) {
        if (valeur instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return valeur == null ? "" : valeur.toString();
    }
}

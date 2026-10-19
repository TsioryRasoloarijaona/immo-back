package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contrôles de saisie communs aux programmes, tranches et articles (FEATURE-patrimoine-ecriture.md §2.8, §2.9). */
final class SaisiePatrimoine {

    static final int COURT = 255;

    private SaisiePatrimoine() {
    }

    /** PATCH : absent ou {@code null} = valeur actuelle. */
    static <T> T ou(T saisie, T actuel) {
        return saisie != null ? saisie : actuel;
    }

    /** Texte OMRAN : jamais NULL, {@code ""} quand rien n'est saisi (§2.4). */
    static String texte(String saisie, String actuel, int max, String libelle) {
        if (saisie != null && saisie.length() > max) {
            throw new InvalidRequestException("%s : %d caractères au plus.".formatted(libelle, max));
        }
        String valeur = ou(saisie, actuel);
        return valeur != null ? valeur : "";
    }

    /** Champ obligatoire (code, description) : comparé et écrit après {@code trim}. */
    static String obligatoire(String saisie, String actuel, String libelle) {
        String valeur = saisie != null ? saisie.trim() : actuel;
        if (valeur == null || valeur.isBlank()) {
            throw new InvalidRequestException(libelle + " est obligatoire.");
        }
        return texte(valeur, null, COURT, libelle);
    }

    static BigDecimal fraction(BigDecimal valeur, String libelle) {
        if (valeur != null && (valeur.signum() < 0 || valeur.compareTo(BigDecimal.ONE) > 0)) {
            throw new InvalidRequestException(libelle + " : taux entre 0 et 100 %.");
        }
        return valeur;
    }

    static BigDecimal positif(BigDecimal valeur, String libelle) {
        if (valeur != null && valeur.signum() < 0) {
            throw new InvalidRequestException(libelle + " : valeur positive ou nulle.");
        }
        return valeur;
    }

    static Integer positif(Integer valeur, String libelle) {
        if (valeur != null && valeur < 0) {
            throw new InvalidRequestException(libelle + " : valeur positive ou nulle.");
        }
        return valeur;
    }

    /** Règle à nous (OMRAN n'en a pas) : sur les valeurs résultantes, corps sinon base (§14.5). */
    static void ordre(LocalDateTime debut, LocalDateTime fin, String libelle) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new InvalidRequestException(libelle + " : la date de fin précède la date de début.");
        }
    }
}

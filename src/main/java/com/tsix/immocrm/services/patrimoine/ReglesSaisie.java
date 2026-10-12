package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.exceptions.ChampInvalideException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Contrôles de saisie communs aux écritures patrimoine (FEATURE-patrimoine-ecriture.md §2.8) : chaque refus est un 400
 * qui nomme le champ du corps. Les longueurs reprennent les {@code @Size} des entités, contrôlées ici pour répondre 400
 * plutôt que de laisser la validation JPA lever une 500.
 */
final class ReglesSaisie {

    static final int LONGUEUR_COURTE = 255;
    static final int LONGUEUR_MOYENNE = 500;
    static final int LONGUEUR_LONGUE = 1000;

    private ReglesSaisie() {
    }

    /** GEN0001 d'OMRAN. Renvoie la valeur sans espaces de bord. */
    static String requis(String valeur, String champ, String libelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new ChampInvalideException(champ, "La valeur du champ « %s » est obligatoire.".formatted(libelle));
        }
        return longueur(valeur.trim(), LONGUEUR_COURTE, champ);
    }

    static <T> T requis(T valeur, String champ, String libelle) {
        if (valeur == null) {
            throw new ChampInvalideException(champ, "La valeur du champ « %s » est obligatoire.".formatted(libelle));
        }
        return valeur;
    }

    static String longueur(String valeur, int max, String champ) {
        if (valeur != null && valeur.length() > max) {
            throw new ChampInvalideException(champ, "Le champ « %s » dépasse %d caractères.".formatted(champ, max));
        }
        return valeur;
    }

    /** Texte facultatif : absent = rien, sinon contrôlé puis posé (chaîne vide = vider). */
    static void texte(String valeur, int max, String champ, Consumer<String> setter) {
        if (valeur != null) {
            setter.accept(longueur(valeur, max, champ));
        }
    }

    static <T> void si(T valeur, Consumer<T> setter) {
        if (valeur != null) {
            setter.accept(valeur);
        }
    }

    /** Taux stocké en fraction (0,20 = 20 %), comme OMRAN. */
    static BigDecimal fraction(BigDecimal valeur, String champ) {
        if (valeur != null && (valeur.signum() < 0 || valeur.compareTo(BigDecimal.ONE) > 0)) {
            throw new ChampInvalideException(champ,
                    "Le champ « %s » est un taux entre 0 et 100 %%.".formatted(champ));
        }
        return valeur;
    }

    static BigDecimal positifOuNul(BigDecimal valeur, String champ) {
        if (valeur != null && valeur.signum() < 0) {
            throw new ChampInvalideException(champ, "Le champ « %s » ne peut pas être négatif.".formatted(champ));
        }
        return valeur;
    }

    static Integer positifOuNul(Integer valeur, String champ) {
        if (valeur != null && valeur < 0) {
            throw new ChampInvalideException(champ, "Le champ « %s » ne peut pas être négatif.".formatted(champ));
        }
        return valeur;
    }

    /** Motif {@code LIKE} d'une recherche de liste : {@code %texte%} en minuscules, {@code null} si rien à chercher. */
    static String motif(String recherche) {
        return recherche == null || recherche.isBlank() ? null : "%" + recherche.trim().toLowerCase() + "%";
    }

    /** Règle à nous (OMRAN n'en a aucune) : fin ≥ début quand les deux sont connues. */
    static void periode(LocalDateTime debut, LocalDateTime fin, String champFin, String libelle) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new ChampInvalideException(champFin,
                    "La date de fin %s doit être postérieure ou égale à la date de début.".formatted(libelle));
        }
    }
}

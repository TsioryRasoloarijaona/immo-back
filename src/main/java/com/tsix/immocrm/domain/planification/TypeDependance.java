package com.tsix.immocrm.domain.planification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

/** Entiers de {@code Infragistics.Win.UltraWinSchedule.TaskDependencyType}, stockés tels quels (FEATURE-n9-planification.md R10). */
@Schema(description = "Type de dépendance : FIN_DEBUT (0), DEBUT_DEBUT (1), FIN_FIN (2), DEBUT_FIN (3) ; absent = FIN_DEBUT.")
public enum TypeDependance {
    FIN_DEBUT(0), DEBUT_DEBUT(1), FIN_FIN(2), DEBUT_FIN(3);

    private final int valeur;

    TypeDependance(int valeur) {
        this.valeur = valeur;
    }

    public int valeur() {
        return valeur;
    }

    /** Entier inconnu → {@link #FIN_DEBUT}, défaut d'Infragistics. */
    public static TypeDependance depuis(int valeur) {
        return Arrays.stream(values()).filter(t -> t.valeur == valeur).findFirst().orElse(FIN_DEBUT);
    }
}

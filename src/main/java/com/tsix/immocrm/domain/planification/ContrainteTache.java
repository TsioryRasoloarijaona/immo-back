package com.tsix.immocrm.domain.planification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

/** Entiers de {@code Infragistics.Win.UltraWinSchedule.TaskConstraint}, stockés tels quels (FEATURE-n9-planification.md R9). */
@Schema(description = "Contrainte de la tâche (entier Infragistics 0-7 en base) ; défaut AU_PLUS_TOT.")
public enum ContrainteTache {
    AU_PLUS_TARD(0), AU_PLUS_TOT(1), FIN_PAS_AVANT(2), FIN_PAS_APRES(3), DOIT_FINIR_LE(4), DOIT_COMMENCER_LE(5),
    DEBUT_PAS_AVANT(6), DEBUT_PAS_APRES(7);

    private final int valeur;

    ContrainteTache(int valeur) {
        this.valeur = valeur;
    }

    public int valeur() {
        return valeur;
    }

    public static ContrainteTache depuis(Integer valeur) {
        return Arrays.stream(values()).filter(c -> valeur != null && c.valeur == valeur).findFirst().orElse(AU_PLUS_TOT);
    }
}

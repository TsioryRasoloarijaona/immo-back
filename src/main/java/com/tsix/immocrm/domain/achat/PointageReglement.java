package com.tsix.immocrm.domain.achat;

import com.tsix.immocrm.domain.StatutReglement;

import java.time.LocalDate;

/**
 * Colonnes de pointage de la vue OMRAN {@code StlSettlementView}, recalculées (la vue n'existe pas en test) :
 * prévue = pointage, à défaut échéance ; reporté = prévue échue et non pointé (FEATURE-achat-fiche-ecritures.md E-B5).
 */
public record PointageReglement(LocalDate prevue, boolean reporte, LocalDate reportee) {

    public static PointageReglement de(String etat, LocalDate pointage, LocalDate echeance, LocalDate aujourdhui) {
        LocalDate prevue = pointage != null ? pointage : echeance;
        boolean echue = prevue != null && !prevue.isAfter(aujourdhui);
        boolean pointe = StatutReglement.POINTED.name().equals(etat);
        LocalDate reportee = echue && !pointe ? aujourdhui : prevue;
        return new PointageReglement(prevue, echue && !pointe, reportee);
    }
}

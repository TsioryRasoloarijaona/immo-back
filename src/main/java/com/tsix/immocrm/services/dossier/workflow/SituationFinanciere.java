package com.tsix.immocrm.services.dossier.workflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Schéma de financement d'un dossier tel que {@code Sale.cs} le calcule ({@code AdvanceAmount}, {@code CreditAmount},
 * {@code SelfFinancingAmount}, réglés) : montants au crédit moins montants au débit, par nature d'échéance.
 * Les loyers ({@code RentsAmount}) valent 0 : aucune ligne {@code RntDocument} n'est rattachée à un dossier de vente.
 */
public record SituationFinanciere(BigDecimal avance, BigDecimal credit, BigDecimal autoFinancement,
                                  BigDecimal avanceReglee, BigDecimal creditRegle, BigDecimal autoFinancementRegle,
                                  BigDecimal modificationsAuSchema, List<Echeance> echeances) {

    /** Une échéance crédit ou auto-financement au crédit, pour le contrôle des dates ({@code Sale.cs:verifAgreement}). */
    public record Echeance(LocalDateTime date, boolean creditBancaire, boolean autoFinancement, BigDecimal reste) {
    }

    public SituationFinanciere {
        echeances = List.copyOf(echeances);
    }

    /** {@code Math.Round(x, 2)} de .NET : arrondi bancaire au centime. */
    static BigDecimal auCentime(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Somme des versements avance + auto-financement + crédit, au centime ({@code Sale.cs:VerifThreshold}). */
    public BigDecimal versements() {
        return auCentime(auCentime(creditRegle).add(auCentime(autoFinancementRegle)).add(auCentime(avanceReglee)));
    }

    /** Avance + crédit + auto-financement (+ loyers, toujours nuls). */
    public BigDecimal financement() {
        return avance.add(credit).add(autoFinancement);
    }
}

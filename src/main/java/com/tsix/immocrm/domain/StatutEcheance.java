package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Statut d'une ligne d'échéancier ({@code ComDocument}), calculé à la lecture, jamais stocké ;
 * {@link #name()} est la forme émise sur {@code EcheanceDto.statutCalcule}.
 */
public enum StatutEcheance {

    /** Reste à payer nul ou négatif — l'échéance est soldée (un sur-paiement compte comme payé). */
    PAYE,
    /** Partiellement réglée et pas encore due. */
    PARTIEL,
    /** Reste à payer et l'échéance est due (aujourd'hui ou avant), réglée en partie ou non : impayé OMRAN. */
    EN_RETARD,
    /** Rien de réglé et l'échéance n'est pas encore due. */
    A_VENIR;

    /**
     * Payé l'emporte, puis le retard (comme l'impayé d'OMRAN : reste > 0 et échéance ≤ aujourd'hui),
     * puis partiel.
     *
     * @param montant       {@code null} traité comme zéro
     * @param regle         somme des règlements affectés, {@code null} traité comme zéro
     * @param dateEcheance  {@code ComDocument.DocDate} ; {@code null} ⇒ jamais en retard
     */
    public static StatutEcheance calculer(BigDecimal montant, BigDecimal regle,
                                           LocalDateTime dateEcheance, LocalDateTime maintenant) {
        BigDecimal montantDu = montant != null ? montant : BigDecimal.ZERO;
        BigDecimal dejaRegle = regle != null ? regle : BigDecimal.ZERO;
        BigDecimal reste = montantDu.subtract(dejaRegle);

        if (reste.compareTo(BigDecimal.ZERO) <= 0) {
            return PAYE;
        }
        // Même borne que l'écran Impayés ; le statut du dossier n'entre pas dans le statut d'une ligne.
        if (Impaye.echue(dateEcheance, maintenant.toLocalDate())) {
            return EN_RETARD;
        }
        if (dejaRegle.compareTo(BigDecimal.ZERO) > 0) {
            return PARTIEL;
        }
        return A_VENIR;
    }
}

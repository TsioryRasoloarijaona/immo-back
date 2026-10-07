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
    /** Partiellement réglée : au moins un règlement affecté, mais il reste à payer. */
    PARTIEL,
    /** Rien de réglé et la date d'échéance est dépassée. */
    EN_RETARD,
    /** Rien de réglé et l'échéance n'est pas encore due. */
    A_VENIR;

    /**
     * Payé l'emporte, puis partiel ; le retard ne se juge que si rien n'a été réglé.
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
        if (dejaRegle.compareTo(BigDecimal.ZERO) > 0) {
            return PARTIEL;
        }
        if (dateEcheance != null && dateEcheance.isBefore(maintenant)) {
            return EN_RETARD;
        }
        return A_VENIR;
    }
}

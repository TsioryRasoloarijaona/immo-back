package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Statut d'une ligne d'échéancier ({@code ComDocument}), <b>calculé à la lecture</b> et jamais
 * stocké : il se déduit entièrement du montant de l'échéance, de la somme des règlements affectés
 * ({@code StlSettlementLine.Amount}) et de la date d'échéance.
 *
 * <p>Exposé en texte sur {@code EcheanceDto.statutCalcule} — les constantes portent donc le nom
 * exact attendu par le front, et {@link #name()} est la seule forme émise sur le fil. Elles
 * étaient jusqu'ici quatre littéraux affectés dans une cascade {@code if/else} au milieu d'une
 * méthode de mapping ({@code EcheancierService.buildDto}), ce qui les rendait invisibles à la
 * recherche et impossibles à tester sans monter un {@code ComDocument}.
 *
 * <p>Même esprit que {@link StatutReglement} et {@link StatutDossier} : la règle métier vit dans
 * {@code domain/}, sans JPA, et le service se contente de l'appeler.
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
     * Ordre de décision volontairement identique à celui qu'appliquait {@code buildDto} : le
     * caractère « payé » l'emporte sur tout le reste, puis « partiel », et le retard ne se juge
     * que sur une échéance dont rien n'a été réglé.
     *
     * @param montant       montant de l'échéance ({@code ComDocument.Amount}), {@code null} traité comme zéro
     * @param regle         somme des règlements affectés, {@code null} traité comme zéro
     * @param dateEcheance  {@code ComDocument.DocDate} ; {@code null} ⇒ jamais en retard
     * @param maintenant    passé en paramètre plutôt que lu via {@code LocalDateTime.now()}, pour
     *                      que la règle reste une fonction pure et testable
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

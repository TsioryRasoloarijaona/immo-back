package com.tsix.immocrm.domain.audit;

/** Une entrée {@code [propriété, ancienne, nouvelle]} du JSON {@code Details}. */
public record ChangementAudit(String propriete, String ancienneValeur, String nouvelleValeur) {
}

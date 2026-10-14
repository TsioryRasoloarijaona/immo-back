package com.tsix.immocrm.domain;

/**
 * Date sur laquelle s'appliquent {@code du} / {@code au} dans la liste des impayés. Pour une échéance de
 * vente, la date du document et la date d'échéance sont la même colonne ({@code StlImpayedView}).
 */
public enum ChampDateImpaye {
    DOCUMENT,
    ECHEANCE,
    /** Date d'échéance + {@code RecoveryAge} jours. */
    RECOUVREMENT
}

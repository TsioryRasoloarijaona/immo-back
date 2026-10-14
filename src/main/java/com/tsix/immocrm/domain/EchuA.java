package com.tsix.immocrm.domain;

/** Condition « Échu à » de la liste des impayés ({@code FEATURE-impayes.md} §2). */
public enum EchuA {
    /** Date d'échéance ({@code DocDate}) au plus tard aujourd'hui. */
    ECHEANCE,
    /** Date de recouvrement ({@code DocDate} + {@code RecoveryAge} jours) au plus tard aujourd'hui. */
    RECOUVREMENT,
    /** Aucune condition de date. */
    TOUS
}

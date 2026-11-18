package com.tsix.immocrm.domain.achat;

/** Pièce d'achat annulée : statut de type « Annulation » ; listée par les états, exclue de leurs totaux (FEATURE-n7-etats-achat.md D4). */
public final class PieceAnnulee {

    /** Vrai pour une pièce {@code d} jointe à son statut {@code bs} ({@code BuyDocumentStatus}). */
    public static final String SQL = "bs.AdmWorkFlowStateTypeId = 'A9E040DB-533F-4744-AE9A-1425793ADEA0'";

    private PieceAnnulee() {
    }
}

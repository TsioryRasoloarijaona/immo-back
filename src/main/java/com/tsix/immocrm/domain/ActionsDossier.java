package com.tsix.immocrm.domain;

import java.util.UUID;

/**
 * Référentiel légataire des actions d'un dossier de vente (PKey fixes d'OMRAN, mêmes en prod et en CI) et
 * règle de la catégorie selon le statut ({@code Sale.cs:AttachNotes}). Voir FEATURE-vente-fiche.md §B1.
 */
public final class ActionsDossier {

    /** {@code ComActionMessageType} « Instruction ». */
    public static final UUID TYPE_INSTRUCTION = UUID.fromString("6441143F-1111-1111-2222-9DB192573333");
    /** {@code ComActionMessageCategory} « Dossier de vente (Réservation) ». */
    public static final UUID CATEGORIE_RESERVATION = UUID.fromString("69F8E7D6-2222-2222-2222-1AA8B1F02008");
    /** {@code ComActionMessageCategory} « Dossier de vente (Livraison) ». */
    public static final UUID CATEGORIE_LIVRAISON = UUID.fromString("69F8E7D6-3333-3333-3333-1AA8B1F02008");
    /** {@code ComActionMessageCategory} « Désistement et annulation ». */
    public static final UUID CATEGORIE_ANNULATION = UUID.fromString("69F8E7D6-9999-9999-9999-1AA8B1F02008");

    private ActionsDossier() {
    }

    /** Réservation → Réservation, sortie → Désistement et annulation, tout autre statut → Livraison. */
    public static UUID categoriePourStatut(StatutDossier statut) {
        if (statut == StatutDossier.RESERVATION) {
            return CATEGORIE_RESERVATION;
        }
        return statut.isSortie() ? CATEGORIE_ANNULATION : CATEGORIE_LIVRAISON;
    }
}

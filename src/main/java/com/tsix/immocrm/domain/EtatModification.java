package com.tsix.immocrm.domain;

/**
 * États d'une demande de modification ({@code YesRessources.Enums.ModificationStatus}), rangés par leur nom dans
 * {@code ComModification.Status}. Seules ACCEPTEE et REALISED comptent dans le montant des modifications du dossier.
 */
public enum EtatModification {
    ENCOURS("En cours"),
    REFUSE_PAR_LE_SERVICE_TECHNIQUE("Refusée par le service technique"),
    REFUSE_PAR_LE_CLIENT("Refusée par le client"),
    ACCEPTEE("Acceptée"),
    REALISED("Réalisée");

    private final String libelle;

    EtatModification(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /** {@code Sale.cs:ModificationsAmount} : acceptée ou réalisée ; code inconnu ou nul non compté. */
    public static boolean compte(String code) {
        return ACCEPTEE.name().equals(code) || REALISED.name().equals(code);
    }
}

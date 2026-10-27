package com.tsix.immocrm.domain.commission;

/** Raison pour laquelle un dossier d'une génération n'a produit aucune ligne (FEATURE-vente-commissions.md §4). */
public enum RaisonIgnoree {
    SANS_PERSONNE("Aucun commercial ou gestionnaire sur le dossier pour ce type."),
    SANS_DATE("Le dossier n'a pas la date de ce type."),
    SANS_REGLE("Aucune règle de commission de la tranche ne s'applique."),
    MONTANT_NUL("Le montant calculé est nul."),
    DEJA_GENERE("Commission de ce type déjà générée pour cette personne."),
    DOSSIER_DESISTE("Le dossier a déjà un désistement."),
    SANS_COMMISSION_A_REPRENDRE("Aucune commission à reprendre."),
    NON_DESISTE("Le dossier n'a pas de date d'annulation.");

    private final String libelle;

    RaisonIgnoree(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}

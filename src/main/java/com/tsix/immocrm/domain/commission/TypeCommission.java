package com.tsix.immocrm.domain.commission;

/** Type d'une ligne {@code ComCommissionLine} ; {@code libelle} = valeur stockée dans {@code CommissionType}. */
public enum TypeCommission {
    CONCRETISATION("Concrétisation"),
    FINALISATION_1("Finalisation 1"),
    FINALISATION_2("Finalisation 2"),
    DESISTEMENT("Désistement");

    private final String libelle;

    TypeCommission(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /** Signe imposé au montant (C2) : négatif pour un désistement, positif sinon ; variantes « (support) » comprises. */
    public static boolean negatif(String libelleStocke) {
        return libelleStocke != null && libelleStocke.startsWith(DESISTEMENT.libelle);
    }
}

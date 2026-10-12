package com.tsix.immocrm.domain;

/**
 * Types de tiers proposés par les sélecteurs des fiches programme et tranche : le même code sert la liste
 * ({@code /api/referentiels-patrimoine/…}) et le contrôle à l'écriture.
 */
public enum TypeTiersPatrimoine {

    /** {@code CfgTierView.GetEmployeeList} : signataire, responsables, responsable SAV. */
    PERSONNEL("EMP", "un personnel"),
    /** Sélecteur « Tiers autrui » ({@code TierDropDown.IsSupplier}). */
    FOURNISSEUR("FOU", "un fournisseur");

    private final String code;
    private final String libelle;

    TypeTiersPatrimoine(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /** {@code CfgTierType.Code}. */
    public String code() {
        return code;
    }

    /** « un personnel » : complète le message du 400. */
    public String libelle() {
        return libelle;
    }
}

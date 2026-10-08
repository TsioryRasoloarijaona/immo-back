package com.tsix.immocrm.domain;

/**
 * Libellés de {@code ComActionMessageType}/{@code SubCategory} dont dépendent les KPI commerciaux
 * (FEATURE-kpi-performance-commerciaux.md §4) : des données saisies, pas des codes techniques.
 * Ne pas additionner {@link #TYPE_VISITE} (visite consignée) et {@link #RESULTAT_VISITE_DECROCHEE} (promise au téléphone).
 */
public final class InteractionKpi {

    public static final String TYPE_APPEL_SORTANT = "Téléphone Sortant";

    public static final String TYPE_APPEL_ENTRANT = "Téléphone entrant";

    public static final String TYPE_VISITE = "Visite";

    /** Résultat d'appel : la « visite décrochée » au sens commercial. */
    public static final String RESULTAT_VISITE_DECROCHEE = "Le prospect veut faire une visite du projet";

    public static final String RESULTAT_RDV_PRIS = "Rendez vous pris";

    private InteractionKpi() {
    }
}

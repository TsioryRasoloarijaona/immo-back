package com.tsix.immocrm.domain;

import java.util.Set;

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

    /** Résultats d'appel « non joint » (FEATURE-suivi-commerciaux.md R2), codes exacts : « Numéro erroné » finit par un espace. */
    public static final String NON_JOINT_NE_REPOND_PAS = "Prospect ne répond pas";
    public static final String NON_JOINT_INJOIGNABLE = "Prospect injoignable ou Boite vocale";
    public static final String NON_JOINT_OCCUPE = "Téléphone constamment occupé";
    public static final String NON_JOINT_NUMERO_ERRONE = "Numéro erroné ";
    public static final String NON_JOINT_CLIENT_INJOIGNABLE = "Client injoignable ou Boite vocale";
    public static final String NON_JOINT_CLIENT_NE_REPOND_PAS = "Le client ne répond pas";
    public static final String NON_JOINT_A_RAPPELER = "A rappeler par le commercial";

    public static final Set<String> RESULTATS_NON_JOINT = Set.of(NON_JOINT_NE_REPOND_PAS, NON_JOINT_INJOIGNABLE,
            NON_JOINT_OCCUPE, NON_JOINT_NUMERO_ERRONE, NON_JOINT_CLIENT_INJOIGNABLE, NON_JOINT_CLIENT_NE_REPOND_PAS,
            NON_JOINT_A_RAPPELER);

    /** {@code ComActionMessageCategory.PKey} : interaction de prospection, puis de dossier de vente (R5, comme SALEVISIT). */
    public static final String CATEGORIE_PROSPECTION = "69f8e7d6-1111-1111-1111-1aa8b1f02008";
    public static final String CATEGORIE_VENTE_RESERVATION = "69f8e7d6-2222-2222-2222-1aa8b1f02008";
    public static final String CATEGORIE_VENTE_LIVRAISON = "69f8e7d6-3333-3333-3333-1aa8b1f02008";
    public static final String CATEGORIE_VENTE_ANNULATION = "69f8e7d6-9999-9999-9999-1aa8b1f02008";

    private InteractionKpi() {
    }
}

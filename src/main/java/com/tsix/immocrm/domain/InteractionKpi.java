package com.tsix.immocrm.domain;

/**
 * Les codes de {@code ComActionMessageType} et {@code ComActionMessageSubCategory} sur lesquels
 * reposent les KPI de performance commerciale
 * ({@code instructions/FEATURE-kpi-performance-commerciaux.md} §4).
 *
 * <p><b>Ce sont des valeurs de données, pas des identifiants techniques</b> : dans le schéma
 * légataire, {@code Code} et {@code Description} portent le même libellé français, saisi par
 * l'application Windows. Les figer ici plutôt que de les disperser dans des requêtes rend visible
 * ce dont dépend le calcul, et laisse un seul endroit à corriger si un libellé change en base.
 *
 * <p>Chaque constante porte son volume réel, relevé sur {@code yesRemsProd} toutes entités
 * confondues. Ces nombres disent lesquelles comptent : un code à trois occurrences ne fonde pas un
 * indicateur.
 *
 * <p><b>Distinction à ne pas effacer</b> : {@link #TYPE_VISITE} est une visite <i>consignée</i>,
 * {@link #RESULTAT_VISITE_DECROCHEE} est l'engagement <i>obtenu au téléphone</i>. Les additionner
 * ferait passer pour excellent un commercial qui décroche beaucoup de rendez-vous et n'en
 * concrétise aucun.
 */
public final class InteractionKpi {

    /** Appel sortant — 40 325 interactions réelles, de loin le premier geste commercial mesurable. */
    public static final String TYPE_APPEL_SORTANT = "Téléphone Sortant";

    /** Appel entrant — 867 interactions. */
    public static final String TYPE_APPEL_ENTRANT = "Téléphone entrant";

    /** Visite effectivement consignée — 4 311 interactions. */
    public static final String TYPE_VISITE = "Visite";

    /**
     * Résultat d'appel : le prospect s'engage à venir voir le projet — 5 715 interactions.
     * C'est la « visite décrochée » au sens commercial du terme.
     */
    public static final String RESULTAT_VISITE_DECROCHEE = "Le prospect veut faire une visite du projet";

    /** Résultat d'appel : rendez-vous fixé — 2 444 interactions. */
    public static final String RESULTAT_RDV_PRIS = "Rendez vous pris";

    private InteractionKpi() {
    }
}

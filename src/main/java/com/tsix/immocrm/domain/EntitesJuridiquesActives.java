package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Codes {@code CfgCompany} (entité juridique) réellement en exploitation. Le reste des entités
 * legacy (AM, BENACEUR, MAYLINE, YESTEST...) sont des sociétés de test/hors périmètre — décision
 * métier confirmée le 26/08/2026 : restreindre à ces deux entités tous les endpoints Projet/
 * Tranche qui listent des données (dropdowns référentiel + listes paginées Patrimoine).
 */
public final class EntitesJuridiquesActives {

    public static final List<String> CODES = List.of("ADMOD", "GHRIZA");

    private EntitesJuridiquesActives() {
    }
}

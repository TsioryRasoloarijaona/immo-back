package com.tsix.immocrm.domain;

import java.util.Collection;

/** Compte d'agence de vente externe (FEATURE-agences-vente.md §3) : codes partagés avec le script 206. */
public final class RoleAgence {

    /** {@code AdmRole.Code} ; le plus restrictif, il l'emporte sur tout autre rôle (R-INS2). */
    public static final String CODE = "Agence";
    /** {@code CfgRelationType.Code} du lien agence (tiers) → vendeur (contact). */
    public static final String RELATION_VENDEUR = "Vendeur d'agence";

    private RoleAgence() {
    }

    public static boolean porte(Collection<String> roles) {
        return roles.contains(CODE);
    }
}

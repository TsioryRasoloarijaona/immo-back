package com.tsix.immocrm.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Passage prospect ↔ client de {@code CfgTierType.Code}, comme {@code Sale.cs:ManageTier} : toute famille
 * prospect devient {@code CLI} ; seul {@code CLI} redevient {@code PRO} (CLIRNT, CLIPRM, CLIROM intacts).
 */
public final class TypeTiersProspect {

    static final String CLIENT = "CLI";
    static final String PROSPECT = "PRO";
    private static final Set<String> PROSPECTS = Set.of(PROSPECT, "PRORNT", "PROPRM", "PROROM");

    private TypeTiersProspect() {
    }

    /** {@code CLI} pour un type prospect ; vide pour un type client, inconnu ou absent. */
    public static Optional<String> codeClientCorrespondant(String codeType) {
        return PROSPECTS.contains(normaliser(codeType)) ? Optional.of(CLIENT) : Optional.empty();
    }

    /** {@code PRO} pour {@code CLI} seulement ; vide sinon. */
    public static Optional<String> codeProspectCorrespondant(String codeType) {
        return CLIENT.equals(normaliser(codeType)) ? Optional.of(PROSPECT) : Optional.empty();
    }

    private static String normaliser(String codeType) {
        return codeType == null ? "" : codeType.trim().toUpperCase(Locale.ROOT);
    }
}

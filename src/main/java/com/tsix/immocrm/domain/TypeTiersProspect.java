package com.tsix.immocrm.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Famille prospect de {@code CfgTierType.Code} et son type client correspondant (FEATURE-lot4 §3). */
public final class TypeTiersProspect {

    private static final Map<String, String> CLIENT_DU_PROSPECT = Map.of(
            "PRO", "CLI",
            "PRORNT", "CLIRNT",
            "PROPRM", "CLIPRM",
            "PROROM", "CLIROM");

    private TypeTiersProspect() {
    }

    /** Code client du type prospect donné ; vide pour un type client, inconnu ou absent. */
    public static Optional<String> codeClientCorrespondant(String codeType) {
        if (codeType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CLIENT_DU_PROSPECT.get(codeType.trim().toUpperCase(Locale.ROOT)));
    }
}

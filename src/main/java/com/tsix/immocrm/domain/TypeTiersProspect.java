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
        return Optional.ofNullable(normaliser(codeType)).map(CLIENT_DU_PROSPECT::get);
    }

    /** Code prospect du type client donné (retour client → prospect) ; vide pour un type prospect, inconnu ou absent. */
    public static Optional<String> codeProspectCorrespondant(String codeType) {
        String code = normaliser(codeType);
        return CLIENT_DU_PROSPECT.entrySet().stream()
                .filter(e -> e.getValue().equals(code))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static String normaliser(String codeType) {
        return codeType == null ? null : codeType.trim().toUpperCase(Locale.ROOT);
    }
}

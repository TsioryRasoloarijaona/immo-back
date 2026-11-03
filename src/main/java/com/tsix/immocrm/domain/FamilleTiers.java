package com.tsix.immocrm.domain;

import java.util.Arrays;
import java.util.Optional;

/** Familles de l'écran Tiers : segment d'URL, {@code CfgTierType.Code} et filtre Nature de la liste. */
public enum FamilleTiers {
    FOURNISSEURS("fournisseurs", "FOU", true),
    CONTACTS("contacts", "CNT", false);

    private final String segment;
    private final String codeType;
    private final boolean filtreNature;

    FamilleTiers(String segment, String codeType, boolean filtreNature) {
        this.segment = segment;
        this.codeType = codeType;
        this.filtreNature = filtreNature;
    }

    public static Optional<FamilleTiers> depuis(String segment) {
        return Arrays.stream(values()).filter(f -> f.segment.equals(segment)).findFirst();
    }

    public String segment() {
        return segment;
    }

    public String codeType() {
        return codeType;
    }

    public boolean filtreNature() {
        return filtreNature;
    }
}

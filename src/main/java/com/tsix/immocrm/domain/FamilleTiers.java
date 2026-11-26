package com.tsix.immocrm.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Familles de l'écran Tiers : segment d'URL, {@code CfgTierType.Code}, filtre Nature de la liste et forme de la fiche. */
public enum FamilleTiers {
    FOURNISSEURS("fournisseurs", "FOU", true),
    CONTACTS("contacts", "CNT", false),
    BANQUES("banques", "BAN", false, Fiche.REDUITE, "DefaultSocialFormForBANK"),
    ORGANISMES("organismes", "ORG", false, Fiche.REDUITE, "DefaultSocialFormForORGANISM"),
    PERSONNELS("personnels", "EMP", false, Fiche.PHYSIQUE, null),
    // Type CNT et nature AGV : la famille « contacts » exclut cette nature (FEATURE-agences-vente.md R-V6).
    AGENCES("agences", "CNT", false, Fiche.COMPLETE, null, "AGV");

    /** Forme de saisie : champs écrits (service), catalogue d'import, colonnes cherchées, origine du code. */
    public enum Fiche {
        COMPLETE(false, List.of("code", "description", "socialReason", "mobile", "phone", "eMail", "ice")),
        REDUITE(true, List.of("code", "description", "mobile", "phone", "phone1", "phone2", "fax", "eMail", "webSite",
                "notes", "address")),
        PHYSIQUE(false, List.of("code", "description", "socialReason", "mobile", "phone", "eMail"));

        private final boolean codeSaisi;
        private final List<String> colonnesCherchees;

        Fiche(boolean codeSaisi, List<String> colonnesCherchees) {
            this.codeSaisi = codeSaisi;
            this.colonnesCherchees = colonnesCherchees;
        }

        public boolean codeSaisi() {
            return codeSaisi;
        }

        public List<String> colonnesCherchees() {
            return colonnesCherchees;
        }
    }

    private final String segment;
    private final String codeType;
    private final boolean filtreNature;
    private final Fiche fiche;
    private final String parametreFormeSociale;
    private final String codeNature;

    FamilleTiers(String segment, String codeType, boolean filtreNature) {
        this(segment, codeType, filtreNature, Fiche.COMPLETE, null);
    }

    FamilleTiers(String segment, String codeType, boolean filtreNature, Fiche fiche, String parametreFormeSociale) {
        this(segment, codeType, filtreNature, fiche, parametreFormeSociale, null);
    }

    FamilleTiers(String segment, String codeType, boolean filtreNature, Fiche fiche, String parametreFormeSociale,
                 String codeNature) {
        this.segment = segment;
        this.codeType = codeType;
        this.filtreNature = filtreNature;
        this.fiche = fiche;
        this.parametreFormeSociale = parametreFormeSociale;
        this.codeNature = codeNature;
    }

    /** {@code CfgTierKind.Code} imposé à la famille ; absent = toute nature du type, sauf {@link #naturesExclues()}. */
    public Optional<String> codeNature() {
        return Optional.ofNullable(codeNature);
    }

    /** Natures du même type réservées à une autre famille. */
    public List<String> naturesExclues() {
        if (codeNature != null) {
            return List.of();
        }
        return Arrays.stream(values()).filter(f -> f.codeNature != null && f.codeType.equals(codeType))
                .map(f -> f.codeNature).toList();
    }

    /** Seule définition de l'appartenance d'un tiers (type, nature) à la famille ; les requêtes en reprennent les termes. */
    public boolean contient(String typeDuTiers, String natureDuTiers) {
        if (!codeType.equals(typeDuTiers)) {
            return false;
        }
        if (codeNature != null) {
            return codeNature.equals(natureDuTiers);
        }
        return natureDuTiers == null || !naturesExclues().contains(natureDuTiers);
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

    public Fiche fiche() {
        return fiche;
    }

    /** Clé {@code AdmParamsGuid} de la forme sociale posée à la création ; absente = saisie sur la fiche. */
    public Optional<String> parametreFormeSociale() {
        return Optional.ofNullable(parametreFormeSociale);
    }
}

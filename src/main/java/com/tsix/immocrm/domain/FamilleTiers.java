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
    PERSONNELS("personnels", "EMP", false, Fiche.PHYSIQUE, null);

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

    FamilleTiers(String segment, String codeType, boolean filtreNature) {
        this(segment, codeType, filtreNature, Fiche.COMPLETE, null);
    }

    FamilleTiers(String segment, String codeType, boolean filtreNature, Fiche fiche, String parametreFormeSociale) {
        this.segment = segment;
        this.codeType = codeType;
        this.filtreNature = filtreNature;
        this.fiche = fiche;
        this.parametreFormeSociale = parametreFormeSociale;
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

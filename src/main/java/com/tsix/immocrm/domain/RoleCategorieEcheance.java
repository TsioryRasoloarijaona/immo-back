package com.tsix.immocrm.domain;

/**
 * Catégories d'échéance désignées par les paramètres {@code AdmParamsGuid} d'OMRAN ({@code GlobalParams.Default*DocumentType}) ;
 * {@code code} = repli quand le paramètre manque (bases de test), {@code null} = pas de repli.
 */
public enum RoleCategorieEcheance {
    PRE_RESERVATION("DefaultBeforeReservationDocumentType", "PRÉ-RÉSERVATION"),
    AVANCE("DefaultAdvanceDocumentType", "AVANCE"),
    AUTO_FINANCEMENT("DefaultAutoFinancementDocumentType", "AUTO-FINANCEMENT"),
    FRAIS_DOSSIER("DefaultFeeDocumentType", "FRAIS DOSSIER"),
    AUTRES_FRAIS("DefaultOtherFeeDocumentType", null),
    FRAIS_SYNDIC("DefaultSyndicFeeDocumentType", "FRAIS SYNDIC");

    private final String parametre;
    private final String code;

    RoleCategorieEcheance(String parametre, String code) {
        this.parametre = parametre;
        this.code = code;
    }

    public String parametre() {
        return parametre;
    }

    public String code() {
        return code;
    }
}

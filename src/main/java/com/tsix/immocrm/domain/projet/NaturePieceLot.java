package com.tsix.immocrm.domain.projet;

/**
 * Pièce à payer créée depuis la fiche lot ({@code BuyDocument.ReferenceType}, FEATURE-lot-pieces-a-payer.md R1-R8) : gestion OMRAN
 * ({@code StrEntity.Code}), drapeau de statut du lot exigé, préfixe de {@code DocRef}, libellés et droit {@code toolAdvance}.
 */
public enum NaturePieceLot {
    MARKET_ADVANCE("AVA", "PrjDocumentDebitMarketAdvance", Drapeau.AVANCE_MARCHE, "Avance sur lot", "Avance sur lot",
            "Vous ne pouvez pas ajouter une avance sur marché dans l'état actuel !", true),
    WORKINGOUT_ADVANCE("W.AVA", "PrjDocumentDebitMarketWorkingOutAdvance", Drapeau.AVANCE_DECOMPTE, "Avance sur décompte",
            "Avance sur Décompte du lot", "Vous ne pouvez pas ajouter une avance sur décompte dans l'état actuel !", true),
    MARKET_WARRANTY("RRG", "PrjDocumentDebitMarketWarranty", Drapeau.AVANCE_MARCHE, "Remboursement de retenue de garantie",
            "Remboursement de retenue de garantie",
            "Vous ne pouvez pas ajouter une restitution de retenue de garantie dans l'état actuel !", false),
    MARKET_PROPORTION("RCP", "PrjDocumentDebitMarketProrataRefund", Drapeau.AUCUN, "Remboursement de compte prorata",
            "Remboursement de compte prorata", null, false);

    /** Drapeau de {@code StrStatus} du lot exigé : {@code BoolField010} avance marché, {@code BoolField008} avance décompte. */
    public enum Drapeau { AVANCE_MARCHE, AVANCE_DECOMPTE, AUCUN }

    private final String prefixe;
    private final String gestion;
    private final Drapeau drapeau;
    private final String libelleLigne;
    private final String prefixeNotes;
    private final String refusEtat;
    private final boolean avance;

    NaturePieceLot(String prefixe, String gestion, Drapeau drapeau, String libelleLigne, String prefixeNotes, String refusEtat,
                   boolean avance) {
        this.prefixe = prefixe;
        this.gestion = gestion;
        this.drapeau = drapeau;
        this.libelleLigne = libelleLigne;
        this.prefixeNotes = prefixeNotes;
        this.refusEtat = refusEtat;
        this.avance = avance;
    }

    public String prefixe() {
        return prefixe;
    }

    public String gestion() {
        return gestion;
    }

    public Drapeau drapeau() {
        return drapeau;
    }

    public String libelleLigne() {
        return libelleLigne;
    }

    public String prefixeNotes() {
        return prefixeNotes;
    }

    /** Message 409 (R4) quand le drapeau exigé manque ; null pour RCP. */
    public String refusEtat() {
        return refusEtat;
    }

    /** Avance (AVA, W.AVA) : devise du lot ; sinon devise par défaut (R1). */
    public boolean avance() {
        return avance;
    }

    /** Créer / supprimer exige {@code toolAdvance} (R2) : tout sauf le prorata. */
    public boolean exigeDroit() {
        return this != MARKET_PROPORTION;
    }

    /** Vrai si le statut du lot porte le drapeau exigé ({@code avanceMarche} = 010, {@code avanceDecompte} = 008). */
    public boolean etatPermis(boolean avanceMarche, boolean avanceDecompte) {
        return switch (drapeau) {
            case AVANCE_MARCHE -> avanceMarche;
            case AVANCE_DECOMPTE -> avanceDecompte;
            case AUCUN -> true;
        };
    }
}

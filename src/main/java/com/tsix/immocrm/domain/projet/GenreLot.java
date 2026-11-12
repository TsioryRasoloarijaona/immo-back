package com.tsix.immocrm.domain.projet;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/** Genre d'un lot = son {@code StrEntityId} ({@code MarketMonitoring*.cs}) ; droit OMRAN de création du genre. */
public enum GenreLot {
    ENTREPRENEURS("E7999F70-7507-40D9-AA78-A2825C42B613", GenreLot.DROIT_NOUVEAU),
    FOURNISSEURS("E7999F70-7507-40D9-AA78-A2825C42B614", GenreLot.DROIT_NOUVEAU_MULTI),
    ADMINISTRATION("E7999F71-7507-40D9-AA78-A2825C42B614", GenreLot.DROIT_NOUVEAU_MULTI),
    MAITRES_OEUVRE("E7999F72-7507-40D9-AA78-A2825C42B614", GenreLot.DROIT_NOUVEAU_MULTI),
    CONCESSIONNAIRES("E7999F74-7507-40D9-AA78-A2825C42B614", GenreLot.DROIT_NOUVEAU_MULTI);

    private static final String DROITS = "Yes.Prj.Front.Market.MarketMonitoringList.";
    public static final String DROIT_NOUVEAU = DROITS + "toolNew";
    // MarketMonitoringList.cs:FilterToolsByRights : les quatre genres « multi » partagent ce droit.
    public static final String DROIT_NOUVEAU_MULTI = DROITS + "toolNewMultiSupplier";
    public static final String DROIT_SUPPRIMER = DROITS + "toolDelete";
    public static final String DROIT_EXPORTER = DROITS + "toolExport";

    private final UUID entite;
    private final String droitCreation;

    GenreLot(String entite, String droitCreation) {
        this.entite = UUID.fromString(entite);
        this.droitCreation = droitCreation;
    }

    public UUID entite() {
        return entite;
    }

    public String droitCreation() {
        return droitCreation;
    }

    public boolean multiFournisseur() {
        return this != ENTREPRENEURS;
    }

    public static Optional<GenreLot> deEntite(UUID entite) {
        return Arrays.stream(values()).filter(g -> g.entite.equals(entite)).findFirst();
    }

    public static Optional<GenreLot> deNom(String nom) {
        return Arrays.stream(values()).filter(g -> g.name().equals(nom)).findFirst();
    }
}

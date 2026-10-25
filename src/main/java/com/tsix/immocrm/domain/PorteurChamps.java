package com.tsix.immocrm.domain;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.util.Arrays;

/** Porteurs des champs spécifiques (GEN0007) : valeur du fil et code d'entité OMRAN ({@code Current.GetType().FullName}). */
public enum PorteurChamps {

    PROJET("projet", "Yes.Cfg.CfgProject", "Programme"),
    TRANCHE("tranche", "Yes.Cfg.CfgTranche", "Tranche"),
    ENTITE("entite", "Yes.Cfg.CfgCompany", "Entité juridique"),
    // Article et sous-article : même classe OMRAN.
    ARTICLE("article", "Yes.Stk.Building.Item", "Article"),
    // Classe de la parcelle ; Yes.Stk.Land.Land est celle du lot de lotissement.
    TERRAIN("terrain", "Yes.Stk.Land.LandProject", "Terrain"),
    PROSPECTION_TERRAIN("prospection-terrain", "Yes.Lnd.Prospection", "Prospection terrain"),
    ETUDE("etude", "Yes.Stk.StkFeasibilityStudy", "Étude de faisabilité");

    private final String fil;
    private final String codeEntite;
    private final String libelle;

    PorteurChamps(String fil, String codeEntite, String libelle) {
        this.fil = fil;
        this.codeEntite = codeEntite;
        this.libelle = libelle;
    }

    public String codeEntite() {
        return codeEntite;
    }

    public String libelle() {
        return libelle;
    }

    /** Valeur du fil ; autre → 400. */
    public static PorteurChamps depuis(String valeur) {
        return Arrays.stream(values()).filter(p -> p.fil.equals(valeur)).findFirst()
                .orElseThrow(() -> new InvalidRequestException("porteur doit valoir projet, tranche, entite, article, "
                        + "terrain, prospection-terrain ou etude : '" + valeur + "'"));
    }
}

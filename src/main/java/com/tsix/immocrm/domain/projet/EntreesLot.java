package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** Entrées de {@link CalculLot} : drapeaux et taux de {@code PrjMarket}, sommes lues en base (absente = 0). */
public record EntreesLot(Drapeaux drapeaux, Taux taux, Map<Somme, BigDecimal> sommes) {

    /**
     * {@code termine} = {@code IsTerminated} de la vue, {@code annule} = statut d'annulation ou de résiliation,
     * {@code gestionnaire} = lot désigné par {@code PrjProject.ManageProrataPrjMarketId}.
     */
    public record Drapeaux(boolean multi, boolean termine, boolean annule, boolean gestionnaire, boolean blocage,
                           boolean retenueSurSupplementaires) {
    }

    /** Taux et forfaits saisis sur le lot, jamais nuls (0 si non saisis). */
    public record Taux(BigDecimal prorata, BigDecimal decRatio, BigDecimal decMontant, BigDecimal trcRatio,
                       BigDecimal trcMontant, BigDecimal avance, BigDecimal garantie, BigDecimal forfaitHT,
                       BigDecimal forfaitTVA, BigDecimal forfaitTTC) {
    }

    /**
     * Sommes d'un lot. Chaque requête de lecture rend ses colonnes dans l'ordre de cet enum, à partir de sa première constante :
     * ne pas réordonner sans la requête. DEC_ = décomptes, AV_ = avenants, FAC_ = factures de décompte, FAV_ = factures
     * d'avenant, AVO_ = avoirs, CMD_ = commandes validées (multi-fournisseur).
     */
    public enum Somme {
        LIGNES_HT, LIGNES_TTC,
        ESTIM_HT, ESTIM_TTC,
        DEP_HT, DEP_TTC, DEP_FACT_HT, DEP_FACT_TTC,
        SUPP_HT, SUPP_TTC, SUPP_FACT_HT, SUPP_FACT_TTC,
        CMD_DEP_HT, CMD_DEP_TTC,
        DEC_HT, DEC_TTC, DEC_STOCK_HT, DEC_STOCK, DEC_FLUCT_HT, DEC_FLUCT, DEC_AVANCE, DEC_AVANCE_DECOMPTE, DEC_RG,
        DEC_PRORATA, DEC_DEC, DEC_TRC, DEC_REMB_PRORATA, DEC_FACT_STOCK_HT, DEC_FACT_STOCK, DEC_FACT_FLUCT_HT,
        DEC_FACT_FLUCT, RETENUES_FORFAIT,
        AV_HT, AV_TTC, AV_RG, AV_PRORATA, AV_DEC, AV_TRC,
        REGUL,
        FAC_HT, FAC_TTC, FAC_AVANCE, FAC_AVANCE_DECOMPTE, FAC_RG, FAC_PRORATA, FAC_DEC, FAC_TRC, FAC_RETENUE,
        FAC_REMB_PRORATA, FAC_REGUL, FAC_TIMBRE,
        FAV_HT, FAV_TTC, FAV_RG, FAV_PRORATA, FAV_DEC, FAV_TRC, FAV_REGUL, FAV_TIMBRE,
        AVO_HT, AVO_TTC, AVO_PRORATA, AVO_DEC, AVO_TRC, AVO_RETENUE, AVO_STOCK, AVO_REMB_PRORATA, AVO_FLUCT, AVO_REGUL,
        AVO_TIMBRE,
        CMD_HT, CMD_TTC, CMD_AVANCE, SANS_CMD_HT, SANS_CMD_TTC, AVEC_CMD_HT,
        AVANCE_VALIDEE, AVANCE_CMD_VALIDEE, AVANCE_DEC_VALIDEE, RG_VALIDEE, RG_PENALITE, PROPORTION,
        REGLE, REGLE_AVOIRS, REGLE_AVANCE, REGLE_AVANCE_CMD, REGLE_AVANCE_DEC, RG_RESTITUEE,
        DEVIS_HT, DEVIS_TTC,
        PREV_REMB_PRORATA, FAC_PRORATA_PROJET
    }

    BigDecimal de(Somme s) {
        return z(sommes.get(s));
    }

    private static BigDecimal z(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}

package com.tsix.immocrm.domain.tresorerie;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Les quatre listes Recettes d'OMRAN ({@code Yes.Stl.Front.Credit}) : nom dans l'URL, droit OMRAN de l'écran, nature
 * forcée de la pièce ({@code StlDocument.DocType}) ; les encaissements lisent {@code StlSettlement} ({@code IsCredit = 1}).
 */
public enum VueRecette {

    PROFORMA("proforma", "Yes.Stl.Front.Credit.InvoiceProformaList", "INVOICE_PROFORMA"),
    FACTURES("factures", "Yes.Stl.Front.Credit.InvoiceList", "INVOICE"),
    AVOIRS("avoirs", "Yes.Stl.Front.Credit.RefundList", "REFUND"),
    ENCAISSEMENTS("encaissements", "Yes.Stl.Front.Credit.SettlementList", null);

    private final String chemin;
    private final String droit;
    private final String nature;

    VueRecette(String chemin, String droit, String nature) {
        this.chemin = chemin;
        this.droit = droit;
        this.nature = nature;
    }

    public static Optional<VueRecette> depuis(String chemin) {
        return Arrays.stream(values()).filter(v -> v.chemin.equals(chemin)).findFirst();
    }

    public String droit() {
        return droit;
    }

    public String nature() {
        return nature;
    }

    public boolean encaissements() {
        return nature == null;
    }

    /** Colonnes de la grille OMRAN de la vue, dans l'ordre d'OMRAN. */
    public List<ColonneRecette> colonnes() {
        ColonneRecette.Famille famille = encaissements() ? ColonneRecette.Famille.ENCAISSEMENT : ColonneRecette.Famille.PIECE;
        return Arrays.stream(ColonneRecette.values()).filter(c -> c.de(famille)).toList();
    }
}

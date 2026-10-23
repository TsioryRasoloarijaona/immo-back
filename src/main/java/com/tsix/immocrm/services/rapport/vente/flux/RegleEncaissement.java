package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.domain.StatutReglement;

/**
 * Seule définition d'une ligne de règlement qui compte dans les flux de vente (FEATURE-tdb-vente-rapports-flux.md §3.3),
 * en SQL natif ({@code IsInAgrementContext} n'est pas mappé). Alias : {@code l} ligne, {@code s} règlement, {@code ty}
 * type de pièce, {@code d} échéance, {@code k} catégorie, {@code f} dossier, {@code st} statut, {@code i} article,
 * {@code v} vocation.
 */
public final class RegleEncaissement {

    /** Jointures : statut interne (dossier sans statut absent), type de pièce interne (comme la vue), vocation externe. */
    public static final String LIGNES = """
            FROM StlSettlementLine l
            JOIN StlSettlement s ON s.PKey = l.StlSettlementId
            JOIN StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId
            JOIN ComDocument d ON d.PKey = l.ComDocumentId
            JOIN ComSettlementCategory k ON k.PKey = d.ComSettlementCategoryId
            JOIN ComFolder f ON f.PKey = d.ComFolderId
            JOIN StrStatus st ON st.PKey = f.StrStatusId
            JOIN StkItem i ON i.PKey = f.StkItemId
            LEFT JOIN StkVocation v ON v.PKey = f.StkVocationId
            """;

    /** Règlement non annulé, pièce comptabilisée ({@code IsNotContabilized} NULL = 0). */
    public static final String COMPTE = " " + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX
            + " AND COALESCE(ty.IsNotContabilized, 0) = 0 ";

    /** Montant signé partagé ({@code IsCredit} NULL compté en encaissement). */
    public static final String MONTANT = StatutReglement.SQL_MONTANT_SIGNE;

    /** Date du mouvement : règlement, sinon échéance. */
    public static final String MOUVEMENT = "COALESCE(s.SettlementDate, s.EcheanceDate)";

    /** Encaissement de vente : catégorie hors frais ({@code IsSaleExpense} NULL = 0), dossier VENTE, article racine. */
    public static final String ENCAISSEMENT = " COALESCE(k.IsSaleExpense, 0) = 0 AND v.Code = 'VENTE' AND i.IsUnderItem = 0 ";

    /** Encaissement de frais : ni vocation ni article exigés (comme {@code ComSettlementLineView}). */
    public static final String FRAIS = " k.IsSaleExpense = 1 ";

    private RegleEncaissement() {
    }
}

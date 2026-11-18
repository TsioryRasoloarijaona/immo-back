package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie;
import com.tsix.immocrm.services.administration.HistoriqueService;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SQL natif de la trésorerie Projet / Achat (FEATURE-n3-recettes-depenses.md). Une ligne de règlement est visible si sa
 * pièce d'achat l'est ({@link RequetesDocumentsAchat#VISIBLE}) ou, pièce de recette sans projet, si le sans-projet l'est.
 */
@Component
class RequetesTresorerie {

    // SQL Server convertit la liste IN même court-circuitée : '' y ferait échouer la conversion en uniqueidentifier.
    private static final UUID AUCUN = new UUID(0, 0);

    private static final String LIGNE_VISIBLE = "((d.PKey IS NOT NULL AND " + RequetesDocumentsAchat.VISIBLE
            + ") OR (lv.StlDocumentId IS NOT NULL AND :sansProjet = 1))";

    private static final String JOUR_FILTRE = "CASE :dateSur WHEN 'ECHEANCE' THEN CAST(s.EcheanceDate AS date) "
            + "WHEN 'POINTAGE' THEN CAST(s.SettlementDate AS date) WHEN 'POINTAGE_REPORTE' THEN mv.Jour "
            + "ELSE CAST(s.DocDate AS date) END";

    private static final String COLONNES_REGLEMENT = "SELECT s.PKey AS id, s.Code AS code, s.Description AS description, "
            + "s.DocDate AS datePiece, s.EcheanceDate AS echeance, s.SettlementDate AS datePointage, "
            + "md.Origine AS dateMouvementOrigine, CAST(CASE WHEN mv.Jour <> md.Origine THEN 1 ELSE 0 END AS bit) "
            + "AS mouvementReporte, mv.Jour AS dateMouvement, MONTH(mv.Jour) AS moisMouvement, "
            + "YEAR(mv.Jour) AS anneeMouvement, ty.Code AS typeCode, ty.Description AS type, "
            + "ty.IsNotContabilized AS nonComptabilisable, s.IsAccounted AS comptabilise, s.Status AS statutCode, "
            + "co.Code AS societeCode, co.Description AS societe, cu.Code AS deviseCode, mo.Code AS moduleCode, "
            + "mo.Description AS module, ti.Code AS tiersCode, ti.Description AS tiers, s.AmountBase AS montantBase, "
            + "s.RsRatio AS tauxRS, s.AmountRS AS montantRS, s.AmountStamp AS timbre, s.Amount AS montant, "
            + "ac.Code AS compteCode, ac.Description AS compte, bq.Code AS banqueCode, bq.Description AS banque, "
            + "de.Code AS depotCode, de.Description AS depot, s.IsDcoumentDeliveredToCenter AS documentRemisAuCentre, "
            + "s.DeliveredToCenterDate AS dateRemiseAuCentre, s.SlipCode AS bordereau, s.SlipDate AS dateBordereau, "
            + "s.ReceiptCode AS codeRecu, s.PropositionSheetCode AS ficheProposition, s.Notes AS notes, "
            + "s.RSAmountSettled AS rsRegle, s.RSSettlementDate AS dateReglementRS, s.RSSheetCode AS ficheRS, "
            + "pa.Description AS payeur, ot.Description AS autreTiers, av.Description AS avocat, "
            + "bf.Code AS dossierAchatCode, bf.Description AS dossierAchat, s.UserCreate AS creePar, "
            + "s.UserCreateDateTime AS creeLe, s.UserUpdate AS modifiePar, s.UserUpdateDateTime AS modifieLe, "
            + "s.ConvertValue AS ConvertValue, s.ConvertUnit AS ConvertUnit, la.NbLignes AS NbLignes, la.Affecte AS Affecte ";

    private static final String SOURCE_REGLEMENT = "FROM StlSettlement s JOIN AdmModule mo ON mo.PKey = s.AdmModuleId "
            + "LEFT JOIN CfgTier ti ON ti.PKey = s.TierId "
            + "LEFT JOIN StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId "
            + "LEFT JOIN CfgTier bq ON bq.PKey = s.CfgBankId LEFT JOIN StlAccount ac ON ac.PKey = s.StlAccountId "
            + "LEFT JOIN CfgCompany co ON co.PKey = s.CfgCompanyId LEFT JOIN StlCurrency cu ON cu.PKey = s.StlCurrencyId "
            + "LEFT JOIN StlDeposit de ON de.PKey = s.StlDepositId LEFT JOIN CfgTier pa ON pa.PKey = s.TierPayerId "
            + "LEFT JOIN CfgTier ot ON ot.PKey = s.CfgTierOtherId LEFT JOIN CfgTier av ON av.PKey = s.CfgLawyerId "
            + "LEFT JOIN BuyFolder bf ON bf.Pkey = s.BuyFolderId "
            + "CROSS APPLY (SELECT COUNT(*) AS NbLignes, SUM(x.Amount) AS Affecte FROM StlSettlementLine x "
            + "WHERE x.StlSettlementId = s.PKey) la "
            + "CROSS APPLY (SELECT CAST(ISNULL(s.SettlementDate, s.EcheanceDate) AS date) AS Origine) md "
            + "CROSS APPLY (SELECT CASE WHEN md.Origine <= :aujourdhui AND ISNULL(s.Status, '') <> 'POINTED' "
            + "THEN :aujourdhui ELSE md.Origine END AS Jour) mv ";

    /** Pièce de la ligne {@code lv} : facture d'achat {@code d} ou pièce de recette {@code sd}. */
    private static final String PIECE_DE_LA_LIGNE = "LEFT JOIN BuyDocument d ON d.PKey = lv.BuyDocumentId "
            + "LEFT JOIN StlDocument sd ON sd.PKey = lv.StlDocumentId ";

    private static final String COLONNES_PIECE = "lv.PKey AS LigneId, lv.Amount AS MontantAffecte, "
            + "COALESCE(d.PKey, sd.PKey) AS PieceId, COALESCE(d.DocType, sd.DocType) AS PieceType, "
            + "COALESCE(d.Code, sd.Code) AS PieceCode, fo.Description AS Fournisseur, d.PrjProjectId AS ProjetId, "
            + "pp.Code AS Projet, d.PrjMarketId AS LotId, pm.Code AS Lot ";

    private static final String JOINTURES_PIECE = PIECE_DE_LA_LIGNE
            + "LEFT JOIN CfgTier fo ON fo.PKey = COALESCE(d.CfgTierId, sd.CfgTierId) "
            + "LEFT JOIN PrjProject pp ON pp.PKey = d.PrjProjectId LEFT JOIN PrjMarket pm ON pm.PKey = d.PrjMarketId ";

    private static final String COLONNES_LIGNE = ", " + COLONNES_PIECE
            + ", COALESCE(d.AmountToPay, sd.AmountToPay) AS MontantPiece, ca.Description AS Categorie ";

    private static final String JOINTURES_LIGNE = "JOIN StlSettlementLine lv ON lv.StlSettlementId = s.PKey "
            + JOINTURES_PIECE + "LEFT JOIN StlCategory ca ON ca.PKey = s.StlCategoryId ";

    /** Règlement visible : société habilitée ET (aucune ligne et sans-projet retenu, OU une ligne visible). */
    private static final String REGLEMENT_VISIBLE = "AND ((:sansProjet = 1 AND la.NbLignes = 0) OR EXISTS (SELECT 1 "
            + "FROM StlSettlementLine lv " + PIECE_DE_LA_LIGNE + "WHERE lv.StlSettlementId = s.PKey AND "
            + LIGNE_VISIBLE + ")) ";

    private static final String FILTRES_REGLEMENT = "WHERE mo.Code = :module AND ISNULL(s.IsCredit, 0) = :credit "
            + "AND s.CfgCompanyId IN (:societes) "
            + "AND (:du IS NULL OR " + JOUR_FILTRE + " >= :du) AND (:au IS NULL OR " + JOUR_FILTRE + " <= :au) "
            + "AND (:sansType = 1 OR s.StlSettlementTypeId IN (:types)) "
            + "AND (:sansStatut = 1 OR ISNULL(s.Status, '') IN (:statuts)) "
            + "AND (:compte IS NULL OR s.StlAccountId = :compte) AND (:tiers IS NULL OR s.TierId = :tiers) "
            + "AND (:comptabilise IS NULL OR ISNULL(s.IsAccounted, 0) = :comptabilise) "
            + "AND (:montantDe IS NULL OR s.Amount >= :montantDe) AND (:montantA IS NULL OR s.Amount <= :montantA) "
            + "AND (:inclureAnnules = 1 OR " + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX + ") "
            + "AND (:motif IS NULL OR s.Code LIKE :motif OR s.Description LIKE :motif OR ti.Description LIKE :motif) ";

    private static final String TRI_REGLEMENT = "ORDER BY s.DocDate DESC, s.Code DESC";

    private static final String REGLEMENTS = COLONNES_REGLEMENT + SOURCE_REGLEMENT + FILTRES_REGLEMENT
            + REGLEMENT_VISIBLE + TRI_REGLEMENT;

    private static final String AFFECTATIONS = COLONNES_REGLEMENT + COLONNES_LIGNE + SOURCE_REGLEMENT + JOINTURES_LIGNE
            + FILTRES_REGLEMENT + "AND " + LIGNE_VISIBLE + " " + TRI_REGLEMENT + ", lv.PKey";

    private static final String FICHE = COLONNES_REGLEMENT + SOURCE_REGLEMENT + "WHERE s.PKey = :id "
            + "AND mo.Code IN ('PRJ', 'BUY') AND ISNULL(s.IsCredit, 0) = 0 AND s.CfgCompanyId IN (:societes) "
            + REGLEMENT_VISIBLE;

    private static final String LIGNES_FICHE = "SELECT " + COLONNES_PIECE + "FROM StlSettlementLine lv " + JOINTURES_PIECE
            + "WHERE lv.StlSettlementId = :id AND " + LIGNE_VISIBLE + " ORDER BY COALESCE(d.Code, sd.Code), lv.PKey";

    // StlDocument n'a pas de projet : la colonne nulle ajoutée laisse VISIBLE appliquer la règle « sans projet ».
    private static final String PIECES_RECETTE = "SELECT d.PKey AS id, "
            + "d.PaymentAuthorizationDate AS dateAutorisationPaiement, d.DocType AS nature, d.Code AS code, "
            + "d.Description AS description, d.DocDate AS datePiece, d.MaturityDate AS echeance, "
            + "d.IsAccounted AS comptabilise, co.Code AS societeCode, co.Description AS societe, cu.Code AS deviseCode, "
            + "cu.Description AS devise, mo.Description AS module, ti.Code AS tiersCode, ti.Description AS tiers, "
            + "d.PersonToContact AS personneAContacter, d.DocRef AS referenceExterne, d.OrderCode AS commande, "
            + "d.AmountHT AS montantHT, d.AmountVat AS montantTVA, d.AmountTTC AS montantTTC, "
            + "d.AmountRefundedHT AS avoirHT, d.AmountRefundedVat AS avoirTVA, d.AmountRefundedTTC AS avoirTTC, "
            + "d.TotalHT AS totalHT, d.TotalVat AS totalTVA, d.TotalTTC AS totalTTC, d.AmountRegul AS regularisation, "
            + "d.FiscalStamp AS timbreFiscal, d.AmountToPay AS montantAPayer, (" + SoldeDocumentAchat.SQL_REGLE_RECETTE
            + ") AS montantRegle, d.Notes AS notes, d.Notes1 AS notes1, bf.Code AS dossierAchatCode, "
            + "bf.Description AS dossierAchat, d.UserCreate AS creePar, d.UserCreateDateTime AS creeLe, "
            + "d.UserUpdate AS modifiePar, d.UserUpdateDateTime AS modifieLe "
            + "FROM (SELECT x.*, CAST(NULL AS uniqueidentifier) AS PrjProjectId FROM StlDocument x) d "
            + "JOIN AdmModule mo ON mo.PKey = d.AdmModuleId LEFT JOIN CfgCompany co ON co.PKey = d.CfgCompanyId "
            + "LEFT JOIN StlCurrency cu ON cu.PKey = d.StlCurrencyId LEFT JOIN CfgTier ti ON ti.PKey = d.CfgTierId "
            + "LEFT JOIN BuyFolder bf ON bf.Pkey = d.BuyFolderId "
            + "WHERE " + RequetesDocumentsAchat.VISIBLE + "AND d.DocType = :nature AND mo.Code = :module "
            + "AND (:du IS NULL OR CAST(CASE :dateSur WHEN 'ECHEANCE' THEN d.MaturityDate ELSE d.DocDate END AS date) "
            + ">= :du) AND (:au IS NULL OR CAST(CASE :dateSur WHEN 'ECHEANCE' THEN d.MaturityDate ELSE d.DocDate END "
            + "AS date) <= :au) AND (:tiers IS NULL OR d.CfgTierId = :tiers) "
            + "AND (:comptabilise IS NULL OR ISNULL(d.IsAccounted, 0) = :comptabilise) "
            + "AND (:montantDe IS NULL OR d.AmountToPay >= :montantDe) "
            + "AND (:montantA IS NULL OR d.AmountToPay <= :montantA) "
            + "AND (:motif IS NULL OR d.Code LIKE :motif OR d.Description LIKE :motif OR ti.Description LIKE :motif) "
            + "ORDER BY d.DocDate DESC, d.Code DESC";

    private final NamedParameterJdbcTemplate jdbc;

    RequetesTresorerie(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Règlements du module et du sens, filtrés et visibles, triés ; une {@code Map} par règlement, clé = alias. */
    List<Map<String, Object>> reglements(PerimetreAchat perimetre, FiltresTresorerie f, boolean credit, LocalDate jour) {
        return jdbc.queryForList(REGLEMENTS, filtres(perimetre, f, credit, jour));
    }

    /** Une {@code Map} par ligne d'affectation visible : colonnes du règlement puis celles de la ligne. */
    List<Map<String, Object>> affectations(PerimetreAchat perimetre, FiltresTresorerie f, boolean credit,
                                           LocalDate jour) {
        return jdbc.queryForList(AFFECTATIONS, filtres(perimetre, f, credit, jour));
    }

    List<Map<String, Object>> fiche(PerimetreAchat perimetre, UUID id, LocalDate jour) {
        return jdbc.queryForList(FICHE, RequetesDocumentsAchat.parametres(perimetre)
                .addValue("id", id.toString()).addValue("aujourdhui", jour, Types.DATE));
    }

    List<Map<String, Object>> lignesFiche(PerimetreAchat perimetre, UUID id) {
        return jdbc.queryForList(LIGNES_FICHE, RequetesDocumentsAchat.parametres(perimetre).addValue("id", id.toString()));
    }

    List<Map<String, Object>> piecesRecette(PerimetreAchat perimetre, FiltresTresorerie f, String nature) {
        return jdbc.queryForList(PIECES_RECETTE, communs(perimetre, f).addValue("nature", nature));
    }

    private static MapSqlParameterSource filtres(PerimetreAchat perimetre, FiltresTresorerie f, boolean credit,
                                                 LocalDate jour) {
        return communs(perimetre, f)
                .addValue("credit", credit ? 1 : 0)
                .addValue("aujourdhui", jour, Types.DATE)
                .addValue("sansType", f.typeIds().isEmpty() ? 1 : 0)
                .addValue("types", RequetesDocumentsAchat.textes(f.typeIds().isEmpty() ? List.of(AUCUN) : f.typeIds()))
                .addValue("sansStatut", f.statuts().isEmpty() ? 1 : 0)
                .addValue("statuts", f.statuts().isEmpty() ? List.of("") : f.statuts())
                .addValue("compte", RequetesDocumentsAchat.texte(f.compteId()), Types.VARCHAR)
                .addValue("inclureAnnules", Boolean.TRUE.equals(f.inclureAnnules()) ? 1 : 0);
    }

    private static MapSqlParameterSource communs(PerimetreAchat perimetre, FiltresTresorerie f) {
        Integer comptabilise = f.comptabilise() == null ? null : Boolean.compare(f.comptabilise(), false);
        return RequetesDocumentsAchat.parametres(perimetre)
                .addValue("module", f.module().name())
                .addValue("dateSur", f.dateSur().name())
                .addValue("du", f.du(), Types.DATE)
                .addValue("au", f.au(), Types.DATE)
                .addValue("tiers", RequetesDocumentsAchat.texte(f.tiersId()), Types.VARCHAR)
                .addValue("comptabilise", comptabilise, Types.INTEGER)
                .addValue("montantDe", f.montantDe(), Types.DECIMAL)
                .addValue("montantA", f.montantA(), Types.DECIMAL)
                .addValue("motif", HistoriqueService.motif(f.recherche()), Types.NVARCHAR);
    }
}

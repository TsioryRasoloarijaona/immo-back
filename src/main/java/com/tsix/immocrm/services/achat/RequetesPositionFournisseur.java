package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.MouvementPosition;
import com.tsix.immocrm.dto.achat.LignePositionDto;
import com.tsix.immocrm.services.patrimoine.ReferencesPatrimoine;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.chaine;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.jour;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.texte;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.uuid;

/** Mouvements de {@code BuySupplierPositionView} recalculés sur tables — FEATURE-achat-impayes-soldes.md §3.1. */
@Component
class RequetesPositionFournisseur {

    private static final String REGLEMENT = "REGLEMENT";
    private static final String NON_RATTACHE = "REGLEMENT_NON_RATTACHE";

    // AmountToPay est déjà net des avoirs non financiers : la vue les réajoute au crédit de leur facture source.
    private static final String PIECES = "SELECT b.PKey AS Id, b.DocType AS Type, b.Code, "
            + "CONCAT_WS(' ', b.DocRef, b.Description, b.Notes) AS Libelle, b.DocDate, b.MaturityDate AS Echeance, "
            + "b.CfgCompanyId, b.CfgTierId, COALESCE(b.PrjProjectId, v.PrjProjectId) AS PrjProjectId, "
            + "CAST(NULL AS uniqueidentifier) AS AdmModuleId, (ISNULL(b.AmountTTC, 0) + ISNULL(b.StockAmountToPay, 0) "
            + "+ ISNULL(b.FluctuationsToPay, 0) + ISNULL(b.ProrataRefundAmount, 0)) * ISNULL(v.VentilationRate, 1) AS Ttc, "
            + "CASE WHEN b.DocType = 'REFUND' THEN ISNULL(b.AmountToPay, 0) ELSE ISNULL(b.AmountToPay + ISNULL((SELECT "
            + "SUM(r.AmountToPay) FROM BuyDocument r WHERE r.DocType = 'REFUND' AND r.IsFinancialRefund = 0 "
            + "AND r.InvoiceSourceId = b.PKey), 0), 0) END * ISNULL(v.VentilationRate, 1) AS Montant, "
            + "CAST(NULL AS decimal(38, 12)) AS Ras, CASE WHEN b.DocType = 'REFUND' THEN 0 ELSE 1 END AS Credit, "
            + "0 AS Reglement FROM BuyDocument b LEFT JOIN BuyVentilationExpense v ON v.BuyDocumentId = b.PKey "
            + "WHERE b.CanBePayed = 1";

    // Une part par ligne de règlement, plus la part non rattachée (base − Σ lignes) si elle n'est pas nulle.
    private static final String REGLEMENTS = "SELECT s.PKey, CASE WHEN x.Rattache = 1 THEN '" + REGLEMENT + "' ELSE '"
            + NON_RATTACHE + "' END, s.Code, CONCAT_WS(' ', s.Description, s.Notes), s.DocDate, "
            + "ISNULL(s.SettlementDate, s.EcheanceDate), s.CfgCompanyId, s.TierId, "
            + "CASE WHEN x.Rattache = 1 THEN COALESCE(pd.PrjProjectId, v.PrjProjectId) END, NULL, NULL, "
            + "x.Part * ISNULL(v.VentilationRate, 1), s.AmountRS * x.Part / s.AmountBase * ISNULL(v.VentilationRate, 1), "
            + "CASE WHEN s.IsCredit = 0 THEN 0 ELSE 1 END, 1 FROM StlSettlement s "
            + "JOIN StrEntity e ON e.PKey = s.StrEntityId JOIN CfgCompany sc ON sc.PKey = s.CfgCompanyId "
            + "CROSS APPLY (SELECT l.BuyDocumentId, l.Amount AS Part, 1 AS Rattache FROM StlSettlementLine l "
            + "WHERE l.StlSettlementId = s.PKey UNION ALL SELECT NULL, s.AmountBase - ISNULL((SELECT SUM(l2.Amount) "
            + "FROM StlSettlementLine l2 WHERE l2.StlSettlementId = s.PKey), 0), 0) x "
            + "LEFT JOIN BuyDocument pd ON pd.PKey = x.BuyDocumentId "
            + "LEFT JOIN BuyVentilationExpense v ON v.BuyDocumentId = x.BuyDocumentId "
            + "WHERE s.AmountBase <> 0 AND (x.Rattache = 1 OR x.Part <> 0)";

    private static final String MOUVEMENTS = "SELECT d.*, t.Description AS FournisseurNom, c.Code AS SocieteCode, "
            + "p.Code AS ProjetCode FROM (" + PIECES + " UNION ALL " + REGLEMENTS + ") d "
            + "JOIN CfgTier t ON t.PKey = d.CfgTierId AND t.CfgTierTypeId = :typeFournisseur "
            + "LEFT JOIN CfgCompany c ON c.PKey = d.CfgCompanyId LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId "
            + "WHERE " + RequetesDocumentsAchat.VISIBLE
            + "AND (:fournisseur IS NULL OR d.CfgTierId = :fournisseur)";

    /** Un mouvement visible, avec sa ligne de fenêtre « Ligne de solde ». */
    record Mouvement(UUID fournisseurId, String fournisseurNom, UUID societeId, String societeCode,
                     LignePositionDto ligne) {

        MouvementPosition position() {
            BigDecimal montant = ligne.reglement() == null ? ligne.aPayer() : ligne.reglement();
            return new MouvementPosition(ligne.dateDocument(), Objects.requireNonNullElse(montant, BigDecimal.ZERO),
                    ligne.credit());
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    RequetesPositionFournisseur(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mouvements des fournisseurs FOU dans le {@code perimetre} ; {@code fournisseur} non nul restreint à ce tiers. */
    List<Mouvement> mouvements(PerimetreAchat perimetre, UUID fournisseur) {
        return jdbc.query(MOUVEMENTS, RequetesDocumentsAchat.parametres(perimetre)
                .addValue("typeFournisseur", ReferencesPatrimoine.TYPE_FOURNISSEUR.toString())
                .addValue("fournisseur", texte(fournisseur), Types.VARCHAR), (rs, n) -> mouvement(rs));
    }

    private static Mouvement mouvement(ResultSet rs) throws SQLException {
        boolean reglement = rs.getInt("Reglement") == 1;
        BigDecimal montant = rs.getBigDecimal("Montant");
        BigDecimal ttc = rs.getBigDecimal("Ttc");
        LignePositionDto ligne = new LignePositionDto(uuid(rs, "Id"), chaine(rs, "Type"), chaine(rs, "Code"),
                chaine(rs, "Libelle"), jour(rs, "DocDate"), jour(rs, "Echeance"), uuid(rs, "PrjProjectId"),
                chaine(rs, "ProjetCode"), ttc, reglement || montant == null || ttc == null ? null : montant.subtract(ttc),
                reglement ? null : montant, reglement ? montant : null, reglement ? rs.getBigDecimal("Ras") : null,
                reglement ? montant : null, rs.getInt("Credit") == 1);
        return new Mouvement(uuid(rs, "CfgTierId"), chaine(rs, "FournisseurNom"), uuid(rs, "CfgCompanyId"),
                chaine(rs, "SocieteCode"), ligne);
    }
}

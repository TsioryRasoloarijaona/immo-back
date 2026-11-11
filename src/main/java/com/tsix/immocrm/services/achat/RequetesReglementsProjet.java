package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.projet.FiltresReglementsProjet;
import com.tsix.immocrm.dto.projet.ReglementProjetDto;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.List;

import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.chaine;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.jour;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.texte;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.uuid;

/** SQL natif des affectations de règlement sur les factures d'un projet (FEATURE-achat-depenses-projet.md §4.2). */
@Component
class RequetesReglementsProjet {

    // Seul sens retenu : PrjWorkingOut.BuyDocumentId (BuyDocument.PrjWorkingOutId = avances) ; TOP 1 = jamais de doublon.
    static final String DECOMPTE = "OUTER APPLY (SELECT TOP 1 wo.PKey AS DecompteId, wo.Code AS DecompteCode, "
            + "wo.Sorting AS DecompteNumero FROM PrjWorkingOut wo WHERE wo.BuyDocumentId = d.PKey "
            + "ORDER BY wo.DocDate, wo.Code) dc ";

    // Visibilité par ligne : le projet de la facture réglée, jamais le règlement entier (§3.1).
    private static final String LIGNES = "SELECT l.PKey AS LigneId, s.PKey AS ReglementId, s.Code AS ReglementCode, "
            + "s.DocDate, s.EcheanceDate, ty.Code AS TypeCode, ty.Description AS TypeDescription, l.Amount, "
            + "CASE WHEN " + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX + " THEN 0 ELSE 1 END AS Annule, "
            + "d.PKey AS FactureId, d.Code AS FactureCode, t.Description AS FournisseurNom, d.PrjProjectId, "
            + "p.Code AS ProjetCode, d.PrjMarketId, m.Code AS LotCode, dc.DecompteId, dc.DecompteCode "
            + "FROM StlSettlementLine l JOIN StlSettlement s ON s.PKey = l.StlSettlementId "
            + "JOIN BuyDocument d ON d.PKey = l.BuyDocumentId "
            + "LEFT JOIN StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId "
            + "LEFT JOIN CfgTier t ON t.PKey = d.CfgTierId LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId "
            + "LEFT JOIN PrjMarket m ON m.PKey = d.PrjMarketId " + DECOMPTE
            + "WHERE d.DocType = 'INVOICE' AND " + RequetesDocumentsAchat.VISIBLE
            // Une facture d'un projet visible peut porter le lot d'un autre projet : le lot filtré doit être visible aussi.
            + "AND (:lot IS NULL OR (d.PrjMarketId = :lot AND m.PrjProjectId IN (:projets))) "
            + "AND (:facture IS NULL OR d.PKey = :facture) "
            + "AND (:du IS NULL OR CAST(s.DocDate AS date) >= :du) AND (:au IS NULL OR CAST(s.DocDate AS date) <= :au) "
            + "AND (:annules = 1 OR " + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX + ") "
            + "ORDER BY s.DocDate DESC, s.Code DESC, l.PKey";

    private final NamedParameterJdbcTemplate jdbc;

    RequetesReglementsProjet(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lignes des factures du {@code perimetre} retenues par les filtres, tri fixe. */
    List<ReglementProjetDto> lignes(PerimetreAchat perimetre, FiltresReglementsProjet f) {
        MapSqlParameterSource p = RequetesDocumentsAchat.parametres(perimetre)
                .addValue("lot", texte(f.lotId()), Types.VARCHAR)
                .addValue("facture", texte(f.factureId()), Types.VARCHAR)
                .addValue("du", f.du(), Types.DATE)
                .addValue("au", f.au(), Types.DATE)
                .addValue("annules", Boolean.TRUE.equals(f.inclureAnnules()) ? 1 : 0);
        return jdbc.query(LIGNES, p, (rs, n) -> new ReglementProjetDto(uuid(rs, "LigneId"), uuid(rs, "ReglementId"),
                chaine(rs, "ReglementCode"), jour(rs, "DocDate"), jour(rs, "EcheanceDate"), chaine(rs, "TypeCode"),
                chaine(rs, "TypeDescription"), rs.getBigDecimal("Amount"), rs.getInt("Annule") == 1,
                uuid(rs, "FactureId"), chaine(rs, "FactureCode"), chaine(rs, "FournisseurNom"),
                uuid(rs, "PrjProjectId"), chaine(rs, "ProjetCode"), uuid(rs, "PrjMarketId"), chaine(rs, "LotCode"),
                uuid(rs, "DecompteId"), chaine(rs, "DecompteCode")));
    }
}

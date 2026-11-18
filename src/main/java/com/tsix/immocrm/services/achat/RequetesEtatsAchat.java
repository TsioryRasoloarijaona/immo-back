package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.MontantTvaLigne;
import com.tsix.immocrm.domain.achat.PieceAnnulee;
import com.tsix.immocrm.domain.achat.SuiviAchat;
import com.tsix.immocrm.dto.achat.EtatTvaDeductibleDto.LigneTvaDeductibleDto;
import com.tsix.immocrm.dto.achat.FiltresEtatsAchat;
import com.tsix.immocrm.dto.achat.PieceSuiviAchatDto;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.chaine;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.jour;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.uuid;

/** SQL natif des états Achat (FEATURE-n7-etats-achat.md) : pièces visibles ({@code VISIBLE}) datées de la période. */
@Component
class RequetesEtatsAchat {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final String DESCRIPTION = "Description";

    private static final String PIECE = "SELECT d.PKey, d.Code, d.DocDate, d.Description, bs.Description AS Statut, "
            + "CASE WHEN " + PieceAnnulee.SQL + " THEN 1 ELSE 0 END AS Annulee, t.Description AS Fournisseur, "
            + "p.Description AS Projet, c.Description AS Societe, ";

    private static final String JOINTURES = " FROM BuyDocument d LEFT JOIN CfgTier t ON t.PKey = d.CfgTierId "
            + "LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId LEFT JOIN PrjMarket m ON m.PKey = d.PrjMarketId "
            + "LEFT JOIN CfgCompany c ON c.PKey = d.CfgCompanyId LEFT JOIN StrEntity e ON e.PKey = d.StrEntityId "
            + "LEFT JOIN BuyDocumentStatus bs ON bs.PKey = d.BuyDocumentStatusId ";

    private static final String FILTRES = "WHERE " + RequetesDocumentsAchat.VISIBLE
            + "AND d.DocDate >= :du AND d.DocDate < :lendemain "
            + "AND (:tiers IS NULL OR t.Description LIKE :tiers) "
            + "AND (:projet IS NULL OR p.Code LIKE :projet OR p.Description LIKE :projet) "
            + "AND (:lot IS NULL OR m.Code LIKE :lot OR m.Description LIKE :lot) ";

    private static final String TVA = PIECE + "l.PKey AS LigneId, l.Description AS LigneDescription, l.Quantity, "
            + "l.UnitPriceHT, l.DiscountRatio, l.VatRatio, d.AmountHT, d.AmountVat, bs.IsValidatedOnAccounting"
            + JOINTURES + "LEFT JOIN BuyDocumentLine l ON l.BuyDocumentId = d.PKey " + FILTRES
            + "AND d.DocType = 'INVOICE' ORDER BY d.DocDate ASC, d.Code ASC, d.PKey ASC, l.Sorting ASC";

    /** Ligne de TVA et drapeau « statut validé en comptabilité » (R4). */
    record LigneTva(LigneTvaDeductibleDto ligne, boolean validee) {
    }

    /** Ligne de suivi : colonnes de la pièce et trois montants ; publique, l'export la lit par réflexion. */
    public record LigneSuivi(PieceSuiviAchatDto piece, SuiviAchat.Montants montants) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    RequetesEtatsAchat(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<LigneTva> tva(PerimetreAchat perimetre, FiltresEtatsAchat f) {
        return jdbc.query(TVA, parametres(perimetre, f), (rs, n) -> ligneTva(rs));
    }

    List<LigneSuivi> suivi(SuiviAchat suivi, PerimetreAchat perimetre, FiltresEtatsAchat f) {
        String sql = PIECE + "e.Description AS Nature, m.Description AS Lot, d.PrjMarketId, "
                + "ISNULL(d.AmountToPay, 0) AS APayer, ISNULL(d.AmountRegul, 0) AS Regul, "
                + suivi.montantSigne() + " AS Montant, CASE WHEN " + suivi.condition() + " THEN 1 ELSE 0 END AS BlocA, "
                + "CASE WHEN d.StrEntityId IN (:natures) THEN 1 ELSE 0 END AS BlocB" + JOINTURES + FILTRES
                + "AND (" + suivi.condition() + " OR d.StrEntityId IN (:natures)) "
                + "ORDER BY d.DocDate ASC, d.Code ASC, d.PKey ASC";
        MapSqlParameterSource p = parametres(perimetre, f)
                .addValue("natures", RequetesDocumentsAchat.textes(suivi.natures()));
        return jdbc.query(sql, p, (rs, n) -> lignesSuivi(suivi, rs)).stream().flatMap(List::stream).toList();
    }

    private static MapSqlParameterSource parametres(PerimetreAchat perimetre, FiltresEtatsAchat f) {
        return RequetesDocumentsAchat.parametres(perimetre)
                .addValue("du", f.du(), Types.DATE)
                .addValue("lendemain", f.au().plusDays(1), Types.DATE)
                .addValue("tiers", motif(f.tiers()), Types.NVARCHAR)
                .addValue("projet", motif(f.projet()), Types.NVARCHAR)
                .addValue("lot", motif(f.lot()), Types.NVARCHAR);
    }

    /** Recherche « contient » comme OMRAN ; vide = pas de filtre. */
    private static String motif(String texte) {
        return texte == null || texte.isBlank() ? null : "%" + texte.trim() + "%";
    }

    private static LigneTva ligneTva(ResultSet rs) throws SQLException {
        boolean sansLignes = rs.getString("LigneId") == null;
        BigDecimal quantite = rs.getBigDecimal("Quantity");
        BigDecimal prix = rs.getBigDecimal("UnitPriceHT");
        BigDecimal remise = rs.getBigDecimal("DiscountRatio");
        MontantTvaLigne montants = sansLignes
                ? MontantTvaLigne.deEnTete(rs.getBigDecimal("AmountHT"), rs.getBigDecimal("AmountVat"))
                : MontantTvaLigne.deLigne(quantite, prix, remise, rs.getBigDecimal("VatRatio"));
        LigneTvaDeductibleDto ligne = new LigneTvaDeductibleDto(uuid(rs, "PKey"), chaine(rs, "Code"),
                jour(rs, "DocDate"), chaine(rs, "Statut"), annulee(rs), chaine(rs, "Fournisseur"), chaine(rs, "Projet"),
                chaine(rs, "Societe"), chaine(rs, sansLignes ? DESCRIPTION : "LigneDescription"), quantite, prix,
                remise == null ? null : remise.multiply(CENT), montants.montantHT(), montants.tauxTvaPct(),
                montants.montantTva(), sansLignes);
        return new LigneTva(ligne, rs.getBoolean("IsValidatedOnAccounting"));
    }

    private static List<LigneSuivi> lignesSuivi(SuiviAchat suivi, ResultSet rs) throws SQLException {
        PieceSuiviAchatDto piece = new PieceSuiviAchatDto(uuid(rs, "PKey"), jour(rs, "DocDate"), chaine(rs, "Code"),
                chaine(rs, DESCRIPTION), chaine(rs, "Nature"), chaine(rs, "Statut"), annulee(rs),
                chaine(rs, "Fournisseur"), chaine(rs, "Projet"), chaine(rs, "Societe"), chaine(rs, "Lot"),
                uuid(rs, "PrjMarketId"));
        return suivi.lignes(rs.getInt("BlocA") == 1, rs.getBigDecimal("Montant"), rs.getInt("BlocB") == 1,
                        rs.getBigDecimal("APayer"), rs.getBigDecimal("Regul"))
                .stream().map(m -> new LigneSuivi(piece, m)).toList();
    }

    private static boolean annulee(ResultSet rs) throws SQLException {
        return rs.getInt("Annulee") == 1;
    }
}

package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Lignes sources de {@link FluxVenteMensuel}, une requête par famille, bornée aux tranches demandées (lots de 2 000).
 * Jointures de la vue {@code ComSalePrevision} : tranche en vente, projet et société internes.
 */
@Component
class LecturesFlux {

    private static final String TRANCHES = "tranches";
    private static final String COL_TRANCHE = "Tranche";
    private static final String COL_LIGNE = "Ligne";

    private static final String TRANCHE_EN_VENTE = """
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId AND t.IsForSale = 1
            JOIN CfgProject p ON p.PKey = t.CfgProjectId
            JOIN CfgCompany c ON c.PKey = p.CfgCompanyId
            """;

    private static final String ETAT = "st.PKey AS Statut, st.Sorting, st.BoolField002 AS Annule, st.BoolField003 AS Desiste";

    private static final String ORIGINE = " f.PKey AS Dossier, f.CfgCommercialId AS Commercial, "
            + "i.StkItemCategoryId AS CategorieArticle, i.IsUnderItem AS SousArticle, ";

    // Statut en jointure externe : un dossier sans statut est lu pour être compté en anomalie, jamais dans une mesure.
    private static final String DOSSIERS = "SELECT" + ORIGINE + "i.StkHierarchyId AS Tranche, " + ETAT + """
            , f.DocDate, f.AdvanceDate, f.AgreementSavingDate, f.DeliveryDateReal, f.CancelDate, f.Price, f.PriceHT,
              (SELECT SUM(e.Amount) FROM ComDocument e JOIN ComSettlementCategory q ON q.PKey = e.ComSettlementCategoryId
               WHERE e.ComFolderId = f.PKey AND q.IsSaleResiliation = 1 AND q.IsCredit = 0) AS DuResiliation
            FROM ComFolder f
            JOIN StkVocation v ON v.PKey = f.StkVocationId AND v.Code = 'VENTE'
            JOIN StkItem i ON i.PKey = f.StkItemId
            """ + TRANCHE_EN_VENTE + """
            LEFT JOIN StrStatus st ON st.PKey = f.StrStatusId
            WHERE i.StkHierarchyId IN (:tranches)""";

    private static final String REGLEMENTS = "SELECT l.PKey AS Ligne," + ORIGINE + "i.StkHierarchyId AS Tranche, "
            + RegleEncaissement.MOUVEMENT
            + " AS Mouvement, " + RegleEncaissement.MONTANT + " AS Montant, k.PKey AS Categorie, CASE WHEN"
            + RegleEncaissement.ENCAISSEMENT + "THEN 1 ELSE 0 END AS Encaissement, CASE WHEN" + RegleEncaissement.FRAIS
            + "THEN 1 ELSE 0 END AS Frais, "
            + "CASE WHEN k.IsSaleAdvance = 1 AND k.IsInAgrementContext = 1 THEN 1 ELSE 0 END AS AvanceAccord, " + ETAT
            + ", f.AdvanceDate, f.AgreementSavingDate " + RegleEncaissement.LIGNES + TRANCHE_EN_VENTE
            + "WHERE i.StkHierarchyId IN (:tranches) AND" + RegleEncaissement.COMPTE;

    private static final String ECHEANCES = "SELECT d.PKey AS Ligne," + ORIGINE + """
            i.StkHierarchyId AS Tranche, d.DocDate, d.Amount
            FROM ComDocument d
            JOIN ComSettlementCategory k ON k.PKey = d.ComSettlementCategoryId
            JOIN ComFolder f ON f.PKey = d.ComFolderId
            JOIN StrStatus st ON st.PKey = f.StrStatusId
            JOIN StkItem i ON i.PKey = f.StkItemId
            """ + TRANCHE_EN_VENTE + "WHERE" + RegleEncaissement.FRAIS
            + "AND d.ComSettlementCategoryId = :categorie AND i.StkHierarchyId IN (:tranches)";

    private static final String REMISES = """
            SELECT SUM(f.DiscountAmount)
            FROM ComFolder f
            JOIN StkVocation v ON v.PKey = f.StkVocationId AND v.Code = 'VENTE'
            JOIN StkItem i ON i.PKey = f.StkItemId AND i.ParentId IS NULL
            JOIN StrStatus st ON st.PKey = f.StrStatusId AND st.BoolField002 = 0 AND st.BoolField003 = 0
            """ + TRANCHE_EN_VENTE + "WHERE i.StkHierarchyId IN (:tranches)";

    private final NamedParameterJdbcTemplate jdbc;

    LecturesFlux(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Statut du dossier ; {@code avecStatut} faux = dossier sans statut (absent de toutes les mesures). */
    record Etat(boolean avecStatut, Integer sorting, Boolean annule, Boolean desiste) {

        boolean atteint(Integer seuil) {
            return avecStatut && seuil != null && sorting != null && sorting >= seuil;
        }

        /** Ni annulé ni désisté, au sens de la vue ({@code BoolField = 0} : NULL n'y est pas). */
        boolean vivant() {
            return Boolean.FALSE.equals(annule) && Boolean.FALSE.equals(desiste);
        }
    }

    /** Dossier d'une ligne source et ce que {@link MouvementFlux} en reprend. */
    record Origine(UUID dossier, UUID commercial, UUID categorieArticle, boolean sousArticle) {
    }

    record Dossier(Origine origine, UUID tranche, Etat etat, LocalDateTime saisie, LocalDateTime avance,
                   LocalDateTime acte, LocalDateTime livraison, LocalDateTime sortie, BigDecimal prix, BigDecimal prixHt,
                   BigDecimal duResiliation) {

        UUID id() {
            return origine.dossier();
        }
    }

    record Ligne(UUID id, Origine origine, UUID tranche, LocalDateTime mouvement, BigDecimal montant, UUID categorie,
                 boolean encaissement, boolean frais, boolean avanceAccord, Etat etat, LocalDateTime avance,
                 LocalDateTime acte) {
    }

    record Echeance(UUID id, Origine origine, UUID tranche, LocalDateTime date, BigDecimal montant) {
    }

    List<Dossier> dossiers(List<UUID> tranches) {
        return LotsTranches.lire(tranches, lot -> jdbc.query(DOSSIERS, Map.of(TRANCHES, LotsTranches.textes(lot)),
                (rs, n) -> new Dossier(origine(rs), uuid(rs, COL_TRANCHE), etat(rs), date(rs, "DocDate"),
                        date(rs, "AdvanceDate"), date(rs, "AgreementSavingDate"), date(rs, "DeliveryDateReal"),
                        date(rs, "CancelDate"), rs.getBigDecimal("Price"), rs.getBigDecimal("PriceHT"),
                        rs.getBigDecimal("DuResiliation"))));
    }

    List<Ligne> reglements(List<UUID> tranches) {
        return LotsTranches.lire(tranches, lot -> jdbc.query(REGLEMENTS, Map.of(TRANCHES, LotsTranches.textes(lot)),
                (rs, n) -> new Ligne(uuid(rs, COL_LIGNE), origine(rs), uuid(rs, COL_TRANCHE), date(rs, "Mouvement"),
                        rs.getBigDecimal("Montant"), uuid(rs, "Categorie"), rs.getInt("Encaissement") == 1,
                        rs.getInt("Frais") == 1, rs.getInt("AvanceAccord") == 1, etat(rs), date(rs, "AdvanceDate"),
                        date(rs, "AgreementSavingDate"))));
    }

    List<Echeance> echeances(List<UUID> tranches, UUID categorie) {
        return LotsTranches.lire(tranches, lot -> jdbc.query(ECHEANCES,
                Map.of(TRANCHES, LotsTranches.textes(lot), "categorie", categorie.toString()),
                (rs, n) -> new Echeance(uuid(rs, COL_LIGNE), origine(rs), uuid(rs, COL_TRANCHE), date(rs, "DocDate"),
                        rs.getBigDecimal("Amount"))));
    }

    private static Origine origine(ResultSet rs) throws SQLException {
        return new Origine(uuid(rs, "Dossier"), uuid(rs, "Commercial"), uuid(rs, "CategorieArticle"),
                rs.getBoolean("SousArticle"));
    }

    BigDecimal remises(List<UUID> tranches) {
        return LotsTranches.lire(tranches, lot -> jdbc.query(REMISES, Map.of(TRANCHES, LotsTranches.textes(lot)),
                        (rs, n) -> rs.getBigDecimal(1))).stream()
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Etat etat(ResultSet rs) throws SQLException {
        int sorting = rs.getInt("Sorting");
        Integer ordre = rs.wasNull() ? null : sorting;
        return new Etat(rs.getString("Statut") != null, ordre, booleen(rs, "Annule"), booleen(rs, "Desiste"));
    }

    private static Boolean booleen(ResultSet rs, String colonne) throws SQLException {
        boolean valeur = rs.getBoolean(colonne);
        return rs.wasNull() ? null : valeur;
    }

    private static UUID uuid(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null ? null : UUID.fromString(valeur);
    }

    private static LocalDateTime date(ResultSet rs, String colonne) throws SQLException {
        Timestamp valeur = rs.getTimestamp(colonne);
        return valeur == null ? null : valeur.toLocalDateTime();
    }
}

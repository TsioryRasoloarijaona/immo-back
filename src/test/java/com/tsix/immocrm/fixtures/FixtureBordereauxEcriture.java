package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot L3b-1 (FEATURE-lot-ecritures.md) : bordereaux estimatif et définitif avec catégorie, lignes décomptées, statut « avec
 * bordereau estimatif », ventilation par catégorie. SQL natif, parents avant enfants (FK de la CI) ; annulé avec la transaction.
 */
@Component
public class FixtureBordereauxEcriture {

    public static final String LIGNES_ESTIMATIF = "PrjMarketLineEstimation";
    public static final String LIGNES_DEFINITIF = "PrjMarketLine";
    public static final String GROUPES_ESTIMATIF = "PrjMarketLineGroupingEstimation";
    public static final String GROUPES_DEFINITIF = "PrjMarketLineGrouping";

    // Absente de yesRemsTest (définition de yesRemsProd) ; le lot l'ajoute en CI.
    public static final String TABLE_VENTILATION = """
            IF OBJECT_ID('dbo.PrjMarketVentilationCategory', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketVentilationCategory (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                    StlCategoryId uniqueidentifier NULL, PrjMarketId uniqueidentifier NULL, VentilationRate decimal(26, 6) NULL,
                    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
                    UserUpdateDateTime datetime NULL)""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureProjetLots lots;

    public FixtureBordereauxEcriture(EntityManager em, JdbcTemplate jdbc, FixtureProjetLots lots) {
        this.em = em;
        this.jdbc = jdbc;
        this.lots = lots;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    public void tableVentilation() {
        maj(TABLE_VENTILATION);
    }

    /** {@code StrStatus.BoolField004 = 1} : « Avec bordereau estimatif ». */
    public void ouvrirEstimatif(UUID statut) {
        maj("UPDATE StrStatus SET BoolField004 = 1 WHERE PKey = ?", s(statut));
    }

    /** Catégorie de dépense ({@code StlCategory}). */
    public UUID categorie(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlCategory (PKey, Code, Description, IsCredit) VALUES (?, ?, ?, 0)", s(id), code, "Catégorie " + code);
        return id;
    }

    /** TVA par défaut (ratio) et catégorie du lot ; nuls acceptés. */
    public void defauts(UUID lot, String tvaRatio, UUID categorie) {
        maj("UPDATE PrjMarket SET DefaultVat = ?, StlCategoryId = ? WHERE PKey = ?",
                tvaRatio == null ? null : new BigDecimal(tvaRatio), s(categorie), s(lot));
    }

    /** {@code PrjMarket.CfgSupplierId} (un tiers existant). */
    public void fournisseur(UUID lot, UUID tiers) {
        maj("UPDATE PrjMarket SET CfgSupplierId = ? WHERE PKey = ?", s(tiers), s(lot));
    }

    /** Groupe de bordereau ; {@code table} = l'une des deux constantes de groupes. */
    public UUID groupe(String table, UUID lot, UUID parent, int ordre, String code, boolean stock) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO " + table + " (PKey, Code, Description, Sorting, ParentId, PrjMarketId, IsStock) VALUES (?, ?, ?, ?, ?, ?, ?)",
                s(id), code, "Groupe " + code, ordre, s(parent), s(lot), stock);
        return id;
    }

    /** Ligne de bordereau : TVA 20 %, FODEC 0, PU TTC = PU HT × 1,2, notes « Note code ». */
    public record Ligne(UUID lot, UUID groupe, int ordre, String code, String quantite, String puHT, UUID categorie,
                        boolean stock) {
    }

    /** {@code table} = l'une des deux constantes de lignes. */
    public UUID ligne(String table, Ligne l) {
        UUID id = UUID.randomUUID();
        BigDecimal pu = new BigDecimal(l.puHT());
        maj("INSERT INTO " + table + " (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC, "
                        + "FODECRatio, IsStock, PrjMarketId, PrjMarketLineGroupingId, StlCategoryId, Notes) "
                        + "VALUES (?, ?, ?, ?, N'U', ?, ?, 0.2, ?, 0, ?, ?, ?, ?, ?)",
                s(id), l.code(), "Ligne " + l.code(), l.ordre(), new BigDecimal(l.quantite()), pu,
                pu.multiply(new BigDecimal("1.2")), l.stock(), s(l.lot()), s(l.groupe()), s(l.categorie()), "Note " + l.code());
        return id;
    }

    /** FODEC (ratio) d'une ligne. */
    public void fodec(String table, UUID ligne, String ratio) {
        maj("UPDATE " + table + " SET FODECRatio = ? WHERE PKey = ?", new BigDecimal(ratio), s(ligne));
    }

    /** Ligne définitive décomptée : un décompte du lot et une {@code PrjWorkingOutLine} qui la cite (qté 1, PU 100). */
    public UUID decompter(UUID lot, UUID ligneDefinitive, String code) {
        UUID decompte = lots.decompte(lot, code, 1, Map.of());
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjWorkingOutLine (Pkey, Sorting, Code, Quantity, UnitPriceHT, PrjWorkingOutId, PrjMarketLineId)
                VALUES (?, 1, ?, 1, 100, ?, ?)""", s(id), code, s(decompte), s(ligneDefinitive));
        return id;
    }

    /** Ventilation préexistante du lot. */
    public void ventilation(UUID lot, UUID categorie, String taux) {
        tableVentilation();
        maj("INSERT INTO PrjMarketVentilationCategory (PKey, StlCategoryId, PrjMarketId, VentilationRate) VALUES (?, ?, ?, ?)",
                s(UUID.randomUUID()), s(categorie), s(lot), new BigDecimal(taux));
    }

    // ------------------------------------------------------------------ lecture (session écrite avant)

    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        return jdbc.queryForList(sql, parametres);
    }

    /** Ligne {@code PKey = id} de {@code table}, ou vide. */
    public List<Map<String, Object>> parId(String table, UUID id) {
        return lignes("SELECT * FROM " + table + " WHERE PKey = ?", s(id));
    }

    /** Ventilations du lot : catégorie (texte majuscule de SQL Server) → taux. */
    public List<Map<String, Object>> ventilations(UUID lot) {
        return lignes("""
                SELECT CAST(StlCategoryId AS varchar(36)) AS categorie, VentilationRate AS taux, UserCreate, UserCreateDateTime
                FROM PrjMarketVentilationCategory WHERE PrjMarketId = ? ORDER BY VentilationRate DESC""", s(lot));
    }
}

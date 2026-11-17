package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot L3a-ter (FEATURE-lot-previsions-reel.md §M1-M7) : modèles de simulation {@code PrjMarketPrevisionModel(Line)},
 * catégories de dépense, prévision de faisabilité qui cite un modèle. SQL natif lié, parents avant enfants (FK de la CI).
 */
@Component
public class FixtureModelesSimulation {

    /** Droit OMRAN de l'écran de configuration des modèles (6 rôles en prod). */
    public static final String DROIT_MODELES = "Yes.Prj.Front.Config.MarketPrevisionModelList";

    // Absente de yesRemsTest et de ci/schema (définition yesRemsProd, FK CASCADE vers le modèle) : DDL validé à part,
    // avant toute écriture du test (sinon verrou sur PrjMarketPrevisionModel tenu par la transaction du test).
    private static final String TABLE_LIGNES = """
            IF OBJECT_ID('dbo.PrjMarketPrevisionModelLine', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketPrevisionModelLine (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                    PointTime decimal(26, 6) NULL, PointAchivement decimal(26, 6) NULL, PointPayment decimal(26, 6) NULL,
                    PrjMarketPrevisionModelId uniqueidentifier NULL
                        CONSTRAINT PrjMarketPrevisionModelLine__PrjMarketPrevisionModel_FK1
                        REFERENCES dbo.PrjMarketPrevisionModel (PKey) ON DELETE CASCADE)""";

    /** Ligne de modèle : fractions 0-1 ({@code PointTime}, {@code PointAchivement}, {@code PointPayment}). */
    public record Ligne(String temps, String avancement, String paiement) {
    }

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public FixtureModelesSimulation(EntityManager em, JdbcTemplate jdbc, DataSource dataSource) {
        this.em = em;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    /** Table des lignes créée si absente, sur une connexion autonome (durable : sert aussi aux tests validés). */
    public void tables() {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(TABLE_LIGNES);
        } catch (SQLException e) {
            throw new IllegalStateException("Création de PrjMarketPrevisionModelLine impossible", e);
        }
    }

    /** Catégorie de dépense ({@code StlCategory}, code = libellé : le libellé exposé est l'un ou l'autre). */
    public UUID categorie(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlCategory (PKey, Code, Description, IsCredit) VALUES (?, ?, ?, 0)", s(id), code, code);
        return id;
    }

    /** Modèle et ses lignes, écrits en base comme OMRAN les aurait laissés. */
    public UUID modele(String code, String description, UUID categorie, Ligne... lignes) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketPrevisionModel (PKey, Code, Description, StlCategoryId) VALUES (?, ?, ?, ?)",
                s(id), code, description, s(categorie));
        for (Ligne l : lignes) {
            maj("""
                    INSERT INTO PrjMarketPrevisionModelLine (PKey, PointTime, PointAchivement, PointPayment, PrjMarketPrevisionModelId)
                    VALUES (?, ?, ?, ?, ?)""", s(UUID.randomUUID()), new BigDecimal(l.temps()), new BigDecimal(l.avancement()),
                    new BigDecimal(l.paiement()), s(id));
        }
        return id;
    }

    /** Prévision de faisabilité ({@code FSPrevision}) qui cite le modèle ; aucune autre colonne renseignée. */
    public UUID previsionFaisabilite(UUID modele) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO FSPrevision (PKey, PrjMarketPrevisionModelId) VALUES (?, ?)", s(id), s(modele));
        return id;
    }

    /** En-tête du modèle relu en base ; vide si supprimé. */
    public List<Map<String, Object>> entete(UUID modele) {
        return lignesSql("""
                SELECT Code, Description, CAST(StlCategoryId AS varchar(36)) AS categorie
                FROM PrjMarketPrevisionModel WHERE PKey = ?""", s(modele));
    }

    /** Lignes du modèle relues en base, triées par temps. */
    public List<Map<String, Object>> lignes(UUID modele) {
        return lignesSql("""
                SELECT PointTime, PointAchivement, PointPayment FROM PrjMarketPrevisionModelLine
                WHERE PrjMarketPrevisionModelId = ? ORDER BY PointTime ASC""", s(modele));
    }

    public int modelesDeCode(String code) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PrjMarketPrevisionModel WHERE Code = ?", Integer.class, code);
        return n == null ? 0 : n;
    }

    private List<Map<String, Object>> lignesSql(String sql, Object... parametres) {
        em.flush();
        em.clear();
        return jdbc.queryForList(sql, parametres);
    }
}

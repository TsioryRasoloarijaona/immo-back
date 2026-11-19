package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot N9a (FEATURE-n9-planification.md) : types de tâche, nivellement de projet et tables qui le citent. Tables et colonnes
 * de yesRemsProd absentes de yesRemsTest (et de la CI avant 199) créées sur une connexion autocommit, à leur définition de
 * prod ; lignes en SQL natif dans la transaction du test, parents avant enfants (FK de la CI).
 */
@Component
public class FixtureReferentielsPlanification {

    // Slugs du moteur /api/referentiels-admin (FEATURE-n9 « API ») et droits OMRAN (D5)
    public static final String TYPES_RECEPTION = "types-reception";
    public static final String NATURES_RECEPTION = "natures-reception";
    // « types-tache » est déjà le slug des types de tâche d'Administration (AdmTaskType) : slug propre au Gantt.
    public static final String TYPES_TACHE = "types-tache-planification";
    public static final String DROIT_TYPES_RECEPTION = "Yes.Prj.Front.Config.PrjMarketReceptionTypeList";
    public static final String DROIT_NATURES_RECEPTION = "Yes.Prj.Front.Config.PrjMarketReceptionKindList";
    public static final String DROIT_TYPES_TACHE = "Yes.Cfg.Front.Gantt.GanttTaskType";

    private static final String AUDIT = "UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, "
            + "UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL";

    private static final String TABLES = """
            IF OBJECT_ID(N'dbo.CfgGanttTaskType', N'U') IS NULL
                CREATE TABLE dbo.CfgGanttTaskType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, Sorting int NULL, %1$s);
            IF OBJECT_ID(N'dbo.CfgGanttTask', N'U') IS NULL
                CREATE TABLE dbo.CfgGanttTask (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, Notes nvarchar(1000) NULL, Sorting int NULL, Depth int NULL,
                    StartDate datetime NULL, Duration int NULL, PercentComplete decimal(26, 6) NULL, AlertBeforeDays int NULL,
                    ConstraintType int NULL, ConstraintDate datetime NULL, Deadline datetime NULL,
                    Predecessors nvarchar(4000) NULL, ResourceNames nvarchar(4000) NULL, IsMilestone bit NULL,
                    ParentId uniqueidentifier NULL, CfgGanttProjectId uniqueidentifier NULL,
                    CfgGanttTaskTypeId uniqueidentifier NULL, PrjMarketId uniqueidentifier NULL, %1$s,
                    StlCategoryId uniqueidentifier NULL, StrEntityLinkedId uniqueidentifier NULL,
                    StrWorkFlowStartId uniqueidentifier NULL, StrWorkFlowEndId uniqueidentifier NULL);
            IF OBJECT_ID(N'dbo.StkLevelingLine', N'U') IS NULL
                CREATE TABLE dbo.StkLevelingLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Sorting int NULL, StkLevelId uniqueidentifier NULL, %1$s);
            IF OBJECT_ID(N'dbo.PrjMarketHierarchy', N'U') IS NULL
                CREATE TABLE dbo.PrjMarketHierarchy (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                    PrjMarketId uniqueidentifier NOT NULL, StkHierarchyId uniqueidentifier NOT NULL, %1$s);
            IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'ParentId') IS NULL
                ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD ParentId uniqueidentifier NULL;
            IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'Code') IS NULL
                ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD Code nvarchar(255) NULL;
            IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'Sorting') IS NULL
                ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD Sorting int NULL;
            IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'UserCreate') IS NULL
                ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD %1$s;
            IF COL_LENGTH(N'dbo.GrhWorkedDay', N'CfgTrancheLevelingDefinitionId') IS NULL
                ALTER TABLE dbo.GrhWorkedDay ADD CfgTrancheLevelingDefinitionId uniqueidentifier NULL;
            """.formatted(AUDIT);

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public FixtureReferentielsPlanification(EntityManager em, JdbcTemplate jdbc, DataSource dataSource) {
        this.em = em;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    /** Tables et colonnes du lot, créées sur une connexion autocommit si absentes (idempotent). */
    public void tables() {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(TABLES);
        } catch (SQLException e) {
            throw new IllegalStateException("Création des tables N9a", e);
        }
    }

    // ------------------------------------------------------------------ type de tâche

    public UUID typeTache(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgGanttTaskType (PKey, Sorting, Code, Description) VALUES (?, 1, ?, ?)", s(id), code, "Type " + code);
        return id;
    }

    /** Tâche hors planification ({@code CfgGanttProjectId} NULL) qui cite ce type. */
    public UUID tacheDeType(UUID type, String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgGanttTask (PKey, Code, Description, CfgGanttTaskTypeId) VALUES (?, ?, ?, ?)",
                s(id), code, "Tâche " + code, s(type));
        return id;
    }

    // ------------------------------------------------------------------ nivellement

    /** {@code StkLeveling} et ses lignes : {@code codesDeNiveau[i]} au rang (Sorting) i + 1. */
    public UUID nivellement(String code, String... codesDeNiveau) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StkLeveling (PKey, Code) VALUES (?, ?)", s(id), code);
        for (int i = 0; i < codesDeNiveau.length; i++) {
            maj("INSERT INTO StkLevelingLine (PKey, Code, Sorting, StkLevelId) VALUES (?, ?, ?, ?)",
                    s(UUID.randomUUID()), codesDeNiveau[i], i + 1, s(id));
        }
        return id;
    }

    public void nivellementDuProjet(UUID projet, UUID nivellement) {
        maj("UPDATE PrjProject SET StkLevelingId = ? WHERE PKey = ?", s(nivellement), s(projet));
    }

    /** Nœud de la définition de nivellement du projet ; {@code parent} nul = racine. */
    public UUID noeud(UUID projet, UUID parent, String code, int ordre) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgTrancheLevelingDefinition (PKey, ParentId, Code, Sorting, CfgTrancheId) VALUES (?, ?, ?, ?, ?)",
                s(id), s(parent), code, ordre, s(projet));
        return id;
    }

    /** {@code PrjMarketHierarchy.StkHierarchyId} = nœud (FK de prod vers la définition de nivellement). */
    public UUID hierarchieDeLot(UUID lot, UUID noeud) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketHierarchy (PKey, PrjMarketId, StkHierarchyId) VALUES (?, ?, ?)", s(id), s(lot), s(noeud));
        return id;
    }

    public UUID journeeTravaillee(UUID projet, UUID noeud) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO GrhWorkedDay (Pkey, PrjProjectId, CfgTrancheLevelingDefinitionId) VALUES (?, ?, ?)",
                s(id), s(projet), s(noeud));
        return id;
    }

    // ------------------------------------------------------------------ lecture

    /** Lignes natives ; les UUID des paramètres sont liés en texte. */
    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        return jdbc.queryForList(sql, texte(parametres));
    }

    public int compter(String sql, Object... parametres) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, texte(parametres));
        return n == null ? 0 : n;
    }

    /** Première colonne de la première ligne en texte, nulle si aucune ligne. */
    public String lire(String sql, Object... parametres) {
        em.flush();
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, texte(parametres));
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private static Object[] texte(Object... parametres) {
        Object[] valeurs = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            valeurs[i] = parametres[i] instanceof UUID u ? u.toString() : parametres[i];
        }
        return valeurs;
    }
}

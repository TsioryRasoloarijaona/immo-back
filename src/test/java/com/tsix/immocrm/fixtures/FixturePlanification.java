package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot N9b (FEATURE-n9-planification.md) : planifications Gantt ({@code CfgGanttProject}), intervenants ({@code CfgGanttOwner})
 * et tâches ({@code CfgGanttTask}). Les 4 tables CfgGantt* manquent à yesRemsTest et à la CI avant {@code ci/schema/199} :
 * créées à leur définition de yesRemsProd sur une connexion autocommit, sans FK (la garde de suppression doit venir du code).
 * Lignes en SQL natif dans la transaction du test ; dates liées en {@link Timestamp}, jamais en littéral.
 */
@Component
public class FixturePlanification {

    // Droits OMRAN (D5)
    public static final String DROIT_GANTT = "Yes.Cfg.Front.Gantt.GanttProject";
    public static final String DROIT_VERSION = "Yes.Prj.Front.Tdb.ProjectPrevision.toolSaveVersion";

    private static final String AUDIT = "UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, "
            + "UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL";

    private static final String TABLES = """
            IF OBJECT_ID(N'dbo.CfgGanttProject', N'U') IS NULL
                CREATE TABLE dbo.CfgGanttProject (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, StartDate datetime NULL, VersionDate datetime NULL, Notes nvarchar(1000) NULL,
                    %1$s, CfgGanttProjectMainVersionId uniqueidentifier NULL, ObjectId uniqueidentifier NULL,
                    StrEntityObjectId uniqueidentifier NULL);
            IF OBJECT_ID(N'dbo.CfgGanttOwner', N'U') IS NULL
                CREATE TABLE dbo.CfgGanttOwner (PKey uniqueidentifier NOT NULL PRIMARY KEY, Visible bit NULL,
                    CfgGanttProjectId uniqueidentifier NULL, CfgTierId uniqueidentifier NULL, %1$s,
                    Code nvarchar(255) NULL, Description nvarchar(255) NULL, Notes nvarchar(1000) NULL);
            """.formatted(AUDIT);

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final FixtureReferentielsPlanification referentiels;

    public FixturePlanification(EntityManager em, JdbcTemplate jdbc, DataSource dataSource,
                                FixtureReferentielsPlanification referentiels) {
        this.em = em;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.referentiels = referentiels;
    }

    /** Les 4 tables CfgGantt* (et celles de N9a), créées si absentes sur une connexion autocommit. */
    public void tables() {
        referentiels.tables();
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(TABLES);
        } catch (SQLException e) {
            throw new IllegalStateException("Création des tables N9b", e);
        }
    }

    // ------------------------------------------------------------------ planifications (R5)

    /** Modèle : {@code ObjectId} NULL, {@code VersionDate} NULL. */
    public UUID modele(String code, LocalDate debut) {
        return planification(code, debut, null, null, null);
    }

    /** Planification d'un projet : {@code ObjectId} = projet, {@code VersionDate} NULL. */
    public UUID planificationProjet(UUID projet, String code, LocalDate debut) {
        return planification(code, debut, projet, null, null);
    }

    /** Version d'une planification : {@code VersionDate} non nul, {@code CfgGanttProjectMainVersionId} = principale. */
    public UUID version(UUID principale, String code, LocalDate dateVersion) {
        return planification(code, dateVersion, null, principale, dateVersion);
    }

    private UUID planification(String code, LocalDate debut, UUID projet, UUID principale, LocalDate dateVersion) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO CfgGanttProject (PKey, Code, Description, Notes, StartDate, VersionDate, CfgGanttProjectMainVersionId,
                    ObjectId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                s(id), code, "Planification " + code, "Notes " + code, ts(debut), ts(dateVersion), s(principale), s(projet));
        return id;
    }

    public void notes(UUID planification, String notes) {
        maj("UPDATE CfgGanttProject SET Notes = ? WHERE PKey = ?", notes, s(planification));
    }

    // ------------------------------------------------------------------ intervenants (R11)

    public UUID intervenant(UUID planification, String code, boolean visible) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgGanttOwner (PKey, Code, Description, Visible, CfgGanttProjectId) VALUES (?, ?, ?, ?, ?)",
                s(id), code, "Intervenant " + code, visible, s(planification));
        return id;
    }

    // ------------------------------------------------------------------ tâches (R9, R10)

    /** Tâche à {@code Sorting} / {@code Depth} donnés (cohérents avec l'arbre), durée en jours. */
    public UUID tache(UUID planification, UUID parent, String code, int rang, int profondeur, LocalDate debut, int duree) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO CfgGanttTask (PKey, Code, Description, Notes, Sorting, Depth, StartDate, Duration, PercentComplete,
                    AlertBeforeDays, ConstraintType, IsMilestone, ParentId, CfgGanttProjectId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0.25, 2, 0, 0, ?, ?)""",
                s(id), code, "Tâche " + code, "Notes " + code, rang, profondeur, ts(debut), duree, s(parent), s(planification));
        return id;
    }

    /** {@code Predecessors} au format OMRAN {@code "<rang>|<type>;…"}. */
    public void predecesseurs(UUID tache, String texte) {
        maj("UPDATE CfgGanttTask SET Predecessors = ? WHERE PKey = ?", texte, s(tache));
    }

    /** {@code ResourceNames} = PKey d'intervenants séparés par {@code ;}. */
    public void ressources(UUID tache, UUID... intervenants) {
        StringBuilder texte = new StringBuilder();
        for (UUID i : intervenants) {
            if (!texte.isEmpty()) {
                texte.append(';');
            }
            texte.append(i);
        }
        maj("UPDATE CfgGanttTask SET ResourceNames = ? WHERE PKey = ?", texte.toString(), s(tache));
    }

    public void contrainteEtEcheance(UUID tache, LocalDate contrainte, LocalDate echeance) {
        maj("UPDATE CfgGanttTask SET ConstraintDate = ?, Deadline = ? WHERE PKey = ?", ts(contrainte), ts(echeance), s(tache));
    }

    public void categorieDeTache(UUID tache, UUID categorie) {
        maj("UPDATE CfgGanttTask SET StlCategoryId = ? WHERE PKey = ?", s(categorie), s(tache));
    }

    public void lotDeTache(UUID tache, UUID lot) {
        maj("UPDATE CfgGanttTask SET PrjMarketId = ? WHERE PKey = ?", s(lot), s(tache));
    }

    // ------------------------------------------------------------------ catégorie de lot (R8)

    public UUID categorie(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlCategory (PKey, Code, Description, IsCredit) VALUES (?, ?, ?, 0)", s(id), code, "Catégorie " + code);
        return id;
    }

    public void categorieDuLot(UUID lot, UUID categorie) {
        maj("UPDATE PrjMarket SET StlCategoryId = ? WHERE PKey = ?", s(categorie), s(lot));
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

    /** PKey (minuscules) de la tâche de ce code dans la planification, nulle si absente. */
    public UUID tacheParCode(UUID planification, String code) {
        String id = lire("SELECT CAST(PKey AS nvarchar(36)) FROM CfgGanttTask WHERE CfgGanttProjectId = ? AND Code = ?",
                planification, code);
        return id == null ? null : UUID.fromString(id);
    }

    /** Planification principale du projet ({@code ObjectId} = projet, {@code VersionDate} NULL), nulle si absente. */
    public UUID planificationDuProjet(UUID projet) {
        String id = lire("SELECT TOP 1 CAST(PKey AS nvarchar(36)) FROM CfgGanttProject "
                + "WHERE ObjectId = ? AND VersionDate IS NULL ORDER BY PKey ASC", projet);
        return id == null ? null : UUID.fromString(id);
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private static Timestamp ts(LocalDate date) {
        return date == null ? null : Timestamp.valueOf(date.atStartOfDay());
    }

    private static Object[] texte(Object... parametres) {
        Object[] valeurs = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            valeurs[i] = parametres[i] instanceof UUID u ? u.toString() : parametres[i];
        }
        return valeurs;
    }
}

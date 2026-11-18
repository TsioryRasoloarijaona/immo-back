package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réunions de chantier (FEATURE-n1-reunions-chantier.md) : tables {@code Bul*} posées en SQL natif, aucune entité du lot.
 * Parents avant enfants (FK de la CI) ; lignes annulées avec la transaction du test, tables créées hors transaction.
 */
@Component
public class FixtureReunionsChantier {

    public static final String REUNIONS = "/api/projet/reunions";
    public static final String TYPES = "/api/projet/config/types-reunion";
    public static final String STATUTS = "/api/projet/config/statuts-reserves";
    public static final String PLAGES = "/api/projet/config/plages-couleurs";

    // Droits D7 (AdmRight.Code de prod)
    public static final String DROIT_LIRE = "Yes.Prj.Front.Bul.MeetingList";
    public static final String DROIT_MODIFIER = "Yes.Prj.Front.Bul.MeetingList.toolNew";
    public static final String DROIT_SUPPRIMER = "Yes.Prj.Front.Bul.MeetingList.toolDelete";
    public static final String DROIT_TYPES = "Yes.Prj.Front.Config.BulMeetingType";
    public static final String DROIT_STATUTS = "Yes.Prj.Front.Config.BulObservationStatusList";
    public static final String DROIT_PLAGES = "Yes.Prj.Front.Config.PrjMeetingConfigColor";

    private static final String AUDIT = "UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, "
            + "UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL";

    /**
     * Colonnes de yesRemsProd. yesRemsTest n'a que {@code BulMeeting} (2 colonnes) et
     * {@code BulMeetingPrjProjectProgressCriteriaLine} ; la CI a les mêmes (165, 170). Idempotent.
     */
    public static final String TABLES = """
            IF COL_LENGTH(N'dbo.BulMeeting', N'MeetingDate') IS NULL ALTER TABLE dbo.BulMeeting ADD MeetingDate datetime NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'NextDate') IS NULL ALTER TABLE dbo.BulMeeting ADD NextDate datetime NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'CfgEmployeeId') IS NULL ALTER TABLE dbo.BulMeeting ADD CfgEmployeeId uniqueidentifier NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'Code') IS NULL ALTER TABLE dbo.BulMeeting ADD Code nvarchar(255) NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'Sorting') IS NULL ALTER TABLE dbo.BulMeeting ADD Sorting int NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'Note') IS NULL ALTER TABLE dbo.BulMeeting ADD Note nvarchar(500) NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'UserCreate') IS NULL ALTER TABLE dbo.BulMeeting ADD UserCreate nvarchar(255) NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'UserCreateDateTime') IS NULL ALTER TABLE dbo.BulMeeting ADD UserCreateDateTime datetime NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'UserUpdate') IS NULL ALTER TABLE dbo.BulMeeting ADD UserUpdate nvarchar(255) NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'UserUpdateDateTime') IS NULL ALTER TABLE dbo.BulMeeting ADD UserUpdateDateTime datetime NULL;
            IF COL_LENGTH(N'dbo.BulMeeting', N'BulMeetingTypeId') IS NULL ALTER TABLE dbo.BulMeeting ADD BulMeetingTypeId uniqueidentifier NULL;
            IF OBJECT_ID(N'dbo.BulMeetingType', N'U') IS NULL
                CREATE TABLE dbo.BulMeetingType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Code nvarchar(25) NULL, Description nvarchar(25) NULL, %1$s);
            IF OBJECT_ID(N'dbo.BulObservationStatus', N'U') IS NULL
                CREATE TABLE dbo.BulObservationStatus (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, ColorName nvarchar(255) NULL, Sorting int NULL, %1$s, IsColsed bit NULL);
            IF OBJECT_ID(N'dbo.PrjMeetingConfigColor', N'U') IS NULL
                CREATE TABLE dbo.PrjMeetingConfigColor (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Description nvarchar(255) NULL, ColorName nvarchar(255) NULL, StartFrom decimal(26, 6) NULL,
                    EndTo decimal(26, 6) NULL, %1$s);
            IF OBJECT_ID(N'dbo.BulMeetingLine', N'U') IS NULL
                CREATE TABLE dbo.BulMeetingLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL, DueDate datetime NULL,
                    Description varchar(500) NULL, Note varchar(500) NULL, IsClosed bit NULL, CfgSupplierId uniqueidentifier NULL,
                    PrjMarketId uniqueidentifier NULL, BulMeetingId uniqueidentifier NULL, BulMeetingLineId uniqueidentifier NULL,
                    BulObservationStatusId uniqueidentifier NULL, %1$s, IsInternal bit NULL);
            IF OBJECT_ID(N'dbo.BulMeetingContact', N'U') IS NULL
                CREATE TABLE dbo.BulMeetingContact (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    BulMeetingId uniqueidentifier NULL, ContactId uniqueidentifier NULL, IsPresent bit NULL, Notify bit NULL);
            IF OBJECT_ID(N'dbo.BulDelegate', N'U') IS NULL
                CREATE TABLE dbo.BulDelegate (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    PrjMarketId uniqueidentifier NULL, BulMeetingId uniqueidentifier NULL, %1$s, Notes nvarchar(500) NULL,
                    Effectif int NULL, CfgSupplierId uniqueidentifier NULL);
            IF OBJECT_ID(N'dbo.BulDelegateContact', N'U') IS NULL
                CREATE TABLE dbo.BulDelegateContact (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    BulDelegateId uniqueidentifier NULL, ContactId uniqueidentifier NULL, IsPresent bit NULL, Notify bit NULL);
            """.formatted(AUDIT);

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final FixtureDossierVente vente;
    private final FixtureProjetLots lots;
    private final FixtureDecomptes decomptes;

    public FixtureReunionsChantier(EntityManager em, JdbcTemplate jdbc, DataSource dataSource, FixtureDossierVente vente,
                                   FixtureProjetLots lots, FixtureDecomptes decomptes) {
        this.em = em;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.vente = vente;
        this.lots = lots;
        this.decomptes = decomptes;
    }

    /** Tables du lot, créées sur une connexion autocommit (un DDL dans la transaction du test serait annulé). */
    public void tables() {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(TABLES);
        } catch (SQLException e) {
            throw new IllegalStateException("Création des tables Bul*", e);
        }
    }

    // ------------------------------------------------------------------ comptes

    /** Compte Administrateur (tous les droits OMRAN) habilité sur ces projets. */
    public RequestPostProcessor admin(String jeton, UUID... projets) {
        AdmUser compte = vente.compte("bul.adm." + jeton, "Administrateur");
        for (UUID projet : projets) {
            lots.habiliter(compte, projet);
        }
        return FixtureDossierVente.connecte(compte, "Administrateur");
    }

    /** Compte non administrateur habilité sur le projet, portant exactement ces droits. */
    public RequestPostProcessor compte(UUID projet, String jeton, String... droits) {
        return decomptes.compte(projet, jeton, droits);
    }

    // ------------------------------------------------------------------ parents

    /** Projet de chantier rattaché à une société active. */
    public UUID projet(String code) {
        UUID societe = vente.tranche("T" + code, true).getCfgProject().getCfgCompany().getId();
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)",
                id.toString(), code, "Projet " + code, societe.toString());
        return id;
    }

    /** Tiers ({@code CfgTier}) : contact, surveillant ou fournisseur. */
    public UUID tiers(String code) {
        UUID id = vente.tier(code).getId();
        em.flush();
        return id;
    }

    public UUID type(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO BulMeetingType (PKey, Sorting, Code, Description) VALUES (?, 1, ?, ?)", id.toString(), code, code);
        return id;
    }

    /** Statut de réserve ({@code BulObservationStatus}) ; {@code ferme} = colonne {@code IsColsed}. */
    public UUID statut(String code, boolean ferme) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BulObservationStatus (PKey, Code, Description, ColorName, Sorting, IsColsed)
                VALUES (?, ?, ?, 'Red', 1, ?)""", id.toString(), code, code, ferme);
        return id;
    }

    public UUID plage(String description) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMeetingConfigColor (PKey, Sorting, Description, ColorName, StartFrom, EndTo)
                VALUES (?, 1, ?, 'Green', 0, 50)""", id.toString(), description);
        return id;
    }

    // ------------------------------------------------------------------ réunion et lignes filles

    public UUID reunion(UUID projet, UUID type, int ordre, String code, LocalDateTime date) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BulMeeting (PKey, PrjProjectId, BulMeetingTypeId, Sorting, Code, MeetingDate)
                VALUES (?, ?, ?, ?, ?, ?)""", id.toString(), projet.toString(), type.toString(), ordre, code,
                Timestamp.valueOf(date));
        return id;
    }

    /** Décision ({@code BulMeetingLine}) ; {@code reprise} = décision d'origine ({@code BulMeetingLineId}), nulle sinon. */
    public UUID decision(UUID reunion, int ordre, String description, UUID statut, UUID reprise) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BulMeetingLine (PKey, Sorting, Description, BulMeetingId, BulObservationStatusId, BulMeetingLineId)
                VALUES (?, ?, ?, ?, ?, ?)""", id.toString(), ordre, description, reunion.toString(), statut.toString(),
                reprise == null ? null : reprise.toString());
        return id;
    }

    public UUID maitreOuvrage(UUID reunion, UUID contact) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO BulMeetingContact (PKey, Sorting, BulMeetingId, ContactId, IsPresent, Notify) VALUES (?, 1, ?, ?, 1, 0)",
                id.toString(), reunion.toString(), contact.toString());
        return id;
    }

    public UUID intervenant(UUID reunion, UUID fournisseur) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO BulDelegate (PKey, Sorting, BulMeetingId, CfgSupplierId, Effectif) VALUES (?, 1, ?, ?, 3)",
                id.toString(), reunion.toString(), fournisseur.toString());
        return id;
    }

    public UUID contactIntervenant(UUID intervenant, UUID contact) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BulDelegateContact (PKey, Sorting, BulDelegateId, ContactId, IsPresent, Notify)
                VALUES (?, 1, ?, ?, 1, 0)""", id.toString(), intervenant.toString(), contact.toString());
        return id;
    }

    /** Niveau de nivellement : {@code CfgTrancheLevelingDefinition.CfgTrancheId} référence {@code PrjProject} (FK de prod). */
    public UUID niveau(UUID projet) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgTrancheLevelingDefinition (PKey, CfgTrancheId) VALUES (?, ?)", id.toString(), projet.toString());
        return id;
    }

    public UUID critere(UUID projet, String description) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjProjectProgressCriteriaLine (PKey, PrjProjectId, Sorting, Description) VALUES (?, ?, 1, ?)",
                id.toString(), projet.toString(), description);
        return id;
    }

    public UUID avancement(UUID reunion, UUID critere, UUID niveau, BigDecimal valeur) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BulMeetingPrjProjectProgressCriteriaLine (PKey, Achievement, BulMeetingId,
                    PrjProjectProgressCriteriaLineId, CfgTrancheLevelingdefinitionId) VALUES (?, ?, ?, ?, ?)""",
                id.toString(), valeur, reunion.toString(), critere.toString(), niveau.toString());
        return id;
    }

    // ------------------------------------------------------------------ lecture

    /** Comptage natif ; {@code ?} lié aux {@code parametres} (UUID passés en texte). */
    public int compter(String sql, Object... parametres) {
        flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, texte(parametres));
        return n == null ? 0 : n;
    }

    /** Première colonne de la première ligne, en texte (UUID en minuscules), nulle si aucune ligne. */
    public String lire(String sql, Object... parametres) {
        flush();
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, texte(parametres));
    }

    /** Écrit les changements en attente et vide le cache : l'appel HTTP suivant relit la base. */
    public void flush() {
        em.flush();
        em.clear();
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
    }

    private static Object[] texte(Object... parametres) {
        Object[] valeurs = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            valeurs[i] = parametres[i] instanceof UUID u ? u.toString() : parametres[i];
        }
        return valeurs;
    }
}

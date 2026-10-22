package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Rapports (FEATURE-tdb-vente-rapports-socle.md §2, §9.1) : lignes {@code AdmReportData} / {@code AdmReportDataRole},
 * rôles avec {@code WithReportRestriction}, comptes. Tout s'écrit dans la transaction du test et s'annule avec elle.
 */
@Component
public class FixtureRapports {

    public static final String TYPE_VENTE = "Yes.Com.Front.Tdb.Reports";
    public static final String TYPE_PATRIMOINE = "Yes.Stk.Front.Tdb.Reports";
    public static final String SUIVI_GLOBAL = "ComTdbSalesMonitoringGlobal";
    public static final String REPARTITION_CLIENTS = "ComTdbRepartitionClient";
    public static final String AUTRES_RAPPORTS = "Autres rapports";

    // Absentes de yesRemsTest (CI : ci/schema/140) ; créées dans la transaction du test, donc annulées avec elle.
    private static final String TABLE_RAPPORTS = """
            IF OBJECT_ID('dbo.AdmReportData', 'U') IS NULL
            CREATE TABLE dbo.AdmReportData (
                PKey uniqueidentifier NOT NULL PRIMARY KEY,
                Code nvarchar(255) NULL,
                Description nvarchar(255) NULL,
                [Type] nvarchar(1000) NULL,
                IsSystem bit NULL,
                Sorting int NULL,
                GroupName nvarchar(255) NULL,
                CfgImageId uniqueidentifier NULL,
                CategoryId uniqueidentifier NULL,
                Category nvarchar(255) NULL,
                OpenBeeSendActivated bit NULL,
                OpenBeeFolderId int NULL)""";
    private static final String TABLE_ROLES = """
            IF OBJECT_ID('dbo.AdmReportDataRole', 'U') IS NULL
            CREATE TABLE dbo.AdmReportDataRole (
                PKey uniqueidentifier NOT NULL PRIMARY KEY,
                AdmRoleId uniqueidentifier NULL,
                AdmReportDataId uniqueidentifier NULL,
                UserCreate nvarchar(255) NULL,
                UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL,
                UserUpdateDateTime datetime NULL)""";

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final FixtureDossierVente dossiers;
    private final AdmRoleRepository roles;
    private final AdmUserRoleRepository rolesComptes;

    public FixtureRapports(JdbcTemplate jdbc, EntityManager entityManager, FixtureDossierVente dossiers,
                           AdmRoleRepository roles, AdmUserRoleRepository rolesComptes) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.dossiers = dossiers;
        this.roles = roles;
        this.rolesComptes = rolesComptes;
    }

    /** Idempotent ; à appeler en tête de chaque test (dans sa transaction). */
    public void tables() {
        jdbc.execute(TABLE_RAPPORTS);
        jdbc.execute(TABLE_ROLES);
    }

    /** Ligne {@code AdmReportData} ; {@code groupe} et {@code ordre} peuvent être nuls. */
    public UUID rapport(String type, String code, String libelle, String groupe, Integer ordre) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmReportData (PKey, Code, Description, [Type], IsSystem, Sorting, GroupName) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?)", id.toString(), code, libelle, type, ordre, groupe);
        return id;
    }

    public UUID rapportVente(String code, String libelle, String groupe, Integer ordre) {
        return rapport(TYPE_VENTE, code, libelle, groupe, ordre);
    }

    public void supprimerRapport(UUID rapportId) {
        jdbc.update("DELETE FROM AdmReportDataRole WHERE AdmReportDataId = ?", rapportId.toString());
        jdbc.update("DELETE FROM AdmReportData WHERE PKey = ?", rapportId.toString());
    }

    /** Ligne {@code AdmReportDataRole} ; appelée deux fois = doublon réel de la prod (§1). */
    public void lier(UUID rapportId, AdmRole role) {
        entityManager.flush();
        jdbc.update("INSERT INTO AdmReportDataRole (PKey, AdmRoleId, AdmReportDataId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), role.getId().toString(), rapportId.toString());
    }

    /** Rôle propre au test ; {@code restreint} = {@code WithReportRestriction} ({@code null} possible). */
    public AdmRole role(String code, Boolean restreint) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setDescription(code);
        AdmRole sauve = roles.saveAndFlush(role);
        // Colonne posée en SQL : le test ne dépend pas du champ que le lot ajoute à AdmRole.
        if (restreint == null) {
            jdbc.update("UPDATE AdmRole SET WithReportRestriction = NULL WHERE PKey = ?", sauve.getId().toString());
        } else {
            jdbc.update("UPDATE AdmRole SET WithReportRestriction = ? WHERE PKey = ?", restreint,
                    sauve.getId().toString());
        }
        return sauve;
    }

    /** Compte actif sans rôle ({@link FixtureDossierVente#compte}), puis les rôles donnés. */
    public AdmUser compte(String code, AdmRole... rolesDuCompte) {
        AdmUser user = dossiers.compte(code);
        for (AdmRole role : rolesDuCompte) {
            attribuer(user, role);
        }
        return user;
    }

    public AdmUserRole attribuer(AdmUser user, AdmRole role) {
        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        return rolesComptes.saveAndFlush(userRole);
    }

    public void retirer(AdmUserRole userRole) {
        entityManager.flush();
        jdbc.update("DELETE FROM AdmUserRole WHERE PKey = ?", userRole.getId().toString());
        relire();
    }

    /** Vide le cache JPA : les colonnes posées en SQL ({@code WithReportRestriction}…) sont relues. */
    public void relire() {
        entityManager.flush();
        entityManager.clear();
    }
}

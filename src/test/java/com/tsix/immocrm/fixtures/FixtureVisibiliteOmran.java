package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lot V1 (FEATURE-visibilite-omran.md) : statuts de workflow d'un lot ou d'un projet, intervenants
 * ({@code StrIntervener} + {@code StrStatusIntervener}, aucun dans yesRemsTest), drapeau {@code WithSequenceFreeRestriction}
 * des rôles, habilitation société, module PRJ des pièces. Parents avant enfants (FK de la CI) ; annulé avec la transaction.
 */
@Component
public class FixtureVisibiliteOmran {

    /** PKey de production du module Projet ({@code AdmModule.Code = 'PRJ'}). */
    public static final UUID MODULE_PRJ = UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00005");

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDossierVente comptes;

    public FixtureVisibiliteOmran(EntityManager em, JdbcTemplate jdbc, FixtureDossierVente comptes) {
        this.em = em;
        this.jdbc = jdbc;
        this.comptes = comptes;
    }

    // ------------------------------------------------------------------ comptes et rôles

    /**
     * Compte actif avec un rôle propre au test ({@code AdmUserRole}) ; {@code restreint} = drapeau OMRAN
     * {@code AdmRole.WithSequenceFreeRestriction} (faux = exempté de la clause de statut).
     */
    public AdmUser compte(String code, String role, boolean restreint) {
        AdmUser user = comptes.compte(code, role);
        em.flush();
        jdbc.update("UPDATE AdmRole SET WithSequenceFreeRestriction = ? WHERE Code = ?", restreint, role);
        return user;
    }

    /** Rôle seul, porté par aucun compte, restreint. */
    public UUID role(String code) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction) VALUES (?, ?, ?, 1)",
                id.toString(), code, code);
        return id;
    }

    /** Pose le drapeau sur tous les rôles de ce code déjà présents (ex. « Administrateur » de la base de test). */
    public void restreindre(String codeRole) {
        em.flush();
        jdbc.update("UPDATE AdmRole SET WithSequenceFreeRestriction = 1 WHERE Code = ?", codeRole);
    }

    public UUID idRole(String code) {
        em.flush();
        return UUID.fromString(jdbc.queryForObject("SELECT CAST(PKey AS varchar(36)) FROM AdmRole WHERE Code = ?",
                String.class, code));
    }

    /** Ligne {@code AdmUserHabilitation.CfgCompanyId}. */
    public void habiliterSociete(AdmUser compte, UUID societe) {
        em.flush();
        jdbc.update("INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgCompanyId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), compte.getId().toString(), societe.toString());
    }

    // ------------------------------------------------------------------ workflow

    /** Entité de workflow propre au test ({@code StrEntity}). */
    public UUID entite(String code) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, code);
        return id;
    }

    public UUID statut(UUID entite, String code) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId) VALUES (?, ?, ?, 1, ?)",
                id.toString(), code, code, entite.toString());
        return id;
    }

    /** Intervenant « rôle » du statut. */
    public void intervenantRole(UUID entite, UUID statut, UUID roleId) {
        intervenant(entite, statut, roleId, null);
    }

    /** Intervenant nominatif ({@code StrIntervener.AdmUserId}) du statut. */
    public void intervenantCompte(UUID entite, UUID statut, UUID admUserId) {
        intervenant(entite, statut, null, admUserId);
    }

    private void intervenant(UUID entite, UUID statut, UUID roleId, UUID admUserId) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("""
                        INSERT INTO StrIntervener (PKey, Code, Description, AdmRoleId, AdmUserId, StrConfigEntityId)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                id.toString(), "INT-" + id.toString().substring(0, 8), "Intervenant de test",
                roleId == null ? null : roleId.toString(), admUserId == null ? null : admUserId.toString(),
                entite.toString());
        jdbc.update("INSERT INTO StrStatusIntervener (PKey, StrIntervenerId, StrStatusId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), id.toString(), statut.toString());
    }

    /** {@code PrjMarket.StrEntityId} / {@code StrStatusId} du lot. */
    public void statutLot(UUID lot, UUID entite, UUID statut) {
        em.flush();
        jdbc.update("UPDATE PrjMarket SET StrEntityId = ?, StrStatusId = ? WHERE PKey = ?",
                entite.toString(), statut.toString(), lot.toString());
    }

    /** {@code PrjProject.StrEntityId} / {@code StrStatusId} du projet (R7). */
    public void statutProjet(UUID projet, UUID entite, UUID statut) {
        em.flush();
        jdbc.update("UPDATE PrjProject SET StrEntityId = ?, StrStatusId = ? WHERE PKey = ?",
                entite.toString(), statut.toString(), projet.toString());
    }

    // ------------------------------------------------------------------ module des pièces (R5)

    /** Module PRJ à sa PKey de production, créé si aucun module de code PRJ n'existe ; rend sa PKey. */
    public UUID modulePrj() {
        em.flush();
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM AdmModule WHERE Code = N'PRJ')
                    INSERT INTO AdmModule (PKey, Code, Description) VALUES (?, N'PRJ', N'Projet')""",
                MODULE_PRJ.toString());
        return UUID.fromString(jdbc.queryForObject(
                "SELECT TOP 1 CAST(PKey AS varchar(36)) FROM AdmModule WHERE Code = N'PRJ' ORDER BY PKey ASC", String.class));
    }

    /** Module d'une autre gestion (code donné), créé pour le test. */
    public UUID autreModule(String code) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO AdmModule (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, code);
        return id;
    }

    public void modulePiece(UUID piece, UUID module) {
        em.flush();
        jdbc.update("UPDATE BuyDocument SET AdmModuleId = ? WHERE PKey = ?", module.toString(), piece.toString());
    }
}

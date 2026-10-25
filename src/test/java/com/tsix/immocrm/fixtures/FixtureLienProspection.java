package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lien étude de faisabilité ↔ prospection terrain (FEATURE-patrimoine-etude-prospection.md §3) : routes et champs du fil,
 * plus les états d'étude que l'API ne pose pas sans effet de bord (blocage sans Verif, société devenue inactive).
 */
@Component
public class FixtureLienProspection {

    public static final String LIEN = FixtureFaisabilite.FICHE + "/prospection-terrain";
    public static final String RECOPIER = LIEN + "/recopier";
    public static final String CHOIX_PROSPECTIONS = FixtureFaisabilite.RACINE + "/choix/prospections";

    public static final String PROSPECTION_TERRAIN_ID = "prospectionTerrainId";
    public static final String PROSPECTION_TERRAIN_DESCRIPTION = "prospectionTerrainDescription";
    public static final String PORTEUR_ETUDE = "etude";
    public static final String MSG_AUCUNE_PROSPECTION = "Aucune prospection liée";

    private static final String P_ID = "id";

    private final EntityManager entityManager;

    public FixtureLienProspection(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static String lien(UUID prospectionId) {
        return CorpsJson.corps().avec(PROSPECTION_TERRAIN_ID, prospectionId).json();
    }

    /** Société de l'étude rendue inactive : l'étude sort du périmètre visible de tous, Administrateur compris. */
    public void desactiverSociete(UUID societeId) {
        executer("UPDATE CfgCompany SET IsActive = 0 WHERE PKey = :id", societeId);
    }

    /** {@code IsLocked = 1} posé directement (Bloquer passe par Verif, hors sujet ici). */
    public void bloquer(UUID etudeId) {
        executer("UPDATE StkFeasibilityStudy SET IsLocked = 1 WHERE PKey = :id", etudeId);
    }

    /** Auteur de modification effacé : la trace écrite ensuite se distingue de celle de la création. */
    public void effacerAuteurModification(UUID etudeId) {
        executer("UPDATE StkFeasibilityStudy SET UserUpdate = NULL, UserUpdateDateTime = NULL WHERE PKey = :id", etudeId);
    }

    private void executer(String sql, UUID id) {
        entityManager.flush();
        entityManager.createNativeQuery(sql).setParameter(P_ID, id).executeUpdate();
        entityManager.clear();
    }
}

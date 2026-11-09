package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lignes du lot P9a (FEATURE-projet-config.md) écrites en SQL natif : {@code PrjMarket} et {@code BuyDocument} n'ont pas
 * d'entité sur la branche, {@code PrjProject} change de colonnes dans le lot. Routes et noms de champs = contrat du fil.
 */
@Component
public class FixtureProjetConfig {

    public static final String PROJETS = "/api/projet/config/projets";
    public static final String PROJET = PROJETS + "/{id}";
    public static final String STATUTS_LOT = "/api/projet/config/statuts-lot";
    public static final String REFERENTIELS = "/api/projet/config/referentiels";
    public static final String SLUG_TYPES_LOT = "types-lot";

    // Champs du corps JSON (statut et entité ne sont plus saisis : §8, réponse 2 du manager)
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String SOCIETE_ID = "societeId";

    // PKey de production : StrEntity « Projet », son seul statut « Nouveau », et les genres de lot à statuts (§8.2)
    public static final UUID ENTITE_PROJET = UUID.fromString("C8D5550A-936C-414A-814D-0D6B57074613");
    public static final UUID STATUT_NOUVEAU = UUID.fromString("DD0D3142-08C8-4F93-BD77-5593EAE178CF");
    public static final UUID LOT_ENTREPRENEURS = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B613");
    public static final UUID LOT_MAITRES_OUVRAGE = UUID.fromString("E7999F72-7507-40D9-AA78-A2825C42B614");

    private static final String ID = "id";
    private static final String P_CODE = "code";
    private static final String P_PROJET = "projet";

    private final EntityManager entityManager;

    public FixtureProjetConfig(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** StrEntity « Projet » et deux genres de lot, statut « Nouveau » du projet : lignes de prod, créées si absentes. */
    public void parametrage() {
        entite(ENTITE_PROJET, "PrjProject", "Projet");
        entite(LOT_ENTREPRENEURS, "PrjMarket", "Lot Entrepreneurs");
        entite(LOT_MAITRES_OUVRAGE, "PrjMarketStudies", "Lot Maitres d'ouvrage");
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrStatus WHERE PKey = :id)
                            INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId)
                            VALUES (:id, N'Nouveau', N'Nouveau', 10, :entite)""")
                .setParameter(ID, STATUT_NOUVEAU).setParameter("entite", ENTITE_PROJET));
    }

    private void entite(UUID id, String code, String description) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, :code, :description)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter("description", description));
    }

    public UUID statut(UUID entiteWorkflow, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, ColorName)
                        VALUES (:id, :code, :code, 1, :entite, N'LimeGreen')""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter("entite", entiteWorkflow));
        return id;
    }

    /** Projet complet, sans habilitation : visible du seul écran d'administration. */
    public UUID projet(String code, UUID societeId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId, StrEntityId, StrStatusId,
                            IsVentilated, defaultVentilationRate, IsACommon)
                        VALUES (:id, :code, :description, :societe, :entite, :statut, 0, 0, 0)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter("description", "Projet " + code)
                .setParameter("societe", societeId).setParameter("entite", ENTITE_PROJET)
                .setParameter("statut", STATUT_NOUVEAU));
        return id;
    }

    public UUID habilitation(UUID userId, UUID projetId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (:id, :user, :projet)""")
                .setParameter(ID, id).setParameter("user", userId).setParameter(P_PROJET, projetId));
        return id;
    }

    /** Ligne qui référence un projet : {@code sql} lie {@code :id}, {@code :code}, {@code :projet} (texte écrit par le test). */
    public UUID reference(String sql, String code, UUID projetId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(sql)
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_PROJET, projetId));
        return id;
    }

    /** Lot ({@code PrjMarket}) d'un type de lot, sans projet. */
    public UUID lotDeType(String code, UUID typeLotId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO PrjMarket (PKey, Code, Description, PrjMarketTypeId) VALUES (:id, :code, :code, :type)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter("type", typeLotId));
        return id;
    }

    /** {@code :id} lié ; le reste du SQL est écrit par le test. */
    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}

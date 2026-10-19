package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * Lignes du lot P6 (FEATURE-patrimoine-referentiels.md) écrites en SQL natif : les entités changent de parent dans le
 * lot, le SQL natif ne dépend pas de leurs setters. Les slugs et noms de champs ci-dessous sont le contrat du fil.
 */
@Component
public class FixtureReferentielsAdmin {

    public static final String RACINE = "/api/referentiels-admin";
    public static final String TABLE = RACINE + "/{table}";
    public static final String LIGNE = RACINE + "/{table}/{id}";

    // Slugs de {table}
    public static final String CATEGORIES = "categories";
    public static final String TYPES = "types";
    public static final String IMMEUBLES = "immeubles";
    public static final String ETAGES = "etages";
    public static final String ORIENTATIONS = "orientations";
    public static final String VOCATIONS = "vocations";
    public static final String NATURES = "natures";
    public static final String FACADES = "facades";
    public static final String VUES = "vues";
    public static final String CATEGORIES_CHARGE = "categories-charge";
    public static final String CONTENANCES = "contenances";
    public static final String TYPES_PRESENTATION = "types-presentation";
    public static final String TYPES_FRAIS = "types-frais";
    public static final String STANDINGS = "standings";
    public static final String VOCATIONS_TERRAIN = "vocations-terrain";
    public static final String NIVEAUX_TERRAIN = "niveaux-terrain";
    public static final String QUOTES_PARTS_EXONEREES = "quotes-parts-exonerees";
    public static final String RUBRIQUES_TRANCHE = "rubriques-tranche";
    public static final String SEUILS_VERSEMENT = "seuils-versement";

    // Champs du corps JSON
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String SORTING = "sorting";
    public static final String NATURE_ID = "natureId";
    public static final String CATEGORIE_COMPTABLE_ID = "categorieComptableId";
    public static final String TYPE_MARCHE_ID = "typeMarcheId";
    public static final String TRANCHE_ID = "trancheId";
    public static final String ANNEE = "annee";
    public static final String STATUT_ID = "statutId";
    public static final String TYPE_REGLEMENT_ID = "typeReglementId";
    public static final String OPERATEUR = "operateur";
    public static final String RATIO = "ratio";
    public static final String SEARCH = "search";

    // PKey de production
    public static final UUID NATURE_TER = UUID.fromString("343710CB-8806-484E-BB2C-9073569C72A4");
    public static final UUID ENTITE_DOSSIER_VENTE = StatutDossier.CONFIG_ENTITY_ID;
    public static final UUID ENTITE_PRM_FOLDER_SALE = UUID.fromString("BDEEBEA2-3ECF-43A3-B9E0-8EE5604E6511");
    public static final UUID REGLEMENT_TOUJOURS = UUID.fromString("BC72ECEA-66A5-4EC0-847B-1B55ADB0A2C5");
    public static final UUID REGLEMENT_AU_COMPTANT = UUID.fromString("F344426B-CB63-4DFD-A357-BC5BBD7AAAF3");

    private static final String ID = "id";
    private static final String P_CODE = "code";
    private static final String P_DESCRIPTION = "description";

    private final EntityManager entityManager;

    public FixtureReferentielsAdmin(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static String jeton() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ référentiels parents

    /** Nature à cette PKey, créée si absente. */
    public void nature(UUID id, String code) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StkKind WHERE PKey = :id)
                            INSERT INTO StkKind (PKey, Code, Description) VALUES (:id, :code, :description)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, code));
    }

    public UUID nature(String code) {
        UUID id = UUID.randomUUID();
        nature(id, code);
        return id;
    }

    public UUID vocation(String code) {
        return codeDescription("INSERT INTO StkVocation (PKey, Code, Description) VALUES (:id, :code, :description)", code);
    }

    /** Renomme une ligne protégée (transaction de test annulée ensuite) : isole la protection par PKey. */
    public void recoder(String table, UUID id, String code) {
        executer(entityManager.createNativeQuery("UPDATE " + table + " SET Code = :code WHERE PKey = :id")
                .setParameter(P_CODE, code).setParameter(ID, id));
    }

    public UUID categorieComptable(String code) {
        return codeDescription("INSERT INTO StlCategory (PKey, Code, Description) VALUES (:id, :code, :description)", code);
    }

    public UUID typeMarche(String code) {
        return codeDescription("INSERT INTO PrjMarketType (PKey, Code, Description) VALUES (:id, :code, :description)",
                code);
    }

    public UUID contenance(String code) {
        return codeDescription("INSERT INTO StkContenance (PKey, Code, Description) VALUES (:id, :code, :description)",
                code);
    }

    // ------------------------------------------------------------------ présentations

    public UUID presentation(UUID trancheId, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkPresentationType (PKey, Code, Description, ColorName, StkHierarchyId)
                        VALUES (:id, :code, :description, N'Gold', :tranche)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Presentation " + code)
                .setParameter("tranche", trancheId));
        return id;
    }

    public UUID contenanceDePresentation(UUID presentationId, UUID contenanceId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkPresentationTypeContenance (PKey, StkPresentationTypeId, StkContenanceId, ItemsNumber)
                        VALUES (:id, :presentation, :contenance, 2)""")
                .setParameter(ID, id).setParameter("presentation", presentationId)
                .setParameter("contenance", contenanceId));
        return id;
    }

    // ------------------------------------------------------------------ enfant en CASCADE d'un étage

    public UUID cheminDEtage(UUID etageId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO StkHierarchyPath (PKey, StkFloorId) VALUES (:id, :etage)")
                .setParameter(ID, id).setParameter("etage", etageId));
        return id;
    }

    // ------------------------------------------------------------------ seuils de versement

    /** Entités de workflow {@code ComFolder} et {@code PrmFolderSale} et types de règlement, à leurs PKey de production. */
    public void parametrageSeuils() {
        entiteWorkflow(ENTITE_DOSSIER_VENTE, "ComFolder");
        entiteWorkflow(ENTITE_PRM_FOLDER_SALE, "PrmFolderSale");
        typeReglement(REGLEMENT_TOUJOURS, "Toujours");
        typeReglement(REGLEMENT_AU_COMPTANT, "Au comptant");
    }

    private void entiteWorkflow(UUID id, String code) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, :code, :description)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, code));
    }

    private void typeReglement(UUID id, String code) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM ComFolderStatusPaymentType WHERE PKey = :id)
                            INSERT INTO ComFolderStatusPaymentType (PKey, Code, Description) VALUES (:id, :code, :description)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, code));
    }

    /** Statut propre au test : aucun seuil existant ne le vise. */
    public UUID statut(UUID entiteWorkflow, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrStatus (PKey, Code, Description, StrConfigEntityId)
                        VALUES (:id, :code, :description, :entite)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, code)
                .setParameter("entite", entiteWorkflow));
        return id;
    }

    public UUID seuilGlobal(UUID entiteWorkflow, UUID statutId, int operateur, BigDecimal ratio) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheStatus (PKey, CfgTrancheId, StrFolderEntityId, StrStatusId,
                            ComFolderStatusPaymentTypeId, ThresholdOperator, ThresholdRatio)
                        VALUES (:id, NULL, :entite, :statut, :reglement, :operateur, :ratio)""")
                .setParameter(ID, id).setParameter("entite", entiteWorkflow).setParameter("statut", statutId)
                .setParameter("reglement", REGLEMENT_TOUJOURS).setParameter("operateur", operateur)
                .setParameter("ratio", ratio));
        return id;
    }

    public UUID seuilDeTranche(UUID trancheId, UUID statutId, int operateur, BigDecimal ratio) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheStatus (PKey, CfgTrancheId, StrFolderEntityId, StrStatusId,
                            ComFolderStatusPaymentTypeId, ThresholdOperator, ThresholdRatio)
                        VALUES (:id, :tranche, :entite, :statut, :reglement, :operateur, :ratio)""")
                .setParameter(ID, id).setParameter("tranche", trancheId).setParameter("entite", ENTITE_DOSSIER_VENTE)
                .setParameter("statut", statutId).setParameter("reglement", REGLEMENT_TOUJOURS)
                .setParameter("operateur", operateur).setParameter("ratio", ratio));
        return id;
    }

    // ------------------------------------------------------------------ lecture

    /** {@code :id} lié ; le reste du SQL est écrit par le test (noms de tables de sa propre liste). */
    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    /** Lignes de {@code table} de cette PKey portant ce code et ce libellé. */
    public int lignes(String table, UUID id, String code, String description) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table
                        + " WHERE PKey = :id AND Code = :code AND Description = :description")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, description)
                .getSingleResult();
        return ((Number) n).intValue();
    }

    public int lignes(String table, UUID id) {
        return compter("SELECT COUNT(*) FROM " + table + " WHERE PKey = :id", id);
    }

    private UUID codeDescription(String sql, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(sql)
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Libelle " + code));
        return id;
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}

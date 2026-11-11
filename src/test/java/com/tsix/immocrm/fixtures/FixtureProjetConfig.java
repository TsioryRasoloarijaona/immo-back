package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    public static final UUID TYPE_STATUT_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");

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
        // Statut de départ = 1er statut de type « Nouveau » (spec K1 §5) : type de prod posé sur « Nouveau » s'il manque.
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = :type)
                            INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (:type, N'Nouveau', N'Nouveau', 0)""")
                .setParameter("type", TYPE_STATUT_NOUVEAU));
        executer(entityManager.createNativeQuery("""
                        UPDATE StrStatus SET StrStatusTypeId = :type WHERE PKey = :id AND StrStatusTypeId IS NULL""")
                .setParameter("type", TYPE_STATUT_NOUVEAU).setParameter(ID, STATUT_NOUVEAU));
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

    // ------------------------------------------------------------------ parité OMRAN (FEATURE-projets-config-parite.md)

    // Champs du fil ajoutés par le lot K1 (§1, §2) et routes (§3, §5) : contrat de la fiche projet
    public static final String DATE_LANCEMENT_PREVISIONNELLE = "dateLancementPrevisionnelle";
    public static final String DATE_ACHEVEMENT_PREVISIONNELLE = "dateAchevementPrevisionnelle";
    public static final String DATE_LANCEMENT_REELLE = "dateLancementReelle";
    public static final String DATE_ACHEVEMENT_REELLE = "dateAchevementReelle";
    public static final String NIVELLEMENT_ID = "nivellementId";
    public static final String PROJET_REFERENCE_ID = "projetReferenceId";
    public static final String LOT_GESTIONNAIRE_PRORATA_ID = "lotGestionnaireProrataId";
    public static final String TRANSITIONS = PROJET + "/transitions";
    public static final String TRANSITION = PROJET + "/transition";
    public static final String TRANSITION_ID = "transitionId";
    public static final String VENTILATIONS = PROJET + "/ventilations";
    public static final String VENTILATION = VENTILATIONS + "/{ligneId}";

    /** {@code StkLeveling} est absente de yesRemsTest (présente en CI) : créée dans la transaction du test si besoin. */
    public UUID nivellement(String code) {
        executer(entityManager.createNativeQuery("""
                IF OBJECT_ID(N'dbo.StkLeveling', N'U') IS NULL
                    CREATE TABLE dbo.StkLeveling (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                        UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                        UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)"""));
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO StkLeveling (PKey, Code) VALUES (:id, :code)")
                .setParameter(ID, id).setParameter(P_CODE, code));
        return id;
    }

    /** Lot ({@code PrjMarket}) rattaché au projet. */
    public UUID lotDuProjet(String code, UUID projetId) {
        return reference("INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId) VALUES (:id, :code, :code, :projet)",
                code, projetId);
    }

    /** Tranche rattachée au projet par {@code CfgTranche.PrjProjectId}. */
    public UUID trancheDuProjet(String code, UUID projetId) {
        return reference("INSERT INTO CfgTranche (PKey, Code, Description, PrjProjectId) VALUES (:id, :code, :code, :projet)",
                code, projetId);
    }

    /**
     * Ligne de ventilation par tranche ({@code PrjProjectCfgTrancheVentilation}). yesRemsTest et la CI ({@code ci/schema/165})
     * n'ont que PKey et PrjProjectId : les colonnes de production sont ajoutées dans la transaction du test si absentes.
     */
    public UUID ventilation(UUID projetId, UUID trancheId, BigDecimal taux) {
        executer(entityManager.createNativeQuery("""
                IF COL_LENGTH(N'dbo.PrjProjectCfgTrancheVentilation', N'CfgTrancheId') IS NULL
                    ALTER TABLE dbo.PrjProjectCfgTrancheVentilation ADD CfgTrancheId uniqueidentifier NULL"""));
        executer(entityManager.createNativeQuery("""
                IF COL_LENGTH(N'dbo.PrjProjectCfgTrancheVentilation', N'VentilationRate') IS NULL
                    ALTER TABLE dbo.PrjProjectCfgTrancheVentilation ADD VentilationRate decimal(26, 6) NULL"""));
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO PrjProjectCfgTrancheVentilation (PKey, PrjProjectId, CfgTrancheId, VentilationRate)
                        VALUES (:id, :projet, :tranche, :taux)""")
                .setParameter(ID, id).setParameter(P_PROJET, projetId).setParameter("tranche", trancheId)
                .setParameter("taux", taux));
        return id;
    }

    /** Transition du workflow projet ({@code StrWorkFlow} de l'entité « Projet »), sans option ni intervenant. */
    public UUID transitionProjet(UUID depuis, UUID vers, String libelle) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                            WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                        VALUES (:id, :depuis, :vers, 10, :code, :code, 0, 0, 0, 0, :entite)""")
                .setParameter(ID, id).setParameter("depuis", depuis).setParameter("vers", vers)
                .setParameter(P_CODE, libelle).setParameter("entite", ENTITE_PROJET));
        return id;
    }

    /** Les 4 dates de travaux du projet, dans l'ordre lancement / achèvement prévisionnels puis réels. */
    public void dates(UUID projetId, LocalDate lancementPrevu, LocalDate achevementPrevu, LocalDate lancementReel,
                      LocalDate achevementReel) {
        executer(entityManager.createNativeQuery("""
                        UPDATE PrjProject SET ProvisionalWorkStartDate = :lp, ProvisionalCompletingWorkDate = :ap,
                            RealWorkStartDate = :lr, RealCompletingWorkDate = :ar WHERE PKey = :id""")
                .setParameter("lp", lancementPrevu).setParameter("ap", achevementPrevu)
                .setParameter("lr", lancementReel).setParameter("ar", achevementReel).setParameter(ID, projetId));
    }

    /** Liens du projet : nivellement, projet référence, lot gestionnaire du compte prorata. */
    public void liens(UUID projetId, UUID nivellement, UUID reference, UUID lotProrata) {
        executer(entityManager.createNativeQuery("""
                        UPDATE PrjProject SET StkLevelingId = :nivellement, ReferenceId = :reference,
                            ManageProrataPrjMarketId = :lot WHERE PKey = :id""")
                .setParameter("nivellement", nivellement).setParameter("reference", reference)
                .setParameter("lot", lotProrata).setParameter(ID, projetId));
    }

    /** Place le projet sur un statut de test. */
    public void statutDuProjet(UUID projetId, UUID statutId) {
        executer(entityManager.createNativeQuery("UPDATE PrjProject SET StrStatusId = :statut WHERE PKey = :id")
                .setParameter("statut", statutId).setParameter(ID, projetId));
    }

    /** Comptage natif ; tous les paramètres liés (dates en {@code LocalDate}, jamais en littéral). */
    public int compter(String sql, Map<String, Object> parametres) {
        entityManager.flush();
        Query query = entityManager.createNativeQuery(sql);
        parametres.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).intValue();
    }

    // ------------------------------------------------------------------ 2ᵉ passe : contrat validé (spec K1, « Contrat »)

    public static final String NIVELLEMENTS = "nivellements";
    public static final String TRANCHE_ID = "trancheId";
    public static final String TAUX = "taux";
    public static final String CRITERES = PROJET + "/criteres";
    public static final String CRITERE = CRITERES + "/{ligneId}";
    public static final String IMPORTER_CRITERES = CRITERES + "/importer";
    public static final String ORDRE = "ordre";
    public static final String CATEGORIE_ID = "categorieId";
    public static final String PROJET_SOURCE_ID = "projetSourceId";
    public static final String TYPE_LOT_DEFAUT = "/api/projet/config/types-lot/defaut";
    public static final String TYPE_LOT_ID = "typeLotId";
    public static final String PARAMETRE_TYPE_LOT = "DefaultMarketType";

    /** Lignes natives en {@code Tuple} ; {@code :id} lié. */
    @SuppressWarnings("unchecked")
    public List<Tuple> lignes(String sql, UUID id) {
        entityManager.flush();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(ID, id).getResultList();
    }

    /** Catégorie de trésorerie ; {@code credit} = sens recette ({@code IsCredit = 1}), sinon dépense. */
    public UUID categorie(String code, boolean credit) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StlCategory (PKey, Code, Description, IsCredit, IsCharge, IsProduct)
                        VALUES (:id, :code, :code, :credit, 1, 1)""")
                .setParameter(ID, id).setParameter(P_CODE, code).setParameter("credit", credit));
        return id;
    }

    /**
     * Critère d'avancement ({@code PrjProjectProgressCriteriaLine}). yesRemsTest et la CI ({@code ci/schema/165}) n'ont que PKey et
     * PrjProjectId : les colonnes de production sont ajoutées dans la transaction du test si absentes.
     */
    public UUID critere(UUID projetId, int ordre, String description, UUID categorieId) {
        colonnesCriteres();
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO PrjProjectProgressCriteriaLine (PKey, Sorting, Description, StlCategoryId, PrjProjectId)
                        VALUES (:id, :ordre, :description, :categorie, :projet)""")
                .setParameter(ID, id).setParameter("ordre", ordre).setParameter("description", description)
                .setParameter("categorie", categorieId).setParameter(P_PROJET, projetId));
        return id;
    }

    public void colonnesCriteres() {
        ajouterColonne("PrjProjectProgressCriteriaLine", "Sorting", "int");
        ajouterColonne("PrjProjectProgressCriteriaLine", "Description", "nvarchar(255)");
        ajouterColonne("PrjProjectProgressCriteriaLine", "StlCategoryId", "uniqueidentifier");
        ajouterColonne("PrjProjectProgressCriteriaLine", "UserCreate", "nvarchar(255)");
        ajouterColonne("PrjProjectProgressCriteriaLine", "UserCreateDateTime", "datetime");
        ajouterColonne("PrjProjectProgressCriteriaLine", "UserUpdate", "nvarchar(255)");
        ajouterColonne("PrjProjectProgressCriteriaLine", "UserUpdateDateTime", "datetime");
    }

    // Noms de table et de colonne écrits en dur par cette classe, jamais venus d'un appelant extérieur.
    private void ajouterColonne(String table, String colonne, String type) {
        executer(entityManager.createNativeQuery("IF COL_LENGTH(N'dbo." + table + "', N'" + colonne + "') IS NULL "
                + "ALTER TABLE dbo." + table + " ADD " + colonne + " " + type + " NULL"));
    }

    /** Ligne de PV de réunion citant le critère ({@code BulMeetingPrjProjectProgressCriteriaLine}, absente de yesRemsTest et de la CI). */
    public UUID citerParPv(UUID critereId) {
        executer(entityManager.createNativeQuery("""
                IF OBJECT_ID(N'dbo.BulMeetingPrjProjectProgressCriteriaLine', N'U') IS NULL
                    CREATE TABLE dbo.BulMeetingPrjProjectProgressCriteriaLine (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                        Achievement decimal(26, 6) NULL, BulMeetingId uniqueidentifier NULL,
                        PrjProjectProgressCriteriaLineId uniqueidentifier NULL, CfgTrancheLevelingdefinitionId uniqueidentifier NULL,
                        UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                        UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)"""));
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO BulMeetingPrjProjectProgressCriteriaLine (PKey, PrjProjectProgressCriteriaLineId)
                        VALUES (:id, :critere)""")
                .setParameter(ID, id).setParameter("critere", critereId));
        return id;
    }

    /** Type de lot ({@code PrjMarketType}). */
    public UUID typeLot(String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO PrjMarketType (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(ID, id).setParameter(P_CODE, code));
        return id;
    }

    /** Ligne {@code AdmParamsGuid} « DefaultMarketType » (Code = clé primaire, absente de yesRemsTest) : supprimée puis réécrite. */
    public void typeLotParDefaut(UUID typeLotId) {
        executer(entityManager.createNativeQuery("DELETE FROM AdmParamsGuid WHERE Code = :code")
                .setParameter(P_CODE, PARAMETRE_TYPE_LOT));
        executer(entityManager.createNativeQuery("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (:code, :valeur, :id)")
                .setParameter(P_CODE, PARAMETRE_TYPE_LOT).setParameter("valeur", typeLotId).setParameter(ID, UUID.randomUUID()));
    }

    /** {@code CodeValue} de « DefaultMarketType » lu en SQL natif (le cache JPA d'{@code AdmParamsGuid} garderait l'ancienne valeur). */
    public String typeLotParDefautEnBase() {
        entityManager.flush();
        Object valeur = entityManager.createNativeQuery(
                        "SELECT CONVERT(nvarchar(36), CodeValue) FROM AdmParamsGuid WHERE Code = :code")
                .setParameter(P_CODE, PARAMETRE_TYPE_LOT).getSingleResult();
        return valeur == null ? null : valeur.toString().toLowerCase();
    }

    /** Société → programme ({@code CfgProject}) → tranche : la société de la tranche est celle de son programme. */
    public UUID trancheDeSociete(String code, UUID societeId) {
        UUID programme = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO CfgProject (PKey, Code, Description, CfgCompanyId) VALUES (:id, :code, :code, :societe)")
                .setParameter(ID, programme).setParameter(P_CODE, "PG-" + code).setParameter("societe", societeId));
        UUID tranche = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO CfgTranche (PKey, Code, Description, CfgProjectId) VALUES (:id, :code, :code, :programme)")
                .setParameter(ID, tranche).setParameter(P_CODE, code).setParameter("programme", programme));
        return tranche;
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}

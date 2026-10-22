package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;

/**
 * Études de faisabilité (FEATURE-patrimoine-faisabilite.md). Les noms de routes et de champs ci-dessous sont le
 * contrat du fil, figé par les tests verrouillés (§2.1). Les lignes de paramétrage sont insérées seulement si leur table
 * existe : sur une base sans le schéma 122, l'échec vient de la route, pas du semis.
 */
@Component
public class FixtureFaisabilite {

    // ------------------------------------------------------------------ routes
    public static final String RACINE = "/api/etudes-faisabilite";
    public static final String FICHE = RACINE + "/{id}";
    public static final String CALCUL = RACINE + "/calcul";
    public static final String UNITES = FICHE + "/unites";
    public static final String TRANCHES = FICHE + "/tranches";
    public static final String BLOQUER = FICHE + "/bloquer";
    public static final String DEBLOQUER = FICHE + "/debloquer";
    public static final String CHOIX_TERRAINS = RACINE + "/choix/terrains";
    public static final String CHOIX_TRANCHES = RACINE + "/choix/tranches";

    public static final String REFERENTIELS = "/api/referentiels-admin/{table}";
    public static final String REFERENTIEL = "/api/referentiels-admin/{table}/{id}";
    public static final String SLUG_UNITES_BASE = "unites-de-base";
    public static final String SLUG_FONCTIONS = "fonctions-specifiques";

    // ------------------------------------------------------------------ champs de la fiche (§2.1)
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String DATE_DOCUMENT = "dateDocument";
    public static final String SOCIETE_ID = "societeId";
    public static final String TERRAIN_ID = "terrainId";
    public static final String STATUT_ID = "statutId";
    public static final String SUPERFICIE = "superficie";
    public static final String SUPERFICIE_EQUIPEMENTS = "superficieEquipements";
    public static final String COEF_EQUIPEMENTS = "coefEquipements";
    public static final String COEF_EMPRISE = "coefEmprise";
    public static final String EMPRISE_AU_SOL = "empriseAuSol";
    public static final String CUF = "cuf";
    public static final String PLANCHER_COUVERT = "plancherCouvert";
    public static final String COEF_VENDABLE = "coefVendable";
    public static final String PLANCHER_VENDABLE = "plancherVendable";
    public static final String PRIX_ACHAT_M2 = "prixAchatM2";
    public static final String TAUX_TVA_TERRAIN = "tauxTvaTerrain";
    public static final String TAUX_FRAIS_ENREGISTREMENT = "tauxFraisEnregistrement";
    public static final String TAUX_AUTRES_FRAIS_TERRAIN = "tauxAutresFraisTerrain";
    public static final String COUT_M2_BATI = "coutConstructionM2Bati";
    public static final String COUT_M2_NON_BATI = "coutConstructionM2NonBati";
    public static final String TAUX_AUTRES_FRAIS_EXECUTION = "tauxAutresFraisExecution";

    // Valeurs calculées (bloc A, §2.2)
    public static final String SUPERFICIE_NETTE = "superficieNette";
    public static final String SURFACE_NON_BATIE = "surfaceNonBatie";
    public static final String PRIX_TERRAIN = "prixTerrain";
    public static final String PRIX_TERRAIN_HT = "prixTerrainHt";
    public static final String PRIX_ACHAT_M2_HT = "prixAchatM2Ht";
    public static final String FRAIS_ENREGISTREMENT = "fraisEnregistrement";
    public static final String FRAIS_ENREGISTREMENT_HT = "fraisEnregistrementHt";
    public static final String AUTRES_FRAIS_TERRAIN = "autresFraisTerrain";
    public static final String AUTRES_FRAIS_TERRAIN_HT = "autresFraisTerrainHt";
    public static final String AUTRES_FRAIS_TERRAIN_TVA = "autresFraisTerrainTva";
    public static final String COUT_TOTAL_TERRAIN = "coutTotalTerrain";
    public static final String COUT_BATI = "coutBati";
    public static final String COUT_NON_BATI = "coutNonBati";
    public static final String AUTRES_FRAIS_EXECUTION = "autresFraisExecution";
    public static final String ERREURS = "erreurs";
    public static final String PREVISIONS_A_RECALCULER = "previsionsARecalculer";

    // Phases (§2.2) : début, durée (mois), fin ; délais entre phases (mois)
    public static final String DEBUT_ACHAT_TERRAIN = "debutAchatTerrain";
    public static final String DUREE_ACHAT_TERRAIN = "dureeAchatTerrain";
    public static final String FIN_ACHAT_TERRAIN = "finAchatTerrain";
    public static final String DEBUT_PERIODE_VALIDATION = "debutPeriodeValidation";
    public static final String DUREE_PERIODE_VALIDATION = "dureePeriodeValidation";
    public static final String FIN_PERIODE_VALIDATION = "finPeriodeValidation";
    public static final String DEBUT_PERMIS_BATIR = "debutPermisBatir";
    public static final String DUREE_PERMIS_BATIR = "dureePermisBatir";
    public static final String FIN_PERMIS_BATIR = "finPermisBatir";
    public static final String DEBUT_ETUDES = "debutEtudes";
    public static final String DUREE_ETUDES = "dureeEtudes";
    public static final String FIN_ETUDES = "finEtudes";
    public static final String DEBUT_TRAVAUX = "debutTravaux";
    public static final String DUREE_TRAVAUX = "dureeTravaux";
    public static final String FIN_TRAVAUX = "finTravaux";
    public static final String DEBUT_CONCRETISATION = "debutConcretisation";
    public static final String DUREE_CONCRETISATION = "dureeConcretisation";
    public static final String FIN_CONCRETISATION = "finConcretisation";
    public static final String DEBUT_FINALISATION = "debutFinalisation";
    public static final String DUREE_FINALISATION = "dureeFinalisation";
    public static final String FIN_FINALISATION = "finFinalisation";
    public static final String DELAI_ACHAT_VALIDATION = "delaiAchatTerrainPeriodeValidation";
    public static final String DELAI_VALIDATION_PERMIS = "delaiPeriodeValidationPermisBatir";
    public static final String DELAI_PERMIS_ETUDES = "delaiPermisBatirEtudes";
    public static final String DELAI_PERMIS_TRAVAUX = "delaiPermisBatirTravaux";
    public static final String DELAI_TRAVAUX_CONCRETISATION = "delaiTravauxConcretisation";
    public static final String DELAI_TRAVAUX_FINALISATION = "delaiTravauxFinalisation";

    // Grilles (§2.4)
    public static final String ID = "id";
    public static final String SORTING = "sorting";
    public static final String UNITE_BASE_ID = "uniteBaseId";
    public static final String QUANTITE = "quantite";
    public static final String TRANCHE_ID = "trancheId";
    public static final String POIDS = "poids";
    public static final String UNITE_ETUDE_ID = "uniteEtudeId";
    public static final String UNITES_DE_TRANCHE = "unites";

    // Configuration (§3)
    public static final String FONCTION_SPECIFIQUE_ID = "fonctionSpecifiqueId";
    public static final String CATEGORIE_ARTICLE_ID = "categorieArticleId";
    public static final String EST_SURFACE = "estSurface";
    public static final String EST_TOTAL = "estTotal";
    public static final String SURFACE_VENDABLE = "surfaceVendable";
    public static final String F_PLANCHER_COUVERT = "plancherCouvert";
    public static final String SURFACE_NETTE = "surfaceNette";
    public static final String SURFACE_BRUTE = "surfaceBrute";
    public static final String F_EMPRISE_AU_SOL = "empriseAuSol";

    // ------------------------------------------------------------------ messages Verif (§2.5, recopiés d'OMRAN)
    private static final String PREFIXE_UNITES = "Veuillez vérifier les quantités des unités de base ; Il faut que la somme des surfaces ";
    public static final String MSG_VENDABLES = PREFIXE_UNITES + "vendables soit égale au plancher vendable.";
    public static final String MSG_BRUT = PREFIXE_UNITES + "brut soit égale au supérfécie terrain.";
    public static final String MSG_NETTE = PREFIXE_UNITES + "nette soit égale au total net.";
    public static final String MSG_EMPRISE = PREFIXE_UNITES
            + "Emprise au sol Batiments(m²) soit égale au Emprise au sol Batiments(m²) totale.";
    public static final String MSG_COUVERTS = PREFIXE_UNITES + "couverts soit égale au plancher couvert.";
    public static final String MSG_TRANCHES = "Veuillez vérifier la ventilation des tranches!";
    public static final String MSG_BLOQUEE = "Étude bloquée";

    public static String msgVentilation(String codeUnite) {
        return "Veuillez vérifier la ventilation de l'unité de base: '" + codeUnite + "' sur les tranches!";
    }

    // ------------------------------------------------------------------ PKey de production (§2.1)
    public static final UUID ETAT_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    public static final UUID STATUT_EN_ETUDE = UUID.fromString("E3D4103F-CD33-4BE7-80C0-9D9F1D805A17");
    public static final UUID ENTITE_ETUDE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B900");

    private static final String P_ID = "id";

    private final EntityManager entityManager;

    public FixtureFaisabilite(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static String jeton() {
        return "P8A" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ paramétrage de production

    /** StrEntity de l'étude, état « Nouveau » et statut « En étude » (départ du workflow), créés si absents. */
    public void parametrage() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'StkFeasibilityStudy', N'Etude de faisabilité')""")
                .setParameter(P_ID, ENTITE_ETUDE));
        executer(entityManager.createNativeQuery("""
                        IF OBJECT_ID('dbo.AdmWorkFlowStateType', 'U') IS NOT NULL
                        BEGIN
                            IF NOT EXISTS (SELECT 1 FROM AdmWorkFlowStateType WHERE PKey = :id)
                                INSERT INTO AdmWorkFlowStateType (PKey, Code, Description) VALUES (:id, N'Nouveau', N'Nouveau')
                        END""")
                .setParameter(P_ID, ETAT_NOUVEAU));
        executer(entityManager.createNativeQuery("""
                        IF OBJECT_ID('dbo.StkFeasibilityStudyStatus', 'U') IS NOT NULL
                        BEGIN
                            IF NOT EXISTS (SELECT 1 FROM StkFeasibilityStudyStatus WHERE PKey = :id)
                                INSERT INTO StkFeasibilityStudyStatus (PKey, Code, Description, Sorting, AdmWorkFlowStateTypeId)
                                VALUES (:id, N'En étude', N'En étude', 1, :etat)
                        END""")
                .setParameter(P_ID, STATUT_EN_ETUDE).setParameter("etat", ETAT_NOUVEAU));
    }

    // ------------------------------------------------------------------ lignes liées à une étude (P8b)

    public UUID prevision(UUID etudeId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO FSPrevision (PKey, Amount, FeasibilityStudyId) VALUES (:id, 1000, :etude)""")
                .setParameter(P_ID, id).setParameter("etude", etudeId));
        return id;
    }

    public UUID financement(UUID etudeId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO FSFunding (PKey, amount, FeasibilityStudyId) VALUES (:id, 1000, :etude)""")
                .setParameter(P_ID, id).setParameter("etude", etudeId));
        return id;
    }

    /** Écrit une colonne du bilan (lecture seule côté API) comme l'aurait fait OMRAN. */
    public void chiffreAffairesStocke(UUID etudeId, BigDecimal montant) {
        executer(entityManager.createNativeQuery("UPDATE StkFeasibilityStudy SET Turnover = :montant WHERE PKey = :id")
                .setParameter("montant", montant).setParameter(P_ID, etudeId));
    }

    // ------------------------------------------------------------------ lecture

    public Tuple etude(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return (Tuple) entityManager.createNativeQuery("SELECT * FROM StkFeasibilityStudy WHERE PKey = :id", Tuple.class)
                .setParameter(P_ID, id).getSingleResult();
    }

    /** {@code :id} lié ; le reste du SQL est écrit par le test. */
    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    /** {@code :id} lié ; le reste du SQL est écrit par le test. */
    public List<Tuple> lignes(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(P_ID, id).getResultList();
    }

    /** PKey de la ligne d'unité de l'étude portant cette unité de base. */
    public UUID ligneUnite(UUID etudeId, UUID uniteBaseId) {
        entityManager.flush();
        Object v = entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), PKey) FROM StkFSBaseUnitStkFeasibilityStudy
                        WHERE FeasibilityStudyId = :id AND StkFSBaseUnitId = :unite""")
                .setParameter(P_ID, etudeId).setParameter("unite", uniteBaseId).getSingleResult();
        return UUID.fromString(v.toString());
    }

    /** PKey de la tranche de l'étude portant ce code. */
    public UUID ligneTranche(UUID etudeId, String code) {
        entityManager.flush();
        Object v = entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), PKey) FROM StkFeasibilityStudyCfgTranche
                        WHERE FeasibilityStudyId = :id AND Code = :code""")
                .setParameter(P_ID, etudeId).setParameter("code", code).getSingleResult();
        return UUID.fromString(v.toString());
    }

    public List<Tuple> historique(UUID etudeId) {
        entityManager.flush();
        return entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), StkFeasibilityStudyStatusId) AS statut,
                               CONVERT(varchar(36), AdmUserId) AS utilisateur, DocDate
                        FROM StkFeasibilityStudyStatusHistory WHERE StkFeasibilityStudyId = :id""", Tuple.class)
                .setParameter(P_ID, etudeId).getResultList();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }

    // ------------------------------------------------------------------ corps JSON

    /** Fiche minimale valide : code, description, date, société. */
    public static CorpsJson fiche(String code, UUID societeId) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Etude " + code)
                .avec(DATE_DOCUMENT, "2026-10-01T00:00:00").avec(SOCIETE_ID, societeId);
    }

    /** Entrées de l'étude YESTEST (§2.2, valeurs de contrôle) par les coefficients. */
    public static CorpsJson yestest(CorpsJson corps) {
        return corps.avec(SUPERFICIE, new BigDecimal("1000")).avec(COEF_EQUIPEMENTS, new BigDecimal("0.01"))
                .avec(COEF_EMPRISE, new BigDecimal("0.3")).avec(CUF, new BigDecimal("3"))
                .avec(COEF_VENDABLE, new BigDecimal("1")).avec(PRIX_ACHAT_M2, new BigDecimal("4500"))
                .avec(TAUX_TVA_TERRAIN, BigDecimal.ZERO)
                .avec(TAUX_FRAIS_ENREGISTREMENT, new BigDecimal("0.02"))
                .avec(TAUX_AUTRES_FRAIS_TERRAIN, new BigDecimal("0.03"));
    }

    /** {@code {"lignes":[…]}} à partir de lignes déjà sérialisées (les tranches portent un tableau imbriqué). */
    public static String lignes(String... lignes) {
        return "{\"lignes\":[" + String.join(",", lignes) + "]}";
    }

    public static String lignes(CorpsJson... lignes) {
        return lignes(Arrays.stream(lignes).map(CorpsJson::json).toArray(String[]::new));
    }

    /** Ligne de tranche avec ses unités ventilées ({@code unites:[…]}). */
    public static String tranche(CorpsJson ligne, CorpsJson... unites) {
        String json = ligne.json();
        String debut = json.substring(0, json.length() - 1);
        String separateur = "{".equals(debut) ? "" : ",";
        String tableau = Arrays.stream(unites).map(CorpsJson::json).collect(Collectors.joining(",", "[", "]"));
        return debut + separateur + "\"" + UNITES_DE_TRANCHE + "\":" + tableau + "}";
    }

    public static CorpsJson ligneTrancheCorps(String code, int sorting, UUID trancheId, BigDecimal poids) {
        return corps().avec(CODE, code).avec(SORTING, sorting).avec(TRANCHE_ID, trancheId).avec(POIDS, poids);
    }

    public static CorpsJson ventilation(UUID uniteEtudeId, BigDecimal quantite) {
        return corps().avec(UNITE_ETUDE_ID, uniteEtudeId).avec(QUANTITE, quantite).avec(POIDS, BigDecimal.ZERO);
    }

    public static CorpsJson ligneUniteCorps(int sorting, UUID uniteBaseId, BigDecimal quantite) {
        return corps().avec(SORTING, sorting).avec(UNITE_BASE_ID, uniteBaseId).avec(QUANTITE, quantite);
    }
}

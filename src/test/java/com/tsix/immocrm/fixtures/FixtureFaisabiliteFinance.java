package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.FICHE;

/**
 * Études de faisabilité, lot P8b-1 (FEATURE-patrimoine-faisabilite-workflow.md §1-3) : routes et noms du fil du
 * financement, des prévisions et de la trésorerie, catégories de trésorerie de test et lecture des lignes dérivées.
 */
@Component
public class FixtureFaisabiliteFinance {

    // ------------------------------------------------------------------ routes (§1.2, §2.3, §2.6)
    public static final String FINANCEMENTS = FICHE + "/financements";
    public static final String ECHEANCIER = FINANCEMENTS + "/echeancier";
    public static final String PREVISIONS = FICHE + "/previsions";
    public static final String CALCUL_PREVISIONS = PREVISIONS + "/calcul";
    public static final String TRESORERIE = FICHE + "/tresorerie";
    public static final String CALCUL_TRESORERIE = TRESORERIE + "/calcul";

    // ------------------------------------------------------------------ financement (§1.2)
    public static final String DATE_DEBLOCAGE = "dateDeblocage";
    public static final String MONTANT = "montant";
    public static final String NOMBRE_REMBOURSEMENTS = "nombreRemboursements";
    public static final String PERIODICITE_MOIS = "periodiciteMois";
    public static final String PERIODE_GRACE_MOIS = "periodeGraceMois";
    public static final String TAUX_CHARGE = "tauxCharge";
    public static final String MONTANT_A_REGLER = "montantARegler";
    public static final String TOTAL_INTERETS = "totalInterets";
    public static final String ECART_BASE = "ecartBase";
    public static final String REMBOURSEMENTS = "remboursements";
    public static final String DATE = "date";
    public static final String MONTANT_BASE = "montantBase";
    public static final String TAUX_INTERET = "tauxInteret";
    public static final String MONTANT_INTERETS = "montantInterets";

    // ------------------------------------------------------------------ prévisions (§2.3)
    public static final String CATEGORIE_ID = "categorieId";
    public static final String CATEGORIE_REFERENCE_ID = "categorieReferenceId";
    public static final String RATIO = "ratio";
    public static final String MONTANT_HT = "montantHt";
    public static final String TAUX_TVA = "tauxTva";
    public static final String EST_POUR_PROJET = "estPourProjet";
    public static final String DATE_APPLICATION = "dateApplication";
    public static final String VERROUILLEE = "verrouillee";
    public static final String INDIVISE = "indivise";
    public static final String VENTILATION = "ventilation";

    // ------------------------------------------------------------------ entrées de l'étude (§1.3) et existantes (P8a)
    public static final String POURCENTAGE_CONCRETISATION = "pourcentageConcretisation";
    public static final String TAUX_IS = "tauxIs";
    public static final String PRIX_VENTE_M2 = "prixVenteM2";
    public static final String NOMBRE_ARTICLES = "nombreArticlesAVendre";
    public static final String ECART_TYPE_TRAVAUX = "ecartTypeTravaux";
    public static final String ECART_TYPE_CONCRETISATION = "ecartTypeConcretisation";
    public static final String ECART_TYPE_FINALISATION = "ecartTypeFinalisation";
    public static final String TAUX_RETENUE = "tauxRetenueGarantie";
    public static final String TAUX_APPLICATION_RETENUE = "tauxApplicationRetenueGarantie";
    public static final String DELAI_RETOUR = "delaiRetourJours";
    public static final String PROPORTION_TERRAIN = "proportionTerrainPourTva";
    public static final String AVEC_FINANCEMENT = "avecFinancement";

    // ------------------------------------------------------------------ messages OMRAN (§2.3, §2.5)
    public static final String MSG_HORS_BILAN = "Vous ne pouvez pas selectionnez une catégorie non définie dans le bilan!";
    public static final String MSG_HIERARCHIE =
            "Vous ne pouvez pas selectionnez cette catégorie possédant une relation hiérarchique avec la catégorie courante!";
    public static final String MSG_BOUCLE = "Vous ne pouvez pas selectionnez cette catégorie car elle crée une boucle infini!";
    public static final String MSG_LIGNE_UTILISEE = "Vous ne pouvez pas supprimer cette ligne!";

    public static String msgEcartType(int mois) {
        return "la valeur de l'écart type doit être entre 0.5 et 1 pour un nombre de mois egal à " + mois;
    }

    // ------------------------------------------------------------------ PKey de production (§2.1, §0 point 3)
    public static final UUID RACINE_DEPENSE = UUID.fromString("27416223-1A88-4AF7-8532-AD53BCA65B7E");
    public static final UUID RACINE_RECETTE = UUID.fromString("273B2C2D-BDFA-4541-94F7-C708A0E79B2C");
    public static final UUID RELATION_PRIX_TERRAIN = UUID.fromString("DFBB5301-F563-4D18-B2D8-B4A9C9F9B001");
    public static final UUID RELATION_ENREGISTREMENT = UUID.fromString("00580DEE-CB36-48E9-BA13-649D53C97002");
    public static final UUID RELATION_AUTRES_FRAIS = UUID.fromString("9CBF361E-B5C3-40A9-81E9-C75BBB9C7003");
    public static final UUID RELATION_INTERETS = UUID.fromString("FE642B2E-78D9-45DB-8706-7104CB6B8004");

    private static final String P_ID = "id";
    private static final String P_ETUDE = "etude";

    private final EntityManager entityManager;

    public FixtureFaisabiliteFinance(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------ catégories de trésorerie (StlCategory)

    /** Les deux racines de production « Dépense » et « Recette », créées si absentes (yesRemsTest et CI n'en ont aucune). */
    public void racines() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StlCategory WHERE PKey = :depense)
                            INSERT INTO StlCategory (PKey, Code, Description, IsCredit, ParentId, IsWithoutPrevision, Sorting)
                            VALUES (:depense, N'centre', N'Dépense', 0, NULL, 0, 0);
                        IF NOT EXISTS (SELECT 1 FROM StlCategory WHERE PKey = :recette)
                            INSERT INTO StlCategory (PKey, Code, Description, IsCredit, ParentId, IsWithoutPrevision, Sorting)
                            VALUES (:recette, N'Recette', N'Recette', 1, NULL, 0, 0)""")
                .setParameter("depense", RACINE_DEPENSE).setParameter("recette", RACINE_RECETTE));
    }

    /** Catégorie de test sous {@code parentId} ; le côté (recette) suit la racine. */
    public UUID categorie(String code, UUID parentId, boolean recette, boolean sansPrevision) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StlCategory (PKey, Code, Description, IsCredit, ParentId, IsWithoutPrevision, Sorting)
                        VALUES (:id, :code, :code, :credit, :parent, :sans, 1)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("credit", recette)
                .setParameter("parent", parentId).setParameter("sans", sansPrevision));
        return id;
    }

    public UUID depense(String code, UUID parentId) {
        return categorie(code, parentId, false, false);
    }

    public UUID recette(String code, UUID parentId) {
        return categorie(code, parentId, true, false);
    }

    /**
     * Arbre de test : Dépense › Travaux › Gros oeuvres ; Dépense › Honoraires › Architecte ; Dépense › Divers ;
     * Dépense › Sans prévision (IsWithoutPrevision) ; Recette › Ventes.
     */
    public record Categories(UUID travaux, UUID grosOeuvres, UUID honoraires, UUID architecte, UUID divers,
                             UUID sansPrevision, UUID ventes) {
    }

    public Categories categoriesDeTest(String jeton) {
        racines();
        UUID travaux = depense("TRA-" + jeton, RACINE_DEPENSE);
        UUID honoraires = depense("HON-" + jeton, RACINE_DEPENSE);
        return new Categories(travaux, depense("GO-" + jeton, travaux), honoraires, depense("ARC-" + jeton, honoraires),
                depense("DIV-" + jeton, RACINE_DEPENSE), categorie("SP-" + jeton, RACINE_DEPENSE, false, true),
                recette("VEN-" + jeton, RACINE_RECETTE));
    }

    /** Lie une des 4 relations « avec les autres modules » à une catégorie (dans la transaction du test : annulé ensuite). */
    public void relier(UUID relationId, UUID categorieId) {
        executer(entityManager.createNativeQuery("""
                        UPDATE StlSettlementRelation SET StlCategoryId = :categorie WHERE PKey = :id;
                        IF @@ROWCOUNT = 0
                            INSERT INTO StlSettlementRelation (PKey, Description, StlCategoryId) VALUES (:id, N'Relation de test', :categorie)""")
                .setParameter(P_ID, relationId).setParameter("categorie", categorieId));
    }

    // ------------------------------------------------------------------ lecture des lignes écrites

    /** Lignes de trésorerie stockées, par type puis date. */
    public List<Tuple> tresorerie(UUID etudeId) {
        return lire("""
                SELECT PrevisionType AS type, DocDate, DateMonth, DateYear, Amount, IsCredit, AmountHT, VatRatio, Notes,
                       CONVERT(varchar(36), StkFeasibilityStudyCfgTrancheId) AS tranche
                FROM StkFSPrevisionLine WHERE StkFeasibilityStudyId = :etude ORDER BY PrevisionType ASC, DocDate ASC""", etudeId);
    }

    public List<Tuple> tresorerie(UUID etudeId, String type) {
        return tresorerie(etudeId).stream().filter(t -> type.equals(t.get("type"))).toList();
    }

    /** Ligne FSPrevision de la catégorie (clé fonctionnelle étude + catégorie). */
    public Tuple prevision(UUID etudeId, UUID categorieId) {
        List<Tuple> lignes = lire("""
                SELECT CONVERT(varchar(36), PKey) AS id, Amount, AmountHT, Ratio, VatRatio, GlobalRatio, Note, Undivided,
                       ApplyOnDate, IsForProject, CONVERT(varchar(36), UnityId) AS unite,
                       CONVERT(varchar(36), ParentId) AS reference, CONVERT(varchar(36), PrjMarketPrevisionModelId) AS modele
                FROM FSPrevision WHERE FeasibilityStudyId = :etude AND StlCategoryId = :categorie""", etudeId, categorieId);
        return lignes.isEmpty() ? null : lignes.get(0);
    }

    /** Lignes par tranche d'une catégorie. */
    public List<Tuple> parTranche(UUID etudeId, UUID categorieId) {
        return lire("""
                SELECT CONVERT(varchar(36), StkTrancheId) AS tranche, AmountHT, Amount, VatRatio, Ratio, Undivided
                FROM StkFSPrevisionStkTranche WHERE FeasibilityStudyId = :etude AND StlCategoryId = :categorie
                ORDER BY AmountHT ASC""", etudeId, categorieId);
    }

    public BigDecimal somme(String sql, UUID etudeId) {
        entityManager.flush();
        Object v = entityManager.createNativeQuery(sql).setParameter(P_ETUDE, etudeId).getSingleResult();
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private List<Tuple> lire(String sql, UUID etudeId) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(P_ETUDE, etudeId).getResultList();
    }

    private List<Tuple> lire(String sql, UUID etudeId, UUID categorieId) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(P_ETUDE, etudeId)
                .setParameter("categorie", categorieId).getResultList();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }

    // ------------------------------------------------------------------ corps JSON

    public static CorpsJson credit(String dateDeblocage, String montant, int nombre, int periodicite, int grace, String taux) {
        return corps().avec(DATE_DEBLOCAGE, dateDeblocage + "T00:00:00").avec(MONTANT, new BigDecimal(montant))
                .avec(NOMBRE_REMBOURSEMENTS, nombre).avec(PERIODICITE_MOIS, periodicite).avec(PERIODE_GRACE_MOIS, grace)
                .avec(TAUX_CHARGE, new BigDecimal(taux));
    }

    public static CorpsJson remboursement(String date, String base, String taux) {
        return corps().avec(DATE, date + "T00:00:00").avec(MONTANT_BASE, new BigDecimal(base))
                .avec(TAUX_INTERET, new BigDecimal(taux));
    }

    /** Ligne de prévision par unité : base = quantité de la ligne d'unité de l'étude, {@code ratio} = prix unitaire. */
    public static CorpsJson parUnite(UUID categorieId, UUID uniteEtudeId, String ratio, String tva) {
        return corps().avec(CATEGORIE_ID, categorieId).avec(FixtureFaisabilite.UNITE_ETUDE_ID, uniteEtudeId)
                .avec(RATIO, new BigDecimal(ratio)).avec(TAUX_TVA, new BigDecimal(tva));
    }

    /** Ligne de prévision en pourcentage du total (enfants compris) d'une autre ligne. */
    public static CorpsJson enPourcentage(UUID categorieId, UUID referenceId, String ratio, String tva) {
        return corps().avec(CATEGORIE_ID, categorieId).avec(CATEGORIE_REFERENCE_ID, referenceId)
                .avec(RATIO, new BigDecimal(ratio)).avec(TAUX_TVA, new BigDecimal(tva));
    }

    /** Phases de l'étude : toutes à {@code debut}, durées nulles sauf celles données (mois), délais nuls. */
    public static CorpsJson phases(CorpsJson corps, String debut, int travaux, int concretisation, int finalisation) {
        return corps.avec(FixtureFaisabilite.DEBUT_ACHAT_TERRAIN, debut + "T00:00:00")
                .avec(FixtureFaisabilite.DUREE_ACHAT_TERRAIN, 0).avec(FixtureFaisabilite.DUREE_PERIODE_VALIDATION, 0)
                .avec(FixtureFaisabilite.DUREE_PERMIS_BATIR, 0).avec(FixtureFaisabilite.DUREE_ETUDES, 0)
                .avec(FixtureFaisabilite.DUREE_TRAVAUX, travaux).avec(FixtureFaisabilite.DUREE_CONCRETISATION, concretisation)
                .avec(FixtureFaisabilite.DUREE_FINALISATION, finalisation)
                .avec(FixtureFaisabilite.DELAI_ACHAT_VALIDATION, 0).avec(FixtureFaisabilite.DELAI_VALIDATION_PERMIS, 0)
                .avec(FixtureFaisabilite.DELAI_PERMIS_ETUDES, 0).avec(FixtureFaisabilite.DELAI_PERMIS_TRAVAUX, 0)
                .avec(FixtureFaisabilite.DELAI_TRAVAUX_CONCRETISATION, 0)
                .avec(FixtureFaisabilite.DELAI_TRAVAUX_FINALISATION, 0);
    }
}

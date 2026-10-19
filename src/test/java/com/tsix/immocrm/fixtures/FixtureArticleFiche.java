package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lignes du lot P2a (FEATURE-patrimoine-article-fiche.md §1-§3), en SQL natif : les tests ne dépendent pas des
 * entités que le lot crée. Flush avant chaque ordre : la CI porte les FK, le parent doit être écrit.
 */
@Component
public class FixtureArticleFiche {

    public static final String ORIENTATION = "StkOrientation";
    public static final String CATEGORIE_CHARGE = "StkCostCategory";
    public static final String FACADE = "StkFacade";
    public static final String VUE = "StkVue";

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String ARTICLE = "article";
    private static final String CONTENANCE = "contenance";
    private static final String NOMBRE = "nombre";
    private static final String OPTION = "option";
    private static final String QUANTITE = "quantite";

    private final EntityManager entityManager;

    public FixtureArticleFiche(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------ référentiels (Code, Description)

    /** {@code table} : une des constantes de la classe. */
    public UUID reference(String table, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO " + table + " (PKey, Code, Description) VALUES (:id, :code, :description)")
                .setParameter(ID, id).setParameter(CODE, code).setParameter("description", libelle(code)));
        return id;
    }

    public static String libelle(String code) {
        return "Libelle " + code;
    }

    /** Contenance à une option ; {@code option1} vide = option 1 grisée (NULL, comme la plupart des lignes de prod). */
    public UUID contenance(String code, String option1, boolean option1AvecQuantite) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkContenance (PKey, Code, Description, Sorting, Option1, IsOption1WithQuantity,
                            Option2, IsOption2WithQuantity, Option3, IsOption3WithQuantity)
                        VALUES (:id, :code, :description, 1, NULLIF(:option, ''), :quantite, NULL, 0, NULL, 0)""")
                .setParameter(ID, id).setParameter(CODE, code).setParameter("description", libelle(code))
                .setParameter(OPTION, option1).setParameter(QUANTITE, option1AvecQuantite));
        return id;
    }

    public UUID presentation(String code, UUID trancheId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkPresentationType (PKey, Code, Description, StkHierarchyId)
                        VALUES (:id, :code, :description, :tranche)""")
                .setParameter(ID, id).setParameter(CODE, code).setParameter("description", libelle(code))
                .setParameter("tranche", trancheId));
        return id;
    }

    /** Ligne modèle d'un type de présentation, recopiée sur l'article au changement de présentation. */
    public UUID contenanceDePresentation(UUID presentationId, UUID contenanceId, int nombre, String option1,
                                         boolean avecOption1, int quantiteOption1) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkPresentationTypeContenance (PKey, StkPresentationTypeId, StkContenanceId, ItemsNumber,
                            Option1, IsWithOption1, QuantityOption1)
                        VALUES (:id, :presentation, :contenance, :nombre, :option, :avec, :quantite)""")
                .setParameter(ID, id).setParameter("presentation", presentationId).setParameter(CONTENANCE, contenanceId)
                .setParameter(NOMBRE, nombre).setParameter(OPTION, option1).setParameter("avec", avecOption1)
                .setParameter(QUANTITE, quantiteOption1));
        return id;
    }

    // ------------------------------------------------------------------ lignes de l'article

    public UUID contenanceArticle(UUID articleId, UUID contenanceId, int nombre) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkItemContenance (PKey, StkItemId, StkContenanceId, ItemsNumber)
                        VALUES (:id, :article, :contenance, :nombre)""")
                .setParameter(ID, id).setParameter(ARTICLE, articleId).setParameter(CONTENANCE, contenanceId)
                .setParameter(NOMBRE, nombre));
        return id;
    }

    public UUID vueArticle(UUID articleId, UUID vueId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkItemVue (PKey, StkItemId, StkVueId) VALUES (:id, :article, :vue)""")
                .setParameter(ID, id).setParameter(ARTICLE, articleId).setParameter("vue", vueId));
        return id;
    }

    /** Tracé de plan de l'article (StkHierarchyPath, sans étage). */
    public UUID cheminPlan(UUID articleId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkHierarchyPath (PKey, SvgPath, StkItemId) VALUES (:id, 'M0 0', :article)""")
                .setParameter(ID, id).setParameter(ARTICLE, articleId));
        return id;
    }

    // ------------------------------------------------------------------ dossiers et références vers l'article

    /** Prospection qui cite l'article (ComProspection.StkItemId, FK NO ACTION en prod). */
    public UUID prospection(String code, UUID articleId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO ComProspection (PKey, Code, StkItemId) VALUES (:id, :code, :article)")
                .setParameter(ID, id).setParameter(CODE, code).setParameter(ARTICLE, articleId));
        return id;
    }

    /** Dossier de location réel : StkItem.RntFolderId porte une FK en CI. */
    public UUID location(String code) {
        return dossierMinimal("INSERT INTO RntFolder (PKey, Code) VALUES (:id, :code)", code);
    }

    /** Dossier de syndic réel : StkItem.SynFolderId porte une FK en CI. */
    public UUID syndic(String code) {
        return dossierMinimal("INSERT INTO SynFolder (PKey, Code) VALUES (:id, :code)", code);
    }

    private UUID dossierMinimal(String sql, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(sql).setParameter(ID, id).setParameter(CODE, code));
        return id;
    }

    /** Pose une colonne dossier ({@code RntFolderId}, {@code SynFolderId}…) sur l'article. */
    public void occuper(UUID articleId, String colonne, UUID dossierId) {
        executer(entityManager.createNativeQuery("UPDATE StkItem SET " + colonne + " = :dossier WHERE PKey = :id")
                .setParameter("dossier", dossierId).setParameter(ID, articleId));
    }

    public void surfaces(UUID articleId, BigDecimal area, BigDecimal equipements, boolean titre) {
        executer(entityManager.createNativeQuery("""
                        UPDATE StkItem SET Area = :area, AreaFloor = :area, EquipementsArea = :equipements, IsTitled = :titre
                        WHERE PKey = :id""")
                .setParameter("area", area).setParameter("equipements", equipements).setParameter("titre", titre)
                .setParameter(ID, articleId));
    }

    public void habilitationEntite(UUID admUserId, UUID entiteId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgCompanyId) VALUES (:id, :user, :entite)""")
                .setParameter(ID, UUID.randomUUID()).setParameter("user", admUserId).setParameter("entite", entiteId));
    }

    // ------------------------------------------------------------------ lecture

    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    /** Lignes d'une requête à un paramètre {@code :id}. */
    @SuppressWarnings("unchecked")
    public List<Tuple> lignes(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(ID, id).getResultList();
    }

    /** Valeurs d'une colonne uniqueidentifier, en majuscules. */
    @SuppressWarnings("unchecked")
    public List<String> guids(String sql, UUID id) {
        entityManager.flush();
        List<Object> valeurs = entityManager.createNativeQuery(sql).setParameter(ID, id).getResultList();
        return valeurs.stream().map(FixturePatrimoine::guid).toList();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}

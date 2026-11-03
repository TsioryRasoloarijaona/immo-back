package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;

/**
 * Pays, villes et référentiels des tiers (FEATURE-admin-referentiels-tiers.md) en SQL natif : les tests ne dépendent pas
 * des entités que le lot complète. Slugs et noms de champs = contrat du fil ; parents insérés avant les enfants (FK de CI).
 */
@Component
public class FixtureReferentielsTiers {

    public static final String CHOIX = FixtureReferentielsAdmin.RACINE + "/choix/{liste}";
    public static final String TIERS = "TIERS";
    public static final String TYPE_TIERS_ID = "typeTiersId";
    public static final String AUTEUR_OMRAN = "Migration 4.5.56.0";
    public static final int TRI = 7;

    public static final String PAYS = "pays";
    public static final String VILLES = "villes";
    public static final String SEXES = "sexes";
    public static final String TITRES = "titres";
    public static final String FORMES_SOCIALES = "formes-sociales";
    public static final String TYPES_TIERS = "types-tiers";
    public static final String NATURES_TIERS = "natures-tiers";
    public static final String ASSUJETTISSEMENTS = "assujettissements";
    public static final String SITUATIONS_FAMILIALES = "situations-familiales";
    public static final String TYPES_RESIDENCE = "types-residence";
    public static final String REGIMES_MATRIMONIAUX = "regimes-matrimoniaux";
    public static final String TYPES_PIECE_IDENTITE = "types-piece-identite";
    public static final String NATIONALITES = "nationalites";
    public static final String PROFESSIONS = "professions";
    public static final String SALAIRES = "salaires";
    public static final String TYPES_RELATION = "types-relation";

    public static final ListeTiers L_PAYS = new ListeTiers(PAYS, "AdmCountry", false);
    public static final ListeTiers L_VILLES = new ListeTiers(VILLES, "AdmCity", false);
    public static final ListeTiers L_SEXES = new ListeTiers(SEXES, "AdmSexType", true);
    public static final ListeTiers L_TITRES = new ListeTiers(TITRES, "AdmTierTitle", true);
    /** yesRemsTest n'a ni {@code Sorting} ni colonnes d'audit sur {@code AdmSocialForm} (la CI les a). */
    public static final ListeTiers L_FORMES_SOCIALES = new ListeTiers(FORMES_SOCIALES, "AdmSocialForm", false, false);
    public static final ListeTiers L_TYPES_TIERS = new ListeTiers(TYPES_TIERS, "CfgTierType", true);
    public static final ListeTiers L_NATURES_TIERS = new ListeTiers(NATURES_TIERS, "CfgTierKind", true);
    public static final ListeTiers L_SALAIRES = new ListeTiers(SALAIRES, "AdmSalary", true);
    public static final ListeTiers L_TYPES_RELATION = new ListeTiers(TYPES_RELATION, "CfgRelationType", false);

    private static final String P_ID = "id";
    private static final String P_CODE = "code";

    private final EntityManager entityManager;

    public FixtureReferentielsTiers(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Une liste du lot : slug du fil, table OMRAN, présence d'une colonne {@code Sorting} et des colonnes
     * {@code UserCreate*} dans yesRemsTest ET en CI.
     */
    public record ListeTiers(String slug, String table, boolean avecTri, boolean avecTrace) {

        public ListeTiers(String slug, String table, boolean avecTri) {
            this(slug, table, avecTri, true);
        }

        /** Corps de création minimal : code, libellé « Libelle {code} », et l'ordre si la table en a un. */
        public CorpsJson saisie(String code) {
            CorpsJson saisie = corps().avec(CODE, code).avec(DESCRIPTION, "Libelle " + code);
            return avecTri ? saisie.avec(SORTING, TRI) : saisie;
        }

        @Override
        public String toString() {
            return slug;
        }
    }

    /** Les 15 listes modifiables (Types de tiers à part : lecture seule). */
    public static Stream<ListeTiers> modifiables() {
        return Stream.of(L_PAYS, L_VILLES, L_SEXES, L_TITRES, L_FORMES_SOCIALES, L_NATURES_TIERS,
                new ListeTiers(ASSUJETTISSEMENTS, "AdmLiability", true),
                new ListeTiers(SITUATIONS_FAMILIALES, "AdmFamilySituation", true),
                new ListeTiers(TYPES_RESIDENCE, "AdmResidenceType", true),
                new ListeTiers(REGIMES_MATRIMONIAUX, "AdmMatrimonialRegime", true),
                new ListeTiers(TYPES_PIECE_IDENTITE, "AdmIdentityDocumentType", true),
                new ListeTiers(NATIONALITES, "AdmNationality", false),
                new ListeTiers(PROFESSIONS, "AdmProfession", false),
                L_SALAIRES, L_TYPES_RELATION);
    }

    public static Stream<ListeTiers> toutes() {
        return Stream.concat(modifiables(), Stream.of(L_TYPES_TIERS));
    }

    public static Stream<String> slugs() {
        return toutes().map(ListeTiers::slug);
    }

    // ------------------------------------------------------------------ lignes écrites par OMRAN

    /** Ligne déjà en base : Description = « Libelle {code} », ordre {@link #TRI} si la table en a un. */
    public UUID ligne(ListeTiers l, String code) {
        return ligne(l, code, TRI);
    }

    public UUID ligne(ListeTiers l, String code, int tri) {
        UUID id = UUID.randomUUID();
        String colonneTri = l.avecTri() ? "Sorting, " : "";
        String valeurTri = l.avecTri() ? ":tri, " : "";
        String colonnesTrace = l.avecTrace() ? ", UserCreate, UserCreateDateTime" : "";
        String valeursTrace = l.avecTrace() ? ", :auteur, :le" : "";
        Query q = entityManager.createNativeQuery("INSERT INTO " + l.table() + " (PKey, " + colonneTri
                        + "Code, Description" + colonnesTrace + ") VALUES (:id, " + valeurTri
                        + ":code, :description" + valeursTrace + ")")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Libelle " + code);
        if (l.avecTri()) {
            q.setParameter("tri", tri);
        }
        if (l.avecTrace()) {
            q.setParameter("auteur", AUTEUR_OMRAN).setParameter("le", LocalDateTime.of(2020, 1, 2, 3, 4));
        }
        executer(q);
        return id;
    }

    public UUID pays(String code) {
        return ligne(L_PAYS, code);
    }

    /** {@code pays} null : ville sans pays, comme 11 villes de production. */
    public UUID ville(String code, UUID pays) {
        UUID id = ligne(L_VILLES, code);
        definir(L_VILLES, id, "AdmCountryId", pays);
        return id;
    }

    public UUID typeTiers(String code) {
        return ligne(L_TYPES_TIERS, code);
    }

    public UUID nature(String code, UUID typeTiers) {
        UUID id = ligne(L_NATURES_TIERS, code);
        definir(L_NATURES_TIERS, id, "CfgTierTypeId", typeTiers);
        return id;
    }

    public UUID sexe(String code) {
        return ligne(L_SEXES, code);
    }

    public UUID titre(String code, UUID sexe) {
        UUID id = ligne(L_TITRES, code);
        definir(L_TITRES, id, "AdmSexTypeId", sexe);
        return id;
    }

    /** {@code colonne} : nom écrit par le test, jamais une saisie. */
    public void definir(ListeTiers l, UUID id, String colonne, Object valeur) {
        executer(entityManager.createNativeQuery("UPDATE " + l.table() + " SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter("valeur", valeur).setParameter(P_ID, id));
    }

    // ------------------------------------------------------------------ références sans FK ou en CASCADE

    /** Paramètre {@code AdmParamsGuid} propre au test (clé = {@code Code}) pointant vers {@code valeur}. */
    public void parametre(String code, UUID valeur) {
        executer(entityManager.createNativeQuery("INSERT INTO AdmParamsGuid (Code, CodeValue) VALUES (:code, :valeur)")
                .setParameter(P_CODE, code).setParameter("valeur", valeur));
    }

    /** Ville d'une prospection, enfant en CASCADE de {@code AdmCity} ; prospection NULL (colonne nullable). */
    public UUID villeDeProspection(UUID ville) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO ComProspectionCity (PKey, AdmCityId) VALUES (:id, :ville)")
                .setParameter(P_ID, id).setParameter("ville", ville));
        return id;
    }

    /** Lien tiers / contact qui porte un type de relation, sans FK ; tiers et contact NULL. */
    public UUID contactDeRelation(UUID relation) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO CfgTierContact (PKey, CfgRelationTypeId) VALUES (:id, :relation)")
                .setParameter(P_ID, id).setParameter("relation", relation));
        return id;
    }

    /** FK NO_ACTION de CI {@code AdmTierTitle → AdmSexType}, posée si absente (yesRemsTest) ; le rollback la retire. */
    public void cleTitreVersSexe() {
        executer(entityManager.createNativeQuery("""
                IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns k
                        JOIN sys.columns c ON c.object_id = k.parent_object_id AND c.column_id = k.parent_column_id
                        WHERE k.parent_object_id = OBJECT_ID('dbo.AdmTierTitle')
                          AND k.referenced_object_id = OBJECT_ID('dbo.AdmSexType') AND c.name = 'AdmSexTypeId')
                    ALTER TABLE dbo.AdmTierTitle WITH NOCHECK ADD CONSTRAINT AdmTierTitle_AdmSexType_FK_test
                        FOREIGN KEY (AdmSexTypeId) REFERENCES dbo.AdmSexType (PKey)"""));
    }

    // ------------------------------------------------------------------ lecture en base

    /** {@code :id} lié ; le reste du SQL est écrit par le test. */
    public int compter(String sql, UUID id) {
        entityManager.flush();
        return ((Number) Objects.requireNonNull(
                entityManager.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult())).intValue();
    }

    public int compter(String sql) {
        entityManager.flush();
        return ((Number) Objects.requireNonNull(entityManager.createNativeQuery(sql).getSingleResult())).intValue();
    }

    public int lignes(ListeTiers l, UUID id) {
        return compter("SELECT COUNT(*) FROM " + l.table() + " WHERE PKey = :id", id);
    }

    public String texte(ListeTiers l, UUID id, String colonne) {
        return Objects.toString(valeur("SELECT " + colonne + " FROM " + l.table() + " WHERE PKey = :id", id), null);
    }

    public UUID guid(ListeTiers l, UUID id, String colonne) {
        Object v = valeur("SELECT CONVERT(varchar(36), " + colonne + ") FROM " + l.table() + " WHERE PKey = :id", id);
        return v == null ? null : UUID.fromString(v.toString());
    }

    private Object valeur(String sql, UUID id) {
        entityManager.flush();
        return entityManager.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
    }
}

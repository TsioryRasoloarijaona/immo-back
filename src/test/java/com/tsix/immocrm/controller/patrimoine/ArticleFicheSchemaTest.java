package com.tsix.immocrm.controller.patrimoine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import com.tsix.immocrm.fixtures.NommageSchemaCi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma du lot P2a — FEATURE-patrimoine-article-fiche.md §9.3, §7 (ArticleFicheSchemaTest) : les six tables absentes
 * de la CI, créées par 110/111 avec les types de yesRemsProd ; les trois référentiels déjà en CI doivent exister aussi
 * dans la base des tests. Noms de colonnes comparés sans la casse (prod : {@code StkItemContenance.itemsNumber}).
 */
@SpringBootTest
@Transactional
class ArticleFicheSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("110_article-fiche_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("111_article-fiche_fk.sql");
    private static final String UNIQUEIDENTIFIER = "uniqueidentifier";
    private static final String NVARCHAR = "nvarchar";
    private static final String DATETIME = "datetime";
    private static final String BIT = "bit";
    private static final String ENTIER = "int(10,0)";
    private static final String PKEY = "PKey";
    private static final String STK_ITEM_ID = "StkItemId";
    private static final String STK_CONTENANCE_ID = "StkContenanceId";
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String USER_CREATE = "UserCreate";
    private static final String USER_CREATE_DATE = "UserCreateDateTime";
    private static final String USER_UPDATE = "UserUpdate";
    private static final String USER_UPDATE_DATE = "UserUpdateDateTime";
    private static final List<String> NOUVELLES_TABLES = List.of("StkVue", "StkItemVue", "StkContenance",
            "StkItemContenance", "StkPresentationTypeContenance", "StkHierarchyPath");

    @Autowired private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private Map<String, String> colonnes(String table) {
        List<Tuple> lignes = entityManager.createNativeQuery("""
                        SELECT COLUMN_NAME AS colonne,
                               DATA_TYPE + ISNULL('(' + CAST(NUMERIC_PRECISION AS varchar) + ',' + CAST(NUMERIC_SCALE AS varchar) + ')', '')
                                   AS type
                        FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :table""", Tuple.class)
                .setParameter("table", table)
                .getResultList();
        Map<String, String> parNom = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        lignes.forEach(t -> parNom.put((String) t.get("colonne"), (String) t.get("type")));
        return parNom;
    }

    private static String lire(Path fichier) throws IOException {
        return Files.readString(fichier, StandardCharsets.UTF_8);
    }

    private static Map<String, String> trace() {
        return Map.of(USER_CREATE, NVARCHAR, USER_CREATE_DATE, DATETIME, USER_UPDATE, NVARCHAR, USER_UPDATE_DATE, DATETIME);
    }

    // ------------------------------------------------------------------ §9.3 tables présentes (yesRemsTest et CI)

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkVue", "StkOrientation", "StkCostCategory", "StkFacade"})
    void r9_3_referentiel_codeDescriptionEtTrace(String table) {
        assertThat(colonnes(table))
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry(CODE, NVARCHAR)
                .containsEntry(DESCRIPTION, NVARCHAR)
                .containsAllEntriesOf(trace());
    }

    @Test
    void r9_3_contenance_optionsEtQuantites() {
        assertThat(colonnes("StkContenance"))
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry(CODE, NVARCHAR)
                .containsEntry(DESCRIPTION, NVARCHAR)
                .containsEntry("Sorting", ENTIER)
                .containsEntry("Option1", NVARCHAR)
                .containsEntry("IsOption1WithQuantity", BIT)
                .containsEntry("Option2", NVARCHAR)
                .containsEntry("IsOption2WithQuantity", BIT)
                .containsEntry("Option3", NVARCHAR)
                .containsEntry("IsOption3WithQuantity", BIT)
                .containsAllEntriesOf(trace());
    }

    @Test
    void r9_3_vueDeLArticle_colonnesDeProd() {
        assertThat(colonnes("StkItemVue"))
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry(STK_ITEM_ID, UNIQUEIDENTIFIER)
                .containsEntry("StkVueId", UNIQUEIDENTIFIER)
                .containsAllEntriesOf(trace());
    }

    @Test
    void r9_3_contenanceDeLArticle_colonnesDeProd() {
        assertThat(colonnes("StkItemContenance"))
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry(STK_ITEM_ID, UNIQUEIDENTIFIER)
                .containsEntry(STK_CONTENANCE_ID, UNIQUEIDENTIFIER)
                .containsAllEntriesOf(optionsDeLigne())
                .containsAllEntriesOf(trace());
    }

    @Test
    void r9_3_contenanceDuTypeDePresentation_colonnesDeProd() {
        assertThat(colonnes("StkPresentationTypeContenance"))
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry("StkPresentationTypeId", UNIQUEIDENTIFIER)
                .containsEntry(STK_CONTENANCE_ID, UNIQUEIDENTIFIER)
                .containsAllEntriesOf(optionsDeLigne())
                .containsAllEntriesOf(trace());
    }

    /** Nombre et 3 options d'une ligne de contenance (article ou type de présentation). */
    private static Map<String, String> optionsDeLigne() {
        Map<String, String> colonnes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        colonnes.put("ItemsNumber", ENTIER);
        for (int n = 1; n <= 3; n++) {
            colonnes.put("Option" + n, NVARCHAR);
            colonnes.put("IsWithOption" + n, BIT);
            colonnes.put("QuantityOption" + n, ENTIER);
        }
        return colonnes;
    }

    @Test
    void r9_3_traceDePlan_colonnesDeProd() {
        assertThat(colonnes("StkHierarchyPath"))
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry("SvgPath", "text")
                .containsEntry(STK_ITEM_ID, UNIQUEIDENTIFIER)
                .containsEntry("StkFloorId", UNIQUEIDENTIFIER);
    }

    // ------------------------------------------------------------------ §9.3 fichiers de CI 110 / 111

    @Test
    void r9_3_fichier110_creeLesSixTables_idempotent() throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertThat(NOUVELLES_TABLES).isNotEmpty().allSatisfy(table -> assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + table + "]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'(\\[?dbo]?\\.)?\\[?" + table + "]?'")));
    }

    @Test
    void r9_3_fichier111_clesEtrangeresDesLignes_idempotent() throws IOException {
        assertThat(FK).exists();
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?StkItem]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?StkVue]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?StkContenance]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?StkPresentationType]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)(IF\\s+NOT\\s+EXISTS|OBJECT_ID)"));
    }

    @Test
    void r9_3_readmeDuSchemaCi_citeLesFichiers110Et111() throws IOException {
        NommageSchemaCi.verifier(110, "article-fiche");
    }
}

package com.tsix.immocrm.controller.patrimoine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma du lot P8a — FEATURE-patrimoine-faisabilite.md §1 et §7.1 (point 6) : 20 tables créées par
 * {@code 122_faisabilite_tables.sql} (idempotent, applicable à yesRemsTest), FK de production dans
 * {@code 123_faisabilite_fk.sql} (sans {@code PrvSettlementPrevision} ni {@code PrvTurnoverPrevision}).
 */
@SpringBootTest
@Transactional
class FaisabiliteSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("122_faisabilite_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("123_faisabilite_fk.sql");
    private static final String UNIQUEIDENTIFIER = "uniqueidentifier";
    private static final String DECIMAL = "decimal(26,6)";
    private static final String DATETIME = "datetime";
    private static final String ENTIER = "int(10,0)";
    private static final String TABLE = "(\\[?dbo]?\\.)?\\[?";

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
        return lignes.stream().collect(Collectors.toMap(t -> (String) t.get("colonne"), t -> (String) t.get("type")));
    }

    private static String lire(Path fichier) throws IOException {
        return Files.readString(fichier, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ tables présentes, types de prod

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkFeasibilityStudy", "StkFSBaseUnitStkFeasibilityStudy", "StkFeasibilityStudyCfgTranche",
            "StkFeasibilityStudyCfgTrancheStkBaseUnit", "StkFSBaseUnit", "SpecificFunction", "StkFeasibilityStudyStatus",
            "PrvType", "PrvVersion", "FSFunding", "FSReimbursement", "FSPrevision", "StkFSPrevisionLine",
            "StkFSPrevisionStkTranche", "StkFeasibilityStudyStatusHistory", "StkFeasibilityStudyStatusDocumentToAttach",
            "StkFeasibilityStudyWorkFlow", "StkFeasibilityStudyWorkFlowAdmRole", "StkFeasibilityStudyWorkFlowDocumentToAttach",
            "PrjMarketPrevisionModel"})
    void r1_tablePresente_avecSaClePKey(String table) {
        assertThat(colonnes(table)).containsEntry("PKey", UNIQUEIDENTIFIER);
    }

    @Test
    void r1_etude_colonnesEtTypesDeProd() {
        assertThat(colonnes("StkFeasibilityStudy"))
                .hasSize(109)
                .containsEntry("Code", "nvarchar")
                .containsEntry("LandDescription", "varchar")
                .containsEntry("Area", DECIMAL)
                .containsEntry("EquipementsCoef", DECIMAL)
                .containsEntry("AreaNet", DECIMAL)
                .containsEntry("TotalCostLand", DECIMAL)
                .containsEntry("OtherCostsExecutionHT", DECIMAL)
                .containsEntry("otherCostsExecutionRatio", DECIMAL)
                .containsEntry("DocDate", DATETIME)
                .containsEntry("StartDatePurchaseLand", DATETIME)
                .containsEntry("DelayStartDateExpenseStartDateFinalization", ENTIER)
                .containsEntry("BackTimeOnDays", ENTIER)
                .containsEntry("IsLocked", "bit")
                .containsEntry("UseLandProportionForTVACalculation", "bit")
                .containsEntry("StkFeasibilityStudyStatusId", UNIQUEIDENTIFIER)
                .containsEntry("StrEntityId", UNIQUEIDENTIFIER)
                .containsEntry("LndLandId", UNIQUEIDENTIFIER)
                .containsEntry("PrvTypeId", UNIQUEIDENTIFIER)
                .containsEntry("PrvVersionId", UNIQUEIDENTIFIER);
    }

    /** §1 : Code / Description en varchar (pas nvarchar), aucune colonne d'audit. */
    @Test
    void r1_uniteDeBase_varchar_sansAudit() {
        assertThat(colonnes("StkFSBaseUnit"))
                .containsEntry("Code", "varchar")
                .containsEntry("Description", "varchar")
                .containsEntry("IsArea", "bit")
                .containsEntry("SpecificFunctionId", UNIQUEIDENTIFIER)
                .containsEntry("StkItemCategoryId", UNIQUEIDENTIFIER)
                .doesNotContainKeys("UserCreate", "UserUpdate");
    }

    @Test
    void r1_fonctionSpecifique_sixDrapeaux() {
        assertThat(colonnes("SpecificFunction"))
                .containsEntry("IsTotal", "bit")
                .containsEntry("SaleAbleArea", "bit")
                .containsEntry("CoveredArea", "bit")
                .containsEntry("AreaNet", "bit")
                .containsEntry("AreaBrut", "bit")
                .containsEntry("AreaCos", "bit")
                .containsEntry("UserCreate", "nvarchar");
    }

    /** §1 : tranche d'étude sans Description ; ventilation sans Sorting. */
    @Test
    void r1_grilles_colonnesDeProd() {
        assertAll(
                () -> assertThat(colonnes("StkFeasibilityStudyCfgTranche"))
                        .containsEntry("Weight", DECIMAL)
                        .containsEntry("OccupancyPermitsDate", DATETIME)
                        .containsEntry("CfgTrancheId", UNIQUEIDENTIFIER)
                        .doesNotContainKey("Description"),
                () -> assertThat(colonnes("StkFeasibilityStudyCfgTrancheStkBaseUnit"))
                        .containsEntry("StkBaseUnitId", UNIQUEIDENTIFIER)
                        .containsEntry("Quantity", DECIMAL)
                        .containsEntry("Weight", DECIMAL)
                        .doesNotContainKey("Sorting"),
                () -> assertThat(colonnes("StkFSBaseUnitStkFeasibilityStudy"))
                        .containsEntry("Quantity", DECIMAL)
                        .doesNotContainKeys("UserCreate", "Code"),
                () -> assertThat(colonnes("StkFeasibilityStudyStatusHistory"))
                        .containsEntry("AdmUserId", UNIQUEIDENTIFIER)
                        .containsEntry("DocDate", DATETIME)
                        .hasSize(5));
    }

    // ------------------------------------------------------------------ fichiers de CI 122 / 123

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkFeasibilityStudy", "StkFSBaseUnitStkFeasibilityStudy", "StkFeasibilityStudyCfgTranche",
            "StkFeasibilityStudyCfgTrancheStkBaseUnit", "StkFSBaseUnit", "SpecificFunction", "StkFeasibilityStudyStatus",
            "PrvType", "PrvVersion", "FSFunding", "FSReimbursement", "FSPrevision", "StkFSPrevisionLine",
            "StkFSPrevisionStkTranche", "StkFeasibilityStudyStatusHistory", "StkFeasibilityStudyStatusDocumentToAttach",
            "StkFeasibilityStudyWorkFlow", "StkFeasibilityStudyWorkFlowAdmRole", "StkFeasibilityStudyWorkFlowDocumentToAttach",
            "PrjMarketPrevisionModel"})
    void r1_fichier122_creeLaTable_idempotent(String table) throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+" + TABLE + table + "]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'" + TABLE + table + "]?'"));
    }

    /** FK sortantes de l'étude (NO_ACTION) et les 5 entrantes en CASCADE, comme en production. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkFeasibilityStudyStatus", "PrvVersion", "PrvType", "CfgCompany", "StrEntity", "LndLand",
            "StkItem", "StkFeasibilityStudy", "StkFSBaseUnit", "StkFSBaseUnitStkFeasibilityStudy",
            "StkFeasibilityStudyCfgTranche", "SpecificFunction", "StkItemCategory", "CfgTranche", "AdmWorkFlowStateType",
            "AdmUser", "FSFunding", "StlCategory"})
    void r1_fichier123_referenceLeParent(String parent) throws IOException {
        assertThat(FK).exists();

        assertThat(lire(FK)).containsPattern("(?i)REFERENCES\\s+" + TABLE + parent + "]?\\s*\\(");
    }

    @Test
    void r1_fichier123_idempotent_sansPrvSettlementNiPrvTurnover() throws IOException {
        assertThat(FK).exists();
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'" + TABLE + "StkFeasibilityStudy"),
                () -> assertThat(sql).containsPattern("(?i)ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(sql).doesNotContain("PrvSettlementPrevision", "PrvTurnoverPrevision"));
    }

    @Test
    void r1_readmeDuSchemaCi_citeLesFichiers122Et123() throws IOException {
        assertThat(lire(SCHEMA_CI.resolve("README.md")))
                .contains("122_faisabilite_tables.sql", "123_faisabilite_fk.sql");
    }
}

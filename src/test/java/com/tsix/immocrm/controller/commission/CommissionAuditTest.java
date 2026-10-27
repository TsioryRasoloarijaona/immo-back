package com.tsix.immocrm.controller.commission;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.montant;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit {@code AdmWebHistorics} — FEATURE-vente-commissions.md §3.3 (classe {@code Yes.Com.Commission.CommissionLine}, libellés
 * {@code CommissionLine}, sans parent ; {@code Added} détaillé, {@code Modified} sur {@code CommissionAmount}, {@code Deleted}
 * {@code [["CommissionLine","","CommissionLine"]]} ; un refus n'écrit rien), réponse M2 ({@code ComCommissionLine} seul).
 *
 * <p>Sans {@code @Transactional} : la trace n'est écrite qu'à la validation. Données posées en SQL validé, effacées en
 * {@code @AfterEach}. La table {@code ComCommissionLine} est créée dans yesRemsTest si absente (définition de production).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommissionAuditTest {

    private static final Logger log = LoggerFactory.getLogger(CommissionAuditTest.class);

    private static final String TYPE = "Yes.Com.Commission.CommissionLine";
    private static final String LIBELLE = "CommissionLine";
    private static final String ADDED = "Added";
    private static final String MODIFIED = "Modified";
    private static final String DELETED = "Deleted";
    private static final LocalDateTime AVANCE = LocalDateTime.of(2024, 2, 23, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;
    @Autowired private FixtureCommissions fixture;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();
    private final List<UUID> lignesGenerees = new ArrayList<>();

    private JdbcTemplate jdbc;
    private UUID dossier;
    private UUID tranche;
    private UUID role;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        fixture.tables();
        UUID entite = inserer("CfgCompany", "Code, Description, IsActive", "ENT-CAU-" + suffixe, "Entite", true);
        UUID projet = inserer("CfgProject", "Code, Description, CfgCompanyId", "PRJ-CAU-" + suffixe, "Projet", entite.toString());
        tranche = inserer("CfgTranche", "Code, Description, CfgProjectId, IsForSale", "TR-CAU-" + suffixe, "Tranche",
                projet.toString(), true);
        UUID nature = inserer("StkKind", "Code, Description", "K-CAU-" + suffixe, "Nature");
        UUID vocation = inserer("StkVocation", "Code, Description", "V-CAU-" + suffixe, "Vocation");
        UUID type = inserer("StkItemType", "Code, Description, StkKindId, ForUnderItem", "T-CAU-" + suffixe, "Type",
                nature.toString(), false);
        UUID categorie = inserer("StkItemCategory", "Code, Description, StkKindId, ForUnderItem", "C-CAU-" + suffixe,
                "Categorie", nature.toString(), false);
        UUID commercial = inserer("CfgTier", "Code, Description, LastName, IsExtern", "EMP-CAU-" + suffixe, "Employe",
                "CAU-" + suffixe, false);
        dossier = inserer("ComFolder", "Code, StkVocationId, CfgCommercialId, AdvanceDate, PriceAfterDiscount",
                "DOS-CAU-" + suffixe, vocation.toString(), commercial.toString(), Timestamp.valueOf(AVANCE),
                new BigDecimal("775000"));
        UUID article = inserer("StkItem", "Code, Description, StkHierarchyId, StkItemTypeId, StkItemCategoryId, "
                        + "StkVocationId, StkKindId, ComFolderId", "ART-CAU-" + suffixe, "Article", tranche.toString(),
                type.toString(), categorie.toString(), vocation.toString(), nature.toString(), dossier.toString());
        jdbc.update("UPDATE ComFolder SET StkItemId = ? WHERE PKey = ?", article.toString(), dossier.toString());
        inserer("ComSalesCommission", "CfgTrancheId, SaleCommissionBasedOnAmount, SaleCommissionPercent",
                tranche.toString(), false, new BigDecimal("0.0003"));

        role = inserer("AdmRole", "Code, Description", "Commissions CAU-" + suffixe, "Commissions");
        UUID compte = inserer("AdmUser", "Code, Description, Disabled", "com.cau." + suffixe, "Compte CAU", false);
        inserer("AdmUserRole", "AdmUserId, AdmRoleId", compte.toString(), role.toString());
        for (String droit : List.of(FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU, FixtureCommissions.SUPPRIMER)) {
            inserer("AdmRight", "Code, AdmRoleId", droit, role.toString());
        }
        AdmUser principal = new AdmUser();
        principal.setId(compte);
        principal.setCode("com.cau." + suffixe);
        qui = FixtureDossierVente.connecte(principal, FixtureCommissions.ROLE_JWT);
    }

    @AfterEach
    void nettoyer() {
        lignesGenerees.forEach(id -> effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id));
        if (dossier != null) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM ComCommissionLine WHERE ComFolderId = ?)",
                    dossier);
            effacer("DELETE FROM ComCommissionLine WHERE ComFolderId = ?", dossier);
            effacer("UPDATE ComFolder SET StkItemId = NULL WHERE PKey = ?", dossier);
        }
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    /** Nettoyage au mieux : une FK de CI inattendue ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private UUID inserer(String table, String colonnes, Object... valeurs) {
        UUID id = UUID.randomUUID();
        Object[] parametres = new Object[valeurs.length + 1];
        parametres[0] = id.toString();
        System.arraycopy(valeurs, 0, parametres, 1, valeurs.length);
        String marques = String.join(",", Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        creees.push(new String[] {table, id.toString()});
        return id;
    }

    // ------------------------------------------------------------------ outils

    private UUID genererLaLigne() throws Exception {
        mockMvc.perform(generation(tranche, T_CONCRETISATION, List.of(dossier)).with(qui)).andExpect(status().isOk());
        List<String> ids = jdbc.queryForList("SELECT CONVERT(varchar(36), PKey) FROM ComCommissionLine WHERE ComFolderId = ?",
                String.class, dossier.toString());
        assertThat(ids).as("lignes générées").hasSize(1);
        UUID ligne = UUID.fromString(ids.get(0));
        lignesGenerees.add(ligne);
        return ligne;
    }

    private List<Map<String, Object>> traces(UUID objet, String action) {
        return jdbc.queryForList("""
                SELECT Action, ObjectType, ObjectTypeLabel, ObjectLabel, ParentObjectType,
                       CAST(Details AS nvarchar(max)) AS Details
                FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", objet.toString(), action);
    }

    private Map<String, Object> seuleTrace(UUID objet, String action) {
        List<Map<String, Object>> t = traces(objet, action);
        assertThat(t).as("traces %s de %s", action, objet).hasSize(1);
        return t.get(0);
    }

    private static List<List<Object>> details(Map<String, Object> trace) {
        return JsonPath.read((String) trace.get("Details"), "$");
    }

    private static void verifierType(Map<String, Object> trace, String action) {
        assertAll(
                () -> assertThat(trace).containsEntry("Action", action).containsEntry("ObjectType", TYPE)
                        .containsEntry("ObjectTypeLabel", LIBELLE).containsEntry("ObjectLabel", LIBELLE),
                () -> assertThat((String) trace.get("ParentObjectType")).isNullOrEmpty());
    }

    private void documenter(UUID ligne) {
        UUID doc = inserer("BuyDocument", "Code, DocType, ReferenceType", "DOC-CAU-" + suffixe, "INVOICE", "SALE_COMMISSION");
        jdbc.update("UPDATE ComCommissionLine SET BuyDocumentId = ? WHERE PKey = ?", doc.toString(), ligne.toString());
    }

    // ------------------------------------------------------------------ Added, Modified, Deleted

    @Test
    void a33_generation_uneTraceAdded_formatMesure() throws Exception {
        UUID ligne = genererLaLigne();

        Map<String, Object> trace = seuleTrace(ligne, ADDED);
        List<List<Object>> d = details(trace);
        assertAll(
                () -> verifierType(trace, ADDED),
                () -> assertThat(d).anySatisfy(t -> assertThat(t).containsExactly("CommissionType", "", "Concrétisation")),
                () -> assertThat(d).anySatisfy(t -> assertThat(t).containsExactly("DocDate", "", "23/02/2024 00:00:00")),
                () -> assertThat(d).anySatisfy(t -> assertThat(t).first().isEqualTo("CommissionAmount")));
    }

    @Test
    void a33_montantModifie_uneTraceModified_ancienEtNouveau() throws Exception {
        UUID ligne = genererLaLigne();

        mockMvc.perform(montant(ligne, new BigDecimal("250")).with(qui)).andExpect(status().isOk());

        Map<String, Object> trace = seuleTrace(ligne, MODIFIED);
        assertAll(
                () -> verifierType(trace, MODIFIED),
                () -> assertThat(details(trace)).anySatisfy(t -> assertAll(
                        () -> assertThat(t).hasSize(3).first().isEqualTo("CommissionAmount"),
                        () -> assertThat(new BigDecimal(String.valueOf(t.get(1)).replace(',', '.')))
                                .isEqualByComparingTo("232.5"),
                        () -> assertThat(new BigDecimal(String.valueOf(t.get(2)).replace(',', '.')))
                                .isEqualByComparingTo("250"))));
    }

    @Test
    void a33_suppression_uneTraceDeleted_formatMesure() throws Exception {
        UUID ligne = genererLaLigne();

        mockMvc.perform(suppression(List.of(ligne)).with(qui)).andExpect(status().isNoContent());

        Map<String, Object> trace = seuleTrace(ligne, DELETED);
        assertAll(
                () -> verifierType(trace, DELETED),
                () -> assertThat(details(trace)).containsExactly(List.of("CommissionLine", "", "CommissionLine")));
    }

    // ------------------------------------------------------------------ un refus n'écrit rien

    /** Témoin : la même ligne, libre, est tracée à la modification (voir le test précédent) ; documentée → 409, rien. */
    @Test
    void a33_refus409_aucuneTraceModifiedNiDeleted() throws Exception {
        UUID ligne = genererLaLigne();
        documenter(ligne);

        mockMvc.perform(montant(ligne, new BigDecimal("250")).with(qui)).andExpect(status().isConflict());
        mockMvc.perform(suppression(List.of(ligne)).with(qui)).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(traces(ligne, MODIFIED)).isEmpty(),
                () -> assertThat(traces(ligne, DELETED)).isEmpty(),
                () -> assertThat(traces(ligne, ADDED)).hasSize(1));
    }

    @Test
    void a33_refus403_aucuneLigneNiTrace() throws Exception {
        jdbc.update("DELETE FROM AdmRight WHERE AdmRoleId = ? AND Code = ?", role.toString(), FixtureCommissions.NOUVEAU);

        mockMvc.perform(generation(tranche, T_CONCRETISATION, List.of(dossier)).with(qui)).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ComCommissionLine WHERE ComFolderId = ?", Integer.class,
                dossier.toString())).isZero();
    }
}

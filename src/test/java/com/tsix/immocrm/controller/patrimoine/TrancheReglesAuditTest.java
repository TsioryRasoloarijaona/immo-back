package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
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
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit et transaction du lot P1c — FEATURE-patrimoine-tranche-regles.md §0, §4 (une transaction), §9.8 (types
 * mesurés dans {@code AdmWebHistorics} de production : {@code Yes.Cfg.SalesCommission} « Commission vendeur »,
 * {@code Yes.Cfg.CfgTrancheFolderEntity} « Définition des finalisations », {@code Yes.Com.Sales.ComDocument}
 * « Financement », tous sans parent ; {@code CfgPrevision} sans trace).
 *
 * <p>Sans {@code @Transactional} : la ligne d'audit n'est écrite qu'à la validation, et l'atomicité d'un refus ne se
 * voit qu'après un vrai rollback. Chaque test valide ses écritures et les efface en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TrancheReglesAuditTest {

    private static final Logger log = LoggerFactory.getLogger(TrancheReglesAuditTest.class);

    private static final String ACTION = "Action";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String OBJECT_TYPE_LABEL = "ObjectTypeLabel";
    private static final String PARENT_TYPE = "ParentObjectType";
    private static final String PARENT_ID = "ParentObjectId";
    private static final String ADDED = "Added";
    private static final String MODIFIED = "Modified";
    private static final String COMMISSIONS = "ComSalesCommission";
    private static final String FINALISATIONS = "CfgTrancheFolderEntity";
    private static final String CFG_TRANCHE_ID = "CfgTrancheId";
    private static final String STK_ITEM = "StkItem";
    private static final String COM_FOLDER = "ComFolder";
    /** Tables du lot et colonne qui porte la tranche. */
    private static final List<String[]> TABLES_DU_LOT = List.of(
            new String[] {COMMISSIONS, CFG_TRANCHE_ID},
            new String[] {FINALISATIONS, CFG_TRANCHE_ID},
            new String[] {"CfgPrevision", "StkHierarchyId"});

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<LigneCreee> creees = new ArrayDeque<>();
    private final List<UUID> objets = new ArrayList<>();
    private final List<UUID> dossiers = new ArrayList<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgTranche tranche;

    private record LigneCreee(String table, UUID id) {
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        admin = fixture.administrateur();
        suivre("AdmUser", admin.getId());
        CfgCompany entite = commeAdmin(() -> fixture.entite("ENT-P1CAU-" + suffixe, true));
        suivre("CfgCompany", entite.getId());
        CfgProject projet = commeAdmin(() -> fixture.projet("PRJ-P1CAU-" + suffixe, entite));
        suivre("CfgProject", projet.getId());
        tranche = commeAdmin(() -> fixture.tranche("TR-P1CAU-" + suffixe, projet));
        suivre("CfgTranche", tranche.getId());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        if (tranche != null) {
            TABLES_DU_LOT.forEach(t -> {
                effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM " + t[0] + " WHERE " + t[1]
                        + " = ?)", tranche.getId());
                effacer("DELETE FROM " + t[0] + " WHERE " + t[1] + " = ?", tranche.getId());
            });
        }
        objets.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            effacer("DELETE FROM AdmMessage WHERE CategoryId = ?", id);
        });
        if (admin != null) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM AdmUserHabilitation WHERE AdmUserId = ?)",
                    admin.getId());
            effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId());
        }
        dossiers.forEach(id -> effacer("UPDATE ComFolder SET StkItemId = NULL WHERE PKey = ?", id));
        while (!creees.isEmpty()) {
            LigneCreee ligne = creees.pop();
            effacer("DELETE FROM " + ligne.table() + " WHERE PKey = ?", ligne.id());
        }
    }

    /** Nettoyage au mieux : une table annexe inconnue (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private void suivre(String table, UUID id) {
        creees.push(new LigneCreee(table, id));
        objets.add(id);
    }

    // ------------------------------------------------------------------ outils

    private <T> T commeAdmin(Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        return transactions.execute(statut -> ecriture.get());
    }

    private UUID inserer(String table, String colonnes, Object... valeurs) {
        UUID id = UUID.randomUUID();
        Object[] parametres = new Object[valeurs.length + 1];
        parametres[0] = id.toString();
        System.arraycopy(valeurs, 0, parametres, 1, valeurs.length);
        String marques = String.join(",", Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        suivre(table, id);
        return id;
    }

    private ResultActions envoyer(String ressource, String corps) throws Exception {
        return mockMvc.perform(put("/api/tranches/{id}/" + ressource, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID seuleLigneDuLot(String table) {
        List<String> ids = jdbc.queryForList("SELECT CONVERT(varchar(36), PKey) FROM " + table + " WHERE CfgTrancheId = ?",
                String.class, tranche.getId().toString());
        assertThat(ids).as("lignes de %s", table).hasSize(1);
        return UUID.fromString(ids.get(0));
    }

    private int compterDuLot(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE CfgTrancheId = ?", Integer.class,
                tranche.getId().toString());
        return n == null ? 0 : n;
    }

    private List<Map<String, Object>> lignesAudit(UUID objetId, String action) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectType, ObjectTypeLabel, ParentObjectType,
                               CONVERT(varchar(36), ParentObjectId) AS ParentObjectId, CAST(Details AS nvarchar(max)) AS Details
                        FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", objetId.toString(), action);
    }

    private Map<String, Object> seuleLigne(UUID objetId, String action) {
        List<Map<String, Object>> trouvees = lignesAudit(objetId, action);
        assertThat(trouvees).as("lignes %s de %s", action, objetId).hasSize(1);
        return trouvees.get(0);
    }

    /** Prod : {@code ParentObjectType} vide, {@code ParentObjectId} nul. */
    private static void verifierType(Map<String, Object> ligne, String action, String type, String libelle) {
        assertAll(
                () -> assertThat(ligne).containsEntry(ACTION, action).containsEntry(OBJECT_TYPE, type)
                        .containsEntry(OBJECT_TYPE_LABEL, libelle).containsEntry(PARENT_ID, null),
                () -> assertThat((String) ligne.get(PARENT_TYPE)).isNullOrEmpty());
    }

    private UUID statutDossier(String code) {
        return inserer("StrStatus", "Code, StrConfigEntityId, Sorting", code, StatutDossier.CONFIG_ENTITY_ID.toString(), 1);
    }

    private UUID entiteDossierVente() {
        Integer present = jdbc.queryForObject("SELECT COUNT(*) FROM StrEntity WHERE PKey = ?", Integer.class,
                StatutDossier.CONFIG_ENTITY_ID.toString());
        if (present == null || present == 0) {
            jdbc.update("INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'ComFolder', N'Dossier de vente')",
                    StatutDossier.CONFIG_ENTITY_ID.toString());
            creees.push(new LigneCreee("StrEntity", StatutDossier.CONFIG_ENTITY_ID));
        }
        return StatutDossier.CONFIG_ENTITY_ID;
    }

    // ------------------------------------------------------------------ §9.8 une ligne Added par table écrite

    @Test
    void r9_8_commissionAjoutee_ligneAdded_salesCommission_sansParent() throws Exception {
        envoyer("commissions", lignes(corps().avec("dateDebut", "2027-01-01T00:00:00").avec("dateFin", null)))
                .andExpect(status().isOk());

        verifierType(seuleLigne(seuleLigneDuLot(COMMISSIONS), ADDED), ADDED, "Yes.Cfg.SalesCommission",
                "Commission vendeur");
    }

    @Test
    void r9_8_finalisationAjoutee_ligneAdded_cfgTrancheFolderEntity_sansParent() throws Exception {
        UUID entite = entiteDossierVente();
        UUID s1 = statutDossier("FORM-" + suffixe);
        UUID s2 = statutDossier("RDC-" + suffixe);

        envoyer("finalisations", lignes(corps().avec("entiteDossierId", entite).avec("statutFinalisation1Id", s1)
                .avec("statutFinalisation2Id", s2)))
                .andExpect(status().isOk());

        verifierType(seuleLigne(seuleLigneDuLot(FINALISATIONS), ADDED), ADDED, "Yes.Cfg.CfgTrancheFolderEntity",
                "Définition des finalisations");
    }

    /** §9.8 : {@code CfgPrevision} n'a aucune trace, pas d'audit. */
    @Test
    void r9_8_previsionsEnregistrees_aucuneLigneDAudit() throws Exception {
        envoyer("previsions-base", "{\"nombreArticles\":1,\"lignes\":[{\"date\":\"2027-01-01T00:00:00\",\"annee\":2027,"
                + "\"mois\":1,\"quantiteConcretisation\":1,\"quantiteFinalisation\":1}]}")
                .andExpect(status().isOk());

        List<String> previsions = jdbc.queryForList("SELECT CONVERT(varchar(36), PKey) FROM CfgPrevision WHERE StkHierarchyId = ?",
                String.class, tranche.getId().toString());
        assertThat(previsions).singleElement().satisfies(p -> assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?", Integer.class, p)).isZero());
    }

    /** §9.8 : OMRAN audite {@code ComDocument} ; la date déplacée apparaît dans les détails (format OMRAN). */
    @Test
    void r9_8_miseAJourSchemas_echeanceDeplacee_ligneModified_comDocument() throws Exception {
        jdbc.update("UPDATE CfgTranche SET StartDateFinalisation = ? WHERE PKey = ?",
                Timestamp.valueOf(LocalDateTime.of(2027, 6, 1, 0, 0)), tranche.getId().toString());
        UUID echeance = echeanceEligible();

        mockMvc.perform(post("/api/tranches/{id}/mettre-a-jour-schemas", tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk());

        Map<String, Object> ligne = seuleLigne(echeance, MODIFIED);
        List<List<Object>> details = JsonPath.read((String) ligne.get("Details"), "$");
        assertAll(
                () -> verifierType(ligne, MODIFIED, "Yes.Com.Sales.ComDocument", "Financement"),
                () -> assertThat(details).anySatisfy(t -> assertThat(t).startsWith("DocDate", "15/01/2027 00:00:00",
                        "01/06/2027 00:00:00")));
    }

    /** Article de la tranche occupé par un dossier, échéance de 1 000 au 15/01/2027, catégorie à date d'octroi. */
    private UUID echeanceEligible() {
        UUID nature = inserer("StkKind", "Code, Description", "K-P1CAU-" + suffixe, "Nature");
        UUID vocation = inserer("StkVocation", "Code, Description", "V-P1CAU-" + suffixe, "Vocation");
        UUID type = inserer("StkItemType", "Code, Description, StkKindId, ForUnderItem", "T-P1CAU-" + suffixe, "Type",
                nature.toString(), false);
        UUID categorie = inserer("StkItemCategory", "Code, Description, StkKindId, ForUnderItem", "C-P1CAU-" + suffixe,
                "Categorie", nature.toString(), false);
        UUID dossier = inserer(COM_FOLDER, "Code, StkVocationId", "DOS-P1CAU-" + suffixe, vocation.toString());
        dossiers.add(dossier);
        UUID article = inserer(STK_ITEM, "Code, Description, StkHierarchyId, StkItemTypeId, StkItemCategoryId, "
                        + "StkVocationId, StkKindId, ComFolderId", "ART-P1CAU-" + suffixe, "Article",
                tranche.getId().toString(), type.toString(), categorie.toString(), vocation.toString(), nature.toString(),
                dossier.toString());
        jdbc.update("UPDATE ComFolder SET StkItemId = ? WHERE PKey = ?", article.toString(), dossier.toString());
        UUID categorieEcheance = inserer("ComSettlementCategory", "Code, Description, IsCredit, IsConfermentBasedDate",
                "CE-P1CAU-" + suffixe, "Categorie echeance", true, true);
        return inserer("ComDocument", "ComFolderId, ComSettlementCategoryId, Amount, DocDate", dossier.toString(),
                categorieEcheance.toString(), 1000, Timestamp.valueOf(LocalDateTime.of(2027, 1, 15, 0, 0)));
    }

    // ------------------------------------------------------------------ §1, §2 : un refus n'écrit rien

    @Test
    void r2_commissionsRefusees400_rienNEstEcrit() throws Exception {
        UUID existante = inserer(COMMISSIONS, "CfgTrancheId, StartDate, EndDate", tranche.getId().toString(),
                Timestamp.valueOf(LocalDateTime.of(2027, 1, 1, 0, 0)), Timestamp.valueOf(LocalDateTime.of(2027, 12, 31, 0, 0)));

        envoyer("commissions", lignes(
                corps().avec("id", existante).avec("dateDebut", "2027-01-01T00:00:00").avec("dateFin", "2027-06-30T00:00:00"),
                corps().avec("dateDebut", "2027-03-01T00:00:00").avec("dateFin", null)))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(jdbc.queryForObject("SELECT EndDate FROM ComSalesCommission WHERE PKey = ?", Timestamp.class,
                        existante.toString())).isEqualTo(Timestamp.valueOf(LocalDateTime.of(2027, 12, 31, 0, 0))),
                () -> assertThat(compterDuLot(COMMISSIONS)).isEqualTo(1));
    }

    @Test
    void r1_finalisationsRefusees400_rienNEstEcrit() throws Exception {
        UUID entite = entiteDossierVente();
        UUID s1 = statutDossier("FORM2-" + suffixe);

        envoyer("finalisations", lignes(
                corps().avec("entiteDossierId", entite).avec("statutFinalisation1Id", s1).avec("statutFinalisation2Id", s1),
                corps().avec("entiteDossierId", entite).avec("statutFinalisation1Id", s1).avec("statutFinalisation2Id", s1)))
                .andExpect(status().isBadRequest());

        assertThat(compterDuLot(FINALISATIONS)).isZero();
    }
}

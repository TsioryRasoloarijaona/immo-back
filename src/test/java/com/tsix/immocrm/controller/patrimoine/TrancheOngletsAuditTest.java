package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
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
import java.math.BigDecimal;
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
import static com.tsix.immocrm.fixtures.FixtureTrancheOnglets.NATURE_TERRAIN;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static com.tsix.immocrm.fixtures.LignesJson.lignesAvec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit et transaction des onglets — FEATURE-patrimoine-tranche-onglets.md §0, §1, §4, §7, §8.4.
 *
 * <p>Sans {@code @Transactional} : la ligne {@code AdmWebHistorics} n'est écrite qu'à la validation (P0), et
 * l'atomicité du PUT ne se voit qu'après un vrai rollback. Chaque test valide ses écritures et les efface en
 * {@code @AfterEach}. Codes suffixés d'un aléa : yesRemsTest est partagée avec d'autres lots.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TrancheOngletsAuditTest {

    private static final Logger log = LoggerFactory.getLogger(TrancheOngletsAuditTest.class);

    private static final String TYPE_TRANCHE = "Yes.Cfg.CfgTranche";
    private static final String ADDED = "Added";
    private static final String ACTION = "Action";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String OBJECT_TYPE_LABEL = "ObjectTypeLabel";
    private static final String PARENT_TYPE = "ParentObjectType";
    private static final String PARENT_ID = "ParentObjectId";
    private static final String TERRAIN_ID = "terrainId";
    private static final String STK_ITEM = "StkItem";
    private static final String CFG_TRANCHE = "CfgTranche";
    private static final String COL_TRANCHE = "CfgTrancheId";
    private static final String TABLE_TERRAINS = "CfgTrancheStkItem";
    private static final String TABLE_ETAGES = "CfgTrancheLevel";
    private static final String TABLE_PLANCHER = "CfgTrancheCoveredFloor";
    /** Tables d'onglets et colonne de la tranche (casse OMRAN de CfgTrancheSettlementCategory). */
    private static final List<String[]> ONGLETS = List.of(
            new String[] {TABLE_TERRAINS, COL_TRANCHE},
            new String[] {TABLE_ETAGES, COL_TRANCHE},
            new String[] {TABLE_PLANCHER, COL_TRANCHE},
            new String[] {"CfgTrancheSettlementCategory", "CfgtrancheId"});

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkKindRepository kindRepository;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<LigneCreee> creees = new ArrayDeque<>();
    private final List<UUID> objets = new ArrayList<>();
    private final List<UUID> tranches = new ArrayList<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgProject projet;
    private CfgTranche tranche;

    private record LigneCreee(String table, UUID id) {
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        admin = fixture.administrateur();
        suivre("AdmUser", admin.getId());
        CfgCompany entite = commeAdmin(() -> fixture.entite("ENT-P1BAU-" + suffixe, true));
        suivre("CfgCompany", entite.getId());
        projet = commeAdmin(() -> fixture.projet("PRJ-P1BAU-" + suffixe, entite));
        suivre("CfgProject", projet.getId());
        tranche = trancheValidee("TR-P1BAU-" + suffixe);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        tranches.forEach(this::effacerOnglets);
        objets.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            effacer("DELETE FROM AdmMessage WHERE CategoryId = ?", id);
            effacer("DELETE FROM AdmAttachedFile WHERE CategoryId = ?", id);
        });
        effacerHabilitations();
        while (!creees.isEmpty()) {
            LigneCreee ligne = creees.pop();
            effacer("DELETE FROM " + ligne.table() + " WHERE PKey = ?", ligne.id());
        }
    }

    private void effacerOnglets(UUID trancheId) {
        ONGLETS.forEach(t -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM " + t[0] + " WHERE " + t[1] + " = ?)",
                    trancheId);
            effacer("DELETE FROM " + t[0] + " WHERE " + t[1] + " = ?", trancheId);
        });
    }

    private void effacerHabilitations() {
        if (admin == null) {
            return;
        }
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM AdmUserHabilitation WHERE AdmUserId = ?)",
                admin.getId());
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId());
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

    private CfgTranche trancheValidee(String code) {
        CfgTranche t = commeAdmin(() -> fixture.tranche(code, projet));
        suivre(CFG_TRANCHE, t.getId());
        tranches.add(t.getId());
        return t;
    }

    private ResultActions envoyer(String onglet, String corps) throws Exception {
        return mockMvc.perform(put("/api/tranches/{id}/{onglet}", tranche.getId(), onglet).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
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

    private UUID terrain(String code) {
        StkKind nature = commeAdmin(() -> kindRepository.findById(NATURE_TERRAIN).orElse(null));
        if (nature == null) {
            nature = commeAdmin(() -> fixture.nature(NATURE_TERRAIN, "TER"));
            creees.push(new LigneCreee("StkKind", NATURE_TERRAIN));
        }
        StkKind ter = nature;
        boolean vocationAbsente = jdbc.queryForObject("SELECT COUNT(*) FROM StkVocation WHERE PKey = ?", Integer.class,
                FixturePatrimoine.VOCATION_PROJET.toString()) == 0;
        StkVocation vocation = commeAdmin(fixture::vocationProjet);
        if (vocationAbsente) {
            creees.push(new LigneCreee("StkVocation", FixturePatrimoine.VOCATION_PROJET));
        }
        StkItemCategory categorie = commeAdmin(() -> fixture.categorie("CAT-" + code, ter, false));
        suivre("StkItemCategory", categorie.getId());
        StkItemType type = commeAdmin(() -> fixture.type("TYP-" + code, ter, false));
        suivre("StkItemType", type.getId());
        return inserer(STK_ITEM, "Code, Description, StkItemTypeId, StkItemCategoryId, StkVocationId, StkKindId",
                code, "Terrain " + code, type.getId().toString(), categorie.getId().toString(),
                vocation.getId().toString(), ter.getId().toString());
    }

    private UUID seuleLigneDOnglet(String table, String colonne) {
        List<String> ids = jdbc.queryForList("SELECT CONVERT(varchar(36), PKey) FROM " + table + " WHERE " + colonne + " = ?",
                String.class, tranche.getId().toString());
        assertThat(ids).as("lignes de %s", table).hasSize(1);
        UUID id = UUID.fromString(ids.get(0));
        objets.add(id);
        return id;
    }

    private List<Map<String, Object>> lignesAudit(UUID objetId, String action) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectType, ObjectTypeLabel, ParentObjectType,
                               CONVERT(varchar(36), ParentObjectId) AS ParentObjectId
                        FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", objetId.toString(), action);
    }

    private Map<String, Object> seuleLigneAdded(UUID objetId) {
        List<Map<String, Object>> trouvees = lignesAudit(objetId, ADDED);
        assertThat(trouvees).as("lignes Added de %s", objetId).hasSize(1);
        return trouvees.get(0);
    }

    private void verifierParentTranche(Map<String, Object> ligne) {
        assertAll(
                () -> assertThat(ligne.get(PARENT_TYPE)).isEqualTo(TYPE_TRANCHE),
                () -> assertThat(FixturePatrimoine.guid(ligne.get(PARENT_ID))).isEqualTo(FixturePatrimoine.guid(tranche.getId())));
    }

    /** Prod : {@code ParentObjectType} vide (ou absent), {@code ParentObjectId} nul. */
    private static void verifierSansParent(Map<String, Object> ligne) {
        assertAll(
                () -> assertThat(ligne.get(PARENT_ID)).isNull(),
                () -> assertThat((String) ligne.get(PARENT_TYPE)).isNullOrEmpty());
    }

    private void verifierType(Map<String, Object> ligne, String type, String libelle) {
        assertAll(
                () -> assertThat(ligne.get(ACTION)).isEqualTo(ADDED),
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo(type),
                () -> assertThat(ligne.get(OBJECT_TYPE_LABEL)).isEqualTo(libelle));
    }

    private UUID ajouterEtage() throws Exception {
        UUID niveau = inserer("LndLevel", "Code, Description", "NIV-P1BAU-" + suffixe, "Niveau " + suffixe);
        envoyer("etages", lignes(corps().avec("niveauTerrainId", niveau))).andExpect(status().isOk());
        return seuleLigneDOnglet(TABLE_ETAGES, COL_TRANCHE);
    }

    private UUID ajouterPlancher() throws Exception {
        StkItemCategory categorie = commeAdmin(() -> fixture.categorie("CP-P1BAU-" + suffixe, fixture.natureBat(), false));
        suivre("StkItemCategory", categorie.getId());
        envoyer("plancher-couvert", lignes(corps().avec("categorieId", categorie.getId()).avec("surface", BigDecimal.TEN)))
                .andExpect(status().isOk());
        return seuleLigneDOnglet(TABLE_PLANCHER, COL_TRANCHE);
    }

    private UUID ajouterFrais() throws Exception {
        UUID categorie = inserer("ComSettlementCategory", "Code, Description, IsCredit, IsSaleExpense",
                "FR-P1BAU-" + suffixe, "Frais " + suffixe, true, true);
        envoyer("frais", lignesAvec("saisiEnTTC", false, corps().avec("categorieReglementId", categorie)
                .avec("montantHT", BigDecimal.TEN).avec("tauxTva", BigDecimal.ZERO).avec("saisiEnTTC", false)))
                .andExpect(status().isOk());
        return seuleLigneDOnglet("CfgTrancheSettlementCategory", "CfgtrancheId");
    }

    // ------------------------------------------------------------------ §8.4 une ligne Added par table d'onglet

    @Test
    void r8_4_terrainAjoute_ligneAddedEnfantDeLaTranche() throws Exception {
        UUID terrain = terrain("TER-P1BAU-" + suffixe);

        envoyer("terrains", lignes(corps().avec(TERRAIN_ID, terrain))).andExpect(status().isOk());

        Map<String, Object> ligne = seuleLigneAdded(seuleLigneDOnglet(TABLE_TERRAINS, COL_TRANCHE));
        verifierType(ligne, "Yes.Cfg.CfgTrancheStkItem", "Tranche");
        verifierParentTranche(ligne);
    }

    /** Aucune ligne en prod : type et libellé du spec §0. */
    @Test
    void r8_4_etageAjoute_ligneAdded_typeCfgTrancheLevel() throws Exception {
        verifierType(seuleLigneAdded(ajouterEtage()), "Yes.Cfg.CfgTrancheLevel", "Etage");
    }

    @Test
    void r0_etageAjoute_ligneAdded_parentEstLaTranche() throws Exception {
        verifierParentTranche(seuleLigneAdded(ajouterEtage()));
    }

    /** Libellé mesuré en prod (sic) ; sans parent comme en prod (décision manager, 19/10). */
    @Test
    void r8_4_plancherAjoute_ligneAdded_typeCfgTrancheCoveredFloor_sansParent() throws Exception {
        Map<String, Object> ligne = seuleLigneAdded(ajouterPlancher());
        verifierType(ligne, "Yes.Cfg.CfgTrancheCoveredFloor", "Prospection de projet");
        verifierSansParent(ligne);
    }

    /** Sans parent comme en prod (décision manager, 19/10). */
    @Test
    void r8_4_fraisAjoute_ligneAdded_typeCfgTrancheSettlementCategory_sansParent() throws Exception {
        Map<String, Object> ligne = seuleLigneAdded(ajouterFrais());
        verifierType(ligne, "Yes.Cfg.CfgTrancheSettlementCategory", "Frais de vente");
        verifierSansParent(ligne);
    }

    /** §0 « Registre d'audit, ajout » : l'habilitation du créateur d'une tranche est tracée comme OMRAN. */
    @Test
    void r8_4_creationTranche_habilitationDuCreateur_ligneAddedHabilitation() throws Exception {
        String code = "TR-P1BAU-HAB-" + suffixe;
        String json = mockMvc.perform(post("/api/tranches").with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("projetId", projet.getId()).avec("code", code)
                                .avec("description", "Tranche " + code).json()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID creee = UUID.fromString(JsonPath.read(json, "$.id"));
        suivre(CFG_TRANCHE, creee);
        tranches.add(creee);

        List<String> habilitations = jdbc.queryForList(
                "SELECT CONVERT(varchar(36), PKey) FROM AdmUserHabilitation WHERE AdmUserId = ?", String.class,
                admin.getId().toString());
        assertThat(habilitations).isNotEmpty().allSatisfy(h ->
                verifierType(seuleLigneAdded(UUID.fromString(h)), "Yes.Adm.AdmUserHabilitation", "Habilitation"));
    }

    // ------------------------------------------------------------------ §4 suppression tracée

    @Test
    void r4_suppressionTranche_ligneDeleted() throws Exception {
        CfgTranche vide = trancheValidee("TR-P1BAU-DEL-" + suffixe);

        mockMvc.perform(delete("/api/tranches/{id}", vide.getId()).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertThat(lignesAudit(vide.getId(), "Deleted")).singleElement()
                .satisfies(l -> assertThat(l.get(OBJECT_TYPE)).isEqualTo(TYPE_TRANCHE));
    }

    // ------------------------------------------------------------------ §1 une transaction

    @Test
    void r1_putRefuse400_rienNEstEcrit() throws Exception {
        UUID t1 = terrain("TER-P1BAU-A-" + suffixe);
        UUID t2 = terrain("TER-P1BAU-B-" + suffixe);
        UUID lien = inserer(TABLE_TERRAINS, "CfgTrancheId, StkItemId, Ratio, Sorting",
                tranche.getId().toString(), t1.toString(), BigDecimal.ONE, 1);

        envoyer("terrains", lignes(
                corps().avec("id", lien).avec(TERRAIN_ID, t1).avec("ratio", new BigDecimal("0.5")),
                corps().avec(TERRAIN_ID, t2),
                corps().avec("ratio", BigDecimal.ONE)))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(jdbc.queryForObject("SELECT Ratio FROM CfgTrancheStkItem WHERE PKey = ?", BigDecimal.class,
                        lien.toString())).isEqualByComparingTo("1"),
                () -> assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CfgTrancheStkItem WHERE CfgTrancheId = ?",
                        Integer.class, tranche.getId().toString())).isEqualTo(1));
    }
}

package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bascule d'{@code IsActive} par l'écran — FEATURE-entites-juridiques.md §4 (R1–R4 de
 * FEATURE-visibilite-transversale-entites.md) : après {@code PATCH isActive=false}, programmes, tranches, articles,
 * prospections et dossiers de l'entité disparaissent des listes et sont introuvables par id ; après
 * {@code isActive=true} ils reviennent. Appelant Administrateur (R1 : exempté de périmètre, pas de visibilité).
 * Aucun cache ne doit garder l'ancien état : chaque test lit avant et après la bascule (VisibiliteEntiteBasculeTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisibiliteEntiteBasculeTest {

    private static final String ENTITE = "/api/entites-juridiques/{id}";
    private static final String TAILLE = "size";
    private static final String GRANDE_PAGE = "2000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItem article;
    private ComProspection prospection;
    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "BSC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("ENT-" + jeton, true);
        projet = fixture.projet("PRJ-" + jeton, entite);
        tranche = fixture.tranche("TR-" + jeton, projet);
        StkKind nature = fixture.natureBat();
        StkItemCategory categorie = fixture.categorie("CAT-" + jeton, nature, false);
        StkItemType type = fixture.type("TYP-" + jeton, nature, false);
        article = article("ART-" + jeton, categorie, type, null);
        prospection = prospection("PSN-" + jeton, tranche);
        dossier = fixture.dossier("DOS-" + jeton);
        StkItem lot = article("LOT-" + jeton, categorie, type, dossier.getId());
        dossier.setStkItem(lot);
        dossier = folderRepository.save(dossier);
    }

    private StkItem article(String code, StkItemCategory categorie, StkItemType type, UUID dossierId) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemCategory(categorie);
        item.setStkItemType(type);
        item.setStkKind(fixture.natureBat());
        item.setStkVocation(fixture.vocationVente());
        item.setStkHierarchy(tranche);
        item.setComFolderId(dossierId);
        return itemRepository.save(item);
    }

    private ComProspection prospection(String code, CfgTranche surTranche) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("PRO-" + code);
        prospect.setLastName("Testeur");
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(tierRepository.save(prospect));
        p.setStkHierarchy(surTranche);
        return prospectionRepository.save(p);
    }

    private void basculer(boolean actif) throws Exception {
        mockMvc.perform(patch(ENTITE, entite.getId()).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("isActive", actif).json()))
                .andExpect(status().isOk());
    }

    private static ResultMatcher contient(UUID id, boolean present) {
        String chemin = "$..[?(@.id=='" + id + "')]";
        return present ? jsonPath(chemin).exists() : jsonPath(chemin).doesNotExist();
    }

    /** Listes (R2/R3) : présence ou absence des cinq lignes de l'entité. */
    private void listes(boolean visibles) throws Exception {
        mockMvc.perform(get("/api/projets").param("search", "PRJ-" + jeton).with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(projet.getId(), visibles));
        mockMvc.perform(get("/api/tranches").param("projetId", projet.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(tranche.getId(), visibles));
        mockMvc.perform(get("/api/articles").param("search", "ART-" + jeton).with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(article.getId(), visibles));
        mockMvc.perform(get("/api/prospections").param(TAILLE, GRANDE_PAGE).with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(prospection.getId(), visibles));
        mockMvc.perform(get("/api/dossiers").param(TAILLE, GRANDE_PAGE).with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(dossier.getId(), visibles));
    }

    /** Accès par id (R4) : 200 si visibles, sinon 404. */
    private void fiches(boolean visibles) throws Exception {
        ResultMatcher attendu = visibles ? status().isOk() : status().isNotFound();
        mockMvc.perform(get("/api/projets/{id}", projet.getId()).with(connecte(admin))).andExpect(attendu);
        mockMvc.perform(get("/api/tranches/{id}", tranche.getId()).with(connecte(admin))).andExpect(attendu);
        mockMvc.perform(get("/api/articles/{id}", article.getId()).with(connecte(admin))).andExpect(attendu);
        mockMvc.perform(get("/api/prospections/{id}", prospection.getId()).with(connecte(admin))).andExpect(attendu);
        mockMvc.perform(get("/api/dossiers/{id}", dossier.getId()).with(connecte(admin))).andExpect(attendu);
    }

    // ------------------------------------------------------------------ §4 dans les deux sens

    @Test
    void r2_r3_desactivationParLEcran_listesSansLesLignesDeLEntite() throws Exception {
        listes(true);

        basculer(false);

        listes(false);
    }

    @Test
    void r4_desactivationParLEcran_fichesIntrouvables() throws Exception {
        fiches(true);

        basculer(false);

        fiches(false);
    }

    @Test
    void r2_r3_reactivationParLEcran_listesRetrouventLesLignes() throws Exception {
        basculer(false);
        listes(false);

        basculer(true);

        listes(true);
    }

    @Test
    void r4_reactivationParLEcran_fichesRetrouvees() throws Exception {
        basculer(false);
        fiches(false);

        basculer(true);

        fiches(true);
    }

    /** Les sélecteurs d'entité des formulaires ({@code GET /api/referentiels/entites}, actives) suivent la bascule. */
    @Test
    void r3_referentielDesEntites_suitLaBascule() throws Exception {
        basculer(false);
        mockMvc.perform(get("/api/referentiels/entites").with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(entite.getId(), false));

        basculer(true);
        mockMvc.perform(get("/api/referentiels/entites").with(connecte(admin)))
                .andExpect(status().isOk()).andExpect(contient(entite.getId(), true));
    }

    /** R2 : une prospection sans tranche n'appartient à aucune entité — la bascule ne la masque pas. */
    @Test
    void r2_prospectionSansTranche_resteVisibleApresDesactivation() throws Exception {
        ComProspection orpheline = prospection("PSN-ORPH-" + jeton, null);

        basculer(false);

        mockMvc.perform(get("/api/prospections/{id}", orpheline.getId()).with(connecte(admin)))
                .andExpect(status().isOk());
    }
}

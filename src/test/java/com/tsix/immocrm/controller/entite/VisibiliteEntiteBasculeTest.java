package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureEntites;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-entites-juridiques.md §4 : basculer « Active » par l'écran masque aussitôt le patrimoine et le commercial
 * de l'entité (listes R2/R3, lectures par id déjà sous R4, administrateur compris), puis le rend.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisibiliteEntiteBasculeTest {

    private static final String CONTENU = "$.content[?(@.id=='%s')]";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;

    private AdmUser admin;
    private CfgCompany societe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItem article;
    private ComFolder dossier;
    private ComProspection prospection;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P5-BASC");
        entites.donnerRole(admin, ROLE_ADMIN);
        FixturePatrimoine.Chaine chaine = fixture.chaine("P5-BASC");
        societe = chaine.societe();
        tranche = chaine.tranche();
        projet = tranche.getCfgProject();
        article = fixture.article(chaine, "TST-P5-BASC-ART");
        dossier = dossierSur(chaine);
        prospection = prospectionSur(tranche);
    }

    private List<String> lecturesParId() {
        // Tranches, articles, audit et notes par id : extension R4 reportée au lot P5b.
        return List.of(
                "/api/projets/" + projet.getId(),
                "/api/projets/" + projet.getId() + "/tranches",
                "/api/prospections/" + prospection.getId(),
                "/api/dossiers/" + dossier.getId());
    }

    private void basculer(boolean actif) throws Exception {
        mockMvc.perform(patch("/api/entites-juridiques/{id}", societe.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("actif", actif))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(actif));
    }

    private void toutesLesLecturesParId(ResultMatcher attendu) throws Exception {
        for (String route : lecturesParId()) {
            mockMvc.perform(get(route).with(connecte(admin, ROLE_ADMIN))).andExpect(attendu);
        }
    }

    private void listes(boolean presentes) throws Exception {
        verifierListe("/api/projets", projet.getId(), presentes);
        verifierListe("/api/tranches", tranche.getId(), presentes);
        verifierListe("/api/articles", article.getId(), presentes);
        verifierListe("/api/prospections", prospection.getId(), presentes);
        verifierListe("/api/dossiers", dossier.getId(), presentes);
    }

    private void verifierListe(String route, UUID id, boolean presente) throws Exception {
        ResultMatcher ligne = presente ? jsonPath(CONTENU.formatted(id)).exists()
                : jsonPath(CONTENU.formatted(id)).doesNotExist();
        mockMvc.perform(get(route).param("size", "2000").with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(ligne);
    }

    @Test
    void desactiver_masqueListesEtLecturesParId_puisReactiver_rendTout() throws Exception {
        toutesLesLecturesParId(status().isOk());
        listes(true);

        basculer(false);
        toutesLesLecturesParId(status().isNotFound());
        listes(false);

        basculer(true);
        toutesLesLecturesParId(status().isOk());
        listes(true);
    }

    @Test
    void entiteInactive_modificationDuProgrammeEn404() throws Exception {
        basculer(false);

        mockMvc.perform(patch("/api/projets/{id}", projet.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("description", "X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void referentielEntites_neProposeQueLesActives() throws Exception {
        basculer(false);

        mockMvc.perform(get("/api/referentiels/entites").with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(societe.getId())).doesNotExist());
    }

    private ComFolder dossierSur(FixturePatrimoine.Chaine chaine) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("TST-P5-BASC-DOS");
        folder.setStkVocation(chaine.vocation());
        folder.setTotalToPay(BigDecimal.ZERO);
        folder = folderRepository.save(folder);
        StkItem lot = fixture.article(chaine, "TST-P5-BASC-LOT");
        lot.setComFolderId(folder.getId());
        folder.setStkItem(itemRepository.save(lot));
        return folderRepository.save(folder);
    }

    private ComProspection prospectionSur(CfgTranche trancheVisee) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TST-P5-BASC-PRO");
        prospect.setLastName("Testeur");
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("TST-P5-BASC-PSN");
        p.setCfgProspect(tierRepository.save(prospect));
        p.setStkHierarchy(trancheVisee);
        return prospectionRepository.save(p);
    }
}

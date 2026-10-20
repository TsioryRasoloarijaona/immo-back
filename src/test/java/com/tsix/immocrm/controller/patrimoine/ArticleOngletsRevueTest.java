package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets.LigneHistorique;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P2b (FEATURE-patrimoine-article-onglets.md) : cohérence du périmètre entre onglets, lecture du groupe. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleOngletsRevueTest {

    private static final String COMMERCIAL = "Commercial";
    private static final LocalDateTime MARS = LocalDateTime.of(2026, 3, 10, 9, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente ventes;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AdmAttachedFileTypeRepository typeRepository;

    private String jeton;
    private AdmUser admin;
    private CfgTranche tranche;
    private StkItem article;

    @BeforeEach
    void setUp() {
        jeton = "P2BRV" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        patrimoine.referentielArticle();
        tranche = ventes.tranche("TR-" + jeton, true);
        article = onglets.article("ART-" + jeton, tranche);
    }

    private String corps(String route, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(route, article.getId()).with(qui))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Commercial habilité sur un autre programme que celui de l'article. */
    private RequestPostProcessor commercialHorsPerimetre() {
        AdmUser compte = ventes.compte("COM-" + jeton, COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(ventes.tranche("TRB-" + jeton, true).getCfgProject());
        habilitationRepository.save(habilitation);
        return FixtureDossierVente.connecte(compte, COMMERCIAL);
    }

    /**
     * L'onglet Dossiers masque la location d'un article hors périmètre ; l'onglet Historique doit masquer sa ligne
     * de statut de la même façon (sinon client et statut de la location fuient par l'historique).
     */
    @Test
    void locationDUnArticleHorsPerimetre_masqueeDansDossiersEtDansHistorique() throws Exception {
        UUID location = onglets.location("LOC-" + jeton, article.getId(),
                ventes.statut(StatutDossier.RESERVATION).getId(), ventes.tier("LOC-" + jeton).getId());
        String libelle = "HIST-LOC-" + jeton;
        onglets.historique(LigneHistorique.de(MARS, libelle, article.getId()).location(location));
        RequestPostProcessor commercial = commercialHorsPerimetre();

        String dossiers = corps("/api/articles/{id}/dossiers", commercial);
        String historique = corps("/api/articles/{id}/historique", commercial);

        List<String> originesDossiers = JsonPath.read(dossiers, "$..origine");
        assertThat(originesDossiers).doesNotContain("LOCATION");
        assertThat(historique).doesNotContain(libelle);
    }

    /**
     * Même filtre que le test verrouillé {@code r4_lecture_listeEtFichier302}, lu par un Matcher : le groupe est bien
     * dans la liste. L'échec du test verrouillé vient de {@code value(List.of(..))} (conversion vers le type de
     * {@code List.of}), pas du code.
     */
    @Test
    void listeDeTranche_contientLeGroupeDepose_luParMatcher() throws Exception {
        AdmAttachedFileType type = new AdmAttachedFileType();
        type.setId(UUID.randomUUID());
        type.setCode("PLAN-" + jeton);
        type.setDescription("Plan " + jeton);
        typeRepository.save(type);
        String json = mockMvc.perform(multipart("/api/tranches/{id}/pieces-jointes", tranche.getId())
                        .file(new MockMultipartFile("fichier", "plan.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "PDF".getBytes(StandardCharsets.UTF_8)))
                        .param("typeId", type.getId().toString())
                        .param("groupe", "GRP-" + jeton)
                        .with(connecte(admin)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String piece = JsonPath.read(json, "$.id");

        mockMvc.perform(get("/api/tranches/{id}/pieces-jointes", tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + piece + "')].groupe").value(contains("GRP-" + jeton)));
    }
}

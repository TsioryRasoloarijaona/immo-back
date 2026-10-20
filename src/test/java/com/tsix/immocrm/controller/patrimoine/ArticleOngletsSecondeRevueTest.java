package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue P2b (FEATURE-patrimoine-article-onglets.md §11) : pièces jointes, R4, clé d'API. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleOngletsSecondeRevueTest {

    private static final String PIECES = "/api/%s/%s/pieces-jointes";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente ventes;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private FixtureEntites entites;
    @Autowired private AdmAttachedFileTypeRepository typeRepository;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private String jeton;
    private AdmUser admin;
    private CfgCompany entite;
    private CfgTranche tranche;
    private StkItem article;
    private AdmAttachedFileType type;

    @BeforeEach
    void setUp() {
        jeton = "P2BSR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        patrimoine.referentielArticle();
        entite = patrimoine.entite("ENT-" + jeton, true);
        tranche = patrimoine.tranche("TR-" + jeton, patrimoine.projet("PRJ-" + jeton, entite));
        article = onglets.article("ART-" + jeton, tranche);
        type = new AdmAttachedFileType();
        type.setId(UUID.randomUUID());
        type.setCode("PLAN-" + jeton);
        type.setDescription("Plan " + jeton);
        typeRepository.save(type);
    }

    private UUID deposer(String segment, UUID id) throws Exception {
        String json = mockMvc.perform(multipart(PIECES.formatted(segment, id))
                        .file(new MockMultipartFile("fichier", "plan.pdf", MediaType.APPLICATION_PDF_VALUE,
                                "PDF".getBytes(StandardCharsets.UTF_8)))
                        .param("typeId", type.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private String cleReferentiels() {
        AdmUser compte = ventes.compte("N8N-" + jeton, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-sr-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    /** Exclusion de portée comparée à l'URI brute : un tiret encodé atteint le même contrôleur. */
    @Test
    void cleApiReferentiels_cheminEncode_resteHorsPortee() throws Exception {
        String cle = cleReferentiels();
        URI encodee = URI.create("/api/tranches/" + tranche.getId() + "/pieces%2Djointes");

        mockMvc.perform(get(encodee).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden());
    }

    @Test
    void fichier_apresSuppression_404() throws Exception {
        UUID piece = deposer("articles", article.getId());
        String route = PIECES.formatted("articles", article.getId()) + "/" + piece;
        mockMvc.perform(delete(route).with(connecte(admin))).andExpect(status().isNoContent());

        mockMvc.perform(get(route + "/fichier").with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void fichier_pieceDUnAutrePorteur_404() throws Exception {
        UUID piece = deposer("tranches", tranche.getId());
        CfgTranche autre = patrimoine.tranche("TR2-" + jeton, tranche.getCfgProject());

        mockMvc.perform(get(PIECES.formatted("tranches", autre.getId()) + "/" + piece + "/fichier").with(connecte(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PIECES.formatted("articles", article.getId()) + "/" + piece + "/fichier").with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void suppression_porteurDUneEntiteInactive_404() throws Exception {
        UUID piece = deposer("tranches", tranche.getId());
        entites.actif(entite.getId(), false);

        mockMvc.perform(delete(PIECES.formatted("tranches", tranche.getId()) + "/" + piece).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void masse_trancheDUneEntiteInactive_404() throws Exception {
        entites.actif(entite.getId(), false);

        mockMvc.perform(multipart("/api/tranches/{id}/pieces-jointes/masse", tranche.getId())
                        .file(new MockMultipartFile("fichiers", article.getCode() + ".pdf",
                                MediaType.APPLICATION_PDF_VALUE, "PDF".getBytes(StandardCharsets.UTF_8)))
                        .param("typeId", type.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}

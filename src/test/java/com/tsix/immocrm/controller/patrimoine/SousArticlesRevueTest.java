package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P4 (FEATURE-patrimoine-sous-articles.md) : points du spec et des réponses du manager (20/10) non couverts par
 * les tests verrouillés — recherche R4-7, {@code champsAVider: ["parentId"]} → 400, article principal sur
 * {@code /api/sous-articles/{id}} → 404, POST libre avec parent occupé → 409, vocation du parent au rattachement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SousArticlesRevueTest {

    private static final String LISTE = "/api/sous-articles";
    private static final String SOUS_ARTICLE = "/api/sous-articles/{id}";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String TOUS = "1000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = sousArticles.contexte("P4R");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private static String ligne(StkItem item) {
        return "$.content[?(@.id=='" + item.getId() + "')]";
    }

    private CorpsJson corpsLibre(String code) {
        return corps().avec("trancheId", c.tranche().getId()).avec("code", code).avec("description", "Parking " + code)
                .avec("categorieId", c.categorieSous().getId()).avec("typeId", c.typeSous().getId())
                .avec("immeubleId", c.immeuble().getId()).avec("etageId", c.etage().getId())
                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN)
                .avec("prixM2ProvisoireHT", new BigDecimal("5000")).avec("prixM2DefinitifHT", new BigDecimal("5000"))
                .avec("tauxTva", new BigDecimal("0.20"));
    }

    // ------------------------------------------------------------------ R4-7 recherche (retenue par le manager)

    /** R4-7 : « … code parent / N° dossier / titre foncier / adresse / notes ». */
    @Test
    void r4_7_recherche_surLeNumeroDeDossier() throws Exception {
        StkItem vendu = sousArticles.sousArticle(c, code("RND"), null);
        String numero = "DOSRV" + c.jeton();
        vendu.setComFolderId(fixture.dossier(numero).getId());
        sousArticles.enregistrer(vendu);

        mockMvc.perform(get(LISTE).param(SEARCH, numero).param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(vendu)).isNotEmpty());
    }

    @Test
    void r4_7_recherche_surLesNotes() throws Exception {
        StkItem annote = sousArticles.sousArticle(c, code("RNO"), null);
        String note = "NOTERV" + c.jeton();
        annote.setNotes("Voir " + note);
        sousArticles.enregistrer(annote);

        mockMvc.perform(get(LISTE).param(SEARCH, note).param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(annote)).isNotEmpty());
    }

    @Test
    void r4_7_recherche_surLeCodeDuProgramme() throws Exception {
        StkItem cible = sousArticles.sousArticle(c, code("RPR"), null);

        mockMvc.perform(get(LISTE).param(SEARCH, c.projet().getCode()).param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(cible)).isNotEmpty());
    }

    // ------------------------------------------------------------------ réponses du manager (20/10)

    @Test
    void reponse_champsAViderParentId_400() throws Exception {
        StkItem libre = sousArticles.sousArticle(c, code("CAV"), null);

        mockMvc.perform(patch(SOUS_ARTICLE, libre.getId()).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("champsAVider", List.of("parentId")).json()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reponse_articlePrincipalSurSousArticles_patch404_delete404() throws Exception {
        StkItem principal = sousArticles.principal(c, code("PRI"), c.tranche());

        mockMvc.perform(patch(SOUS_ARTICLE, principal.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("description", "x").json()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(SOUS_ARTICLE, principal.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reponse_postLibreAvecParentOccupe_409() throws Exception {
        StkItem parent = sousArticles.principal(c, code("POC"), c.tranche());
        parent.setComFolderId(fixture.dossier("DOS-" + code("POC")).getId());
        sousArticles.enregistrer(parent);

        mockMvc.perform(post(LISTE).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLibre(code("NEW")).avec("parentId", parent.getId()).json()))
                .andExpect(status().isConflict());
    }

    /**
     * R4-3 : « Parent : même tranche (décidé) et même vocation … 400 pour un nouveau rattachement ». P4 rend la
     * vocation d'un libre modifiable ; la route de la fiche parent ({@code …/affecter}) ne contrôle pas la vocation.
     */
    @Test
    void r4_3_affecterDepuisLaFicheParent_vocationDifferente_400() throws Exception {
        StkItem parent = sousArticles.principal(c, code("PVO"), c.tranche());
        StkItem libre = sousArticles.sousArticle(c, code("LVO"), null);
        libre.setStkVocation(sousArticles.vocation("VOCR-" + c.jeton()));
        sousArticles.enregistrer(libre);

        mockMvc.perform(post("/api/articles/{id}/sous-articles/{sid}/affecter", parent.getId(), libre.getId())
                        .with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }
}

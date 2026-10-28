package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Adresse;
import com.tsix.immocrm.utils.LecturePdf;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Répétition d'une page (FEATURE-vente-formulaire-barid.md §3.3, §8.1 « Moteur ») : {@code data-repeter} sur un {@code div}
 * avec {@code data-saut-page} = une page par élément, saut entre deux éléments, aucun après le dernier ; balise non
 * permise → 400. Dossier de test : un acquéreur et deux co-acquéreurs (trois éléments).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class RepetitionPageTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String REPETEE = "<div data-repeter=\"destinataires\" data-saut-page>"
            + "<p>Page de <span data-champ=\"destinataire.nom\">Nom</span></p></div>";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureFormulairesBarid fixture;
    @Autowired private FixtureEtatVentes ev;

    private String p;
    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        p = FixtureEtatVentes.prefixe("BAMR");
        CfgTranche t = fixture.tranche("T-" + p, true, fixture.pays("PAYS-" + p));
        dossier = fixture.dossier(t, "DOS-" + p, ev.statutEnCours("ENC-" + p),
                fixture.client("REPUN" + p, null, Adresse.aucune()));
        fixture.coAcquereur(dossier, fixture.client("REPDEUX" + p, null, Adresse.aucune()),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        fixture.coAcquereur(dossier, fixture.client("REPTROIS" + p, null, Adresse.aucune()),
                LocalDateTime.of(2025, 1, 2, 0, 0));
        fixture.relire();
    }

    private ResultActions creer(String contenu) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", "TST_REP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        m.put("libelle", "Répétition");
        m.put("contenu", contenu);
        return mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(m)));
    }

    @Test
    void repeterSurDiv_accepte() throws Exception {
        creer(REPETEE).andExpect(status().isCreated());
    }

    @Test
    void repeterSurBaliseNonPermise_400() throws Exception {
        creer("<h1 data-repeter=\"destinataires\">Titre</h1>").andExpect(status().isBadRequest());
    }

    @Test
    void troisElements_troisPages_dansLOrdre_aucunePageApresLeDernier() throws Exception {
        String reponse = creer(REPETEE).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        byte[] pdf = mockMvc.perform(get(BASE + "/{id}/generer", json.readTree(reponse).get("id").asString())
                        .param("ficheId", dossier.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).as("pages").isEqualTo(3);
        soft.assertThat(LecturePdf.textePage(pdf, 1)).contains("Page de REPUN" + p).doesNotContain("REPDEUX");
        soft.assertThat(LecturePdf.textePage(pdf, 2)).contains("Page de REPDEUX" + p).doesNotContain("REPUN");
        soft.assertThat(LecturePdf.textePage(pdf, 3)).contains("Page de REPTROIS" + p).doesNotContain("REPDEUX");
        soft.assertAll();
    }
}

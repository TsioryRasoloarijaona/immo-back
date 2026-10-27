package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Adresse;
import com.tsix.immocrm.utils.LecturePdf;
import com.tsix.immocrm.utils.LecturePdf.Glyphe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blocs positionnés et taille de police (FEATURE-vente-formulaire-barid.md §3.2, §3.4, §8.1 « Moteur ») : {@code div[data-bloc]}
 * en centimètres, bornés à la page A4, {@code data-taille} 6–72 pt, Helvetica gras si {@code data-gras}. Mesurés sur le PDF
 * rendu d'un modèle à marges nulles (origine du bloc = coin haut-gauche de la page) : un acquéreur et un co-acquéreur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class BlocsPositionnesTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String DETAIL = "$.detail";
    private static final float X_CM = 5f;
    private static final float Y_CM = 10f;
    private static final float TAILLE = 20f;
    private static final float TOLERANCE_PT = 3f;
    private static final String BLOC_NOM = "<div data-bloc data-x=\"5\" data-y=\"10\" data-l=\"10\" data-h=\"3\" "
            + "data-taille=\"20\" data-gras=\"true\" data-align=\"left\"><span data-champ=\"destinataire.nom\">Nom</span></div>";
    private static final String BLOC_PIED = "<div data-bloc data-x=\"2.5\" data-y=\"20\" data-l=\"8\" data-h=\"2\" "
            + "data-taille=\"9\">PIED <span data-champ=\"destinataire.dossier\">D</span></div>";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureFormulairesBarid fixture;
    @Autowired private FixtureEtatVentes ev;

    private String p;
    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        p = FixtureEtatVentes.prefixe("BAMB");
        CfgTranche t = fixture.tranche("T-" + p, true, fixture.pays("PAYS-" + p));
        dossier = fixture.dossier(t, "DOS-" + p, ev.statutEnCours("ENC-" + p),
                fixture.client("BLOCUN" + p, null, Adresse.aucune()));
        fixture.coAcquereur(dossier, fixture.client("BLOCDEUX" + p, null, Adresse.aucune()),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        fixture.relire();
    }

    private static String repete(String... blocs) {
        return "<div data-repeter=\"destinataires\" data-saut-page>" + String.join("", blocs) + "</div>";
    }

    private ResultActions creer(String contenu) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", "TST_BLOC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        m.put("libelle", "Blocs");
        m.put("contenu", contenu);
        m.put("miseEnPage", Map.of("margeHaut", 0, "margeBas", 0, "margeGauche", 0, "margeDroite", 0));
        return mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(m)));
    }

    private String idCree(String contenu) throws Exception {
        String reponse = creer(contenu).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("id").asString();
    }

    private byte[] pdf() throws Exception {
        return mockMvc.perform(get(BASE + "/{id}/generer", idCree(repete(BLOC_NOM, BLOC_PIED)))
                        .param("ficheId", dossier.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static Glyphe exiger(Glyphe glyphe, String mot) {
        assertThat(glyphe).as("mot %s dans la page", mot).isNotNull();
        return glyphe;
    }

    // ------------------------------------------------------------------ enregistrement

    @Test
    void bloc_enregistre_contenuConserve() throws Exception {
        String id = idCree(repete(BLOC_NOM));

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").value(containsStringIgnoringCase("data-bloc")))
                .andExpect(jsonPath("$.contenu").value(containsStringIgnoringCase("data-x=\"5\"")));
    }

    @Test
    void bloc_horsPageEnLargeur_400AvecLeBlocFautif() throws Exception {
        creer(repete("<div data-bloc data-x=\"15\" data-y=\"1\" data-l=\"7\" data-h=\"1\">X</div>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsStringIgnoringCase("bloc")));
    }

    @Test
    void bloc_horsPageEnHauteur_400AvecLeBlocFautif() throws Exception {
        creer(repete("<div data-bloc data-x=\"1\" data-y=\"28\" data-l=\"5\" data-h=\"2\">X</div>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsStringIgnoringCase("bloc")));
    }

    @Test
    void bloc_auBordDeLaPage_accepte() throws Exception {
        creer(repete("<div data-bloc data-x=\"11\" data-y=\"1\" data-l=\"10\" data-h=\"1\">X</div>"))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "73"})
    void taille_horsBornes6a72_400(String taille) throws Exception {
        creer(repete("<div data-bloc data-x=\"1\" data-y=\"1\" data-l=\"5\" data-h=\"1\" data-taille=\"" + taille
                + "\">X</div>"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "72"})
    void taille_auxBornes_acceptee(String taille) throws Exception {
        creer(repete("<div data-bloc data-x=\"1\" data-y=\"1\" data-l=\"5\" data-h=\"1\" data-taille=\"" + taille
                + "\">X</div>"))
                .andExpect(status().isCreated());
    }

    @Test
    void bloc_contenantUnTableau_400() throws Exception {
        creer(repete("<div data-bloc data-x=\"1\" data-y=\"1\" data-l=\"5\" data-h=\"3\">"
                + "<table><tbody><tr><td>X</td></tr></tbody></table></div>"))
                .andExpect(status().isBadRequest());
    }

    /** Image réellement déposée : le refus vient du bloc, pas d'une image inconnue. */
    @Test
    void bloc_contenantUneImage_400() throws Exception {
        String image = ImagesModeleOutils.deposerPng(mockMvc, json, 10);

        creer(repete("<div data-bloc data-x=\"1\" data-y=\"1\" data-l=\"5\" data-h=\"3\">" + image + "</div>"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bloc_compatibleAvecDataSi() throws Exception {
        creer(repete("<div data-bloc data-x=\"1\" data-y=\"1\" data-l=\"5\" data-h=\"1\" data-si=\"destinataire.titre\">"
                + "<span data-champ=\"destinataire.titre\">T</span></div>"))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ rendu

    @Test
    void rendu_blocPlaceAuCentimetrePres() throws Exception {
        Glyphe g = exiger(LecturePdf.glyphe(pdf(), 1, "BLOCUN"), "BLOCUN");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(g.x()).as("x").isCloseTo(X_CM * LecturePdf.CM, within(TOLERANCE_PT));
        soft.assertThat(g.ligneDeBase()).as("ligne de base sous le haut du bloc")
                .isBetween(Y_CM * LecturePdf.CM, Y_CM * LecturePdf.CM + TAILLE * 1.3f);
        soft.assertAll();
    }

    @Test
    void rendu_tailleEnPointsEtGras() throws Exception {
        byte[] pdf = pdf();
        Glyphe nom = exiger(LecturePdf.glyphe(pdf, 1, "BLOCUN"), "BLOCUN");
        Glyphe pied = exiger(LecturePdf.glyphe(pdf, 1, "PIED"), "PIED");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nom.taille()).as("taille du bloc gras").isCloseTo(TAILLE, within(0.5f));
        soft.assertThat(nom.police()).as("police du bloc gras").containsIgnoringCase("Helvetica")
                .containsIgnoringCase("Bold");
        soft.assertThat(pied.taille()).as("taille du bloc normal").isCloseTo(9f, within(0.5f));
        soft.assertThat(pied.police()).as("police du bloc normal").doesNotContainIgnoringCase("Bold");
        soft.assertThat(pied.x()).as("x décimal").isCloseTo(2.5f * LecturePdf.CM, within(TOLERANCE_PT));
        soft.assertAll();
    }

    @Test
    void rendu_blocRattacheALaPageDeSaRepetition() throws Exception {
        byte[] pdf = pdf();
        Glyphe page2 = exiger(LecturePdf.glyphe(pdf, 2, "BLOCDEUX"), "BLOCDEUX");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).isEqualTo(2);
        soft.assertThat(LecturePdf.textePage(pdf, 1)).doesNotContain("BLOCDEUX");
        soft.assertThat(page2.x()).isCloseTo(X_CM * LecturePdf.CM, within(TOLERANCE_PT));
        soft.assertThat(page2.ligneDeBase()).isBetween(Y_CM * LecturePdf.CM, Y_CM * LecturePdf.CM + TAILLE * 1.3f);
        soft.assertAll();
    }
}

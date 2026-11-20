package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_PROCEDURES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * N6a Procédures d'achat sur le moteur {@code /api/referentiels-admin}, slug {@code procedures-achat} (FEATURE-n6-demandes-achat.md
 * « Endpoints ») : droit {@code Yes.Buy.Front.Config.BuySelectionKind}, suppression bloquée par {@code BuySelection.BuySelectionKindId}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProceduresAchatReferentielTest {

    private static final String SLUG = "procedures-achat";
    private static final String NOMBRE = "SELECT COUNT(*) FROM BuySelectionKind WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat fixture;
    @Autowired private FixtureNomenclatureAchat nomenclature;

    private Cadre c;
    private RequestPostProcessor gestionnaire;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("PA");
        gestionnaire = nomenclature.avecDroits(DROIT_PROCEDURES);
    }

    private MockHttpServletResponse envoyer(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andReturn().getResponse();
    }

    private static String corpsDe(MockHttpServletResponse r) throws Exception {
        return r.getContentAsString(StandardCharsets.UTF_8);
    }

    private int nombre(UUID id) {
        return fixture.compter(NOMBRE, id.toString());
    }

    @Test
    void lecture_avecLeDroit_200_sansLeDroit_403() throws Exception {
        MockHttpServletResponse avec = envoyer(get(TABLE, SLUG).with(gestionnaire));
        MockHttpServletResponse sans = envoyer(get(TABLE, SLUG).with(nomenclature.avecDroits()));

        assertThat(avec.getStatus()).as(corpsDe(avec)).isEqualTo(200);
        List<String> ids = JsonPath.read(corpsDe(avec), "$[*].id");
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ids).contains(c.procedure().toString());
            s.assertThat(sans.getStatus()).isEqualTo(403);
        });
    }

    @Test
    void creation_codeEtDescription_201() throws Exception {
        String code = "PROC-N-" + c.jeton();

        MockHttpServletResponse r = envoyer(post(TABLE, SLUG).with(gestionnaire).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("code", code).avec("description", "Procédure " + code).json()));

        assertThat(r.getStatus()).as(corpsDe(r)).isEqualTo(201);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BuySelectionKind WHERE Code = ?", code)).isEqualTo(1);
    }

    @Test
    void suppression_procedureCiteeParUneDia_409_procedureLibre_supprimee() throws Exception {
        fixture.demande(c, c.code("PA"), c.prep());
        UUID libre = fixture.procedure("PLIBRE-" + c.jeton(), c.entite());

        MockHttpServletResponse refus = envoyer(delete(LIGNE, SLUG, c.procedure()).with(gestionnaire));
        MockHttpServletResponse temoin = envoyer(delete(LIGNE, SLUG, libre).with(gestionnaire));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.getStatus()).isEqualTo(409);
            s.assertThat(String.valueOf((Object) JsonPath.read(corpsDeSilencieux(refus), "$.detail")))
                    .startsWith("Suppression impossible : ");
            s.assertThat(nombre(c.procedure())).isEqualTo(1);
            s.assertThat(temoin.getStatus()).isBetween(200, 204);
            s.assertThat(nombre(libre)).isZero();
        });
    }

    private static String corpsDeSilencieux(MockHttpServletResponse r) {
        try {
            String corps = r.getContentAsString(StandardCharsets.UTF_8);
            return corps.isEmpty() ? "{\"detail\":null}" : corps;
        } catch (java.io.UnsupportedEncodingException e) {
            return "{\"detail\":null}";
        }
    }
}

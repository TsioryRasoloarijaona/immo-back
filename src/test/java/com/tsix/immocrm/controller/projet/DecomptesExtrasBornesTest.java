package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureDecompteExtras;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.champs;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.json;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Bornes de longueur et catégorie inconnue d'une extra : 400 français avant toute écriture. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesExtrasBornesTest {

    private static final String EXTRAS = "extras";
    private static final String DESIGNATION = "designation";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureDecompteExtras extras;

    private UUID d;
    private RequestPostProcessor modifier;

    @BeforeEach
    void setUp() {
        String jeton = FixtureProjetLots.jeton();
        UUID projet = fixture.projet("PB-" + jeton);
        UUID lot = fixture.lot(projet, "LB-" + jeton, fixture.fournisseur("FB-" + jeton));
        modifier = decomptes.compte(projet, jeton, DROIT_MODIFIER);
        d = fixture.decompte(lot, "B1-" + jeton, 1, Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private MockHttpServletResponse enregistrer(Map<String, Object> extra) throws Exception {
        decomptes.flush();
        String corps = json(champs(EXTRAS, List.of(extra)));
        return mockMvc.perform(put(DECOMPTES + "/" + d).contentType(MediaType.APPLICATION_JSON)
                .content(corps).with(modifier)).andReturn().getResponse();
    }

    private void assertRefuse(Map<String, Object> extra, String champ) throws Exception {
        MockHttpServletResponse r = enregistrer(extra);

        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(r.getContentAsString(StandardCharsets.UTF_8)).contains(champ);
        assertThat(extras.extras(d)).isEmpty();
    }

    @Test
    void codeTropLong_400() throws Exception {
        assertRefuse(champs(DESIGNATION, "Charpente", "code", "c".repeat(256)), "code");
    }

    @Test
    void uniteTropLongue_400() throws Exception {
        assertRefuse(champs(DESIGNATION, "Charpente", "unite", "u".repeat(256)), "unité");
    }

    @Test
    void designationTropLongue_400() throws Exception {
        assertRefuse(champs(DESIGNATION, "d".repeat(256)), "désignation");
    }

    @Test
    void notesTropLongues_400() throws Exception {
        assertRefuse(champs(DESIGNATION, "Charpente", "notes", "n".repeat(1001)), "notes");
    }

    @Test
    void categorieInconnue_400() throws Exception {
        assertRefuse(champs(DESIGNATION, "Charpente", "categorieId", UUID.randomUUID().toString()), "Catégorie inconnue");
    }

    @Test
    void valeursALaBorne_acceptees() throws Exception {
        MockHttpServletResponse r = enregistrer(champs(DESIGNATION, "d".repeat(255), "code", "c".repeat(255),
                "unite", "u".repeat(255), "notes", "n".repeat(1000)));

        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(extras.extras(d)).hasSize(1);
    }
}

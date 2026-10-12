package com.tsix.immocrm.controller.chantier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.chantier.JsonChantier.element;
import static com.tsix.immocrm.controller.chantier.JsonChantier.montant;
import static com.tsix.immocrm.fixtures.FixtureChantier.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetsChantierTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureChantier fixture;

    private FixtureChantier.Chaine chaine;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        chaine = fixture.creer();
        admin = fixture.administrateur();
    }

    private String projets() throws Exception {
        return mockMvc.perform(get("/api/chantier/projets").with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void liste_totauxDuProjet_egauxALaSommeDesLots() throws Exception {
        Map<String, Object> projetA = element(projets(), "$", chaine.projetA().getId());

        // 3 lots : bordereau (HT 2 666,666667 / TTC 3 200), forfait (5 000 / 6 000), vide (0).
        assertAll(
                () -> assertThat(projetA).containsEntry("nbLots", 3).containsEntry("nbDecomptes", 3)
                        .containsEntry("estCentreDeCouts", false),
                () -> assertThat(montant(projetA, "totalMarchesHT")).isEqualByComparingTo("7666.666667"),
                () -> assertThat(montant(projetA, "totalMarchesTTC")).isEqualByComparingTo("9200"),
                () -> assertThat(montant(projetA, "totalDecomptesTTC")).isEqualByComparingTo("840"),
                () -> assertThat(montant(projetA, "totalFacture")).isEqualByComparingTo("346"),
                () -> assertThat(montant(projetA, "totalRegle")).isEqualByComparingTo("216"),
                () -> assertThat(montant(projetA, "totalResteARegler")).isEqualByComparingTo("130"),
                () -> assertThat(projetA.get("societeCode").toString()).startsWith("STE-CH-"));
    }

    @Test
    void liste_trieeParCode_croissant() throws Exception {
        List<String> codes = JsonPath.read(projets(), "$[*].code");

        assertThat(codes).isNotEmpty().isSortedAccordingTo(String::compareTo);
    }

    @Test
    void liste_projetSansLotAuCodeAnnuel_estUnCentreDeCouts() throws Exception {
        String json = projets();

        assertAll(
                () -> assertThat(element(json, "$", chaine.centreDeCouts().getId()))
                        .containsEntry("estCentreDeCouts", true).containsEntry("nbLots", 0),
                // Un projet avec un lot n'est jamais un centre de coûts.
                () -> assertThat(element(json, "$", chaine.projetB().getId()))
                        .containsEntry("estCentreDeCouts", false));
    }

    @Test
    void fiche_totauxEtTextesVidesEnNull() throws Exception {
        mockMvc.perform(get("/api/chantier/projets/{id}", chaine.projetA().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(chaine.projetA().getCode()))
                .andExpect(jsonPath("$.nbLots").value(3))
                .andExpect(jsonPath("$.totalFacture").value(346.0))
                .andExpect(jsonPath("$.notes").value(nullValue()))
                .andExpect(jsonPath("$.adresse").value(nullValue()))
                .andExpect(jsonPath("$.dateDebut").value(nullValue()))
                .andExpect(jsonPath("$.creePar").value("fixture"));
    }

    @Test
    void fiche_idInconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/api/chantier/projets/{id}", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void fiche_idMalForme_renvoie400() throws Exception {
        mockMvc.perform(get("/api/chantier/projets/{id}", "pas-un-uuid").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }
}

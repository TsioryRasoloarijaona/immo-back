package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.ids;
import static com.tsix.immocrm.fixtures.AppelsTiers.lier;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Famille Tiers « agences » et unicité du lien « Vendeur d'agence » — FEATURE-agences-vente.md §3 (R-INS1), §8 « Lien ». */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersAgencesTest {

    private static final String AGENCES = "agences";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixtureTiers;
    @Autowired private FixtureAgences agences;

    private Jeu j;
    private RequestPostProcessor admin;
    private UUID agenceA;
    private UUID agenceB;
    private UUID relation;

    @BeforeEach
    void setUp() {
        j = fixtureTiers.jeu();
        admin = connecte(j.admin());
        agenceA = agences.agence(j.code("AGA"));
        agenceB = agences.agence(j.code("AGB"));
        relation = agences.relationVendeur();
    }

    // ------------------------------------------------------------------ R-INS1

    @Test
    void rIns1_vendeurDejaLieAUneAgence_secondLienVendeurDAgence_409_rienEcrit() throws Exception {
        UUID vendeur = agences.vendeur(j.code("VEN"));
        agences.lier(agenceA, vendeur);

        mockMvc.perform(lier(agenceB, vendeur, relation, admin)).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fixtureTiers.lignesLien(agenceB, vendeur, relation)).isZero(),
                () -> assertThat(agences.liensDuVendeur(vendeur)).isEqualTo(1));
    }

    @Test
    void rIns1_temoin_vendeurSansAgence_lienVendeurDAgence_2xx() throws Exception {
        UUID vendeur = agences.vendeur(j.code("VLIB"));

        mockMvc.perform(lier(agenceB, vendeur, relation, admin)).andExpect(status().is2xxSuccessful());

        assertThat(fixtureTiers.lignesLien(agenceB, vendeur, relation)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ famille « agences » (§3 procédure, étape 1)

    @Test
    void familleAgences_liste_rendLesFichesDeNatureAGV_pasLesContactsOrdinaires() throws Exception {
        UUID contact = agences.contactOrdinaire(j.code("CNT"));

        String page = mockMvc.perform(lister(AGENCES, admin).param("recherche", j.jeton()).param("size", "200"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(ids(page)).contains(agenceA.toString(), agenceB.toString()).doesNotContain(contact.toString());
    }

    @Test
    void familleAgences_fiche_200() throws Exception {
        mockMvc.perform(lire(AGENCES, agenceA, admin)).andExpect(status().isOk());
    }
}

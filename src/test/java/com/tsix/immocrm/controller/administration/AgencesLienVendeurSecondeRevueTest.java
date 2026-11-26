package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue A1a (FEATURE-agences-vente.md R-INS1) : le lien « Vendeur d'agence » s'écrit aussi par
 * {@code POST /api/prospects/{id}/contacts}, qui accepte tout tiers et tout type de lien. Un 2ᵉ lien d'un même vendeur
 * par ce chemin étend la liste des vendeurs d'une autre agence (AgenceDuCompte) : il doit être refusé comme par la fiche.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesLienVendeurSecondeRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixtureTiers;
    @Autowired private FixtureAgences agences;

    @Test
    void rIns1_secondLienVendeurDAgence_parLaRouteContactsDuProspect_409_rienEcrit() throws Exception {
        Jeu j = fixtureTiers.jeu();
        UUID agenceA = agences.agence(j.code("AGA"));
        UUID agenceB = agences.agence(j.code("AGB"));
        UUID vendeur = agences.vendeur(j.code("VEN"));
        agences.lier(agenceA, vendeur);
        UUID relation = agences.relationVendeur();

        mockMvc.perform(post("/api/prospects/{id}/contacts", agenceB).with(connecte(j.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationTypeId\":\"" + relation + "\",\"contactProspectId\":\"" + vendeur + "\"}"))
                .andExpect(status().isConflict());

        assertThat(agences.liensDuVendeur(vendeur)).isEqualTo(1);
    }

    /** R-V2c : la chaîne compte → vendeur → agence ne doit pas être écrite par le compte agence lui-même. */
    @Test
    void compteAgence_rattacheUnCommercialDuSiegeASonAgence_parLaRouteContactsDuProspect_refuse() throws Exception {
        Jeu j = fixtureTiers.jeu();
        UUID agenceA = agences.agence(j.code("AGA"));
        UUID vendeurA = agences.vendeur(j.code("VENA"));
        agences.lier(agenceA, vendeurA);
        UUID commercialSiege = agences.vendeur(j.code("SIEGE"));
        UUID relation = agences.relationVendeur();
        var compteA = agences.compteAgence(j.code("CPTA"), vendeurA);

        int statut = mockMvc.perform(post("/api/prospects/{id}/contacts", agenceA).with(connecte(compteA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationTypeId\":\"" + relation + "\",\"contactProspectId\":\"" + commercialSiege + "\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(statut).isIn(403, 404);
        assertThat(agences.liensDuVendeur(commercialSiege)).isZero();
    }
}

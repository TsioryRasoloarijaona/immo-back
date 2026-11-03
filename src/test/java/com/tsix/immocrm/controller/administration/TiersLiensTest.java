package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.LIEN;
import static com.tsix.immocrm.fixtures.AppelsTiers.LIENS;
import static com.tsix.immocrm.fixtures.AppelsTiers.lier;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liens tiers / contact ({@code CfgTierContact}) — FEATURE-admin-tiers.md, règle 12. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersLiensTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;
    private UUID fournisseur;
    private UUID contact;
    private UUID relation;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
        fournisseur = j.tiers(FOURNISSEURS, "F1").inserer();
        contact = j.tiers(CONTACTS, "C1").inserer();
        relation = j.relation();
    }

    @Test
    void liens_lecture_rendLeContactLie() throws Exception {
        j.lien(fournisseur, contact, relation);

        String corps = mockMvc.perform(get(LIENS, fournisseur).with(connecte(j.admin())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(corps).contains(contact.toString());
    }

    @Test
    void liens_creation_ecritCfgTierContact_pasCfgTierCfgContactId() throws Exception {
        mockMvc.perform(lier(fournisseur, contact, relation, connecte(j.admin()))).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(fixture.lignesLien(fournisseur, contact, relation)).isEqualTo(1),
                () -> assertThat(fixture.ligne(fournisseur)).containsEntry("CfgContactId", null));
    }

    @Test
    void r12_contactInconnu_400_rienEcrit() throws Exception {
        mockMvc.perform(lier(fournisseur, UUID.randomUUID(), relation, connecte(j.admin())))
                .andExpect(status().isBadRequest());

        assertThat(fixture.liens(fournisseur)).isZero();
    }

    @Test
    void r9_relationInconnue_400_rienEcrit() throws Exception {
        mockMvc.perform(lier(fournisseur, contact, UUID.randomUUID(), connecte(j.admin())))
                .andExpect(status().isBadRequest());

        assertThat(fixture.liens(fournisseur)).isZero();
    }

    @Test
    void r12_doublonMemeTiersMemeContact_409_memeAvecAutreRelation() throws Exception {
        j.lien(fournisseur, contact, relation);

        mockMvc.perform(lier(fournisseur, contact, j.relation(), connecte(j.admin()))).andExpect(status().isConflict());

        assertThat(fixture.liens(fournisseur)).isEqualTo(1);
    }

    @Test
    void r12_deleteDUnLienDUnAutreTiers_404_lienGarde() throws Exception {
        UUID autre = j.tiers(FOURNISSEURS, "F2").inserer();
        UUID lien = j.lien(autre, contact, relation);
        UUID sien = j.lien(fournisseur, contact, relation);
        mockMvc.perform(delete(LIEN, fournisseur, sien).with(connecte(j.admin()))).andExpect(status().is2xxSuccessful());

        mockMvc.perform(delete(LIEN, fournisseur, lien).with(connecte(j.admin()))).andExpect(status().isNotFound());

        assertThat(fixture.liens(autre)).isEqualTo(1);
    }

    @Test
    void liens_suppression_retireLaLigne_leContactReste() throws Exception {
        UUID lien = j.lien(fournisseur, contact, relation);

        mockMvc.perform(delete(LIEN, fournisseur, lien).with(connecte(j.admin()))).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(fixture.liens(fournisseur)).isZero(),
                () -> assertThat(fixture.existe(contact)).isTrue());
    }
}

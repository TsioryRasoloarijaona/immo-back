package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression — FEATURE-admin-tiers.md, règle 11. La fixture pose les FK de la CI dans la transaction du test :
 * NO_ACTION (client dont le commercial est le tiers, lien où il est le contact), CASCADE (liens, actions).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersSuppressionTest {

    private static final String FOU = FOURNISSEURS.famille();

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
        fixture.poserClesEtrangeres();
    }

    private String refus(String famille, UUID id) throws Exception {
        String corps = mockMvc.perform(supprimer(famille, id, connecte(j.admin())))
                .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        fixture.oublierEcritureRefusee();
        return corps;
    }

    @Test
    void r11_referenceNoAction_409_messageMetier() throws Exception {
        UUID fournisseur = j.tiers(FOURNISSEURS, "F1").inserer();
        j.referencer(fournisseur);

        String corps = refus(FOU, fournisseur);

        String detail = JsonPath.read(corps, "$.detail");
        assertThat(detail).isNotBlank().doesNotContainIgnoringCase("constraint").doesNotContainIgnoringCase("sql");
    }

    @Test
    void r11_referenceNoAction_409_aucuneLigneTouchee() throws Exception {
        UUID fournisseur = j.tiers(FOURNISSEURS, "F2").inserer();
        UUID contact = j.tiers(CONTACTS, "C1").inserer();
        j.lien(fournisseur, contact, j.relation());
        j.action(fournisseur);
        j.referencer(fournisseur);
        Map<String, Object> avant = fixture.ligne(fournisseur);

        refus(FOU, fournisseur);

        assertAll(
                () -> assertThat(fixture.ligne(fournisseur)).isEqualTo(avant),
                () -> assertThat(fixture.liens(fournisseur)).isEqualTo(1),
                () -> assertThat(fixture.actions(fournisseur)).isEqualTo(1));
    }

    @Test
    void r11_contactLieAUnTiers_409_ligneGardee() throws Exception {
        UUID fournisseur = j.tiers(FOURNISSEURS, "F3").inserer();
        UUID contact = j.tiers(CONTACTS, "C2").inserer();
        j.lien(fournisseur, contact, j.relation());

        refus(CONTACTS.famille(), contact);

        assertAll(
                () -> assertThat(fixture.existe(contact)).isTrue(),
                () -> assertThat(fixture.liens(fournisseur)).isEqualTo(1));
    }

    @Test
    void r11_tiersLibre_204_ligneSupprimee_liensEtActionsPartis() throws Exception {
        UUID fournisseur = j.tiers(FOURNISSEURS, "F4").inserer();
        UUID contact = j.tiers(CONTACTS, "C3").inserer();
        j.lien(fournisseur, contact, j.relation());
        j.action(fournisseur);

        mockMvc.perform(supprimer(FOU, fournisseur, connecte(j.admin()))).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(fixture.existe(fournisseur)).isFalse(),
                () -> assertThat(fixture.liens(fournisseur)).isZero(),
                () -> assertThat(fixture.actions(fournisseur)).isZero(),
                () -> assertThat(fixture.existe(contact)).as("le contact lié reste").isTrue());
    }

    @Test
    void r11_contactLibre_204() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C4").inserer();

        mockMvc.perform(supprimer(CONTACTS.famille(), contact, connecte(j.admin()))).andExpect(status().isNoContent());

        assertThat(fixture.existe(contact)).isFalse();
    }
}

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsMorale;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fiche et cloisonnement par famille — FEATURE-admin-tiers.md, règle 2, {@code FicheTiersDto}, données de paie. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersFicheTest {

    private static final String FOU = FOURNISSEURS.famille();
    private static final String CNT = CONTACTS.famille();

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private String ok(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ FicheTiersDto

    @Test
    void fiche_rendLesLibellesEtLesTraces() throws Exception {
        UUID nature = j.nature(FOURNISSEURS);
        LocalDateTime cree = LocalDateTime.of(2026, 3, 10, 9, 15);
        LocalDateTime modifie = LocalDateTime.of(2026, 3, 11, 10, 30);
        UUID id = j.tiers(FOURNISSEURS, "F1").avec("CfgTierKindId", nature).avec("AdmCountryId", j.pays())
                .avec("AdmSocialFormId", j.formeMorale()).avec("StlCurrencyId", j.devise())
                .avec("UserCreate", "Auteur A").avec("UserCreateDateTime", cree)
                .avec("UserUpdate", "Auteur B").avec("UserUpdateDateTime", modifie).inserer();

        String corps = ok(lire(FOU, id, connecte(j.admin())));

        Map<String, Object> fiche = JsonPath.read(corps, "$");
        String code = j.code("F1");
        assertThat(fiche).containsKeys("creeLe", "modifieLe")
                .containsEntry("id", id.toString()).containsEntry("code", code)
                .containsEntry("description", "Tiers " + code)
                .containsEntry("creePar", "Auteur A").containsEntry("modifiePar", "Auteur B")
                .containsEntry("natureId", nature.toString()).containsEntry("nature", j.libelleDe(nature))
                .containsEntry("paysId", j.pays().toString()).containsEntry("pays", j.libelleDe(j.pays()))
                .containsEntry("formeSocialeId", j.formeMorale().toString())
                .containsEntry("formeSociale", j.libelleDe(j.formeMorale()))
                .containsEntry("deviseId", j.devise().toString()).containsEntry("devise", j.libelleDe(j.devise()));
    }

    @Test
    void ficheEtListe_nExposentNiSalaireNiSecuriteSociale() throws Exception {
        String secu = "SECU-" + j.jeton();
        UUID id = j.tiers(FOURNISSEURS, "F2").avec("SocialSecurityNumber", secu)
                .avec("SalaryBase", new BigDecimal("98765.430000")).inserer();

        String fiche = ok(lire(FOU, id, connecte(j.admin())));
        String liste = ok(lister(FOU, connecte(j.admin())).param("recherche", j.jeton()));

        assertAll(
                () -> assertThat(fiche).contains(id.toString()).doesNotContain(secu, "98765"),
                () -> assertThat(liste).contains(id.toString()).doesNotContain(secu, "98765"));
    }

    @Test
    void idInconnu_404_temoinFicheExistante200() throws Exception {
        UUID id = j.tiers(FOURNISSEURS, "F3").inserer();
        ok(lire(FOU, id, connecte(j.admin())));

        mockMvc.perform(lire(FOU, UUID.randomUUID(), connecte(j.admin()))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ règle 2

    @Test
    void r2_get_idDUnAutreType_404() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C1").inserer();
        UUID fournisseur = j.tiers(FOURNISSEURS, "F4").inserer();
        ok(lire(CNT, contact, connecte(j.admin())));

        mockMvc.perform(lire(FOU, contact, connecte(j.admin()))).andExpect(status().isNotFound());
        mockMvc.perform(lire(CNT, fournisseur, connecte(j.admin()))).andExpect(status().isNotFound());
    }

    @Test
    void r2_put_contactParFournisseurs_404_rienNeChange() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C2").inserer();
        Map<String, Object> avant = fixture.ligne(contact);
        ok(modifier(CNT, j.tiers(CONTACTS, "C3").inserer(), corpsMorale(j, "Temoin " + j.jeton()), connecte(j.admin())));

        mockMvc.perform(modifier(FOU, contact, corpsMorale(j, "Pirate " + j.jeton()), connecte(j.admin())))
                .andExpect(status().isNotFound());

        assertThat(fixture.ligne(contact)).isEqualTo(avant);
    }

    @Test
    void r2_delete_contactParFournisseurs_404_ligneGardee() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C4").inserer();
        UUID temoin = j.tiers(FOURNISSEURS, "F5").inserer();
        mockMvc.perform(supprimer(FOU, temoin, connecte(j.admin()))).andExpect(status().isNoContent());

        mockMvc.perform(supprimer(FOU, contact, connecte(j.admin()))).andExpect(status().isNotFound());

        assertThat(fixture.existe(contact)).isTrue();
    }
}

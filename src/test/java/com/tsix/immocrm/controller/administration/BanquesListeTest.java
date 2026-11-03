package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureBanques;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.AppelsTiers.NATURES;
import static com.tsix.immocrm.fixtures.AppelsTiers.corpsMorale;
import static com.tsix.immocrm.fixtures.AppelsTiers.ids;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureBanques.corps;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.BANQUES;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.ORGANISMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste, recherche, natures et cloisonnement — FEATURE-admin-banques-organismes.md, section Back et règle 1. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BanquesListeTest {

    private static final String BAN = BANQUES.famille();
    private static final String ORG = ORGANISMES.famille();
    private static final String RECHERCHE = "recherche";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBanques banques;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = banques.jeu();
    }

    private String page(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String duJeton(String famille) throws Exception {
        return page(lister(famille, connecte(j.admin())).param(RECHERCHE, j.jeton()));
    }

    private static Stream<Arguments> colonnesCherchees() {
        return Stream.of("banques", "organismes").flatMap(famille -> Stream.of("Code", "Description", "Mobile", "Phone",
                "Phone1", "Phone2", "Fax", "EMail", "WebSite", "Notes", "Address").map(c -> Arguments.of(famille, c)));
    }

    private static TypeTiers type(String famille) {
        return BAN.equals(famille) ? BANQUES : ORGANISMES;
    }

    // ------------------------------------------------------------------ règle 1

    @Test
    void r1_chaqueListe_neRendQueLeTypeDeSaFamille() throws Exception {
        UUID banque = j.tiers(BANQUES, "B1").inserer();
        UUID organisme = j.tiers(ORGANISMES, "O1").inserer();
        j.tiers(FOURNISSEURS, "F1").inserer();
        j.tiersSansType("N1");

        String listeBanques = duJeton(BAN);
        String listeOrganismes = duJeton(ORG);

        assertAll(
                () -> assertThat(ids(listeBanques)).containsExactly(banque.toString()),
                () -> assertThat(ids(listeOrganismes)).containsExactly(organisme.toString()));
    }

    @Test
    void r1_get_idDUnAutreType_404_temoin200() throws Exception {
        UUID banque = j.tiers(BANQUES, "B2").inserer();
        UUID organisme = j.tiers(ORGANISMES, "O2").inserer();
        UUID fournisseur = j.tiers(FOURNISSEURS, "F2").inserer();
        page(lire(BAN, banque, connecte(j.admin())));

        mockMvc.perform(lire(BAN, organisme, connecte(j.admin()))).andExpect(status().isNotFound());
        mockMvc.perform(lire(ORG, banque, connecte(j.admin()))).andExpect(status().isNotFound());
        mockMvc.perform(lire(BAN, fournisseur, connecte(j.admin()))).andExpect(status().isNotFound());
        mockMvc.perform(lire(FOURNISSEURS.famille(), banque, connecte(j.admin()))).andExpect(status().isNotFound());
    }

    @Test
    void r1_put_idDUnAutreType_404_rienNeChange() throws Exception {
        UUID organisme = j.tiers(ORGANISMES, "O3").inserer();
        UUID banque = j.tiers(BANQUES, "B3").inserer();
        Map<String, Object> avant = banques.colonnes(organisme);
        page(modifier(BAN, banque, corps(j, j.code("B3"), "TEMOIN " + j.jeton()), connecte(j.admin())));

        mockMvc.perform(modifier(BAN, organisme, corps(j, j.code("PIRATE"), "PIRATE " + j.jeton()), connecte(j.admin())))
                .andExpect(status().isNotFound());

        assertThat(banques.colonnes(organisme)).isEqualTo(avant);
    }

    @Test
    void r1_put_banqueParFournisseurs_404() throws Exception {
        UUID banque = j.tiers(BANQUES, "B4").inserer();
        page(lire(BAN, banque, connecte(j.admin())));

        mockMvc.perform(modifier(FOURNISSEURS.famille(), banque, corpsMorale(j, "Pirate " + j.jeton()), connecte(j.admin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void r1_delete_idDUnAutreType_404_ligneGardee() throws Exception {
        UUID organisme = j.tiers(ORGANISMES, "O4").inserer();
        UUID temoin = j.tiers(BANQUES, "B5").inserer();
        mockMvc.perform(supprimer(BAN, temoin, connecte(j.admin()))).andExpect(status().isNoContent());

        mockMvc.perform(supprimer(BAN, organisme, connecte(j.admin()))).andExpect(status().isNotFound());

        assertThat(fixture.existe(organisme)).isTrue();
    }

    // ------------------------------------------------------------------ recherche

    @ParameterizedTest
    @MethodSource("colonnesCherchees")
    void back_recherche_contient_surChaqueColonne(String famille, String colonne) throws Exception {
        String marque = "Q" + j.jeton();
        UUID cible = j.tiers(type(famille), "T1").avec(colonne, "aa" + marque + "zz").inserer();
        j.tiers(type(famille), "T2").inserer();

        String page = page(lister(famille, connecte(j.admin())).param(RECHERCHE, marque));

        assertThat(ids(page)).containsExactly(cible.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ICE", "FiscalCode"})
    void back_recherche_horsDesColonnesDuSpec_neTrouvePas(String colonne) throws Exception {
        String marque = "Q" + j.jeton();
        j.tiers(BANQUES, "B6").avec(colonne, marque).inserer();

        String page = page(lister(BAN, connecte(j.admin())).param(RECHERCHE, marque));

        assertThat(ids(page)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"50%off", "50_off", "a[bc]d"})
    void back_recherche_caracteresDeMotifEchappes(String motif) throws Exception {
        String litteral = motif + j.jeton();
        String leurre = motif.replace("%", "xx").replace("_", "x").replace("[bc]", "b") + j.jeton();
        UUID cible = j.tiers(BANQUES, "B7").avec("Notes", litteral).inserer();
        j.tiers(BANQUES, "B8").avec("Notes", leurre).inserer();

        String page = page(lister(BAN, connecte(j.admin())).param(RECHERCHE, litteral));

        assertThat(ids(page)).containsExactly(cible.toString());
    }

    // ------------------------------------------------------------------ natures

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void back_natureId_ignore(TypeTiers type) throws Exception {
        UUID nature = j.nature(type);
        UUID avecNature = j.tiers(type, "T3").avec("CfgTierKindId", nature).inserer();
        UUID sansNature = j.tiers(type, "T4").inserer();

        String page = page(lister(type.famille(), connecte(j.admin())).param(RECHERCHE, j.jeton())
                .param("natureId", j.nature(type).toString()));

        assertThat(ids(page)).containsExactlyInAnyOrder(avecNature.toString(), sansNature.toString());
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void back_natures_cfgTierKindDuType_seulement(TypeTiers type) throws Exception {
        UUID nature = j.nature(type);
        UUID autre = j.nature(FOURNISSEURS);

        String corps = page(get(NATURES, type.famille()).with(connecte(j.admin())));

        List<String> natures = JsonPath.read(corps, "$[*].id");
        assertThat(natures).contains(nature.toString()).doesNotContain(autre.toString());
    }

    // ------------------------------------------------------------------ LigneTiersDto

    @Test
    void back_ligneTiersDto_colonnesDeLEcran() throws Exception {
        UUID nature = j.nature(BANQUES);
        UUID contact = j.tiers(TypeTiers.CONTACTS, "C1").inserer();
        UUID id = j.tiers(BANQUES, "B9").avec("CfgTierKindId", nature).avec("FiscalCode", "MAT9")
                .avec("FiscalRegistry", "RC9").avec("Mobile", "0661000009").avec("Phone", "0522000009")
                .avec("Fax", "0522000010").avec("Address", "9 rue Test").avec("EMail", "b9@test.ma")
                .avec("WebSite", "b9.ma").avec("Notes", "Note B9").inserer();
        j.lien(id, contact, j.relation());

        String page = duJeton(BAN);

        Map<String, Object> ligne = JsonPath.read(page, "$.content[0]");
        String code = j.code("B9");
        assertThat(ligne).containsEntry("id", id.toString()).containsEntry("code", code)
                .containsEntry("description", "Tiers " + code).containsEntry("nature", j.libelleDe(nature))
                .containsEntry("matriculeFiscal", "MAT9").containsEntry("registreCommerce", "RC9")
                .containsEntry("mobile", "0661000009").containsEntry("telephone", "0522000009")
                .containsEntry("fax", "0522000010").containsEntry("adresse", "9 rue Test")
                .containsEntry("email", "b9@test.ma").containsEntry("siteWeb", "b9.ma").containsEntry("notes", "Note B9")
                .containsEntry("contacts", "Tiers " + j.code("C1"));
    }
}

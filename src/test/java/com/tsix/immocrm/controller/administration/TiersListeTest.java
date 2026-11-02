package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static com.tsix.immocrm.fixtures.AppelsTiers.NATURES;
import static com.tsix.immocrm.fixtures.AppelsTiers.ids;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste, recherche et natures — FEATURE-admin-tiers.md, règles 1, 3, 4, 15, tri et {@code LigneTiersDto}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersListeTest {

    private static final String FOU = FOURNISSEURS.famille();
    private static final String CNT = CONTACTS.famille();
    private static final String RECHERCHE = "recherche";
    private static final String NATURE_ID = "natureId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private String page(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String duJeton(String famille) throws Exception {
        return page(lister(famille, connecte(j.admin())).param(RECHERCHE, j.jeton()));
    }

    private static long total(String page) {
        return ((Number) JsonPath.read(page, "$.totalElements")).longValue();
    }

    // ------------------------------------------------------------------ règle 1

    @Test
    void r1_chaqueListe_neRendQueLeTypeDeSaFamille() throws Exception {
        UUID fournisseur = j.tiers(FOURNISSEURS, "F1").inserer();
        UUID contact = j.tiers(CONTACTS, "C1").inserer();

        String fournisseurs = duJeton(FOU);
        String contacts = duJeton(CNT);

        assertAll(
                () -> assertThat(ids(fournisseurs)).containsExactly(fournisseur.toString()),
                () -> assertThat(ids(contacts)).containsExactly(contact.toString()));
    }

    @Test
    void r1_ligneSansType_nApparaitNullePart() throws Exception {
        UUID sansType = j.tiersSansType("N1");
        UUID temoin = j.tiers(FOURNISSEURS, "F2").inserer();

        String fournisseurs = duJeton(FOU);
        String contacts = duJeton(CNT);

        assertAll(
                () -> assertThat(ids(fournisseurs)).contains(temoin.toString()).doesNotContain(sansType.toString()),
                () -> assertThat(ids(contacts)).doesNotContain(sansType.toString()));
    }

    // ------------------------------------------------------------------ règle 3

    @ParameterizedTest
    @ValueSource(strings = {"Code", "Description", "SocialReason", "Mobile", "Phone", "EMail", "ICE"})
    void r3_recherche_contient_surChaqueColonne(String colonne) throws Exception {
        String marque = "Q" + j.jeton();
        UUID cible = j.tiers(FOURNISSEURS, "F3").avec(colonne, "aa" + marque + "zz").inserer();
        j.tiers(FOURNISSEURS, "F4").inserer();

        String page = page(lister(FOU, connecte(j.admin())).param(RECHERCHE, marque));

        assertThat(ids(page)).containsExactly(cible.toString());
    }

    @Test
    void r3_recherche_horsDesColonnesDuSpec_neTrouvePas() throws Exception {
        String marque = "Q" + j.jeton();
        j.tiers(FOURNISSEURS, "F5").avec("Notes", marque).inserer();

        String page = page(lister(FOU, connecte(j.admin())).param(RECHERCHE, marque));

        assertThat(ids(page)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"50%off", "50_off", "a[bc]d"})
    void r3_caracteresDeMotif_echappes(String motif) throws Exception {
        String litteral = motif + j.jeton();
        String leurre = motif.replace("%", "xx").replace("_", "x").replace("[bc]", "b") + j.jeton();
        UUID cible = j.tiers(FOURNISSEURS, "F6").avec("SocialReason", litteral).inserer();
        j.tiers(FOURNISSEURS, "F7").avec("SocialReason", leurre).inserer();

        String page = page(lister(FOU, connecte(j.admin())).param(RECHERCHE, litteral));

        assertThat(ids(page)).containsExactly(cible.toString());
    }

    @Test
    void r3_rechercheVide_pasDeFiltre_memeTotalQueSansParametre() throws Exception {
        j.tiers(FOURNISSEURS, "F8").inserer();

        String sans = page(lister(FOU, connecte(j.admin())));
        String vide = page(lister(FOU, connecte(j.admin())).param(RECHERCHE, ""));

        assertThat(total(vide)).isEqualTo(total(sans)).isPositive();
    }

    // ------------------------------------------------------------------ règle 4

    @Test
    void r4_natureId_restreintLesFournisseurs() throws Exception {
        UUID nature = j.nature(FOURNISSEURS);
        UUID retenu = j.tiers(FOURNISSEURS, "F9").avec("CfgTierKindId", nature).inserer();
        j.tiers(FOURNISSEURS, "F10").avec("CfgTierKindId", j.nature(FOURNISSEURS)).inserer();
        j.tiers(FOURNISSEURS, "F11").inserer();

        String page = page(lister(FOU, connecte(j.admin())).param(RECHERCHE, j.jeton())
                .param(NATURE_ID, nature.toString()));

        assertThat(ids(page)).containsExactly(retenu.toString());
    }

    @Test
    void r4_natureAbsente_tout() throws Exception {
        j.tiers(FOURNISSEURS, "F12").avec("CfgTierKindId", j.nature(FOURNISSEURS)).inserer();
        j.tiers(FOURNISSEURS, "F13").inserer();

        assertThat(ids(duJeton(FOU))).hasSize(2);
    }

    @Test
    void natureId_ignoreePourLesContacts() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C2").avec("CfgTierKindId", j.nature(CONTACTS)).inserer();

        String page = page(lister(CNT, connecte(j.admin())).param(RECHERCHE, j.jeton())
                .param(NATURE_ID, j.nature(CONTACTS).toString()));

        assertThat(ids(page)).containsExactly(contact.toString());
    }

    // ------------------------------------------------------------------ tri et pagination

    @Test
    void tri_parCodeAscendant_puisPKey() throws Exception {
        String codeC = j.code("C");
        UUID c1 = j.tiers(FOURNISSEURS, "Z1").avec("Code", codeC).inserer();
        UUID c2 = j.tiers(FOURNISSEURS, "Z2").avec("Code", codeC).inserer();
        UUID b = j.tiers(FOURNISSEURS, "B").inserer();
        UUID a = j.tiers(FOURNISSEURS, "A").inserer();

        List<String> ordre = ids(duJeton(FOU));

        List<String> memeCode = fixture.ordreParPKey(c1, c2);
        assertThat(ordre).containsExactly(a.toString(), b.toString(), memeCode.get(0), memeCode.get(1));
    }

    @Test
    void pagination_tailleParDefaut50() throws Exception {
        String page = page(lister(FOU, connecte(j.admin())));

        assertAll(
                () -> assertThat(((Number) JsonPath.read(page, "$.page")).intValue()).isZero(),
                () -> assertThat(((Number) JsonPath.read(page, "$.size")).intValue()).isEqualTo(50));
    }

    // ------------------------------------------------------------------ règle 15

    @ParameterizedTest
    @CsvSource({"-1, 50", "1000001, 50", "0, 0", "0, 201"})
    void r15_horsBornes_400(int numero, int taille) throws Exception {
        mockMvc.perform(lister(FOU, connecte(j.admin())).param("page", String.valueOf(numero))
                        .param("size", String.valueOf(taille)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1000000, 200"})
    void r15_bornesIncluses_200(int numero, int taille) throws Exception {
        mockMvc.perform(lister(FOU, connecte(j.admin())).param("page", String.valueOf(numero))
                        .param("size", String.valueOf(taille)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ famille

    @ParameterizedTest
    @ValueSource(strings = {"prospects", "inconnue"})
    void familleInconnue_404_temoinFournisseurs200(String famille) throws Exception {
        mockMvc.perform(lister(FOU, connecte(j.admin()))).andExpect(status().isOk());

        mockMvc.perform(lister(famille, connecte(j.admin()))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ LigneTiersDto

    @Test
    void ligneTiersDto_nomsDuFil() throws Exception {
        UUID nature = j.nature(FOURNISSEURS);
        UUID id = j.tiers(FOURNISSEURS, "F14")
                .avec("Phone", "0522000001").avec("Fax", "0522000002").avec("Mobile", "0661000003")
                .avec("EMail", "f14@test.ma").avec("WebSite", "f14.ma").avec("Address", "1 rue Test").avec("Notes", "Note F14")
                .avec("IsExtern", true).avec("ICE", "ICE14").avec("CfgTierKindId", nature).avec("AdmCountryId", j.pays())
                .avec("AdmCountryResidenceId", j.pays()).avec("BankAgency", "Agence 14").avec("Rib", "RIB14")
                .avec("Iban", "IBAN14").avec("FiscalCode", "MAT14").avec("FiscalRegistry", "RC14")
                .avec("FiscalDouane", "DOU14").avec("CodeCountable", "CC14").avec("CodeCountableCollective", "CCC14")
                .avec("AdmLiabilityId", j.assujettissement()).inserer();

        String page = duJeton(FOU);

        Map<String, Object> ligne = JsonPath.read(page, "$.content[0]");
        String code = j.code("F14");
        assertThat(ligne).containsKey("contacts")
                .containsEntry("id", id.toString()).containsEntry("code", code)
                .containsEntry("description", "Tiers " + code).containsEntry("raisonSociale", "Tiers " + code)
                .containsEntry("telephone", "0522000001").containsEntry("fax", "0522000002")
                .containsEntry("mobile", "0661000003").containsEntry("email", "f14@test.ma")
                .containsEntry("siteWeb", "f14.ma").containsEntry("adresse", "1 rue Test").containsEntry("notes", "Note F14")
                .containsEntry("estPrestataire", true).containsEntry("ice", "ICE14")
                .containsEntry("nature", j.libelleDe(nature)).containsEntry("pays", j.libelleDe(j.pays()))
                .containsEntry("paysResidence", j.libelleDe(j.pays())).containsEntry("agence", "Agence 14")
                .containsEntry("rib", "RIB14").containsEntry("iban", "IBAN14").containsEntry("matriculeFiscal", "MAT14")
                .containsEntry("registreCommerce", "RC14").containsEntry("douane", "DOU14")
                .containsEntry("codeComptable", "CC14").containsEntry("codeComptableCollectif", "CCC14")
                .containsEntry("assujettissement", j.libelleDe(j.assujettissement()));
    }

    // ------------------------------------------------------------------ natures

    @Test
    void natures_rendLesCfgTierKindDuType_seulement() throws Exception {
        UUID nature = j.nature(FOURNISSEURS);
        UUID autre = j.nature(CONTACTS);

        String corps = page(get(NATURES, FOU).with(connecte(j.admin())));

        List<String> idsNatures = JsonPath.read(corps, "$[*].id");
        List<Map<String, Object>> retenue = JsonPath.read(corps, "$[?(@.id == '" + nature + "')]");
        assertAll(
                () -> assertThat(idsNatures).contains(nature.toString()).doesNotContain(autre.toString()),
                () -> assertThat(retenue).singleElement().satisfies(n -> assertThat(n)
                        .containsEntry("code", j.codeDe(nature)).containsEntry("libelle", j.libelleDe(nature))));
    }
}

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
class DecomptesChantierTest {

    private static final String DECOMPTES = "/api/chantier/decomptes";
    private static final String DECOMPTE = "/api/chantier/decomptes/{id}";
    private static final String LOT_ID = "lotId";
    private static final String CODES = "$.content[*].code";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureChantier fixture;

    private FixtureChantier.Chaine chaine;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        chaine = fixture.creer();
        admin = fixture.administrateur();
    }

    private String lire(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private List<String> codesDuLotFiltres(String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(DECOMPTES).param(LOT_ID, chaine.lotBordereau().getId().toString());
        for (int i = 0; i < parametres.length; i += 2) {
            requete = requete.param(parametres[i], parametres[i + 1]);
        }
        return JsonPath.read(lire(requete), CODES);
    }

    // ------------------------------------------------------------------ liste par lot

    @Test
    void parLot_ordreDesNumeros_etSoldeParFacture() throws Exception {
        String json = lire(get("/api/chantier/lots/{id}/decomptes", chaine.lotBordereau().getId()));
        List<Integer> numeros = JsonPath.read(json, "$[*].numero");
        Map<String, Object> d1 = JsonPath.read(json, "$[0]");
        Map<String, Object> d2 = JsonPath.read(json, "$[1]");
        Map<String, Object> d3 = JsonPath.read(json, "$[2]");

        assertAll(
                () -> assertThat(numeros).containsExactly(1, 2, 3),
                // Le règlement annulé de 100 sur la facture 1 n'entre pas dans le réglé.
                () -> assertThat(montant(d1, "montantRegle")).isEqualByComparingTo("216"),
                () -> assertThat(montant(d1, "resteARegler")).isEqualByComparingTo("0"),
                () -> assertThat(d1).containsEntry("factureStatutDescription", "VALIDÉ DG"),
                () -> assertThat(d2).containsEntry("factureId", null).containsEntry("factureCode", null),
                () -> assertThat(montant(d2, "montantRegle")).isEqualByComparingTo("0"),
                () -> assertThat(montant(d2, "cumulNetRetenueGarantie")).isEqualByComparingTo("756"),
                // Reste calculé sur le net de la facture (130, régularisation comprise), pas sur celui du décompte (120).
                () -> assertThat(montant(d3, "resteARegler")).isEqualByComparingTo("130"),
                () -> assertThat(d3).containsEntry("dernier", true));
    }

    @Test
    void parLot_lotInconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/api/chantier/lots/{id}/decomptes", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ liste globale

    @Test
    void globale_triParDefautDateDecroissante() throws Exception {
        assertThat(codesDuLotFiltres()).containsExactly(chaine.decompteExtras().getCode(),
                chaine.decompte2().getCode(), chaine.decompte1().getCode());
    }

    @Test
    void globale_triParNumeroCroissant() throws Exception {
        assertThat(codesDuLotFiltres("sort", "numero,asc")).containsExactly(chaine.decompte1().getCode(),
                chaine.decompte2().getCode(), chaine.decompteExtras().getCode());
    }

    @Test
    void globale_filtreProjetEtFournisseur() throws Exception {
        List<String> parProjet = JsonPath.read(lire(get(DECOMPTES)
                .param("projetId", chaine.projetB().getId().toString())), CODES);
        List<String> parFournisseur = JsonPath.read(lire(get(DECOMPTES)
                .param("fournisseurId", chaine.fournisseurB().getId().toString())), CODES);

        assertAll(
                () -> assertThat(parProjet).containsExactly(chaine.decompteB().getCode()),
                () -> assertThat(parFournisseur).containsExactly(chaine.decompteB().getCode()));
    }

    @Test
    void globale_filtrePeriode_bornesIncluses() {
        assertAll(
                () -> assertThat(codesDuLotFiltres("du", "2026-02-10", "au", "2026-02-10"))
                        .containsExactly(chaine.decompte2().getCode()),
                () -> assertThat(codesDuLotFiltres("du", "2026-02-11"))
                        .containsExactly(chaine.decompteExtras().getCode()),
                () -> assertThat(codesDuLotFiltres("au", "2026-01-10"))
                        .containsExactly(chaine.decompte1().getCode()));
    }

    @Test
    void globale_filtreSansFactureEtStatutFacture() {
        assertAll(
                () -> assertThat(codesDuLotFiltres("sansFacture", "true"))
                        .containsExactly(chaine.decompte2().getCode()),
                () -> assertThat(codesDuLotFiltres("sansFacture", "false")).hasSize(3),
                () -> assertThat(codesDuLotFiltres("statutFactureId", chaine.statutFactureValide().toString()))
                        .containsExactly(chaine.decompte1().getCode()));
    }

    @Test
    void globale_rechercheCodeDecompteCodeLotFournisseur() throws Exception {
        List<String> parCodeLot = JsonPath.read(lire(get(DECOMPTES).param("search", chaine.lotB().getCode())), CODES);
        List<String> parFournisseur = JsonPath.read(lire(get(DECOMPTES).param("search", "TOREE TEST")), CODES);

        assertAll(
                () -> assertThat(codesDuLotFiltres("search", chaine.decompte2().getCode()))
                        .containsExactly(chaine.decompte2().getCode()),
                () -> assertThat(parCodeLot).contains(chaine.decompteB().getCode()),
                () -> assertThat(parFournisseur).contains(chaine.decompteB().getCode()));
    }

    @Test
    void globale_dateMalFormee_renvoie400() throws Exception {
        mockMvc.perform(get(DECOMPTES).param("du", "10/02/2026").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void globale_cleDeTriInconnue_renvoie400() throws Exception {
        mockMvc.perform(get(DECOMPTES).param("sort", "docDate,desc").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ fiche

    @Test
    void fiche_montantsStockesFactureReglementsEtLignes() throws Exception {
        mockMvc.perform(get(DECOMPTE, chaine.decompte1().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.dateDocument").value("2026-01-10"))
                .andExpect(jsonPath("$.montantHT").value(200.0))
                .andExpect(jsonPath("$.montantTVA").value(40.0))
                .andExpect(jsonPath("$.montantTTC").value(240.0))
                .andExpect(jsonPath("$.retenueGarantie").value(24.0))
                .andExpect(jsonPath("$.tauxRetenueGarantie").value(10.0))
                .andExpect(jsonPath("$.montantNetRetenueGarantie").value(216.0))
                .andExpect(jsonPath("$.netAPayer").value(216.0))
                .andExpect(jsonPath("$.cumulTTC").value(240.0))
                .andExpect(jsonPath("$.notes").value("DECOMPTE N° 1"))
                .andExpect(jsonPath("$.facture.code").value(chaine.facture1().getCode()))
                .andExpect(jsonPath("$.facture.montantAPayer").value(216.0))
                .andExpect(jsonPath("$.facture.okPourPaiement").value(true))
                .andExpect(jsonPath("$.reglements.length()").value(2))
                .andExpect(jsonPath("$.reglements[?(@.annule == true)].montant").value(100.0))
                .andExpect(jsonPath("$.reglements[?(@.annule == false)].typeDescription").value("VIREMENT"))
                .andExpect(jsonPath("$.montantRegle").value(216.0))
                .andExpect(jsonPath("$.resteARegler").value(0.0))
                .andExpect(jsonPath("$.lignes.length()").value(3))
                .andExpect(jsonPath("$.lignes[0].quantiteMarche").value(10.0))
                .andExpect(jsonPath("$.lignes[0].quantiteCumulee").value(2.0))
                .andExpect(jsonPath("$.lignes[0].montantHTPeriode").value(200.0))
                .andExpect(jsonPath("$.lignes[0].avancementLignePct").value(20.0));
    }

    @Test
    void fiche_cumulTTCEtCumulNetDeRG_coherentsAvecLesDecomptesPrecedents() throws Exception {
        String d1 = lire(get(DECOMPTE, chaine.decompte1().getId()));
        String d2 = lire(get(DECOMPTE, chaine.decompte2().getId()));
        BigDecimal netRg1 = new BigDecimal(JsonPath.read(d1, "$.montantNetRetenueGarantie").toString());
        BigDecimal netRg2 = new BigDecimal(JsonPath.read(d2, "$.montantNetRetenueGarantie").toString());
        BigDecimal cumulNet2 = new BigDecimal(JsonPath.read(d2, "$.cumulNetRetenueGarantie").toString());
        BigDecimal cumulTTC2 = new BigDecimal(JsonPath.read(d2, "$.cumulTTC").toString());

        assertAll(
                () -> assertThat(netRg1.add(netRg2)).isEqualByComparingTo(cumulNet2),
                () -> assertThat(cumulTTC2).isEqualByComparingTo("840"));
    }

    @Test
    void fiche_sommeDesLignes_egaleAuHTStocke_extrasEtRetenues() throws Exception {
        String json = lire(get(DECOMPTE, chaine.decompte2().getId()));
        List<Object> montantsLignes = JsonPath.read(json, "$.lignes[*].montantHTPeriode");
        BigDecimal somme = montantsLignes.stream().map(v -> new BigDecimal(v.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stocke = new BigDecimal(JsonPath.read(json, "$.montantHT").toString());
        Map<String, Object> extra = JsonPath.read(json, "$.extras[0]");
        Map<String, Object> ligne1 = JsonPath.read(json, "$.lignes[0]");

        assertAll(
                () -> assertThat(somme).isEqualByComparingTo(stocke).isEqualByComparingTo("500"),
                () -> assertThat(montant(ligne1, "quantiteAnterieure")).isEqualByComparingTo("2"),
                () -> assertThat(montant(ligne1, "quantiteCumulee")).isEqualByComparingTo("5"),
                () -> assertThat(montant(ligne1, "avancementLignePct")).isEqualByComparingTo("50"),
                () -> assertThat(montant(extra, "montantHT")).isEqualByComparingTo("100"),
                () -> assertThat(montant(extra, "montantTTC")).isEqualByComparingTo("120"),
                () -> assertThat(montant(extra, "tauxTVA")).isEqualByComparingTo("20"),
                () -> assertThat((String) JsonPath.read(json, "$.retenues[0].libelle")).isEqualTo("FOURNITURE FAUX CADRES"),
                () -> assertThat(new BigDecimal(JsonPath.read(json, "$.retenuesNonRemboursables").toString()))
                        .isEqualByComparingTo("50"));
    }

    @Test
    void fiche_decompteSansFacture() throws Exception {
        mockMvc.perform(get(DECOMPTE, chaine.decompte2().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facture").value(nullValue()))
                .andExpect(jsonPath("$.reglements.length()").value(0))
                .andExpect(jsonPath("$.montantRegle").value(0.0))
                .andExpect(jsonPath("$.resteARegler").value(0.0));
    }

    @Test
    void fiche_decompteExtrasSeuls_HTNulLignesVides() throws Exception {
        mockMvc.perform(get(DECOMPTE, chaine.decompteExtras().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantHT").value(0.0))
                .andExpect(jsonPath("$.travauxSupplementaires").value(120.0))
                .andExpect(jsonPath("$.netAPayer").value(120.0))
                .andExpect(jsonPath("$.montantBrut").value(120.0))
                .andExpect(jsonPath("$.lignes.length()").value(0))
                .andExpect(jsonPath("$.extras.length()").value(1))
                .andExpect(jsonPath("$.facture.regularisation").value(10.0))
                .andExpect(jsonPath("$.facture.montantAPayer").value(130.0))
                .andExpect(jsonPath("$.facture.okPourPaiement").value(false))
                .andExpect(jsonPath("$.resteARegler").value(130.0))
                .andExpect(jsonPath("$.dernier").value(true));
    }

    @Test
    void fiche_decompteInconnu_renvoie404() throws Exception {
        mockMvc.perform(get(DECOMPTE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}

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
class LotsChantierTest {

    private static final String LOTS_DU_PROJET = "/api/chantier/projets/{id}/lots";
    private static final String CONTENT = "$.content";

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

    private String lotsDuProjetA(String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(LOTS_DU_PROJET, chaine.projetA().getId());
        for (int i = 0; i < parametres.length; i += 2) {
            requete = requete.param(parametres[i], parametres[i + 1]);
        }
        return lire(requete);
    }

    // ------------------------------------------------------------------ liste

    @Test
    void liste_paginee_triParDefautCodeDecroissant() throws Exception {
        String json = lotsDuProjetA();
        List<String> codes = JsonPath.read(json, "$.content[*].code");

        assertAll(
                () -> assertThat((Integer) JsonPath.read(json, "$.totalElements")).isEqualTo(3),
                () -> assertThat(codes).containsExactly(chaine.lotVide().getCode(), chaine.lotForfait().getCode(),
                        chaine.lotBordereau().getCode()));
    }

    @Test
    void liste_lotAvecBordereau_montantsCumulsEtSoldeFacture() throws Exception {
        Map<String, Object> lot = element(lotsDuProjetA(), CONTENT, chaine.lotBordereau().getId());

        assertAll(
                () -> assertThat(montant(lot, "montantHT")).isEqualByComparingTo("2666.666667"),
                () -> assertThat(montant(lot, "montantTVA")).isEqualByComparingTo("533.333333"),
                () -> assertThat(montant(lot, "montantTTC")).isEqualByComparingTo("3200"),
                () -> assertThat(lot).containsEntry("montantsDepuisBordereau", true).containsEntry("nbDecomptes", 3)
                        .containsEntry("dateDebut", "2024-01-02").containsEntry("delaiJours", 366),
                () -> assertThat(montant(lot, "tauxRetenueGarantie")).isEqualByComparingTo("10"),
                () -> assertThat(montant(lot, "cumulDecomptesHT")).isEqualByComparingTo("700"),
                () -> assertThat(montant(lot, "cumulDecomptesTTC")).isEqualByComparingTo("840"),
                () -> assertThat(montant(lot, "cumulTravauxSupplementaires")).isEqualByComparingTo("240"),
                () -> assertThat(montant(lot, "cumulRetenueGarantie")).isEqualByComparingTo("84"),
                () -> assertThat(montant(lot, "avancementPct")).isEqualByComparingTo("26.25"),
                // Facture 1 (216) soldée — son règlement annulé de 100 ne compte pas ; facture extras (130) non réglée.
                () -> assertThat(montant(lot, "totalFacture")).isEqualByComparingTo("346"),
                () -> assertThat(montant(lot, "totalRegle")).isEqualByComparingTo("216"),
                () -> assertThat(montant(lot, "resteARegler")).isEqualByComparingTo("130"),
                () -> assertThat(lot.get("fournisseurNom").toString()).startsWith("WASSILA TEST"));
    }

    @Test
    void liste_lotSansLigne_repliSurLeForfait() throws Exception {
        Map<String, Object> lot = element(lotsDuProjetA(), CONTENT, chaine.lotForfait().getId());

        assertAll(
                () -> assertThat(montant(lot, "montantHT")).isEqualByComparingTo("5000"),
                () -> assertThat(montant(lot, "montantTTC")).isEqualByComparingTo("6000"),
                () -> assertThat(lot).containsEntry("montantsDepuisBordereau", false).containsEntry("nbDecomptes", 0),
                () -> assertThat(montant(lot, "avancementPct")).isEqualByComparingTo("0"),
                () -> assertThat(montant(lot, "totalFacture")).isEqualByComparingTo("0"));
    }

    @Test
    void liste_marcheNul_avancementNull_fournisseurNull() throws Exception {
        Map<String, Object> lot = element(lotsDuProjetA(), CONTENT, chaine.lotVide().getId());

        assertThat(lot).containsEntry("avancementPct", null).containsEntry("fournisseurId", null)
                .containsEntry("fournisseurNom", null);
    }

    @Test
    void liste_rechercheSurLeNomDuFournisseur() throws Exception {
        List<String> ids = JsonPath.read(lotsDuProjetA("search", "WASSILA"), "$.content[*].id");

        assertThat(ids).containsExactlyInAnyOrder(chaine.lotBordereau().getId().toString(),
                chaine.lotForfait().getId().toString());
    }

    @Test
    void liste_rechercheSurLeCode() throws Exception {
        List<String> ids = JsonPath.read(lotsDuProjetA("search", chaine.lotVide().getCode()), "$.content[*].id");

        assertThat(ids).containsExactly(chaine.lotVide().getId().toString());
    }

    @Test
    void liste_filtresStatutTypeFournisseur() throws Exception {
        String parFournisseur = lotsDuProjetA("fournisseurId", chaine.fournisseur().getId().toString());
        String parStatut = lotsDuProjetA("statutId", chaine.statutLot().toString());
        String parType = lotsDuProjetA("typeId", chaine.typeLot().toString());
        String statutInconnu = lotsDuProjetA("statutId", UUID.randomUUID().toString());

        assertAll(
                () -> assertThat((Integer) JsonPath.read(parFournisseur, "$.totalElements")).isEqualTo(2),
                () -> assertThat((Integer) JsonPath.read(parStatut, "$.totalElements")).isEqualTo(3),
                () -> assertThat((Integer) JsonPath.read(parType, "$.totalElements")).isEqualTo(3),
                () -> assertThat((Integer) JsonPath.read(statutInconnu, "$.totalElements")).isZero());
    }

    @Test
    void liste_triParDateEtFournisseur_acceptes() throws Exception {
        lotsDuProjetA("sort", "dateDebut,asc");
        String json = lotsDuProjetA("sort", "fournisseurNom,asc");

        // Tri sur une association facultative : le lot sans fournisseur ne disparaît pas.
        assertThat((Integer) JsonPath.read(json, "$.totalElements")).isEqualTo(3);
    }

    @Test
    void liste_cleDeTriInconnue_renvoie400() throws Exception {
        mockMvc.perform(get(LOTS_DU_PROJET, chaine.projetA().getId()).param("sort", "montantHT,asc")
                        .with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void liste_projetInconnu_renvoie404() throws Exception {
        mockMvc.perform(get(LOTS_DU_PROJET, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void liste_filtreMalForme_renvoie400() throws Exception {
        mockMvc.perform(get(LOTS_DU_PROJET, chaine.projetA().getId()).param("statutId", "x").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ référentiels

    @Test
    void referentiels_statutDeLotAvecTypeEtEntite_typesEtStatutsFacture() throws Exception {
        String json = lire(get("/api/chantier/referentiels"));
        Map<String, Object> statut = element(json, "$.statutsLot", chaine.statutLot());
        List<String> types = JsonPath.read(json, "$.typesLot[*].id");
        List<String> statutsFacture = JsonPath.read(json, "$.statutsFacture[*].id");

        assertAll(
                () -> assertThat(statut).containsEntry("ordre", 40).containsEntry("typeCode", "Intermédiaire")
                        .containsEntry("entiteCode", "PrjMarket"),
                () -> assertThat(types).contains(chaine.typeLot().toString()),
                () -> assertThat(statutsFacture).contains(chaine.statutFactureValide().toString()));
    }

    // ------------------------------------------------------------------ fiche

    @Test
    void fiche_forfaitEstimationTauxEtDates() throws Exception {
        mockMvc.perform(get("/api/chantier/lots/{id}", chaine.lotBordereau().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projetId").value(chaine.projetA().getId().toString()))
                .andExpect(jsonPath("$.entiteCode").value("PrjMarket"))
                .andExpect(jsonPath("$.montantForfaitaireHT").value(9999.0))
                .andExpect(jsonPath("$.estimationHT").value(9999.0))
                .andExpect(jsonPath("$.estimationDepuisBordereau").value(false))
                .andExpect(jsonPath("$.retenueGarantieTheorique").value(320.0))
                .andExpect(jsonPath("$.cumulNetAPayer").value(946.0))
                .andExpect(jsonPath("$.tvaDefaut").value(20.0))
                .andExpect(jsonPath("$.dateDebutPrevisionnelle").value("2023-06-23"))
                .andExpect(jsonPath("$.dateFin").value("2025-01-02"))
                .andExpect(jsonPath("$.descriptionLongue").value(nullValue()))
                .andExpect(jsonPath("$.motifAnnulation").value(nullValue()))
                .andExpect(jsonPath("$.clos").value(false));
    }

    @Test
    void fiche_lotInconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/api/chantier/lots/{id}", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ bordereau

    @Test
    void bordereau_lignesOrdonneesEtTotauxEgauxAuMontantMarche() throws Exception {
        mockMvc.perform(get("/api/chantier/lots/{id}/bordereau", chaine.lotBordereau().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes.length()").value(3))
                .andExpect(jsonPath("$.lignes[0].ordre").value(1))
                .andExpect(jsonPath("$.lignes[0].groupe").value("Gros oeuvre"))
                .andExpect(jsonPath("$.lignes[0].tauxTVA").value(20.0))
                .andExpect(jsonPath("$.lignes[0].totalHT").value(1000.0))
                .andExpect(jsonPath("$.lignes[1].groupe").value(nullValue()))
                .andExpect(jsonPath("$.lignes[2].totalHT").value(666.666667))
                .andExpect(jsonPath("$.lignes[2].totalTTC").value(800.0))
                .andExpect(jsonPath("$.totaux.nbLignes").value(3))
                .andExpect(jsonPath("$.totaux.totalHT").value(2666.666667))
                .andExpect(jsonPath("$.totaux.totalTVA").value(533.333333))
                .andExpect(jsonPath("$.totaux.totalTTC").value(3200.0));
    }

    @Test
    void bordereau_lotAuForfait_vide() throws Exception {
        mockMvc.perform(get("/api/chantier/lots/{id}/bordereau", chaine.lotForfait().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes.length()").value(0))
                .andExpect(jsonPath("$.totaux.nbLignes").value(0))
                .andExpect(jsonPath("$.totaux.totalHT").value(0.0));
    }

    @Test
    void bordereau_lotInconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/api/chantier/lots/{id}/bordereau", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}

package com.tsix.immocrm.controller.modele;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modèle semé {@code FORMULAIRE_BARID} (FEATURE-vente-formulaire-barid.md §3 « Le modèle semé », §2 mise en page) : créé au
 * démarrage s'il manque, actif, A4 marges 0,5 cm, sans fond, un seul {@code div} répété sur {@code destinataires} portant
 * les cinq blocs du §2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class ModeleFormulaireBaridTest {

    private static final String BASE = "/api/modeles-documents";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFormulairesBarid fixture;

    private String detail(UUID id) throws Exception {
        return mockMvc.perform(get(BASE + "/{id}", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal cm(Element bloc, String attribut) {
        return new BigDecimal(bloc.attr(attribut).replace(',', '.')).stripTrailingZeros();
    }

    @Test
    void seme_auDemarrage_actifCategorieDossierVenteEtLibelle() throws Exception {
        fixture.semerModeles();

        String liste = mockMvc.perform(get(BASE + "/admin").param("categorie", "DOSSIER_VENTE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> barid = JsonPath.read(liste, "$[?(@.code == 'FORMULAIRE_BARID')]");
        assertThat(barid).hasSize(1).first()
                .satisfies(m -> assertThat(m).containsEntry("libelle", "Formulaire Barid Al-Maghrib")
                        .containsEntry("actif", true));
    }

    @Test
    void seme_A4MargesDemiCentimetre_sansFond() throws Exception {
        UUID id = fixture.semerModeles();

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miseEnPage.format").value("A4"))
                .andExpect(jsonPath("$.miseEnPage.margeHaut").value(5))
                .andExpect(jsonPath("$.miseEnPage.margeBas").value(5))
                .andExpect(jsonPath("$.miseEnPage.margeGauche").value(5))
                .andExpect(jsonPath("$.miseEnPage.margeDroite").value(5))
                .andExpect(jsonPath("$.miseEnPage.fondImageId").doesNotExist());
    }

    @Test
    void seme_unDivRepeteSurDestinataires_cinqBlocsDuFormulaire() throws Exception {
        String contenu = JsonPath.read(detail(fixture.semerModeles()), "$.contenu");
        Element corps = Jsoup.parseBodyFragment(contenu).body();

        List<Element> repetitions = corps.select("div[data-repeter=destinataires]");
        assertThat(repetitions).hasSize(1);
        List<String> positions = repetitions.get(0).select("div[data-bloc]").stream()
                .map(b -> cm(b, "data-x").toPlainString() + ";" + cm(b, "data-y").toPlainString() + ";"
                        + cm(b, "data-l").toPlainString() + ";" + cm(b, "data-h").toPlainString())
                .toList();
        // Positions absolues sur la feuille = zones du §2 + (0,5 ; 0,9) cm (amendement 28/10).
        assertThat(positions).containsExactlyInAnyOrder("0.7;11.7;8.8;3.2", "0.7;14.3;8;1", "9.5;11.7;10.2;3.4",
                "9.1;14.3;10;1", "5.7;22.7;10.2;1.6");
    }

    @Test
    void seme_champsDuFormulaire_dansLeDivRepete() throws Exception {
        String contenu = JsonPath.read(detail(fixture.semerModeles()), "$.contenu");
        Element repetition = Jsoup.parseBodyFragment(contenu).body().selectFirst("div[data-repeter=destinataires]");

        assertThat(repetition).isNotNull();
        List<String> champs = repetition.select("[data-champ]").eachAttr("data-champ");
        assertThat(champs).contains("destinataire.titre", "destinataire.nom", "destinataire.adresseSansCodePostal",
                "destinataire.codePostal", "destinataire.societe", "destinataire.societeAdresseSansCodePostal",
                "destinataire.societeCodePostal", "destinataire.adresse");
    }

    @Test
    void seme_blocBasDePage_neufPointsGrasCentre_autresQuinzeVirguleSoixanteQuinze() throws Exception {
        String contenu = JsonPath.read(detail(fixture.semerModeles()), "$.contenu");
        List<Element> blocs = Jsoup.parseBodyFragment(contenu).body().select("div[data-bloc]");

        assertThat(blocs).hasSize(5);
        for (Element bloc : blocs) {
            boolean basDePage = cm(bloc, "data-y").compareTo(new BigDecimal("22.7")) == 0;
            assertThat(cm(bloc, "data-taille")).as("taille du bloc y=%s", bloc.attr("data-y"))
                    .isEqualByComparingTo(basDePage ? "9" : "15.75");
            assertThat(bloc.attr("data-gras")).as("gras du bloc y=%s", bloc.attr("data-y")).isEqualTo("true");
            if (basDePage) {
                assertThat(bloc.attr("data-align")).isEqualTo("center");
            }
        }
    }
}

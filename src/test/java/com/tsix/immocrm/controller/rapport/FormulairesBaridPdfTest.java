package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.utils.LecturePdf;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFormulairesBarid.CODE_RAPPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Impression du lot {@code GET /api/rapports/vente/ComSaleFormulaireBaridMaroc/formulaires} (FEATURE-vente-formulaire-barid.md
 * §4 « Endpoint d'impression », §6 « Données personnelles », §8.1 « Lot / rapport ») : toutes les pages dans l'ordre de la
 * liste du hub, plafond 600, modèle absent ou inactif 404, aucun destinataire 404. Jeu : {@link FixtureFormulairesBarid#jeu}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormulairesBaridPdfTest {

    static final String FORMULAIRES = AppelsRapports.descripteur(VENTE, CODE_RAPPORT) + "/formulaires";
    private static final String TRANCHE_ID = "trancheId";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFormulairesBarid fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private Jeu jeu;
    private UUID modele;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String p = FixtureEtatVentes.prefixe("BAMP");
        fixture.rapport();
        AdmRole roleLibre = rapports.role("Libre " + p, false);
        libre = connecte(rapports.compte("bam.pdf." + p, roleLibre));
        jeu = fixture.jeu(p);
        modele = fixture.semerModeles();
    }

    private MockHttpServletRequestBuilder formulaires(Object... tranches) {
        MockHttpServletRequestBuilder requete = get(FORMULAIRES).with(libre);
        for (Object t : tranches) {
            requete.param(TRANCHE_ID, t.toString());
        }
        return requete;
    }

    private MockHttpServletResponse imprimer(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse();
    }

    // ------------------------------------------------------------------ réponse

    @Test
    void pdf_typeNomHorodateEtCachePrive() throws Exception {
        MockHttpServletResponse reponse = imprimer(formulaires(jeu.t1().getId(), jeu.t2().getId()));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(reponse.getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE);
        soft.assertThat(reponse.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .containsPattern("formulaires-barid_\\d{8}_\\d{6}\\.pdf");
        soft.assertThat(reponse.getHeader(HttpHeaders.CACHE_CONTROL)).contains("private").contains("no-store");
        soft.assertAll();
    }

    @Test
    void pdf_rapprochement_autantDePagesQueDeLignesDuHub() throws Exception {
        List<Map<String, Object>> lignesHub = lignes(section(appels.sections(get(resultat(VENTE, CODE_RAPPORT))
                .param(TRANCHE_ID, jeu.t1().getId().toString()).param(TRANCHE_ID, jeu.t2().getId().toString())
                .with(libre)), "destinataires"));

        byte[] pdf = imprimer(formulaires(jeu.t1().getId(), jeu.t2().getId())).getContentAsByteArray();

        assertThat(LecturePdf.pages(pdf)).isEqualTo(lignesHub.size()).isEqualTo(5);
    }

    @Test
    void pdf_pageN_estLeNiemeDestinataire_triParNom() throws Exception {
        byte[] pdf = imprimer(formulaires(jeu.t1().getId(), jeu.t2().getId())).getContentAsByteArray();

        SoftAssertions soft = new SoftAssertions();
        List<String> ordre = FixtureFormulairesBarid.ORDRE;
        for (int i = 0; i < ordre.size(); i++) {
            String page = LecturePdf.textePage(pdf, i + 1);
            soft.assertThat(page).as("page %d", i + 1).contains(jeu.nom(ordre.get(i)));
            for (int autre = 0; autre < ordre.size(); autre++) {
                if (autre != i) {
                    soft.assertThat(page).as("page %d", i + 1).doesNotContain(jeu.nom(ordre.get(autre)));
                }
            }
        }
        soft.assertAll();
    }

    @Test
    void pdf_pageDUnCoAcquereur_porteLaSocieteDuDossier() throws Exception {
        byte[] pdf = imprimer(formulaires(jeu.t1().getId(), jeu.t2().getId())).getContentAsByteArray();

        assertThat(LecturePdf.textePage(pdf, 3)).contains(jeu.nom("CHERKAOUI"))
                .contains(jeu.t2().getCfgProject().getCfgCompany().getDescription());
    }

    @Test
    void pdf_filtreStatut() throws Exception {
        byte[] pdf = imprimer(formulaires(jeu.t1().getId(), jeu.t2().getId())
                .param("statutId", jeu.s1().getId().toString())).getContentAsByteArray();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).isEqualTo(2);
        soft.assertThat(LecturePdf.textePage(pdf, 1)).contains(jeu.nom("BENNANI"));
        soft.assertThat(LecturePdf.textePage(pdf, 2)).contains(jeu.nom("ZIANI"));
        soft.assertAll();
    }

    // ------------------------------------------------------------------ refus

    @Test
    void pdf_aucunDestinataire_404() throws Exception {
        mockMvc.perform(formulaires(jeu.ti().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(DETAIL).value(containsString("Aucun destinataire")));
    }

    @Test
    void pdf_modeleInactif_404() throws Exception {
        mockMvc.perform(put("/api/modeles-documents/{id}", modele).with(user("admin").roles("Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("actif", false))))
                .andExpect(status().isOk());

        mockMvc.perform(formulaires(jeu.t1().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(DETAIL).value("Le modèle FORMULAIRE_BARID est absent ou inactif"));
    }

    @Test
    void pdf_trancheIdInvalide_400() throws Exception {
        mockMvc.perform(get(FORMULAIRES).param(TRANCHE_ID, "pas-un-uuid").with(libre))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pdf_plafond_601Pages_400() throws Exception {
        CfgTranche grosse = fixture.trancheAvecCoAcquereurs(jeu.p(), 600);

        mockMvc.perform(formulaires(grosse.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value("Affinez les filtres : 601 pages (maximum 600)."));
    }
}

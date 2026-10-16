package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureActeReservation;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.services.modele.InitialiseurModeles;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code /api/modeles-documents} (FEATURE-modeles-documents.md §6, §11) et bascule de l'acte de réservation (§8). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class ModeleDocumentControllerTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String PAR_ID = BASE + "/{id}";
    private static final String VERSIONS = PAR_ID + "/versions";
    private static final String RESTAURER = VERSIONS + "/{numero}/restaurer";
    private static final String GENERER = PAR_ID + "/generer";
    private static final String APERCU = PAR_ID + "/apercu";
    private static final String ACTE = "/api/dossiers/{id}/documents/acte-reservation";
    private static final String DOSSIER_VENTE = "DOSSIER_VENTE";
    private static final String CATEGORIE = "categorie";
    private static final String CONTENU = "contenu";
    private static final String FICHE_ID = "ficheId";
    private static final String ACTIF = "actif";
    private static final String ORDRE = "ordre";
    private static final String ID = "id";
    private static final String CONTENU_A = "<p>Bonjour <span data-champ=\"client.nom\">Nom du client</span></p>";
    private static final String CONTENU_B = "<h2 data-numero>Objet</h2><p>Version B</p>";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper json;
    @Autowired
    private FixtureActeReservation fixtureActe;
    @Autowired
    private FixtureDossierVente fixtureDossier;
    @Autowired
    private AdmUserHabilitationRepository habilitations;
    @Autowired
    private InitialiseurModeles initialiseur;

    private static RequestPostProcessor commercial() {
        return user("commercial").roles("Commercial");
    }

    private String corps(Object valeur) {
        return json.writeValueAsString(valeur);
    }

    private static Map<String, Object> modele(String code, String contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(CATEGORIE, DOSSIER_VENTE);
        m.put("code", code);
        m.put("libelle", "Lettre " + code);
        m.put(CONTENU, contenu);
        return m;
    }

    private JsonNode creer(String code, String contenu) throws Exception {
        String reponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(modele(code, contenu))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse);
    }

    private static String textePdf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper extracteur = new PDFTextStripper();
            extracteur.setSortByPosition(true);
            return extracteur.getText(document).replaceAll("\\s+", " ");
        }
    }

    @Test
    void creerModifierRestaurer_chaqueEnregistrementAjouteUneVersion() throws Exception {
        String id = creer("TST_VERSIONS", CONTENU_A).get(ID).asString();

        mockMvc.perform(put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, CONTENU_B))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCourante").value(2))
                .andExpect(jsonPath("$.contenu").value(CONTENU_B));

        mockMvc.perform(post(RESTAURER, id, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCourante").value(3))
                .andExpect(jsonPath("$.contenu").value(CONTENU_A));

        mockMvc.perform(get(VERSIONS, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numero").value(org.hamcrest.Matchers.contains(3, 2, 1)))
                .andExpect(jsonPath("$[0].courante").value(true))
                .andExpect(jsonPath("$[2].courante").value(false));
    }

    @Test
    void modifierSansContenu_neCreePasDeVersion() throws Exception {
        String id = creer("TST_SANS_VERSION", CONTENU_A).get(ID).asString();

        mockMvc.perform(put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("libelle", "Renommé", ACTIF, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCourante").value(1))
                .andExpect(jsonPath("$.libelle").value("Renommé"))
                .andExpect(jsonPath("$.actif").value(false));
    }

    @Test
    void codeDejaPrisDansLaCategorie_409() throws Exception {
        creer("TST_DOUBLON", CONTENU_A);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(modele("TST_DOUBLON", CONTENU_A))))
                .andExpect(status().isConflict());
    }

    @Test
    void contenuHorsListeBlanche_400ALaCreationEtALaModification() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(modele("TST_XSS", "<p onclick=\"x()\">a</p>"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Attribut interdit : onclick sur <p>"));

        String id = creer("TST_XSS_PUT", CONTENU_A).get(ID).asString();
        mockMvc.perform(put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, "<script>alert(1)</script>"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codeInvalideOuCategorieInconnue_400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(modele("minuscules", CONTENU_A))))
                .andExpect(status().isBadRequest());

        Map<String, Object> inconnue = modele("TST_CAT", CONTENU_A);
        inconnue.put(CATEGORIE, "PROSPECTION");
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(corps(inconnue)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void miseEnPageHorsBornes_400() throws Exception {
        Map<String, Object> m = modele("TST_MARGES", CONTENU_A);
        m.put("miseEnPage", Map.of("margeHaut", 90));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(corps(m)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogueDesChamps_groupesEtListes() throws Exception {
        mockMvc.perform(get(BASE + "/categories/{c}/champs", DOSSIER_VENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupes[0].champs[0].cle").value("dossier.code"))
                .andExpect(jsonPath("$.listes[*].cle").value(org.hamcrest.Matchers.contains("sousArticles", "echeancesSolde")));

        mockMvc.perform(get(BASE + "/categories/{c}/champs", "INCONNUE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listeImprimer_actifsSeulementTriesParOrdre() throws Exception {
        Map<String, Object> second = modele("TST_ORDRE_B", CONTENU_A);
        second.put(ORDRE, 2);
        Map<String, Object> premier = modele("TST_ORDRE_A", CONTENU_A);
        premier.put(ORDRE, 1);
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(corps(second)))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(corps(premier)))
                .andExpect(status().isCreated());
        String inactif = creer("TST_ORDRE_INACTIF", CONTENU_A).get(ID).asString();
        mockMvc.perform(put(PAR_ID, inactif).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(ACTIF, false))))
                .andExpect(status().isOk());

        String reponse = mockMvc.perform(get(BASE).param(CATEGORIE, DOSSIER_VENTE)
                        .with(commercial()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> codes = json.readTree(reponse).findValuesAsString("code");
        assertThat(codes).containsSubsequence("TST_ORDRE_A", "TST_ORDRE_B").doesNotContain("TST_ORDRE_INACTIF");
    }

    @Test
    void concevoirSansRoleAdministrateur_403() throws Exception {
        String id = creer("TST_DROITS", CONTENU_A).get(ID).asString();
        List<MockHttpServletRequestBuilder> requetes = List.of(
                get(BASE + "/categories"),
                get(BASE + "/categories/DOSSIER_VENTE/champs"),
                get(BASE + "/admin"),
                get(PAR_ID, id),
                get(VERSIONS, id),
                post(BASE).contentType(MediaType.APPLICATION_JSON).content(corps(modele("TST_DROITS_2", CONTENU_A))),
                put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON).content(corps(Map.of(ORDRE, 3))),
                post(BASE + "/{id}/versions/1/restaurer", id),
                post(APERCU, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, CONTENU_A))));

        for (MockHttpServletRequestBuilder requete : requetes) {
            mockMvc.perform(requete.with(commercial())).andExpect(status().isForbidden());
        }
    }

    @Test
    void modeleInconnu_404() throws Exception {
        UUID inconnu = UUID.randomUUID();

        mockMvc.perform(get(PAR_ID, inconnu)).andExpect(status().isNotFound());
        mockMvc.perform(get(VERSIONS, inconnu)).andExpect(status().isNotFound());
        mockMvc.perform(put(PAR_ID, inconnu).contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of(ORDRE, 1)))).andExpect(status().isNotFound());
        mockMvc.perform(get(GENERER, inconnu).param(FICHE_ID, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void versionInconnue_404() throws Exception {
        String id = creer("TST_VERSION_INCONNUE", CONTENU_A).get(ID).asString();

        mockMvc.perform(post(RESTAURER, id, 9)).andExpect(status().isNotFound());
    }

    @Test
    void generer_pdfEtWordPourUnUtilisateurQuiVoitLeDossier() throws Exception {
        ComFolder dossier = fixtureActe.dossier("GEN", false, true);
        String id = creer("TST_LETTRE_BIENVENUE",
                "<p>Bienvenue <span data-champ=\"client.nom\">Nom</span></p><table><tbody>"
                        + "<tr data-repeter=\"echeancesSolde\"><td><span data-champ=\"echeance.montant\">M</span></td></tr>"
                        + "</tbody></table>").get(ID).asString();

        byte[] pdf = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString())
                        .with(commercial()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(
                        "tst-lettre-bienvenue-" + dossier.getCode().toLowerCase(java.util.Locale.ROOT) + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textePdf(pdf)).contains("Bienvenue Prénom Nom CLI-TST-GEN").contains("540 000.00 DH");

        mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString())
                        .param("format", "docx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, DOCX));
    }

    @Test
    void generer_dossierInvisible_404() throws Exception {
        ComFolder invisible = fixtureActe.dossier("INVIS", false, false);
        String id = creer("TST_INVISIBLE", CONTENU_A).get(ID).asString();

        mockMvc.perform(get(GENERER, id).param(FICHE_ID, invisible.getId().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(APERCU, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, CONTENU_A, FICHE_ID, invisible.getId().toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void generer_dossierHorsPerimetreDuCommercial_404() throws Exception {
        ComFolder dossier = fixtureActe.dossier("PERIM", false, true);
        CfgTranche autre = fixtureDossier.tranche("TST-PERIM-AUTRE", true);
        AdmUser commercial = fixtureDossier.compte("TST-PERIM", "Commercial");
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercial);
        habilitation.setCfgProject(autre.getCfgProject());
        habilitations.save(habilitation);
        String id = creer("TST_PERIMETRE", CONTENU_A).get(ID).asString();

        mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString())
                        .with(FixtureDossierVente.connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());
    }

    @Test
    void generer_modeleInactif_404EtFormatInconnu_400() throws Exception {
        ComFolder dossier = fixtureActe.dossier("INACTIF", false, true);
        String id = creer("TST_INACTIF", CONTENU_A).get(ID).asString();

        mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString())
                        .param("format", "odt"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(ACTIF, false))))
                .andExpect(status().isOk());
        mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void apercu_contenuNonEnregistreEnPdf() throws Exception {
        ComFolder dossier = fixtureActe.dossier("APERCU", false, true);
        String id = creer("TST_APERCU", CONTENU_A).get(ID).asString();

        byte[] pdf = mockMvc.perform(post(APERCU, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, "<p>Aperçu <span data-champ=\"client.cin\">CIN</span></p>",
                                FICHE_ID, dossier.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(textePdf(pdf)).contains("Aperçu A742122");
        mockMvc.perform(get(PAR_ID, id)).andExpect(jsonPath("$.versionCourante").value(1));
        mockMvc.perform(post(APERCU, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, CONTENU_A))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acteDeReservation_passeParLeModeleActifEtRetombeSurLeRenduCodeSinon() throws Exception {
        ComFolder dossier = fixtureActe.dossier("BASCULE", false, true);
        initialiseur.initialiserActeReservation();
        JsonNode acte = json.readTree(mockMvc.perform(get(BASE + "/admin").param(CATEGORIE, DOSSIER_VENTE))
                        .andReturn().getResponse().getContentAsString())
                .findParents("code").stream()
                .filter(n -> "ACTE_RESERVATION".equals(n.get("code").asString()))
                .findFirst().orElseThrow();
        String id = acte.get(ID).asString();
        String contenu = json.readTree(mockMvc.perform(get(PAR_ID, id))
                .andReturn().getResponse().getContentAsString()).get(CONTENU).asString();
        mockMvc.perform(put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(CONTENU, contenu.replace("Ci-après, dénommé(e)", "Phrase modifiée,")))))
                .andExpect(status().isOk());

        byte[] pdf = mockMvc.perform(get(ACTE, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("acte-reservation-" + dossier.getCode() + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textePdf(pdf)).contains("Phrase modifiée,");

        mockMvc.perform(get(ACTE, dossier.getId()).param("format", "docx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, DOCX));

        mockMvc.perform(put(PAR_ID, id).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of(ACTIF, false))))
                .andExpect(status().isOk());
        byte[] repli = mockMvc.perform(get(ACTE, dossier.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textePdf(repli)).contains("Ci-après, dénommé(e)").doesNotContain("Phrase modifiée");
    }
}

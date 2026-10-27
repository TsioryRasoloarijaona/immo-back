package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Adresse;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import com.tsix.immocrm.utils.LecturePdf;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.FormulairesBaridPdfTest.FORMULAIRES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFormulairesBarid.CODE_MODELE;
import static com.tsix.immocrm.fixtures.FixtureFormulairesBarid.CODE_RAPPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits du n° 35 et du modèle {@code FORMULAIRE_BARID} (règle 6 ; FEATURE-vente-formulaire-barid.md §4 « Ordre des
 * contrôles », §6, §8.1 « Droits (rejets testés) », réponses du manager 4 et 5) : droit du rapport ({@code AdmReportDataRole})
 * sur le hub, l'impression du lot, la liste « Documents générés » et {@code /api/modeles-documents/{id}/generer}.
 * <pre>
 *   rôles : libre (WithReportRestriction = 0) · « Commercial Chef » et « Assistante direction » restreints, liés au n° 35
 *           · « Commercial » restreint, non lié · compte sans rôle
 *   T (société active) : dossier D, acquéreur DROITUN + co-acquéreur DROITDEUX ; tout commercial est affecté au projet de T
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormulairesBaridDroitsTest {

    private static final int OK = 200;
    private static final int INTERDIT = 403;
    private static final int INTROUVABLE = 404;
    private static final String MODELES = "/api/modeles-documents";
    private static final String GENERER = MODELES + "/{id}/generer";
    private static final String FICHE_ID = "ficheId";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFormulairesBarid fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private AppelsRapports appels;
    private String p;
    private AdmRole roleLibre;
    private AdmRole roleChef;
    private AdmRole roleAssistante;
    private AdmRole roleCommercial;
    private CfgTranche t;
    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        p = FixtureEtatVentes.prefixe("BAMD");
        UUID rapport = fixture.rapport();
        roleLibre = rapports.role("Profil quelconque " + p, false);
        roleChef = rapports.role("Commercial Chef " + p, true);
        roleAssistante = rapports.role("Assistante direction " + p, true);
        roleCommercial = rapports.role("Commercial " + p, true);
        rapports.lier(rapport, roleChef);
        rapports.lier(rapport, roleAssistante);
        t = fixture.tranche("T-" + p, true, fixture.pays("PAYS-" + p));
        dossier = fixture.dossier(t, "DOS-" + p, ev.statutEnCours("ENC-" + p),
                fixture.client("DROITUN " + p, null, Adresse.aucune()));
        fixture.coAcquereur(dossier, fixture.client("DROITDEUX " + p, null, Adresse.aucune()),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        rapports.relire();
    }

    /** Compte affecté au projet de T (un commercial sans projet est sans restriction de périmètre). */
    private AdmUser compte(String code, AdmRole... roles) {
        AdmUser compte = rapports.compte(code + "." + p, roles);
        ev.affecter(compte, t.getCfgProject());
        rapports.relire();
        return compte;
    }

    private RequestPostProcessor qui(String code, AdmRole... roles) {
        return connecte(compte(code, roles), COMMERCIAL);
    }

    private MockHttpServletRequestBuilder imprimer(RequestPostProcessor qui) {
        return get(FORMULAIRES).param(TRANCHE_ID, t.getId().toString()).with(qui);
    }

    private MockHttpServletRequestBuilder generer(UUID modele, RequestPostProcessor qui) {
        return get(GENERER, modele).param(FICHE_ID, dossier.getId().toString()).with(qui);
    }

    private List<String> documentsGeneres(RequestPostProcessor qui) throws Exception {
        return JsonPath.read(appels.corps(get(MODELES).param("categorie", "DOSSIER_VENTE").with(qui)), "$[*].code");
    }

    private void assertQuatreRoutesInterdites(RequestPostProcessor qui) throws Exception {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, CODE_RAPPORT)).with(qui))).as("descripteur")
                .isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE_RAPPORT)).with(qui))).as("resultat").isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(export(VENTE, CODE_RAPPORT)).param("format", "xlsx").with(qui)))
                .as("export").isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(imprimer(qui))).as("formulaires").isEqualTo(INTERDIT);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ hub et impression du lot

    @Test
    void rd_roleLibreQuelconque_voitLeRapport_et200() throws Exception {
        RequestPostProcessor libre = qui("libre", roleLibre);
        fixture.semerModeles();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.codesVisibles(libre)).contains(CODE_RAPPORT);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE_RAPPORT)).with(libre))).isEqualTo(OK);
        soft.assertThat(appels.statut(imprimer(libre))).isEqualTo(OK);
        soft.assertAll();
    }

    @Test
    void rd_commercialChefLie_200() throws Exception {
        RequestPostProcessor chef = qui("chef", roleChef);
        fixture.semerModeles();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE_RAPPORT)).with(chef))).isEqualTo(OK);
        soft.assertThat(appels.statut(imprimer(chef))).isEqualTo(OK);
        soft.assertAll();
    }

    @Test
    void rd_assistanteDirectionLiee_200() throws Exception {
        RequestPostProcessor assistante = qui("assist", roleAssistante);
        fixture.semerModeles();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE_RAPPORT)).with(assistante))).isEqualTo(OK);
        soft.assertThat(appels.statut(imprimer(assistante))).isEqualTo(OK);
        soft.assertAll();
    }

    @Test
    void rd_commercialRestreintNonLie_absentDeLaListe_et403SurLesQuatreRoutes() throws Exception {
        RequestPostProcessor commercial = qui("com", roleCommercial);

        assertThat(appels.codesVisibles(commercial)).doesNotContain(CODE_RAPPORT);
        assertQuatreRoutesInterdites(commercial);
    }

    @Test
    void rd_sansRole_403SurLesQuatreRoutes() throws Exception {
        assertQuatreRoutesInterdites(qui("sansrole"));
    }

    @Test
    void rd_roleRetire_effetALAppelSuivantSansReconnexion() throws Exception {
        fixture.semerModeles();
        AdmUser compte = compte("retrait");
        AdmUserRole attribution = rapports.attribuer(compte, roleLibre);
        RequestPostProcessor session = connecte(compte, COMMERCIAL);
        assertThat(appels.statut(imprimer(session))).isEqualTo(OK);

        rapports.retirer(attribution);

        assertThat(appels.statut(imprimer(session))).isEqualTo(INTERDIT);
    }

    // ------------------------------------------------------------------ ordre des contrôles : 404 → 403 → 400 → 404 modèle

    @Test
    void rd_ordre_moduleInconnuAvantDroit_404() throws Exception {
        assertThat(appels.statut(get("/api/rapports/inconnu/" + CODE_RAPPORT + "/formulaires").with(qui("o1"))))
                .isEqualTo(INTROUVABLE);
    }

    @Test
    void rd_ordre_codeInconnuAvantDroit_404() throws Exception {
        assertThat(appels.statut(get(descripteur(VENTE, "Inexistant" + p) + "/formulaires").with(qui("o2"))))
                .isEqualTo(INTROUVABLE);
    }

    @Test
    void rd_ordre_droitAvantParametres_403() throws Exception {
        RequestPostProcessor commercial = qui("o3", roleCommercial);

        assertThat(appels.statut(get(FORMULAIRES).param(TRANCHE_ID, "pas-un-uuid").with(commercial)))
                .isEqualTo(INTERDIT);
    }

    /** Aucun modèle semé : le droit est contrôlé avant le modèle (403, pas 404 « modèle absent »). */
    @Test
    void rd_ordre_droitAvantModele_403() throws Exception {
        assertThat(appels.statut(imprimer(qui("o4", roleCommercial)))).isEqualTo(INTERDIT);
    }

    @Test
    void rd_cleApiToutesPortees_403HorsPortee() throws Exception {
        AdmUser compte = dossiers.compte("N8N-" + p, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("barid-" + p);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(get(FORMULAIRES).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ modèle FORMULAIRE_BARID depuis la fiche dossier

    @Test
    void modele_commercialNonLie_absentDesDocumentsGeneres_lesAutresRestent() throws Exception {
        fixture.semerModeles();

        assertThat(documentsGeneres(qui("liste.com", roleCommercial))).contains("ACTE_RESERVATION")
                .doesNotContain(CODE_MODELE);
    }

    @Test
    void modele_roleLibre_presentDansLesDocumentsGeneres() throws Exception {
        fixture.semerModeles();

        assertThat(documentsGeneres(qui("liste.libre", roleLibre))).contains(CODE_MODELE);
    }

    @Test
    void modele_commercialChefLie_presentDansLesDocumentsGeneres() throws Exception {
        fixture.semerModeles();

        assertThat(documentsGeneres(qui("liste.chef", roleChef))).contains(CODE_MODELE);
    }

    @Test
    void modele_commercialNonLie_generer403() throws Exception {
        UUID modele = fixture.semerModeles();

        assertThat(appels.statut(generer(modele, qui("gen.com", roleCommercial)))).isEqualTo(INTERDIT);
    }

    @Test
    void modele_sansRole_generer403() throws Exception {
        UUID modele = fixture.semerModeles();

        assertThat(appels.statut(generer(modele, qui("gen.sansrole")))).isEqualTo(INTERDIT);
    }

    @Test
    void modele_commercialChefLie_generer_pagesDeCeDossier() throws Exception {
        UUID modele = fixture.semerModeles();

        byte[] pdf = mockMvc.perform(generer(modele, qui("gen.chef", roleChef)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).isEqualTo(2);
        soft.assertThat(LecturePdf.textePage(pdf, 1)).contains("DROITUN " + p);
        soft.assertThat(LecturePdf.textePage(pdf, 2)).contains("DROITDEUX " + p);
        soft.assertAll();
    }

    @Test
    void modele_roleLibre_dossierHorsPerimetre_404() throws Exception {
        UUID modele = fixture.semerModeles();
        AdmUser compte = rapports.compte("gen.horsperim." + p, roleLibre);
        CfgTranche autre = fixture.tranche("TA-" + p, true, fixture.pays("PAYSA-" + p));
        ev.affecter(compte, autre.getCfgProject());
        rapports.relire();

        assertThat(appels.statut(generer(modele, connecte(compte, COMMERCIAL)))).isEqualTo(INTROUVABLE);
    }

    @Test
    void modele_formatDocx_400PdfSeulement() throws Exception {
        UUID modele = fixture.semerModeles();

        mockMvc.perform(generer(modele, qui("gen.docx", roleLibre)).param("format", "docx"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ce formulaire n'existe qu'en PDF."));
    }

    /** Témoin : un modèle sans contrôle d'accès garde le droit existant (lire le dossier). */
    @Test
    void modele_autreModele_commercialNonLie_generer200() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", "TST_TEMOIN_" + p.substring(4));
        m.put("libelle", "Témoin");
        m.put("contenu", "<p>Témoin <span data-champ=\"dossier.code\">code</span></p>");
        String reponse = mockMvc.perform(post(MODELES).with(user("admin").roles("Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(m)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID temoin = UUID.fromString(json.readTree(reponse).get("id").asString());

        assertThat(appels.statut(generer(temoin, qui("gen.temoin", roleCommercial)))).isEqualTo(OK);
    }
}

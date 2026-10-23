package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.PATRIMOINE;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.XLSX;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.codesVisibles;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.libellesVisibles;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.ACTIONNAIRES;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.CODES_HORS_LOT;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.CODES_P10A;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.MOUVEMENTS;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SUPERFICIES;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.VALORISATION;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits et visibilité des rapports du stock (FEATURE-patrimoine-rapports.md §8 T1, T2, T3, T10).
 * <pre>
 *   TA : projet A, entité active, en vente      TB : projet B, entité active, en vente
 *   TI : projet I, entité INACTIVE, en vente
 * </pre>
 * Une tranche = un article VENTE avec une pièce et un dossier avancé le 01/02/2075. Le « commercial A » a un rôle libre
 * pour les rapports et n'est affecté qu'au projet A.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsPatrimoineDroitsVisibiliteTest {

    private static final int OK = 200;
    private static final int INTERDIT = 403;
    private static final int INTROUVABLE = 404;
    private static final String SYNTHESE = "synthese";
    private static final String DU = "du";
    private static final String AU = "au";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clients;
    @Autowired private CleApiService cleApiService;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole roleLibre;
    private AdmRole roleMouvements;
    private AdmRole roleSansLien;
    private CfgProject projetA;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche ti;
    private StkItem articleA;
    private StkItem articleB;
    private StkItem articleI;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RPD");
        Map<String, UUID> rapports = rp.rapportsP10a();
        rp.rapportsHorsLot();
        roleLibre = fixture.role("Libre " + prefixe, false);
        roleMouvements = fixture.role("Commercial Chef " + prefixe, true);
        roleSansLien = fixture.role("Sans lien " + prefixe, true);
        fixture.lier(rapports.get(MOUVEMENTS), roleMouvements);

        projetA = ev.projet("PA-" + prefixe, true);
        ta = ev.tranche("TA-" + prefixe, projetA, true);
        tb = ev.tranche("TB-" + prefixe, ev.projet("PB-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);
        StrStatus enCours = ev.statutEnCours("CONC-" + prefixe);
        articleA = articleAvecPieceEtDossier(ta, categorie, type, enCours);
        articleB = articleAvecPieceEtDossier(tb, categorie, type, enCours);
        articleI = articleAvecPieceEtDossier(ti, categorie, type, enCours);
        fixture.relire();
    }

    private StkItem articleAvecPieceEtDossier(CfgTranche t, StkItemCategory categorie, StkItemType type,
                                              StrStatus statut) {
        StkItem article = ev.article(t.getCode() + "-1", t, categorie, type);
        rp.piece(article, null, false);
        ComFolder dossier = ev.vendre(article, "DOS-" + t.getCode(), statut, null, null);
        dossier.setAdvanceDate(LocalDateTime.of(2075, 2, 1, 0, 0));
        ev.enregistrer(dossier);
        return article;
    }

    private RequestPostProcessor qui(String code, AdmRole... roles) {
        return connecte(fixture.compte(code + "." + prefixe, roles));
    }

    private RequestPostProcessor commercialA() {
        AdmUser compte = fixture.compte("com." + prefixe, roleLibre);
        ev.affecter(compte, projetA);
        fixture.relire();
        return connecte(compte);
    }

    /** Résultat d'un rapport à tranches ; les Mouvements reçoivent l'année 2075. */
    private static MockHttpServletRequestBuilder requete(String code, RequestPostProcessor qui,
                                                         CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(PATRIMOINE, code)).with(qui);
        if (MOUVEMENTS.equals(code)) {
            requete.param(DU, "2075-01-01").param(AU, "2075-12-31");
        }
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    /** Lignes de toutes les sections, sauf la synthèse des Mouvements (une ligne de compteurs, toujours présente). */
    private List<Map<String, Object>> lignesDeDonnees(MockHttpServletRequestBuilder requete) throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete);
        assertThat(sections).isNotEmpty();
        List<Map<String, Object>> toutes = new ArrayList<>();
        sections.stream().filter(s -> !SYNTHESE.equals(s.get("id"))).forEach(s -> toutes.addAll(lignes(s)));
        return toutes;
    }

    /** Codes d'article lus dans la section détaillée d'un rapport (section, colonne). */
    private List<Object> codesArticles(String code, RequestPostProcessor qui) throws Exception {
        String sectionId;
        String colonne = "code";
        switch (code) {
            case SUPERFICIES -> sectionId = "articles";
            case VERIFICATION -> sectionId = "pieces";
            default -> {
                sectionId = "mouvements";
                colonne = "article";
            }
        }
        String nom = colonne;
        return lignes(section(appels.sections(requete(code, qui)), sectionId)).stream().map(l -> l.get(nom))
                .toList();
    }

    // ------------------------------------------------------------------ T1 : liste et module

    @Test
    void t1_roleLibre_listeExactementLesCinqRapportsP10a() throws Exception {
        RequestPostProcessor libre = qui("libre", roleLibre);

        assertThat(codesVisibles(appels, libre)).containsExactlyInAnyOrderElementsOf(CODES_P10A)
                .doesNotContainAnyElementsOf(CODES_HORS_LOT);
        assertThat(libellesVisibles(appels, libre)).containsExactlyInAnyOrder("VALORISATION DU STOCK",
                "MOUVEMENTS DU STOCK", "ETAT DES SUPERFICIES VENDABLE", "ACTIONNAIRES - ENTITE JURIDIQUES",
                "VERIFICATION DES ATTACHEMENTS");
    }

    @ParameterizedTest
    @ValueSource(strings = {VALORISATION, MOUVEMENTS, SUPERFICIES, ACTIONNAIRES, VERIFICATION})
    void t1_descripteur_modulePatrimoine_etCode(String code) throws Exception {
        mockMvc.perform(get(descripteur(PATRIMOINE, code)).with(qui("desc", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.module").value(PATRIMOINE));
    }

    @ParameterizedTest
    @ValueSource(strings = {VALORISATION, MOUVEMENTS, SUPERFICIES, ACTIONNAIRES, VERIFICATION})
    void t1_rapportPatrimoineDemandeSousLaVente_404(String code) throws Exception {
        assertThat(appels.statut(get(descripteur("vente", code)).with(qui("vte", roleLibre))))
                .isEqualTo(INTROUVABLE);
    }

    // ------------------------------------------------------------------ T2 : rôles restreints

    @Test
    void t2_restreintLieAuxMouvements_listeMouvementsSeul() throws Exception {
        assertThat(codesVisibles(appels, qui("chef", roleMouvements))).containsExactly(MOUVEMENTS);
    }

    @Test
    void t2_restreintLieAuxMouvements_200SurMouvements() throws Exception {
        RequestPostProcessor chef = qui("chef200", roleMouvements);

        assertThat(appels.statut(get(descripteur(PATRIMOINE, MOUVEMENTS)).with(chef))).isEqualTo(OK);
        assertThat(appels.statut(requete(MOUVEMENTS, chef, ta))).isEqualTo(OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {VALORISATION, SUPERFICIES, ACTIONNAIRES, VERIFICATION})
    void t2_restreintLieAuxMouvements_403SurLesQuatreAutres(String code) throws Exception {
        RequestPostProcessor chef = qui("chef403", roleMouvements);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(PATRIMOINE, code)).with(chef))).as("descripteur")
                .isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(resultat(PATRIMOINE, code)).with(chef))).as("resultat")
                .isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(export(PATRIMOINE, code)).param(FORMAT, XLSX).with(chef))).as("export")
                .isEqualTo(INTERDIT);
        soft.assertAll();
    }

    @Test
    void t2_restreintSansLien_listeVide() throws Exception {
        mockMvc.perform(get(liste(PATRIMOINE)).with(qui("sanslien", roleSansLien)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------ T3 : entité inactive, périmètre

    @ParameterizedTest
    @ValueSource(strings = {VALORISATION, MOUVEMENTS, SUPERFICIES, VERIFICATION})
    void t3_trancheIdDUneEntiteInactive_sectionsVides(String code) throws Exception {
        assertThat(lignesDeDonnees(requete(code, qui("inactive", roleLibre), ti))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {VALORISATION, MOUVEMENTS, SUPERFICIES, VERIFICATION})
    void t3_trancheIdHorsPerimetre_sectionsVides_jamaisTout(String code) throws Exception {
        assertThat(lignesDeDonnees(requete(code, commercialA(), tb))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {MOUVEMENTS, SUPERFICIES, VERIFICATION})
    void t3_sansTrancheId_entiteInactiveAbsente(String code) throws Exception {
        assertThat(codesArticles(code, qui("tous", roleLibre)))
                .contains(articleA.getCode(), articleB.getCode())
                .doesNotContain(articleI.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {MOUVEMENTS, SUPERFICIES, VERIFICATION})
    void t3_sansTrancheId_perimetreRestreint_seulementLeProjetAffecte(String code) throws Exception {
        assertThat(codesArticles(code, commercialA()))
                .contains(articleA.getCode())
                .doesNotContain(articleB.getCode(), articleI.getCode());
    }

    @Test
    void t3_melange_seuleLaTrancheVisibleRetenue() throws Exception {
        List<Object> codes = lignes(section(appels.sections(requete(SUPERFICIES, qui("mix", roleLibre), ta, ti)),
                "articles")).stream().map(l -> l.get("code")).toList();

        assertThat(codes).containsExactly(articleA.getCode());
    }

    // ------------------------------------------------------------------ T10 : clé d'API

    @ParameterizedTest
    @ValueSource(strings = {"/api/rapports/patrimoine", "/api/rapports/patrimoine/" + VALORISATION,
            "/api/rapports/patrimoine/" + VALORISATION + "/resultat",
            "/api/rapports/patrimoine/" + VALORISATION + "/export?format=xlsx"})
    void t10_cleApiToutesPortees_compteAdministrateur_403HorsPortee(String route) throws Exception {
        AdmUser compte = dossiers.compte("N8N-" + prefixe, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("rapports-patrimoine-" + prefixe);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clients.save(client);

        mockMvc.perform(get(route).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

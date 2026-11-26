package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ConnexionDeTest;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Corrections A1d (FEATURE-agences-vente.md R-V9) : programmes du formulaire de prospection limités aux projets de
 * l'agence, onglets de la fiche dossier ouverts en lecture sur un dossier visible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA1dCorrectionsTest {

    private static final int OK = 200;
    private static final int INTERDIT = 403;
    private static final int INTROUVABLE = 404;
    private static final String IDS_PROJETS = "$.content[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private CfgProject projetA;
    private CfgProject projetB;
    private UUID dossierA;
    private UUID dossierB;
    private RequestPostProcessor compteA;
    private RequestPostProcessor siege;

    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        AdmUser administrateur = patrimoine.administrateur();
        CfgTranche trancheA = plans.tranche("CRA-" + jeton, true);
        CfgTranche trancheB = plans.tranche("CRB-" + jeton, true);
        projetA = trancheA.getCfgProject();
        projetB = trancheB.getCfgProject();
        UUID agence = agences.agence("CRG-" + jeton);
        UUID vendeur = agences.vendeur("CRV-" + jeton);
        agences.lier(agence, vendeur);
        dossierA = dossier(trancheA, "CRL-" + jeton, vendeur);
        dossierB = dossier(trancheB, "CRM-" + jeton, vendeur);

        AdmUser compte = agences.compteAgence("CRC-" + jeton, vendeur);
        plans.habiliter(compte, projetA);
        identifiants(compte);
        identifiants(administrateur);
        compteA = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
        siege = ConnexionDeTest.connecte(administrateur);
    }

    private UUID dossier(CfgTranche tranche, String code, UUID vendeur) {
        StkItem article = plans.article(tranche, code, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        agences.commercialDuDossier(cree.getId(), vendeur);
        plans.comFolderId(article.getId(), cree.getId());
        return cree.getId();
    }

    private void identifiants(AdmUser compte) {
        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(compte);
        credential.setPasswordHash("non-utilise");
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        em.persist(credential);
        em.flush();
    }

    private MvcResult lire(String chemin, RequestPostProcessor compte) throws Exception {
        return mockMvc.perform(get(chemin).with(compte)).andReturn();
    }

    private List<String> idsProjets(String search, RequestPostProcessor compte) throws Exception {
        MockHttpServletRequestBuilder requete = get("/api/projets").param("size", "200").with(compte);
        if (search != null) {
            requete.param("search", search);
        }
        MvcResult reponse = mockMvc.perform(requete).andReturn();
        assertThat(reponse.getResponse().getStatus()).isEqualTo(OK);
        return JsonPath.read(reponse.getResponse().getContentAsString(), IDS_PROJETS);
    }

    @Test
    void listeDesProgrammes_agence_neVoitQueSesProjetsAffectes() throws Exception {
        List<String> tous = idsProjets(null, compteA);
        List<String> horsPerimetre = idsProjets(projetB.getCode(), compteA);

        assertThat(tous).contains(projetA.getId().toString()).doesNotContain(projetB.getId().toString());
        assertThat(horsPerimetre).isEmpty();
    }

    @Test
    void listeDesProgrammes_siege_voitUnProjetNonAffecte() throws Exception {
        assertThat(idsProjets(projetB.getCode(), siege)).contains(projetB.getId().toString());
    }

    @Test
    void tranchesDuProgramme_agence_projetAffecte200_nonAffecte404() throws Exception {
        int affecte = lire("/api/projets/" + projetA.getId() + "/tranches", compteA).getResponse().getStatus();
        int nonAffecte = lire("/api/projets/" + projetB.getId() + "/tranches", compteA).getResponse().getStatus();

        assertThat(affecte).isEqualTo(OK);
        assertThat(nonAffecte).isEqualTo(INTROUVABLE);
    }

    @Test
    void tranchesDuProgramme_siege_projetNonAffecte200() throws Exception {
        assertThat(lire("/api/projets/" + projetB.getId() + "/tranches", siege).getResponse().getStatus())
                .isEqualTo(OK);
    }

    @ParameterizedTest(name = "fiche programme fermée : {0}")
    @ValueSource(strings = {"/api/projets/{projet}", "/api/projets/export"})
    void ficheEtExportDesProgrammesRestentFermes(String modele) throws Exception {
        String chemin = modele.replace("{projet}", projetA.getId().toString());
        assertThat(lire(chemin, compteA).getResponse().getStatus()).isEqualTo(INTERDIT);
    }

    @ParameterizedTest(name = "onglet ouvert sur un dossier visible : {0}")
    @ValueSource(strings = {"cadeaux", "cadeaux/catalogue", "cadeaux/raisons", "modifications", "modalite-paiement",
            "sous-articles", "acquereurs", "statut/etapes-suivantes", "echeancier/schema"})
    void ongletDuDossierVisible_agence_200(String onglet) throws Exception {
        assertThat(lire("/api/dossiers/" + dossierA + "/" + onglet, compteA).getResponse().getStatus())
                .isEqualTo(OK);
    }

    @ParameterizedTest(name = "onglet refusé sur un dossier invisible : {0}")
    @ValueSource(strings = {"cadeaux", "cadeaux/catalogue", "modifications", "modalite-paiement", "sous-articles",
            "acquereurs", "statut/etapes-suivantes", "echeancier/schema"})
    void ongletDuDossierInvisible_agence_404(String onglet) throws Exception {
        assertThat(lire("/api/dossiers/" + dossierB + "/" + onglet, compteA).getResponse().getStatus())
                .isEqualTo(INTROUVABLE);
    }

    @ParameterizedTest(name = "aide à l'édition fermée : {0}")
    @ValueSource(strings = {"edition", "statut/transition-suggeree", "sous-articles/disponibles", "reinitialiser/apercu"})
    void aidesALEditionRestentFermees(String onglet) throws Exception {
        assertThat(lire("/api/dossiers/" + dossierA + "/" + onglet, compteA).getResponse().getStatus())
                .isEqualTo(INTERDIT);
    }

    @ParameterizedTest(name = "choix d'administration de la tranche fermé : {0}")
    @ValueSource(strings = {"terrains-disponibles", "categories-frais", "entites-finalisation"})
    void choixDAdministrationDeLaTrancheFermes(String choix) throws Exception {
        assertThat(lire("/api/tranches/" + choix, compteA).getResponse().getStatus()).isEqualTo(INTERDIT);
    }
}

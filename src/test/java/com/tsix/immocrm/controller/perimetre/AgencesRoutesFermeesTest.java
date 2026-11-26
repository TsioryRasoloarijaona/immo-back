package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routes fermées aux comptes agence — FEATURE-agences-vente.md §4 V9, R-V9, R-INS2, §6 « Caché », §8.
 * Compte A : rôle Agence, vendeur lié à l'agence, projet de la tranche P affecté ; témoin : un compte siège.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesRoutesFermeesTest {

    private static final String MESSAGE_FERMEE = "Cette fonction n'est pas ouverte aux agences";
    private static final String DETAIL = "$.detail";
    private static final String TRANCHE = "{tranche}";
    private static final String DOSSIER_PROPRE = "{dossier}";
    private static final String ROLES = "/api/administration/roles";
    private static final String PROFIL = "/api/mon-compte";
    private static final String AGENCE_CHAMP = "$.agence";
    private static final int INTERDIT = 403;

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche trancheP;
    private UUID vendeurA;
    private UUID dossierA;
    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        AdmUser administrateur = patrimoine.administrateur();
        siege = connecte(administrateur);
        trancheP = plans.tranche("RFP-" + jeton, true);

        UUID agenceA = agences.agence("RFA-" + jeton);
        vendeurA = agences.vendeur("RFV-" + jeton);
        agences.lier(agenceA, vendeurA);

        StkItem article = plans.article(trancheP, "RFL-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        dossierA = cree.getId();
        agences.commercialDuDossier(dossierA, vendeurA);
        plans.comFolderId(article.getId(), dossierA);

        compteA = compteAgence("RFC");
        identifiants(administrateur);
    }

    private RequestPostProcessor compteAgence(String prefixe, String... autresRoles) {
        return compteAgence(prefixe, vendeurA, autresRoles);
    }

    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur, String... autresRoles) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur, autresRoles);
        plans.habiliter(compte, trancheP.getCfgProject());
        identifiants(compte);
        String[] roles = new String[autresRoles.length + 1];
        roles[0] = FixtureAgences.ROLE_AGENCE;
        System.arraycopy(autresRoles, 0, roles, 1, autresRoles.length);
        return FixtureDossierVente.connecte(compte, roles);
    }

    /** {@code GET /api/mon-compte} lit {@code AppUserCredential} (404 sans elle). */
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

    private String url(String modele) {
        return modele.replace(TRANCHE, trancheP.getId().toString()).replace(DOSSIER_PROPRE, dossierA.toString());
    }

    private int statut(String modele, RequestPostProcessor compte) throws Exception {
        return mockMvc.perform(get(url(modele)).with(compte)).andReturn().getResponse().getStatus();
    }

    // Une GET représentative par famille « caché » du §6 / R-V9 (Projet : TerrainService, EtudeFaisabiliteService).
    @ParameterizedTest(name = "R-V9 caché : {0}")
    @ValueSource(strings = {
            "/api/boite-de-tri",
            "/api/hotline/recherche",
            "/api/pre-prospections",
            "/api/equipe/affectations",
            "/api/kpi/commerciaux",
            "/api/etat-des-ventes",
            "/api/etat-des-ventes/synthese",
            "/api/suivi-commerciaux",
            "/api/rapports/ventes",
            "/api/encaissements",
            "/api/impayes",
            "/api/desistements",
            "/api/commissions/tranches",
            "/api/tranches/{tranche}/reajustement-superficies",
            "/api/suivi-affectations",
            "/api/projet/decomptes",
            "/api/terrains",
            "/api/etudes-faisabilite",
            "/api/achat/demandes-achat",
            "/api/utilisateurs",
            ROLES,
            "/api/administration/tiers/prospects",
            "/api/import-patrimoine/articles/colonnes",
            "/api/dashboard/annuel",
            "/api/dashboard/nouveaux-pre-prospects",
            "/api/notes"
    })
    void rv9_routeCachee_403PourCompteAgence(String route) throws Exception {
        mockMvc.perform(get(url(route)).with(compteA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));
    }

    // Témoin : le siège n'est pas fermé (R-V9 « Le siège : aucun changement »), le 403 agence vient donc de la fermeture.
    @ParameterizedTest(name = "R-V9 témoin siège : {0}")
    @ValueSource(strings = {
            "/api/boite-de-tri",
            "/api/hotline/recherche",
            "/api/pre-prospections",
            "/api/equipe/affectations",
            "/api/kpi/commerciaux",
            "/api/etat-des-ventes",
            "/api/etat-des-ventes/synthese",
            "/api/suivi-commerciaux",
            "/api/rapports/ventes",
            "/api/encaissements",
            "/api/impayes",
            "/api/desistements",
            "/api/tranches/{tranche}/reajustement-superficies",
            "/api/suivi-affectations",
            "/api/projet/decomptes",
            "/api/terrains",
            "/api/etudes-faisabilite",
            "/api/utilisateurs",
            ROLES,
            "/api/administration/tiers/prospects",
            "/api/import-patrimoine/articles/colonnes",
            "/api/dashboard/annuel",
            "/api/dashboard/nouveaux-pre-prospects",
            "/api/notes"
    })
    void rv9_temoinSiege_routeCacheeNonFermee(String route) throws Exception {
        assertThat(statut(route, siege)).as(route).isNotEqualTo(INTERDIT);
    }

    @Test
    void rv9_ecritureAdministration_403EtRienEcrit() throws Exception {
        String code = "RFR-" + jeton;

        mockMvc.perform(post(ROLES).with(compteA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));

        em.flush();
        Integer roles = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM AdmRole WHERE Code = ?", Integer.class, code);
        assertThat(roles).isZero();
    }

    @Test
    void rv9_statutDossierParAgence_403EtStatutInchange() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        em.flush();
        String avant = statutEnBase(jdbc);

        mockMvc.perform(patch("/api/dossiers/{id}/statut", dossierA).with(compteA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CONCRETISATION\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));

        em.flush();
        em.clear();
        assertThat(statutEnBase(jdbc)).isEqualTo(avant);
    }

    private String statutEnBase(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT CAST(StrStatusId AS NVARCHAR(36)) FROM ComFolder WHERE PKey = ?",
                String.class, dossierA.toString());
    }

    // Décision manager A1d (5) : ajout de règlement par une agence fermé en A1d.
    @Test
    void rv9_ajoutReglementParAgence_403() throws Exception {
        mockMvc.perform(post("/api/dossiers/{id}/reglements", dossierA).with(compteA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));
    }

    // Décision manager A1d (1) : la fermeture lit le rôle Agence, indépendamment du vendeur et des projets.
    @Test
    void rv9_compteAgenceSansVendeur_403SurRouteCachee() throws Exception {
        RequestPostProcessor sansVendeur = compteAgence("RFS", (UUID) null);

        mockMvc.perform(get(ROLES).with(sansVendeur))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));
    }

    // R-V9 : « Le 403 passe avant tout chargement (pas de 404 qui trahirait l'existence d'un id) ».
    @Test
    void rv9_403AvantChargement_idInconnu() throws Exception {
        mockMvc.perform(get("/api/desistements/{id}", UUID.randomUUID()).with(compteA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));
    }

    // R-V9 liste blanche : écrans « Visible » du §6 et référentiels du formulaire de prospection.
    @ParameterizedTest(name = "R-V9 liste blanche : {0}")
    @ValueSource(strings = {
            "/api/dashboard",
            "/api/journee",
            "/api/calendrier/evenements",
            "/api/prospects",
            "/api/prospections",
            "/api/devis",
            "/api/clients",
            "/api/tranches",
            "/api/articles",
            "/api/dossiers",
            "/api/dossiers/{dossier}",
            "/api/mon-compte",
            "/api/referentiels/origines-prospection",
            "/api/referentiels/supports-origine",
            "/api/referentiels/projets",
            "/api/referentiels/types-bien",
            "/api/notifications"
    })
    void rv9_routeVisible_resteOuvertePourCompteAgence(String route) throws Exception {
        assertThat(statut(route, compteA)).as(route).isNotEqualTo(INTERDIT);
    }

    @Test
    void rv9_profilCourant_agenceVraiPourCompteAgence() throws Exception {
        mockMvc.perform(get(PROFIL).with(compteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath(AGENCE_CHAMP).value(true));
    }

    @Test
    void rv9_profilCourant_agenceFauxPourSiege() throws Exception {
        mockMvc.perform(get(PROFIL).with(siege))
                .andExpect(status().isOk())
                .andExpect(jsonPath(AGENCE_CHAMP).value(false));
    }

    @Test
    void rIns2_agenceEtAdministrateur_403SurAdministration() throws Exception {
        RequestPostProcessor agenceAdmin = compteAgence("RFX", "Administrateur");

        mockMvc.perform(get(ROLES).with(agenceAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));
    }
}

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Seconde revue A1d : routes d'un module « Caché » (§6 : Projet, Patrimoine) servies sous un motif ouvert de la liste
 * blanche ({@code GET /api/tranches/*}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA1dSecondeRevueTest {

    private static final int INTERDIT = 403;

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        AdmUser administrateur = patrimoine.administrateur();
        CfgTranche tranche = plans.tranche("RSP-" + jeton, true);
        UUID agence = agences.agence("RSA-" + jeton);
        UUID vendeur = agences.vendeur("RSV-" + jeton);
        agences.lier(agence, vendeur);
        StkItem article = plans.article(tranche, "RSL-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        agences.commercialDuDossier(cree.getId(), vendeur);
        plans.comFolderId(article.getId(), cree.getId());

        AdmUser compte = agences.compteAgence("RSC-" + jeton, vendeur);
        plans.habiliter(compte, tranche.getCfgProject());
        identifiants(compte);
        identifiants(administrateur);
        compteA = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
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

    /**
     * {@code GET /api/tranches/*} (fiche tranche) couvre aussi {@code /api/tranches/terrains-disponibles} : la liste
     * des terrains de vocation Projet (module Projet / Patrimoine, « Caché »), non filtrée par périmètre.
     */
    @ParameterizedTest(name = "module caché sous /api/tranches/* : {0}")
    @ValueSource(strings = {"/api/tranches/terrains-disponibles"})
    void terrainsDuModuleProjetFermesAuxAgences(String chemin) throws Exception {
        int statut = mockMvc.perform(get(chemin).with(compteA)).andReturn().getResponse().getStatus();
        assertThat(statut).isEqualTo(INTERDIT);
    }
}

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Revue A1d : routes « Caché » du §6 que la liste blanche laisse pourtant passer pour un compte agence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA1dRevueTest {

    private static final int INTERDIT = 403;

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private UUID dossierA;
    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        AdmUser administrateur = patrimoine.administrateur();
        CfgTranche tranche = plans.tranche("RVP-" + jeton, true);
        UUID agence = agences.agence("RVA-" + jeton);
        UUID vendeur = agences.vendeur("RVV-" + jeton);
        agences.lier(agence, vendeur);
        StkItem article = plans.article(tranche, "RVL-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        dossierA = cree.getId();
        agences.commercialDuDossier(dossierA, vendeur);
        plans.comFolderId(article.getId(), dossierA);

        AdmUser compte = agences.compteAgence("RVC-" + jeton, vendeur);
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

    /** §6 « Caché » : Désistements. {@code GET /api/dossiers/**} les ouvre via /api/dossiers/{id}/desistement. */
    @ParameterizedTest(name = "désistement fermé : {0}")
    @ValueSource(strings = {"/api/dossiers/{dossier}/desistement", "/api/dossiers/{dossier}/desistement/simulation"})
    void desistementDunDossierEstFermeAuxAgences(String modele) throws Exception {
        int statut = mockMvc.perform(get(modele.replace("{dossier}", dossierA.toString())).with(compteA))
                .andReturn().getResponse().getStatus();
        assertThat(statut).isEqualTo(INTERDIT);
    }

    /** §6 « Caché » : Pré-prospections. {@code transformer} est la bascule pré-prospection → prospection. */
    @ParameterizedTest(name = "pré-prospection fermée : {0}")
    @ValueSource(strings = {"/api/prospections/{id}/transformer"})
    void transformerUnePreProspectionEstFermeAuxAgences(String modele) throws Exception {
        int statut = mockMvc.perform(post(modele.replace("{id}", UUID.randomUUID().toString()))
                        .with(compteA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(statut).isEqualTo(INTERDIT);
    }

    /** Colonnes des écrans cachés (Encaissements, Impayés, Suivi) servies par {@code GET /api/referentiels/**}. */
    @ParameterizedTest(name = "référentiel d'écran caché : {0}")
    @ValueSource(strings = {"/api/referentiels/colonnes/encaissements", "/api/referentiels/colonnes/impayes",
            "/api/referentiels/colonnes/suivi-actions", "/api/referentiels/colonnes/etat-des-ventes",
            "/api/referentiels/motifs-desistement"})
    void referentielsDecransCachesSontFermes(String chemin) throws Exception {
        int statut = mockMvc.perform(get(chemin).with(compteA)).andReturn().getResponse().getStatus();
        assertThat(statut).isEqualTo(INTERDIT);
    }
}

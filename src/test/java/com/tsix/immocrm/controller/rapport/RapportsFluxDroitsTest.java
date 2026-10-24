package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_MOIS;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.CODES;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.COMPARAISON;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.DETAIL_CA;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ETAT_VENTES;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.SITUATION_GLOBALE;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.SUIVI_CA;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des rapports de flux par le socle (FEATURE-tdb-vente-rapports-flux.md §1 « Droits », §8.1 R-V4, R-V5) : rôle restreint
 * non lié ⇒ 403 sur les trois routes ; Commercial Chef lié aux n° 6, 5, 3 (pas au n° 14, comme en prod) ; clé d'API ⇒ 403
 * {@code HORS_PORTEE}. Chaque appel porte un {@code trancheId} visible (le n° 3 l'exige).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsFluxDroitsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private AppelsRapports appels;
    private Map<String, UUID> ids;
    private Jeu jeu;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        ids = RapportsFluxSupport.installer(rapports);
        jeu = fixture.jeu("DRF");
        tranche = jeu.tranche("T", jeu.projet("P", true));
        rapports.relire();
    }

    private MockHttpServletRequestBuilder avecTranche(MockHttpServletRequestBuilder requete) {
        return requete.param(TRANCHE_ID, tranche.getId().toString()).param("annee", "2024");
    }

    private RequestPostProcessor compteAvec(AdmRole role) {
        RequestPostProcessor qui = connecte(rapports.compte("drf." + role.getCode(), role));
        rapports.relire();
        return qui;
    }

    @ParameterizedTest
    @ValueSource(strings = {ETAT_VENTES, ACTIVITE_PAR_PROJET, ACTIVITE_PAR_MOIS, COMPARAISON, DETAIL_CA,
            SITUATION_GLOBALE, SUIVI_CA})
    void r_v4_roleRestreintLieAAucunRapport_403SurDescripteurResultatExport(String code) throws Exception {
        RequestPostProcessor restreint = compteAvec(rapports.role("Restreint " + jeu.prefixe(), true));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(avecTranche(get(descripteur(VENTE, code))).with(restreint))).as("descripteur")
                .isEqualTo(403);
        soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(restreint))).as("resultat")
                .isEqualTo(403);
        soft.assertThat(appels.exporter(avecTranche(get(export(VENTE, code))).param("format", "xlsx").with(restreint))
                .getResponse().getStatus()).as("export").isEqualTo(403);
        soft.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {ETAT_VENTES, ACTIVITE_PAR_PROJET, ACTIVITE_PAR_MOIS, COMPARAISON, DETAIL_CA,
            SITUATION_GLOBALE, SUIVI_CA})
    void r_v4_roleRestreintLieAuRapport_200SurLeResultat(String code) throws Exception {
        AdmRole role = rapports.role("Lie " + jeu.prefixe(), true);
        rapports.lier(ids.get(code), role);
        RequestPostProcessor lie = compteAvec(role);

        mockMvc.perform(avecTranche(get(resultat(VENTE, code))).with(lie)).andExpect(status().isOk());
    }

    @Test
    void r_v4_commercialChef_403SurLEtatDesVentes_200SurLesAutres() throws Exception {
        AdmRole chef = rapports.role("Commercial Chef " + jeu.prefixe(), true);
        for (String code : List.of(ACTIVITE_PAR_PROJET, ACTIVITE_PAR_MOIS, COMPARAISON)) {
            rapports.lier(ids.get(code), chef);
        }
        RequestPostProcessor qui = compteAvec(chef);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, ETAT_VENTES))).with(qui))).as(ETAT_VENTES)
                .isEqualTo(403);
        for (String code : List.of(ACTIVITE_PAR_PROJET, ACTIVITE_PAR_MOIS, COMPARAISON)) {
            soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(qui))).as(code).isEqualTo(200);
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R1b : n° 7, 11, 15

    @Test
    void r_v4_commercialChef_200SurLesN7N11N15() throws Exception {
        AdmRole chef = rapports.role("Commercial Chef " + jeu.prefixe(), true);
        for (String code : List.of(DETAIL_CA, SITUATION_GLOBALE, SUIVI_CA)) {
            rapports.lier(ids.get(code), chef);
        }
        RequestPostProcessor qui = compteAvec(chef);

        SoftAssertions soft = new SoftAssertions();
        for (String code : List.of(DETAIL_CA, SITUATION_GLOBALE, SUIVI_CA)) {
            soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(qui))).as(code).isEqualTo(200);
        }
        soft.assertAll();
    }

    @Test
    void r_v4_directionGenerale_403SurLeN15_200SurLesN7EtN11() throws Exception {
        AdmRole dg = rapports.role("Direction Generale " + jeu.prefixe(), true);
        rapports.lier(ids.get(DETAIL_CA), dg);
        rapports.lier(ids.get(SITUATION_GLOBALE), dg);
        RequestPostProcessor qui = compteAvec(dg);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, SUIVI_CA))).with(qui))).as(SUIVI_CA)
                .isEqualTo(403);
        for (String code : List.of(DETAIL_CA, SITUATION_GLOBALE)) {
            soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(qui))).as(code).isEqualTo(200);
        }
        soft.assertAll();
    }

    @Test
    void r_v5_cleDApiAvecToutesLesPortees_403HorsPortee_surLesSeptRapports() throws Exception {
        AdmUser compte = dossiers.compte("N8N-" + jeu.prefixe(), "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("flux-" + jeu.prefixe());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        for (String code : CODES) {
            mockMvc.perform(avecTranche(get(resultat(VENTE, code))).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        }
    }
}

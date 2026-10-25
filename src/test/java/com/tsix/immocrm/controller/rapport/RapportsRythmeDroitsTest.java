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
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CODES_R2A;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CODES_R2B;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CONCRET_ENCAISSEMENT;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.PAR_COMMERCIAL;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_VENTES;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des rapports R2a par le socle (FEATURE-tdb-vente-rapports-rythme.md §1 « Lignes de rôle », §7.1 R-V4, R-V5) : rôle
 * restreint non lié ⇒ 403 sur les trois routes ; chacun des quatre rôles de la prod (Administrateur, Commercial Chef, Direction
 * foncière, Direction Générale), restreint et lié ⇒ 200 ; clé d'API ⇒ 403 {@code HORS_PORTEE}. Avant le code, un code sans
 * classe répond 404 : ces tests sont rouges.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsRythmeDroitsTest {

    private static final List<String> ROLES_DE_LA_PROD = List.of("Administrateur", "Commercial Chef",
            "Direction foncière", "Direction Générale");

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
        ids = RapportsRythmeSupport.installer(rapports);
        jeu = fixture.jeu("DR2");
        tranche = jeu.tranche("T", jeu.projet("P", true));
        rapports.relire();
    }

    private MockHttpServletRequestBuilder avecTranche(MockHttpServletRequestBuilder requete) {
        return requete.param(TRANCHE_ID, tranche.getId().toString());
    }

    private RequestPostProcessor compteAvec(AdmRole role) {
        RequestPostProcessor qui = connecte(rapports.compte("dr2." + UUID.randomUUID().toString().substring(0, 6),
                role));
        rapports.relire();
        return qui;
    }

    @ParameterizedTest
    @ValueSource(strings = {RYTHME_VENTES, PAR_COMMERCIAL, CONCRET_ENCAISSEMENT})
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
    @ValueSource(strings = {RYTHME_VENTES, PAR_COMMERCIAL, CONCRET_ENCAISSEMENT})
    void r_v4_quatreRolesDeLaProd_restreintsEtLies_200SurDescripteurEtResultat(String code) throws Exception {
        SoftAssertions soft = new SoftAssertions();
        for (String nom : ROLES_DE_LA_PROD) {
            AdmRole role = rapports.role(nom + " " + jeu.prefixe(), true);
            rapports.lier(ids.get(code), role);
            RequestPostProcessor lie = compteAvec(role);
            soft.assertThat(appels.statut(avecTranche(get(descripteur(VENTE, code))).with(lie)))
                    .as("%s descripteur", nom).isEqualTo(200);
            soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(lie)))
                    .as("%s resultat", nom).isEqualTo(200);
        }
        soft.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {RYTHME_VENTES, PAR_COMMERCIAL, CONCRET_ENCAISSEMENT})
    void r_v4_roleLibre_200SurLExportXlsx(String code) throws Exception {
        RequestPostProcessor libre = compteAvec(rapports.role("Libre " + jeu.prefixe(), false));

        assertThat(appels.exporter(avecTranche(get(export(VENTE, code)))
                .param("format", "xlsx").with(libre)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void r_v5_cleDApiAvecToutesLesPortees_403HorsPortee_surLesTroisRapports() throws Exception {
        AdmUser compte = dossiers.compte("DR2-" + jeu.prefixe(), "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("rythme-" + jeu.prefixe());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        for (String code : CODES_R2A) {
            mockMvc.perform(avecTranche(get(resultat(VENTE, code))).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        }
    }

    // ------------------------------------------------------------------ R2b : n° 9, 10, 22, 24, 27 (§7.1 R-V4, R-V5)

    @Test
    void r_v4_r2b_roleRestreintLieAAucunRapport_403SurDescripteurResultatExport_desCinqCodes() throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);
        RequestPostProcessor restreint = compteAvec(rapports.role("Restreint B " + jeu.prefixe(), true));

        SoftAssertions soft = new SoftAssertions();
        for (String code : CODES_R2B) {
            soft.assertThat(appels.statut(avecTranche(get(descripteur(VENTE, code))).with(restreint)))
                    .as("%s descripteur", code).isEqualTo(403);
            soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(restreint)))
                    .as("%s resultat", code).isEqualTo(403);
            soft.assertThat(appels.exporter(avecTranche(get(export(VENTE, code))).param("format", "xlsx")
                    .with(restreint)).getResponse().getStatus()).as("%s export", code).isEqualTo(403);
        }
        soft.assertAll();
    }

    @Test
    void r_v4_r2b_quatreRolesDeLaProd_restreintsEtLies_200SurDescripteurEtResultat_desCinqCodes() throws Exception {
        Map<String, UUID> r2b = RapportsRythmeSupport.installerR2b(rapports);
        SoftAssertions soft = new SoftAssertions();
        for (String nom : ROLES_DE_LA_PROD) {
            AdmRole role = rapports.role(nom + " B " + jeu.prefixe(), true);
            CODES_R2B.forEach(code -> rapports.lier(r2b.get(code), role));
            RequestPostProcessor lie = compteAvec(role);
            for (String code : CODES_R2B) {
                soft.assertThat(appels.statut(avecTranche(get(descripteur(VENTE, code))).with(lie)))
                        .as("%s %s descripteur", nom, code).isEqualTo(200);
                soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(lie)))
                        .as("%s %s resultat", nom, code).isEqualTo(200);
            }
        }
        soft.assertAll();
    }

    @Test
    void r_v4_r2b_roleLieAUnSeulRapport_403SurLesQuatreAutres() throws Exception {
        Map<String, UUID> r2b = RapportsRythmeSupport.installerR2b(rapports);
        AdmRole role = rapports.role("Direction Générale B1 " + jeu.prefixe(), true);
        rapports.lier(r2b.get(RapportsRythmeSupport.TAUX_VENTE_STOCK), role);
        RequestPostProcessor lie = compteAvec(role);

        SoftAssertions soft = new SoftAssertions();
        for (String code : CODES_R2B) {
            int attendu = RapportsRythmeSupport.TAUX_VENTE_STOCK.equals(code) ? 200 : 403;
            soft.assertThat(appels.statut(avecTranche(get(resultat(VENTE, code))).with(lie))).as(code)
                    .isEqualTo(attendu);
        }
        soft.assertAll();
    }

    @Test
    void r_v4_r2b_roleLibre_200SurLExportXlsx_desCinqCodes() throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);
        RequestPostProcessor libre = compteAvec(rapports.role("Libre B " + jeu.prefixe(), false));

        SoftAssertions soft = new SoftAssertions();
        for (String code : CODES_R2B) {
            soft.assertThat(appels.exporter(avecTranche(get(export(VENTE, code))).param("format", "xlsx").with(libre))
                    .getResponse().getStatus()).as(code).isEqualTo(200);
        }
        soft.assertAll();
    }

    @Test
    void r_v5_r2b_cleDApiAvecToutesLesPortees_403HorsPortee_surLesCinqRapports() throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);
        String cle = cleApiToutesPortees("rythme-b-");

        for (String code : CODES_R2B) {
            mockMvc.perform(avecTranche(get(resultat(VENTE, code))).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        }
    }

    private String cleApiToutesPortees(String nom) {
        AdmUser compte = dossiers.compte("DR2B-" + jeu.prefixe(), "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom(nom + jeu.prefixe());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }
}

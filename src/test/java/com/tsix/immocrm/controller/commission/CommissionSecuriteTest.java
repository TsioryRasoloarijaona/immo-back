package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.LectureExport;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DOCUMENT_DE_LIGNE;
import static com.tsix.immocrm.fixtures.ApiCommissions.DOSSIERS;
import static com.tsix.immocrm.fixtures.ApiCommissions.DROITS;
import static com.tsix.immocrm.fixtures.ApiCommissions.EXPORT_LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.EXPORT_TRANCHES;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.TRANCHES;
import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.montant;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static com.tsix.immocrm.fixtures.ApiCommissions.tranche;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.IMPRIMER;
import static com.tsix.immocrm.fixtures.FixtureCommissions.NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureCommissions.SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits (règle 6) — FEATURE-vente-commissions.md §3.2 (droits lus dans {@code AdmRight} par {@code AdmUserRole} à chaque appel,
 * jamais le JWT : le jeton de ces tests porte un rôle non administrateur), décisions du 25/10, réponse M3
 * ({@code GET /droits}, droit absent = refusé), §8.1 « Droits / visibilité ». Chaque refus est vérifié sans écriture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionSecuriteTest {

    private static final LocalDate JOUR = LocalDate.of(2024, 2, 23);
    private static final BigDecimal TROIS_CENTS = new BigDecimal("300");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private Contexte c;
    private UUID trancheId;
    private UUID commercial;
    private Dossier d;
    private UUID ligne;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMS");
        trancheId = c.tranche().getId();
        commercial = fixture.personne("COM-" + c.jeton(), false);
        fixture.regleMontant(trancheId, new BigDecimal("100"));
        d = fixture.dossier(c, "D1", commercial, new BigDecimal("775000"), JOUR);
        ligne = fixture.ligne(fixture.dossier(c, "D2", commercial, new BigDecimal("775000"), JOUR), CONCRETISATION,
                commercial, TROIS_CENTS, JOUR, null);
    }

    private Map<String, MockHttpServletRequestBuilder> lectures() {
        Map<String, MockHttpServletRequestBuilder> m = new LinkedHashMap<>();
        m.put("liste des tranches", get(TRANCHES));
        m.put("tranche", tranche(trancheId));
        m.put("dossiers", get(DOSSIERS, trancheId));
        m.put("lignes", get(LIGNES, trancheId));
        m.put("factures", get(FACTURES, trancheId));
        m.put("document de la ligne", get(DOCUMENT_DE_LIGNE, ligne));
        return m;
    }

    private Map<String, MockHttpServletRequestBuilder> ecritures() {
        Map<String, MockHttpServletRequestBuilder> m = new LinkedHashMap<>();
        m.put("generation", generation(trancheId, T_CONCRETISATION, List.of(d.id())));
        m.put("montant", montant(ligne, new BigDecimal("250")));
        m.put("document a payer", document(List.of(ligne)));
        return m;
    }

    private int statut(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        return LectureExport.executer(mockMvc, requete.with(qui)).getResponse().getStatus();
    }

    private void verifierRienEcrit() {
        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(decimal(fixture.ligneParId(ligne).get("CommissionAmount"))).isEqualByComparingTo(TROIS_CENTS),
                () -> assertThat(fixture.ligneParId(ligne).get("BuyDocumentId")).isNull());
    }

    // ------------------------------------------------------------------ sans l'entrée d'écran : 403 partout

    @Test
    void d32_sansEntreeDEcran_403SurToutesLesRoutes_rienEcrit() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton()).qui();
        Map<String, MockHttpServletRequestBuilder> toutes = new LinkedHashMap<>(lectures());
        toutes.putAll(ecritures());
        toutes.put("suppression", suppression(List.of(ligne)));
        toutes.put("export lignes", get(EXPORT_LIGNES, trancheId));
        toutes.put("export tranches", get(EXPORT_TRANCHES));
        toutes.put("droits", get(DROITS));

        SoftAssertions soft = new SoftAssertions();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : toutes.entrySet()) {
            soft.assertThat(statut(e.getValue(), qui)).as(e.getKey()).isEqualTo(403);
        }
        soft.assertAll();
        verifierRienEcrit();
    }

    /** M3 : un droit d'outil sans l'entrée d'écran n'ouvre rien (« rôle sans l'écran → 403 »). */
    @Test
    void d32_toolNewSansEntreeDEcran_403EnEcriture() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), NOUVEAU, SUPPRIMER, IMPRIMER).qui();

        SoftAssertions soft = new SoftAssertions();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : ecritures().entrySet()) {
            soft.assertThat(statut(e.getValue(), qui)).as(e.getKey()).isEqualTo(403);
        }
        soft.assertThat(statut(get(TRANCHES), qui)).as("liste").isEqualTo(403);
        soft.assertAll();
        verifierRienEcrit();
    }

    // ------------------------------------------------------------------ écran sans toolNew : lecture seule

    @Test
    void d32_ecranSansToolNew_lecture200() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN).qui();

        SoftAssertions soft = new SoftAssertions();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : lectures().entrySet()) {
            int attendu = "document de la ligne".equals(e.getKey()) ? 404 : 200;
            soft.assertThat(statut(e.getValue(), qui)).as(e.getKey()).isEqualTo(attendu);
        }
        soft.assertAll();
    }

    @Test
    void d32_ecranSansToolNew_generationMontantDocument403_rienEcrit() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, SUPPRIMER, IMPRIMER).qui();

        SoftAssertions soft = new SoftAssertions();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : ecritures().entrySet()) {
            soft.assertThat(statut(e.getValue(), qui)).as(e.getKey()).isEqualTo(403);
        }
        soft.assertAll();
        verifierRienEcrit();
    }

    @Test
    void d32_sansToolDelete_suppression403_ligneConservee() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, NOUVEAU, IMPRIMER).qui();

        mockMvc.perform(suppression(List.of(ligne)).with(qui)).andExpect(status().isForbidden());

        assertThat(fixture.compter("SELECT COUNT(*) FROM ComCommissionLine WHERE PKey = :id", ligne)).isEqualTo(1);
    }

    @Test
    void d32_avecToolDelete_suppression204() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, SUPPRIMER).qui();

        mockMvc.perform(suppression(List.of(ligne)).with(qui)).andExpect(status().isNoContent());

        assertThat(fixture.compter("SELECT COUNT(*) FROM ComCommissionLine WHERE PKey = :id", ligne)).isZero();
    }

    @Test
    void d32_sansToolPrint_exports403() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER).qui();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(statut(get(EXPORT_LIGNES, trancheId), qui)).as("lignes").isEqualTo(403);
        soft.assertThat(statut(get(EXPORT_TRANCHES), qui)).as("tranches").isEqualTo(403);
        soft.assertAll();
    }

    @Test
    void d32_avecToolPrint_exports200() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, IMPRIMER).qui();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(statut(get(EXPORT_LIGNES, trancheId), qui)).as("lignes").isEqualTo(200);
        soft.assertThat(statut(get(EXPORT_TRANCHES), qui)).as("tranches").isEqualTo(200);
        soft.assertAll();
    }

    /** Écriture permise avec l'écran et toolNew, rôle quelconque (pas de liste de rôles en dur). */
    @Test
    void d32_ecranEtToolNew_roleQuelconque_generation200() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, NOUVEAU).qui();

        mockMvc.perform(generation(trancheId, T_CONCRETISATION, List.of(d.id())).with(qui)).andExpect(status().isOk());

        assertThat(fixture.lignesDuDossier(d.id())).hasSize(1);
    }

    // ------------------------------------------------------------------ droits relus à chaque appel

    @Test
    void d32_droitRetire_refuseSansReconnexion() throws Exception {
        Compte compte = fixture.compte(c.jeton(), ECRAN, NOUVEAU);
        RequestPostProcessor qui = compte.qui();
        mockMvc.perform(get(TRANCHES).with(qui)).andExpect(status().isOk());

        fixture.retirerDroit(compte.role(), NOUVEAU);
        fixture.relire();

        mockMvc.perform(generation(trancheId, T_CONCRETISATION, List.of(d.id())).with(qui))
                .andExpect(status().isForbidden());
        assertThat(fixture.lignesDuDossier(d.id())).isEmpty();
    }

    @Test
    void d32_ecranRetire_lecture403SansReconnexion() throws Exception {
        Compte compte = fixture.compte(c.jeton(), ECRAN);
        mockMvc.perform(get(TRANCHES).with(compte.qui())).andExpect(status().isOk());

        fixture.retirerDroit(compte.role(), ECRAN);
        fixture.relire();

        mockMvc.perform(get(TRANCHES).with(compte.qui())).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ M3 : GET /droits

    @Test
    void m3_droits_tousLesOutils_vrais() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER).qui();

        mockMvc.perform(get(DROITS).with(qui)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ecriture").value(true))
                .andExpect(jsonPath("$.suppression").value(true))
                .andExpect(jsonPath("$.impression").value(true));
    }

    /** Direction fonciere en prod : écran + impression seulement. */
    @Test
    void m3_droits_ecranEtImpression_lectureSeule() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), ECRAN, IMPRIMER).qui();

        mockMvc.perform(get(DROITS).with(qui)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ecriture").value(false))
                .andExpect(jsonPath("$.suppression").value(false))
                .andExpect(jsonPath("$.impression").value(true));
    }

    // ------------------------------------------------------------------ clé d'API

    @Test
    void d32_cleDApiToutesPortees_403HorsPortee() throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("commissions-" + c.jeton());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER).compte());
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.of(2026, 10, 27, 0, 0));
        clientRepository.save(client);

        mockMvc.perform(get(TRANCHES).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        mockMvc.perform(generation(trancheId, T_CONCRETISATION, List.of(d.id()))
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        assertThat(fixture.lignesDuDossier(d.id())).isEmpty();
    }
}

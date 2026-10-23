package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.services.dossier.liste.StatutsVisiblesListe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.LISTE;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.SESSION_DU_TEST;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.ids;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.requete;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.verifierCoherence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lot H, R5 (règle 6) : les nouveaux filtres ne contournent ni le périmètre, ni les entités inactives, ni la session. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeHistoriqueVisibiliteTest {

    private static final String TOTAL_MIN = "totalMin";
    private static final String SOCIETE = "societeId";
    private static final String SANS_STATUT = "sansStatut";
    private static final String SORTIS = "inclureSortis";
    private static final String VRAI = "true";
    private static final String COMMERCIAL = "Commercial";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureEtatVentes fixtureEtat;
    @Autowired
    private FixtureStatutsDossier statuts;
    @MockitoBean
    private StatutsVisiblesListe statutsVisibles;

    private String prefixe;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private CfgTranche trancheInactive;
    private ComFolder dA;
    private ComFolder dSansA;
    private ComFolder dB;
    private ComFolder dSansB;
    private ComFolder dInactif;
    private RequestPostProcessor restreint;
    private RequestPostProcessor administrateur;

    /** Commercial affecté au seul projet de la tranche A ; dossiers à 500 partout, plus un sans statut par tranche. */
    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 6);
        prefixe = "HVIS" + jeton;
        trancheA = fixture.tranche("TR-VA-" + jeton, true);
        trancheB = fixture.tranche("TR-VB-" + jeton, true);
        trancheInactive = fixture.tranche("TR-VI-" + jeton, false);
        dA = dossier("A", trancheA, StatutDossier.RESERVATION);
        dSansA = dossier("B", trancheA, null);
        dB = dossier("C", trancheB, StatutDossier.RESERVATION);
        dSansB = dossier("D", trancheB, null);
        dInactif = dossier("E", trancheInactive, StatutDossier.RESERVATION);

        AdmUser commercial = fixture.compte("hv.com." + jeton);
        fixtureEtat.affecter(commercial, trancheA.getCfgProject());
        AdmUser admin = fixture.compte("hv.admin." + jeton, "Administrateur");
        fixtureEtat.affecter(admin, trancheA.getCfgProject());
        fixtureEtat.relire();
        restreint = FixtureDossierVente.connecte(commercial, COMMERCIAL);
        administrateur = FixtureDossierVente.connecte(admin, "Administrateur");
    }

    private ComFolder dossier(String suffixe, CfgTranche tranche, StatutDossier statut) {
        ComFolder f = fixture.dossier(prefixe + suffixe, tranche, null);
        f.setTotalToPay(new BigDecimal("500"));
        f.setStrStatus(statut == null ? null : statuts.statut(statut));
        return f;
    }

    private String societe(CfgTranche tranche) {
        return tranche.getCfgProject().getCfgCompany().getId().toString();
    }

    private String[] avec(String... paires) {
        String[] tout = new String[paires.length + 4];
        tout[0] = "search";
        tout[1] = prefixe;
        tout[2] = SORTIS;
        tout[3] = VRAI;
        System.arraycopy(paires, 0, tout, 4, paires.length);
        return tout;
    }

    // ---- R5a : commercial restreint

    @Test
    void r5a_commercialRestreint_nouveauxFiltres_neVoitQueSonPerimetre() throws Exception {
        assertThat(ids(mockMvc, restreint, avec(TOTAL_MIN, "500")))
                .containsExactlyInAnyOrder(dA.getId().toString(), dSansA.getId().toString());
        assertThat(ids(mockMvc, restreint, avec(SANS_STATUT, VRAI)))
                .containsExactly(dSansA.getId().toString());
        assertThat(ids(mockMvc, restreint, avec(SOCIETE, societe(trancheA))))
                .containsExactlyInAnyOrder(dA.getId().toString(), dSansA.getId().toString());
    }

    @Test
    void r5a_commercialRestreint_societeHorsPerimetre_listeVide() throws Exception {
        assertThat(ids(mockMvc, restreint, avec(SOCIETE, societe(trancheB)))).isEmpty();
        assertThat(ids(mockMvc, restreint, avec(SOCIETE, societe(trancheB), SANS_STATUT, VRAI,
                TOTAL_MIN, "0"))).isEmpty();
    }

    @Test
    void r5a_commercialRestreint_coherenceDesCinqRoutes_nouveauxFiltres() throws Exception {
        verifierCoherence(mockMvc, restreint, 2, avec(TOTAL_MIN, "500", SOCIETE, societe(trancheA)));
        verifierCoherence(mockMvc, restreint, 1, avec(SANS_STATUT, VRAI));
        verifierCoherence(mockMvc, restreint, 0, avec(SOCIETE, societe(trancheB)));
    }

    @Test
    void r5a_temoin_administrateurVoitLaSocieteB() throws Exception {
        assertThat(ids(mockMvc, administrateur, avec(SOCIETE, societe(trancheB))))
                .containsExactlyInAnyOrder(dB.getId().toString(), dSansB.getId().toString());
    }

    // ---- R5b : entité inactive

    @Test
    void r5b_entiteInactive_absenteDesCinqRoutes_avecLesNouveauxFiltres() throws Exception {
        assertThat(ids(mockMvc, SESSION_DU_TEST, avec(TOTAL_MIN, "500")))
                .isNotEmpty()
                .doesNotContain(dInactif.getId().toString())
                .containsExactlyInAnyOrder(dA.getId().toString(), dSansA.getId().toString(),
                        dB.getId().toString(), dSansB.getId().toString());
        verifierCoherence(mockMvc, SESSION_DU_TEST, 4, avec(TOTAL_MIN, "500"));
        verifierCoherence(mockMvc, SESSION_DU_TEST, 0, avec(SOCIETE, societe(trancheInactive)));
    }

    // ---- R5c : tranche hors périmètre

    @Test
    void r5c_trancheHorsPerimetre_aucuneLigne_memeAvecLesNouveauxFiltres() throws Exception {
        assertThat(ids(mockMvc, restreint, avec("trancheId", trancheB.getId().toString()))).isEmpty();
        assertThat(ids(mockMvc, restreint, avec("trancheId", trancheB.getId().toString(), SANS_STATUT, VRAI,
                TOTAL_MIN, "0"))).isEmpty();
        assertThat(ids(mockMvc, restreint, avec("trancheId", trancheA.getId().toString(),
                "trancheId", trancheB.getId().toString())))
                .containsExactlyInAnyOrder(dA.getId().toString(), dSansA.getId().toString());
        verifierCoherence(mockMvc, restreint, 0, avec("trancheId", trancheB.getId().toString()));
    }

    // ---- R5d : session

    @WithAnonymousUser
    @ParameterizedTest
    @ValueSource(strings = {"", "/groupes", "/totaux", "/totaux-colonnes", "/export"})
    void r5d_sansSession_401_surLesCinqRoutes(String suffixe) throws Exception {
        mockMvc.perform(requete(LISTE + suffixe, SESSION_DU_TEST, "groupBy", "statut", TOTAL_MIN, "1",
                        "totalMax", "9", SOCIETE, societe(trancheA), SANS_STATUT, VRAI))
                .andExpect(status().isUnauthorized());
    }

    // ---- Statuts visibles : « sans statut » ne contourne pas la restriction

    @Test
    void r5_statutsVisibles_sansStatutNeMontreRien() throws Exception {
        when(statutsVisibles.pourUtilisateurCourant())
                .thenReturn(Optional.of(Set.of(statuts.statut(StatutDossier.RESERVATION).getId())));

        assertThat(ids(mockMvc, SESSION_DU_TEST, avec(SANS_STATUT, VRAI))).isEmpty();
        assertThat(ids(mockMvc, SESSION_DU_TEST, avec(SANS_STATUT, VRAI, "statut", "RESERVATION")))
                .containsExactlyInAnyOrder(dA.getId().toString(), dB.getId().toString());
    }
}

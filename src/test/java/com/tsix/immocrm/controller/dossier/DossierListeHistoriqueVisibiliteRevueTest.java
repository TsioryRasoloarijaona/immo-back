package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.SESSION_DU_TEST;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.ids;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.totalListe;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.verifierCoherence;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue 2 du lot H (règle 6) : le OU statut / sans statut et la société restent sous le périmètre. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeHistoriqueVisibiliteRevueTest {

    private static final String SANS_STATUT = "sansStatut";
    private static final String STATUT = "statut";
    private static final String SOCIETE = "societeId";
    private static final String VRAI = "true";
    private static final String LIGNES = "lignes";
    private static final String AVEC_LIBRES = "DOSSIERS_ET_LIBRES";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureEtatVentes fixtureEtat;
    @Autowired
    private FixtureStatutsDossier statuts;

    private String prefixe;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private ComFolder dA;
    private ComFolder dSansA;
    private ComFolder dB;
    private ComFolder dSansB;
    private ComFolder dInactif;
    private ComFolder dInactifSans;
    private RequestPostProcessor restreint;

    /** Commercial affecté au seul projet A ; un lot libre par tranche A / B (article d'un dossier annulé, détaché). */
    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 6);
        prefixe = "HVR" + jeton;
        trancheA = fixture.tranche("TR-RA-" + jeton, true);
        trancheB = fixture.tranche("TR-RB-" + jeton, true);
        CfgTranche trancheInactive = fixture.tranche("TR-RI-" + jeton, false);
        dA = dossier("A", trancheA, StatutDossier.RESERVATION);
        dSansA = dossier("B", trancheA, null);
        dB = dossier("C", trancheB, StatutDossier.RESERVATION);
        dSansB = dossier("D", trancheB, null);
        dInactif = dossier("E", trancheInactive, StatutDossier.RESERVATION);
        dInactifSans = dossier("F", trancheInactive, null);
        libre("LA", trancheA);
        libre("LB", trancheB);

        AdmUser commercial = fixture.compte("hvr.com." + jeton);
        fixtureEtat.affecter(commercial, trancheA.getCfgProject());
        fixtureEtat.relire();
        restreint = FixtureDossierVente.connecte(commercial, "Commercial");
    }

    private ComFolder dossier(String suffixe, CfgTranche tranche, StatutDossier statut) {
        ComFolder f = fixture.dossier(prefixe + suffixe, tranche, null);
        f.setTotalToPay(new BigDecimal("500"));
        f.setStrStatus(statut == null ? null : statuts.statut(statut));
        return f;
    }

    private void libre(String suffixe, CfgTranche tranche) {
        ComFolder annule = dossier(suffixe, tranche, StatutDossier.ANNULATION);
        annule.getStkItem().setComFolderId(null);
    }

    private String societe(CfgTranche tranche) {
        return tranche.getCfgProject().getCfgCompany().getId().toString();
    }

    private String[] avec(String... paires) {
        String[] tout = new String[paires.length + 4];
        tout[0] = "search";
        tout[1] = prefixe;
        tout[2] = "inclureSortis";
        tout[3] = VRAI;
        System.arraycopy(paires, 0, tout, 4, paires.length);
        return tout;
    }

    @Test
    void ouStatutSansStatut_commercialRestreint_resteDansSonPerimetre_surLesCinqRoutes() throws Exception {
        assertThat(ids(mockMvc, restreint, avec(STATUT, "RESERVATION", SANS_STATUT, VRAI)))
                .containsExactlyInAnyOrder(dA.getId().toString(), dSansA.getId().toString());
        verifierCoherence(mockMvc, restreint, 2, avec(STATUT, "RESERVATION", SANS_STATUT, VRAI));
    }

    @Test
    void ouStatutSansStatut_entiteInactive_absente_surLesCinqRoutes() throws Exception {
        assertThat(ids(mockMvc, SESSION_DU_TEST, avec(STATUT, "RESERVATION", SANS_STATUT, VRAI)))
                .doesNotContain(dInactif.getId().toString(), dInactifSans.getId().toString())
                .containsExactlyInAnyOrder(dA.getId().toString(), dSansA.getId().toString(),
                        dB.getId().toString(), dSansB.getId().toString());
        verifierCoherence(mockMvc, SESSION_DU_TEST, 4, avec(STATUT, "RESERVATION", SANS_STATUT, VRAI));
    }

    @Test
    void societeHorsPerimetre_avecLotsLibres_listeVide() throws Exception {
        assertThat(totalListe(mockMvc, restreint, avec(LIGNES, AVEC_LIBRES, SOCIETE, societe(trancheB)))).isZero();
        assertThat(totalListe(mockMvc, restreint, avec(LIGNES, AVEC_LIBRES, SOCIETE, societe(trancheB),
                SANS_STATUT, VRAI))).isZero();
        // Témoin : sa propre société rend ses deux dossiers non sortis, l'annulé de A et le lot libre de A.
        assertThat(totalListe(mockMvc, restreint, avec(LIGNES, AVEC_LIBRES, SOCIETE, societe(trancheA))))
                .isEqualTo(4);
    }
}

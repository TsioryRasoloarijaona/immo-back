package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.LISTE;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.SESSION_DU_TEST;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.ids;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.requete;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.verifierCoherence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lot H (FEATURE-tdb-vente-historique-ventes.md §3 et §5) : filtres totalMin, totalMax, societeId, sansStatut. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeHistoriqueTest {

    private static final String TOTAL_MIN = "totalMin";
    private static final String TOTAL_MAX = "totalMax";
    private static final String SOCIETE = "societeId";
    private static final String SANS_STATUT = "sansStatut";
    private static final String STATUT = "statut";
    private static final String SORTIS = "inclureSortis";
    private static final String VRAI = "true";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureStatutsDossier statuts;
    @Autowired
    private EntityManager em;

    private String prefixe;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private ComFolder d100;
    private ComFolder d200;
    private ComFolder d300;
    private ComFolder dNull;
    private ComFolder d0;
    private ComFolder dSans;
    private ComFolder dB;

    /** Société A : 100 réservé, 200 concrétisé, 300 annulé, NULL concrétisé, 0 désisté, 150 sans statut. Société B : 250. */
    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 6);
        prefixe = "HIST" + jeton;
        trancheA = fixture.tranche("TR-HA-" + jeton, true);
        trancheB = fixture.tranche("TR-HB-" + jeton, true);
        d100 = dossier("A", trancheA, "100", StatutDossier.RESERVATION);
        d200 = dossier("B", trancheA, "200", StatutDossier.CONCRETISATION);
        d300 = dossier("C", trancheA, "300", StatutDossier.ANNULATION);
        dNull = dossier("D", trancheA, null, StatutDossier.CONCRETISATION);
        d0 = dossier("E", trancheA, "0", StatutDossier.DESISTEMENT);
        dSans = dossier("F", trancheA, "150", null);
        dB = dossier("G", trancheB, "250", StatutDossier.RESERVATION);
        em.flush();
        em.clear();
    }

    private ComFolder dossier(String suffixe, CfgTranche tranche, String total, StatutDossier statut) {
        ComFolder f = fixture.dossier(prefixe + suffixe, tranche, null);
        f.setTotalToPay(total == null ? null : new BigDecimal(total));
        f.setStrStatus(statut == null ? null : statuts.statut(statut));
        return f;
    }

    private String[] avec(String... paires) {
        return Stream.concat(Stream.of("search", prefixe), Arrays.stream(paires)).toArray(String[]::new);
    }

    private void attendre(ComFolder[] attendus, String... paires) throws Exception {
        String[] attendusIds = Arrays.stream(attendus).map(f -> f.getId().toString()).toArray(String[]::new);
        assertThat(ids(mockMvc, SESSION_DU_TEST, avec(paires))).containsExactlyInAnyOrder(attendusIds);
    }

    private String societe(CfgTranche tranche) {
        return tranche.getCfgProject().getCfgCompany().getId().toString();
    }

    private static ComFolder[] ensemble(ComFolder... dossiers) {
        return dossiers;
    }

    // ---- R1 : fourchette sur TotalToPay

    @Test
    void r1_totalMin_seul_borneIncluse_exclutLeNull() throws Exception {
        attendre(ensemble(d200, d300, dB), SORTIS, VRAI, TOTAL_MIN, "200");
    }

    @Test
    void r1_totalMax_seul_borneIncluse_exclutLeNull() throws Exception {
        attendre(ensemble(d100, d0), SORTIS, VRAI, TOTAL_MAX, "100");
    }

    @Test
    void r1_totalMinEtMax_bornesIncluses() throws Exception {
        attendre(ensemble(d100, d200, dSans), SORTIS, VRAI, TOTAL_MIN, "100", TOTAL_MAX, "200");
        attendre(ensemble(d300), SORTIS, VRAI, TOTAL_MIN, "300", TOTAL_MAX, "300");
    }

    @Test
    void r1_totalMinZero_inclutLesZerosMaisPasLesNull() throws Exception {
        attendre(ensemble(d100, d200, d300, d0, dSans, dB), SORTIS, VRAI, TOTAL_MIN, "0");
    }

    @Test
    void r1_totalMaxGrand_exclutQuandMemeLesNull() throws Exception {
        attendre(ensemble(d100, d200, d300, d0, dSans, dB), SORTIS, VRAI, TOTAL_MAX, "99999999");
    }

    @Test
    void r1_totalMinSuperieurAuTotalMax_400_nommeLesChamps() throws Exception {
        mockMvc.perform(requete(LISTE, SESSION_DU_TEST, avec(TOTAL_MIN, "500", TOTAL_MAX, "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(TOTAL_MIN)))
                .andExpect(jsonPath("$.detail", containsString(TOTAL_MAX)));
        mockMvc.perform(requete(LISTE + "/totaux", SESSION_DU_TEST, avec(TOTAL_MIN, "500", TOTAL_MAX, "100")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r1_bornesMalFormeesOuNegatives_400() throws Exception {
        mockMvc.perform(requete(LISTE, SESSION_DU_TEST, avec(TOTAL_MIN, "abc")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(requete(LISTE, SESSION_DU_TEST, avec(TOTAL_MAX, "xyz")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(requete(LISTE, SESSION_DU_TEST, avec(TOTAL_MIN, "-1")))
                .andExpect(status().isBadRequest());
    }

    // ---- R2 : société

    @Test
    void r2_societeId_neGardeQueLesDossiersDeCetteSociete() throws Exception {
        attendre(ensemble(d100, d200, d300, dNull, d0, dSans), SORTIS, VRAI, SOCIETE, societe(trancheA));
        attendre(ensemble(dB), SORTIS, VRAI, SOCIETE, societe(trancheB));
    }

    @Test
    void r2_societeInconnue_listeVide_pas404() throws Exception {
        attendre(ensemble(), SORTIS, VRAI, SOCIETE, UUID.randomUUID().toString());
    }

    @Test
    void r2_societeEtTotalMin_secumulent() throws Exception {
        attendre(ensemble(d200, d300), SORTIS, VRAI, SOCIETE, societe(trancheA), TOTAL_MIN, "200");
    }

    // ---- R3 : sans statut

    @Test
    void r3_sansStatut_seul_neRendQueLesDossiersSansStatut() throws Exception {
        attendre(ensemble(dSans), SANS_STATUT, VRAI);
    }

    @Test
    void r3_sansStatutFaux_neChangeRien() throws Exception {
        attendre(ensemble(d100, d200, d300, dNull, d0, dSans, dB), SORTIS, VRAI, SANS_STATUT, "false");
    }

    @Test
    void r3_statutPlusSansStatut_ajouteLesDossiersSansStatut() throws Exception {
        attendre(ensemble(d100, dB, dSans), STATUT, "RESERVATION", SANS_STATUT, VRAI);
        attendre(ensemble(d100, dB, dSans), SORTIS, VRAI, STATUT, "RESERVATION", SANS_STATUT, VRAI);
    }

    @Test
    void r3_statutAnnulationPlusSansStatut_garderLesAnnules() throws Exception {
        attendre(ensemble(d300, dSans), STATUT, "ANNULATION", SANS_STATUT, VRAI);
    }

    @Test
    void r3_inclureSortis_sansFiltreStatut_gardeAnnulesEtResilies() throws Exception {
        attendre(ensemble(d100, d200, d300, dNull, d0, dSans, dB), SORTIS, VRAI);
    }

    // ---- R4 : liste = groupes = totaux = export

    @Test
    void r4_coherence_sansLesNouveauxFiltres_sortisInclus() throws Exception {
        verifierCoherence(mockMvc, SESSION_DU_TEST, 7, avec(SORTIS, VRAI));
    }

    @Test
    void r4_coherence_borneMax() throws Exception {
        verifierCoherence(mockMvc, SESSION_DU_TEST, 2, avec(SORTIS, VRAI, TOTAL_MAX, "100"));
    }

    @Test
    void r4_coherence_tousLesNouveauxFiltresEnsemble() throws Exception {
        verifierCoherence(mockMvc, SESSION_DU_TEST, 3, avec(SORTIS, VRAI, SOCIETE, societe(trancheA),
                TOTAL_MIN, "100", TOTAL_MAX, "300", STATUT, "RESERVATION", STATUT, "CONCRETISATION",
                SANS_STATUT, VRAI));
    }

    @Test
    void r4_coherence_sansStatutSeul() throws Exception {
        verifierCoherence(mockMvc, SESSION_DU_TEST, 1, avec(SANS_STATUT, VRAI));
    }

    // ---- R6 : montant lu tel quel

    @Test
    void r6_totalAPayer_dansLaListeFiltree_egaleLeTotalDuDossier() throws Exception {
        String json = HistoriqueVentesSupport.corps(mockMvc, requete(LISTE, SESSION_DU_TEST,
                avec(SORTIS, VRAI, TOTAL_MIN, "250", TOTAL_MAX, "250", "colonnes", "code,totalAPayer")));
        assertThat(((Number) com.jayway.jsonpath.JsonPath.read(json, "$.totalElements")).intValue()).isEqualTo(1);
        assertThat(new BigDecimal(com.jayway.jsonpath.JsonPath.read(json, "$.content[0].totalAPayer").toString()))
                .isEqualByComparingTo("250");
    }

    // ---- R7 : pas de requête par ligne

    @Test
    void r7_listeFiltree_nAugmentePasLeNombreDeRequetesAvecLesLignes() throws Exception {
        String jeton = UUID.randomUUID().toString().substring(0, 6);
        for (int i = 0; i < 20; i++) {
            ComFolder f = fixture.dossier("HR7" + jeton + "-" + i, trancheA, null);
            f.setTotalToPay(new BigDecimal("100"));
            f.setStrStatus(statuts.statut(StatutDossier.RESERVATION));
        }
        long pourDeux = compterRequetes("HR7" + jeton, 2);
        long pourVingt = compterRequetes("HR7" + jeton, 20);

        assertThat(pourVingt).as("2 lignes : %d requêtes, 20 lignes : %d", pourDeux, pourVingt).isEqualTo(pourDeux);
    }

    private long compterRequetes(String marqueur, int taille) throws Exception {
        org.hibernate.stat.Statistics stats = em.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        em.flush();
        em.clear();
        stats.clear();
        mockMvc.perform(requete(LISTE, SESSION_DU_TEST, "search", marqueur, TOTAL_MIN, "1", SORTIS, VRAI,
                        SOCIETE, societe(trancheA), "size", String.valueOf(taille)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(taille));
        return stats.getPrepareStatementCount();
    }
}

package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/dossiers/totaux-colonnes} — voir {@code instructions/FEATURE-totaux-listes.md}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierTotauxColonnesTest {

    private static final String URL_TOTAUX = "/api/dossiers/totaux-colonnes";
    private static final String URL_LISTE = "/api/dossiers";
    private static final String AGREGATS = "agregats";
    private static final String SEARCH = "search";
    private static final String LIGNES = "lignes";
    private static final String CHAMP_DATE = "champDate";
    private static final String FICHE_BANCAIRE = "FICHE_BANCAIRE";
    private static final String NOMBRE_TOTAL = "$.nombreTotal";
    private static final String TOTAL_ELEMENTS = "$.totalElements";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureStatutsDossier statuts;
    @Autowired
    private FixtureReferentielStk referentielStk;
    @Autowired
    private EntityManager em;

    private CfgTranche tranche;
    private String prefixe;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TR-TCD-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "TCD" + UUID.randomUUID().toString().substring(0, 6);
    }

    private ComFolder dossier(String suffixe, StatutDossier statut, BigDecimal prix) {
        ComFolder f = fixture.dossier(prefixe + suffixe, tranche, null);
        f.setStrStatus(statuts.statut(statut));
        f.setPrice(prix);
        f.setDocDate(LocalDateTime.of(2026, 9, 16, 8, 30, 15));
        return f;
    }

    /** Article sans dossier (lot libre), référentiels repris de l'article d'un dossier de la fixture. */
    private StkItem lotLibre(String code, StkItem modele) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setStkItemType(modele.getStkItemType());
        article.setStkItemCategory(modele.getStkItemCategory());
        article.setStkVocation(modele.getStkVocation());
        article.setStkHierarchy(tranche);
        article.setStkKind(referentielStk.natureParDefaut());
        article.setArea(new BigDecimal("80"));
        article.setComSalePrice(new BigDecimal("950000"));
        em.persist(article);
        return article;
    }

    private void relire() {
        em.flush();
        em.clear();
    }

    private long totalElementsDeLaListe(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, TOTAL_ELEMENTS)).longValue();
    }

    private long nombreTotalDesTotaux(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, NOMBRE_TOTAL)).longValue();
    }

    // ------------------------------------------------------------------ R1 — mêmes lignes que la liste

    @Test
    void r1_nombreTotal_egaleLeTotalElementsDeLaListe_avecFiltreStatut() throws Exception {
        dossier("A", StatutDossier.RESERVATION, new BigDecimal("100.00"));
        dossier("B", StatutDossier.RESERVATION, new BigDecimal("200.00"));
        dossier("C", StatutDossier.CONCRETISATION, new BigDecimal("400.00"));
        relire();

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(SEARCH, prefixe).param("statut", "RESERVATION"));
        assertThat(liste).isEqualTo(2L);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe).param("statut", "RESERVATION")
                        .param(AGREGATS, "prix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(liste))
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux.prix").value(300.0));
    }

    /** R1 : les dossiers sortis sont exclus par défaut, réintégrés par {@code inclureSortis}, comme la liste. */
    @Test
    void r1_sortis_suiventInclureSortisCommeLaListe() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION, new BigDecimal("100.00"));
        dossier("B", StatutDossier.ANNULATION, new BigDecimal("999.00"));
        relire();

        long listeDefaut = totalElementsDeLaListe(get(URL_LISTE).param(SEARCH, prefixe));
        long listeAvecSortis = totalElementsDeLaListe(avecSortis(URL_LISTE));
        assertThat(new long[] {listeDefaut, listeAvecSortis}).containsExactly(1L, 2L);
        long totauxDefaut = nombreTotalDesTotaux(get(URL_TOTAUX).param(SEARCH, prefixe));
        long totauxAvecSortis = nombreTotalDesTotaux(avecSortis(URL_TOTAUX));

        assertThat(totauxDefaut).isEqualTo(listeDefaut).isEqualTo(1L);
        assertThat(totauxAvecSortis).isEqualTo(listeAvecSortis).isEqualTo(2L);
    }

    private MockHttpServletRequestBuilder avecSortis(String url) {
        return get(url).param(SEARCH, prefixe).param("inclureSortis", "true");
    }

    /** R1 : {@code champDate} + période, et {@code dateVide}, comme la liste. */
    @Test
    void r1_champDateEtDateVide_commeLaListe() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION, null).setBankRecordDate(LocalDateTime.of(2025, 3, 15, 0, 0));
        dossier("B", StatutDossier.CONCRETISATION, null).setBankRecordDate(LocalDateTime.of(2025, 5, 15, 0, 0));
        dossier("C", StatutDossier.CONCRETISATION, null);
        relire();

        long listePeriode = totalElementsDeLaListe(surMars2025(URL_LISTE));
        long listeVide = totalElementsDeLaListe(sansFicheBancaire(URL_LISTE));
        assertThat(new long[] {listePeriode, listeVide}).containsExactly(1L, 1L);
        long totauxPeriode = nombreTotalDesTotaux(surMars2025(URL_TOTAUX));
        long totauxVide = nombreTotalDesTotaux(sansFicheBancaire(URL_TOTAUX));

        assertThat(totauxPeriode).isEqualTo(listePeriode).isEqualTo(1L);
        assertThat(totauxVide).isEqualTo(listeVide).isEqualTo(1L);
    }

    private MockHttpServletRequestBuilder surMars2025(String url) {
        return get(url).param(SEARCH, prefixe).param(CHAMP_DATE, FICHE_BANCAIRE)
                .param("dateDebut", "2025-03-01T00:00:00").param("dateFin", "2025-03-31T23:59:59");
    }

    private MockHttpServletRequestBuilder sansFicheBancaire(String url) {
        return get(url).param(SEARCH, prefixe).param(CHAMP_DATE, FICHE_BANCAIRE).param("dateVide", "true");
    }

    @Test
    void r1_nombreTotal_egaleLeTotalElementsDeLaListe_sansFiltre() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION, null);
        relire();

        long liste = totalElementsDeLaListe(get(URL_LISTE));
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX));

        assertThat(totaux).isEqualTo(liste).isPositive();
    }

    @Test
    void sansAgregats_seulLeNombreTotal() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION, BigDecimal.TEN);
        dossier("B", StatutDossier.CONCRETISATION, BigDecimal.ONE);
        relire();

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux").isEmpty());
    }

    // ------------------------------------------------------------------ R3 — toutes les lignes, pas une page

    @Test
    void r3_agregats_portentSurToutesLesLignes_pasSurLaPage() throws Exception {
        for (int i = 1; i <= 12; i++) {
            dossier("-%02d".formatted(i), StatutDossier.CONCRETISATION, BigDecimal.valueOf(i));
        }
        relire();

        mockMvc.perform(get(URL_LISTE).param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(12));

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe)
                        .param(AGREGATS, "prix", "prix:AVG", "prix:MIN", "prix:MAX", "code:COUNT",
                                "dateDocument:MIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(12))
                .andExpect(jsonPath("$.totaux.prix").value(78.0))
                .andExpect(jsonPath("$.totaux['prix:AVG']").value(6.5))
                .andExpect(jsonPath("$.totaux['prix:MIN']").value(1.0))
                .andExpect(jsonPath("$.totaux['prix:MAX']").value(12.0))
                .andExpect(jsonPath("$.totaux['code:COUNT']").value(12))
                .andExpect(jsonPath("$.totaux['dateDocument:MIN']").value(Matchers.startsWith("2026-09-16")));
    }

    @Test
    void extremumEtMoyenne_surColonneVide_rendentNull() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION, null);
        dossier("B", StatutDossier.CONCRETISATION, null);
        relire();

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe)
                        .param(AGREGATS, "montantAvance:MIN", "montantAvance:MAX", "montantAvance:AVG",
                                "montantAvance:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux['montantAvance:MIN']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['montantAvance:MAX']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['montantAvance:AVG']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['montantAvance:COUNT']").value(0));
    }

    // ------------------------------------------------------------------ R6 — lignes : les lots libres n'entrent pas

    @Test
    void r6_dossiersEtLibres_neCompteQueLesDossiers() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION, new BigDecimal("120000.00"));
        lotLibre(prefixe + "-LIBRE", f.getStkItem());
        relire();

        mockMvc.perform(get(URL_LISTE).param(SEARCH, prefixe).param(LIGNES, "DOSSIERS_ET_LIBRES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(2));

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe).param(LIGNES, "DOSSIERS_ET_LIBRES")
                        .param(AGREGATS, "prix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(1))
                .andExpect(jsonPath("$.totaux.prix").value(120000.0));
    }

    @Test
    void r6_libres_rendNombreTotalZero() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION, new BigDecimal("120000.00"));
        lotLibre(prefixe + "-LIBRE", f.getStkItem());
        relire();

        mockMvc.perform(get(URL_LISTE).param(SEARCH, prefixe).param(LIGNES, "LIBRES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(1));

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe).param(LIGNES, "LIBRES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(0));
    }

    /** R1/R6 : en {@code lignes=DOSSIERS} (explicite), l'invariant avec la liste tient. */
    @Test
    void r6_lignesDossiers_egaleLaListe() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION, null);
        lotLibre(prefixe + "-LIBRE", f.getStkItem());
        relire();

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(SEARCH, prefixe).param(LIGNES, "DOSSIERS"));
        assertThat(liste).isEqualTo(1L);
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX).param(SEARCH, prefixe).param(LIGNES, "DOSSIERS"));

        assertThat(totaux).isEqualTo(liste).isEqualTo(1L);
    }

    // ------------------------------------------------------------------ R4 / R5 — jetons refusés

    @Test
    void r4_sommeSurUneDate_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "dateDocument:SUM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r4_nombreSurUnTexte_rend200() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION, null);
        relire();

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, prefixe).param(AGREGATS, "statut:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux['statut:COUNT']").value(1));
    }

    @Test
    void jetonInconnu_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "prix:MEDIANE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fonctionsValides", Matchers.hasItems("SUM", "AVG", "MIN", "MAX", "COUNT")));
    }
}

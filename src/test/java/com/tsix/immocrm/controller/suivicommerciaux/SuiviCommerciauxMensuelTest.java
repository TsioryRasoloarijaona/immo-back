package com.tsix.immocrm.controller.suivicommerciaux;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.cle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/suivi-commerciaux/mensuel} — FEATURE-suivi-commerciaux.md R11, §7.5. Mois = mars 2025, année = 2025,
 * filtré sur une tranche et deux commerciaux propres au test :
 * <pre>
 *   A : 4 prospections en mars (+1 en juin, +1 sur une autre tranche) ; 2 visites de prospection en mars, 1 visite
 *       de vente en juin, toutes sans article ; vente V1 accord 10/03 (saisie janv., issue d'une prospection,
 *       1 000 000) ; vente V2 accord 07/2025 (saisie mars, 500 000) ; versements sur V1 : 250 000 crédit comptabilisé
 *       daté par DocDate (mars), 30 000 crédit comptabilisé à échéance août, 100 000 au débit, 70 000 non comptabilisé
 *   B : aucune prospection, visite ni vente en 2025 ; un versement de 50 000 en mars sur une vente accordée en 2024
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxMensuelTest {

    private static final String URL = "/api/suivi-commerciaux/mensuel";
    private static final int ANNEE = 2025;
    private static final int MARS = 3;
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String MOIS = "mois";
    private static final String BLOC_ANNEE = "annee";
    private static final String NB_PROSPECTIONS = "nbProspections";
    private static final String NB_VENTES = "nbVentes";
    private static final String VERSEMENTS = "versements";
    private static final String PRIX_ARTICLES = "prixArticles";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviCommerciaux fixture;

    private String prefixe;
    private CfgTranche tranche;
    private AdmUser commercialA;
    private AdmUser commercialB;
    private AdmUser admin;

    private static LocalDateTime le(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour).atTime(10, 0);
    }

    @BeforeEach
    void setUp() {
        prefixe = FixtureSuiviCommerciaux.prefixe("SCM");
        tranche = fixture.tranche("TR-" + prefixe, true);
        CfgTranche autreTranche = fixture.tranche("TRX-" + prefixe, true);
        commercialA = fixture.compte("scm.a." + prefixe, "Commercial");
        commercialB = fixture.compte("scm.b." + prefixe, "Commercial");
        admin = fixture.compte("scm.adm." + prefixe, ADMINISTRATEUR);
        ComActionMessageType visite = fixture.visite();

        ComProspection origine = fixture.prospection(tranche, fixture.personne(prefixe + "-P0"), commercialA,
                le(MARS, 5));
        for (int i = 1; i < 4; i++) {
            fixture.prospection(tranche, fixture.personne(prefixe + "-P" + i), commercialA, le(MARS, 5 + i));
        }
        fixture.prospection(tranche, fixture.personne(prefixe + "-PJ"), commercialA, le(6, 2));
        fixture.prospection(autreTranche, fixture.personne(prefixe + "-PX"), commercialA, le(MARS, 6));

        fixture.interaction(commercialA, origine, visite, null, le(MARS, 12));
        fixture.interaction(commercialA, origine, visite, null, le(MARS, 13));

        ComFolder v1 = fixture.vente(prefixe + "-V1", commercialA, tranche, origine, le(MARS, 10), le(1, 15),
                new BigDecimal("1000000"));
        fixture.vente(prefixe + "-V2", commercialA, tranche, null, le(7, 1), le(MARS, 20), new BigDecimal("500000"));
        fixture.visiteVente(commercialA, v1, visite, FixtureSuiviCommerciaux.CATEGORIE_VENTE_2222, le(6, 10));

        fixture.versement(v1, "250000", true, true, null, le(MARS, 20));
        fixture.versement(v1, "30000", true, true, le(8, 5), le(MARS, 21));
        fixture.versement(v1, "100000", false, true, null, le(MARS, 22));
        fixture.versement(v1, "70000", true, false, null, le(MARS, 23));

        ComFolder ancienne = fixture.vente(prefixe + "-VB", commercialB, tranche, null,
                LocalDate.of(2024, 11, 5).atTime(10, 0), LocalDate.of(2024, 11, 1).atTime(10, 0),
                new BigDecimal("800000"));
        fixture.versement(ancienne, "50000", true, true, null, le(MARS, 25));
        fixture.relire();
    }

    private String mensuel() throws Exception {
        return mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)).param("mois", String.valueOf(MARS))
                        .param("trancheIds", tranche.getId().toString())
                        .param("commercialIds", cle(commercialA), cle(commercialB))
                        .with(connecte(admin, ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static Map<String, Object> ligne(String corps, String bloc, AdmUser compte) {
        List<Map<String, Object>> lignes = JsonPath.read(corps,
                "$." + bloc + ".lignes[?(@.commercialCle=='" + cle(compte) + "')]");
        assertThat(lignes).as("ligne %s de %s", bloc, compte.getCode()).hasSize(1);
        return lignes.getFirst();
    }

    private static Map<String, Object> bloc(String corps, String bloc, String sousBloc) {
        return JsonPath.read(corps, "$." + bloc + "." + sousBloc);
    }

    /** Valeurs numériques des champs, dans l'ordre ; un champ absent ou nul échoue. */
    private static double[] nombres(Map<String, Object> ligne, String... champs) {
        double[] valeurs = new double[champs.length];
        for (int i = 0; i < champs.length; i++) {
            assertThat(ligne).as(champs[i]).containsKey(champs[i]);
            Object valeur = ligne.get(champs[i]);
            assertThat(valeur).as(champs[i]).isInstanceOf(Number.class);
            valeurs[i] = ((Number) valeur).doubleValue();
        }
        return valeurs;
    }

    // ------------------------------------------------------------------ compteurs du mois

    @Test
    void r11_mois_prospectionsDuMoisSurLesTranchesFiltrees() throws Exception {
        assertThat(nombres(ligne(mensuel(), MOIS, commercialA), NB_PROSPECTIONS)).containsExactly(4);
    }

    @Test
    void r11_mois_visitesSansArticle_prospectionEtVenteSeparees() throws Exception {
        assertThat(nombres(ligne(mensuel(), MOIS, commercialA), "nbVisitesProspection", "nbVisitesVente"))
                .containsExactly(2, 0);
    }

    /** Vente datée par {@code AgreementDate} : V1 (accord en mars, saisie en janvier) oui, V2 (saisie en mars) non. */
    @Test
    void r11_mois_ventesSurDateDAccord() throws Exception {
        assertThat(nombres(ligne(mensuel(), MOIS, commercialA), NB_VENTES, "nbVentesIssuesProspection", PRIX_ARTICLES))
                .containsExactly(1, 1, 1_000_000);
    }

    /** Règle 6 : seules les lignes au crédit et comptabilisées, datées par {@code ISNULL(EcheanceDate, DocDate)}. */
    @Test
    void r11_mois_versementsCreditComptabilisesSeulement() throws Exception {
        assertThat(nombres(ligne(mensuel(), MOIS, commercialA), VERSEMENTS)).containsExactly(250_000);
    }

    @Test
    void r11_mois_ratiosOmran() throws Exception {
        assertThat(nombres(ligne(mensuel(), MOIS, commercialA),
                "perfVente", "perfVisite", "perfProspection", "perfRecouvrement"))
                .containsExactly(new double[] {0.25, 0.5, 0.25, 0.25}, within(1e-9));
    }

    @Test
    void r11_divisionParZero_ratioNull() throws Exception {
        Map<String, Object> b = ligne(mensuel(), MOIS, commercialB);

        assertThat(nombres(b, NB_PROSPECTIONS, NB_VENTES, VERSEMENTS)).containsExactly(0, 0, 50_000);
        assertThat(b).extractingByKeys("perfVente", "perfVisite", "perfProspection", "perfRecouvrement")
                .containsOnlyNulls();
    }

    // ------------------------------------------------------------------ bloc année

    @Test
    void r11_annee_compteursDeLAnnee() throws Exception {
        assertThat(nombres(ligne(mensuel(), BLOC_ANNEE, commercialA), NB_PROSPECTIONS, "nbVisitesProspection",
                "nbVisitesVente", NB_VENTES, "nbVentesIssuesProspection", PRIX_ARTICLES, VERSEMENTS))
                .containsExactly(5, 2, 1, 2, 1, 1_500_000, 280_000);
    }

    // ------------------------------------------------------------------ Total / Moyenne

    @Test
    void r11_total_sommeDesLignes() throws Exception {
        String corps = mensuel();

        assertThat(JsonPath.<List<Object>>read(corps, "$.mois.lignes")).hasSize(2);
        assertThat(nombres(bloc(corps, MOIS, "total"), NB_PROSPECTIONS, NB_VENTES, VERSEMENTS))
                .containsExactly(4, 1, 300_000);
        assertThat(nombres(bloc(corps, BLOC_ANNEE, "total"), NB_PROSPECTIONS, NB_VENTES, VERSEMENTS))
                .containsExactly(5, 2, 330_000);
    }

    @Test
    void r11_moyenne_parLigne() throws Exception {
        assertThat(nombres(bloc(mensuel(), MOIS, "moyenne"), NB_PROSPECTIONS, VERSEMENTS))
                .containsExactly(new double[] {2, 150_000}, within(1e-6));
    }
}

package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_MOIS;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.encaissementsReel;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Alignement du n° 5 avec {@code GET /api/dashboard/annuel} (FEATURE-tdb-vente-rapports-flux.md §4.1, §12.3 test 3) : mêmes
 * encaissements sans décaissement ni {@code IsCredit} NULL ; sinon l'écart vaut exactement leur effet et le dashboard ne change
 * pas. Tranche T, année 2025, dossier MIN concrétisé ; encaissements 1 000 (03) et 2 000 (04).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiviteParMoisDashboardTest {

    private static final int ANNEE = 2025;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    private AppelsRapports appels;
    private RequestPostProcessor administrateur;
    private Jeu jeu;
    private CfgTranche tranche;
    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        jeu = fixture.jeu("DSH");
        administrateur = connecte(rapports.compte("dsh." + jeu.prefixe(),
                rapports.role("Libre " + jeu.prefixe(), false)), "Administrateur");
        tranche = jeu.tranche("T", jeu.projet("P", true));
        dossier = jeu.dater(jeu.dossier(tranche, jeu.statuts().minute()), LocalDate.of(ANNEE, 1, 10),
                LocalDate.of(ANNEE, 2, 10), null, null);
        jeu.encaisser(dossier, "1000", LocalDate.of(ANNEE, 3, 3));
        jeu.encaisser(dossier, "2000", LocalDate.of(ANNEE, 4, 4));
    }

    private BigDecimal encaissementsN5() throws Exception {
        fixture.relire();
        Map<String, Object> mensuel = section(appels.sections(demande(ACTIVITE_PAR_MOIS, administrateur, ANNEE,
                tranche)), "mensuel");
        return nombre(totaux(mensuel).get(encaissementsReel(mensuel)));
    }

    private BigDecimal encaissementsDashboard() throws Exception {
        fixture.relire();
        String corps = appels.corps(get("/api/dashboard/annuel").param("annee", String.valueOf(ANNEE))
                .param("trancheId", tranche.getId().toString()).with(administrateur));
        return nombre(JsonPath.read(corps, "$.encaissements.montantTotal"));
    }

    @Test
    void dashboard_sansDecaissementNiIsCreditNull_memesEncaissementsQueLeN5() throws Exception {
        BigDecimal dashboard = encaissementsDashboard();

        assertThat(encaissementsN5()).isEqualByComparingTo(dashboard).isEqualByComparingTo("3000");
    }

    @Test
    void dashboard_decaissementEtIsCreditNull_ecartEgalLeurEffet_dashboardInchange() throws Exception {
        ComDocument echeance = jeu.echeance(dossier, jeu.categorie(Genre.AUTOFINANCEMENT, true), new BigDecimal("340"),
                LocalDate.of(ANNEE, 5, 5));
        jeu.regler(echeance, new BigDecimal("300"), false, IN_WORK, null, LocalDate.of(ANNEE, 5, 5));
        jeu.regler(echeance, new BigDecimal("40"), null, null, null, LocalDate.of(ANNEE, 5, 6));

        BigDecimal dashboard = encaissementsDashboard();
        BigDecimal n5 = encaissementsN5();

        assertThat(dashboard).as("le dashboard ne somme que les crédits").isEqualByComparingTo("3000");
        assertThat(n5.subtract(dashboard)).as("−300 de décaissement + 40 d'IsCredit NULL").isEqualByComparingTo("-260");
    }
}

package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureHistorique;
import com.tsix.immocrm.fixtures.FixtureHistorique.Ligne;
import com.tsix.immocrm.web.export.LignesExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsHistorique.ACTION;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.EXPORT;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.TEXTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.UTILISATEUR;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.periode;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureHistorique.AJOUT;
import static com.tsix.immocrm.fixtures.FixtureHistorique.MODIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Export : règle 10, mêmes filtres que la liste (les refus d'accès sont dans {@link HistoriqueSecuriteTest}). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoriqueExportTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final LocalDate JOUR = LocalDate.of(2012, 3, 10);
    private static final int PLAFOND = LignesExport.LIGNES_EXPORT_MAX;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureHistorique fixture;
    @Autowired private Clock clock;

    private final String jeton = FixtureHistorique.jeton();
    private final String retenue = jeton + "-RETENUE";
    private final String ecartee = jeton + "-ECARTEE";
    private AppelsHistorique appels;

    @BeforeEach
    void connecterAdministrateur() {
        appels = new AppelsHistorique(mockMvc, connecte(dossiers.compte("ADM-HIST", ADMINISTRATEUR), ADMINISTRATEUR));
    }

    private Map<String, String> jour(String... paires) {
        return periode(JOUR, JOUR, paires);
    }

    private Ligne ligneDuJour() {
        return Ligne.de(JOUR.atTime(10, 0), jeton);
    }

    private LocalDateTime ilYa(int jours) {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).minusDays(jours);
    }

    private void assertRetenueSeule(List<String> lignes) {
        assertThat(lignes).isNotEmpty()
                .anyMatch(ligne -> ligne.contains(retenue))
                .noneMatch(ligne -> ligne.contains(ecartee));
    }

    @Test
    void r10_reponse_classeurXlsxEnPieceJointe_avecLaLigneFiltree() throws Exception {
        fixture.ligne(ligneDuJour().avecLibelle(retenue));

        MockHttpServletResponse reponse = appels.export(jour(TEXTE, jeton)).getResponse();

        assertAll(
                () -> assertThat(reponse.getContentType()).startsWith(XLSX),
                () -> assertThat(reponse.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                        .startsWith("attachment").contains(".xlsx"),
                () -> assertThat(appels.lignesExportees(jour(TEXTE, jeton)))
                        .anyMatch(ligne -> ligne.contains(retenue)));
    }

    @Test
    void r10_sansDates_defaut30Jours() throws Exception {
        fixture.ligne(Ligne.de(ilYa(10), jeton).avecLibelle(retenue));
        fixture.ligne(Ligne.de(ilYa(40), jeton).avecLibelle(ecartee));

        assertRetenueSeule(appels.lignesExportees(Map.of(TEXTE, jeton)));
    }

    @Test
    void r10_filtreDate_dateFinIncluse() throws Exception {
        fixture.ligne(Ligne.de(JOUR.atTime(23, 59, 59), jeton).avecLibelle(retenue));
        fixture.ligne(Ligne.de(JOUR.plusDays(1).atStartOfDay(), jeton).avecLibelle(ecartee));

        assertRetenueSeule(appels.lignesExportees(jour(TEXTE, jeton)));
    }

    @Test
    void r10_filtreTexte() throws Exception {
        String sansRapport = "Sans rapport " + UUID.randomUUID();
        fixture.ligne(ligneDuJour().avecLibelle(retenue));
        fixture.ligne(ligneDuJour().avecType(FixtureHistorique.TYPE_NEUTRE).avecLibelle(sansRapport));

        assertThat(appels.lignesExportees(jour(TEXTE, jeton))).isNotEmpty()
                .anyMatch(ligne -> ligne.contains(retenue))
                .noneMatch(ligne -> ligne.contains(sansRapport));
    }

    @Test
    void r10_filtreUtilisateur() throws Exception {
        String nom = jeton + " Alice";
        fixture.ligne(ligneDuJour().avecUtilisateur(nom).avecLibelle(retenue));
        fixture.ligne(ligneDuJour().avecUtilisateur(nom + " Martin").avecLibelle(ecartee));

        assertRetenueSeule(appels.lignesExportees(jour(UTILISATEUR, nom)));
    }

    @Test
    void r10_filtreAction() throws Exception {
        fixture.ligne(ligneDuJour().avecAction(MODIFICATION).avecLibelle(retenue));
        fixture.ligne(ligneDuJour().avecAction(AJOUT).avecLibelle(ecartee));

        assertRetenueSeule(appels.lignesExportees(jour(TEXTE, jeton, ACTION, MODIFICATION)));
    }

    @Test
    void r10_memeValidation_actionHorsDesTrois_400() throws Exception {
        appels.appeler(EXPORT, jour(ACTION, "Viewed")).andExpect(status().isBadRequest());
    }

    @Test
    void r10_memeValidation_dateDebutApresDateFin_400() throws Exception {
        appels.appeler(EXPORT, periode(JOUR.plusDays(1), JOUR)).andExpect(status().isBadRequest());
    }

    @Test
    void r10_detailsEtDescription_nonExposes() throws Exception {
        String description = jeton + "-DESCRIPTION";
        String details = jeton + "-DETAILS";
        fixture.ligne(ligneDuJour().avecLibelle(retenue).avecDescription(description).avecDetails(details));

        assertThat(appels.lignesExportees(jour(TEXTE, jeton))).isNotEmpty()
                .anyMatch(ligne -> ligne.contains(retenue))
                .noneMatch(ligne -> ligne.contains(description) || ligne.contains(details));
    }

    @Test
    void r10_plafond_memeLimiteQueLesAutresExports_etPasLaTailleDePage() throws Exception {
        fixture.enMasse(PLAFOND + 3, JOUR.atTime(10, 0), retenue);

        List<String> lignes = appels.lignesExportees(jour(TEXTE, retenue));

        assertThat(lignes.stream().filter(ligne -> ligne.contains(retenue))).hasSize(PLAFOND);
    }
}

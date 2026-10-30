package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureHistorique;
import com.tsix.immocrm.fixtures.FixtureHistorique.Ligne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsHistorique.LISTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.PAGE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.SIZE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.TEXTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.periode;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Liste et utilisateurs : règles 1 et 9, contrat de la ligne. FEATURE-admin-historique.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoriqueListeTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final LocalDate JOUR = LocalDate.of(2012, 3, 10);
    private static final int AGE_DANS_LA_PERIODE = 10;
    private static final int AGE_HORS_PERIODE = 40;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureHistorique fixture;
    @Autowired private Clock clock;

    private final String jeton = FixtureHistorique.jeton();
    private AppelsHistorique appels;

    @BeforeEach
    void connecterAdministrateur() {
        appels = new AppelsHistorique(mockMvc, connecte(dossiers.compte("ADM-HIST", ADMINISTRATEUR), ADMINISTRATEUR));
    }

    private LocalDateTime ilYa(int jours) {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).minusDays(jours);
    }

    private Map<String, String> jour(String... paires) {
        return periode(JOUR, JOUR, paires);
    }

    // ------------------------------------------------------------------ règle 1

    @Test
    void r1_sansAucunParametre_pageParDefaut_recente_dateDecroissante() throws Exception {
        LocalDateTime maintenant = ilYa(0);
        UUID recente = fixture.ligne(Ligne.de(maintenant, jeton));

        String corps = appels.json(LISTE, Map.of());

        List<String> ids = JsonPath.read(corps, "$.content[*].id");
        List<String> textes = JsonPath.read(corps, "$.content[*].dateAction");
        List<LocalDateTime> dates = textes.stream().map(LocalDateTime::parse).toList();
        // un jour de marge : l'inclusion exacte de la borne des 30 jours n'est pas fixée par le spec
        LocalDateTime borne = maintenant.toLocalDate().minusDays(31).atStartOfDay();
        Integer page = JsonPath.read(corps, "$.page");
        Integer taille = JsonPath.read(corps, "$.size");
        assertAll(
                () -> assertThat(ids).contains(recente.toString()),
                () -> assertThat(dates).isNotEmpty()
                        .isSortedAccordingTo(Comparator.reverseOrder())
                        .allMatch(date -> !date.isBefore(borne)),
                () -> assertThat(page).isZero(),
                () -> assertThat(taille).isEqualTo(50));
    }

    @Test
    void r1_sansDates_ligneDansLes30Jours_sort_ligneAncienne_exclue() throws Exception {
        UUID recente = fixture.ligne(Ligne.de(ilYa(AGE_DANS_LA_PERIODE), jeton));
        fixture.ligne(Ligne.de(ilYa(AGE_HORS_PERIODE), jeton));

        assertThat(appels.ids(Map.of(TEXTE, jeton))).containsExactly(recente.toString());
    }

    @Test
    void r1_dateDecroissante() throws Exception {
        UUID moyenne = fixture.ligne(Ligne.de(JOUR.atTime(12, 0), jeton));
        UUID ancienne = fixture.ligne(Ligne.de(JOUR.atTime(8, 0), jeton));
        UUID recente = fixture.ligne(Ligne.de(JOUR.atTime(18, 0), jeton));

        assertThat(appels.ids(jour(TEXTE, jeton)))
                .containsExactly(recente.toString(), moyenne.toString(), ancienne.toString());
    }

    @Test
    void r1_dateEgale_ordreParPKey_stableEntreLesPages() throws Exception {
        for (int i = 0; i < 5; i++) {
            fixture.ligne(Ligne.de(JOUR.atTime(9, 30), jeton));
        }
        List<String> parPKey = fixture.idsParPKey("Yes.Test." + jeton);

        List<String> parcours = parcoursPages();

        // le spec ne fixe pas le sens : l'un des deux ordres de PKey, sans doublon ni trou entre les pages
        assertThat(parcours).isIn(parPKey, parPKey.reversed());
        assertThat(parcoursPages()).isEqualTo(parcours);
    }

    private List<String> parcoursPages() throws Exception {
        List<String> parcours = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            parcours.addAll(appels.ids(jour(TEXTE, jeton, PAGE, String.valueOf(page), SIZE, "2")));
        }
        return parcours;
    }

    // ------------------------------------------------------------------ contrat

    @Test
    void contrat_ligne_septChampsDuSpec_detailsEtDescriptionNonExposes() throws Exception {
        Ligne ligne = Ligne.de(JOUR.atTime(14, 30), jeton).avecAction(FixtureHistorique.MODIFICATION);
        UUID id = fixture.ligne(ligne);

        String corps = appels.json(LISTE, jour(TEXTE, jeton));

        Map<String, Object> lue = JsonPath.read(corps, "$.content[0]");
        String dateAction = JsonPath.read(corps, "$.content[0].dateAction");
        assertAll(
                () -> assertThat(lue)
                        .containsOnlyKeys("id", "objetId", "dateAction", "utilisateur", "entite", "libelleEntite", "action")
                        .containsEntry("id", id.toString())
                        .containsEntry("objetId", ligne.objetId().toString())
                        .containsEntry("utilisateur", FixtureHistorique.UTILISATEUR)
                        .containsEntry("entite", ligne.type())
                        .containsEntry("libelleEntite", ligne.libelle())
                        .containsEntry("action", FixtureHistorique.MODIFICATION),
                () -> assertThat(dateAction).startsWith("2012-03-10T14:30"));
    }

    // ------------------------------------------------------------------ règle 9

    @Test
    void r9_utilisateurs_distincts_tries() throws Exception {
        String premier = jeton + " Alice";
        String deuxieme = jeton + " Mona";
        String troisieme = jeton + " Zoe";
        fixture.ligne(Ligne.de(JOUR.atTime(8, 0), jeton).avecUtilisateur(troisieme));
        fixture.ligne(Ligne.de(JOUR.atTime(9, 0), jeton).avecUtilisateur(premier));
        fixture.ligne(Ligne.de(JOUR.atTime(10, 0), jeton).avecUtilisateur(deuxieme));
        fixture.ligne(Ligne.de(JOUR.atTime(11, 0), jeton).avecUtilisateur(premier));

        assertThat(appels.utilisateurs(jour()).stream().filter(nom -> nom.startsWith(jeton)))
                .containsExactly(premier, deuxieme, troisieme);
    }

    @Test
    void r9_utilisateurs_sansNullNiVide() throws Exception {
        String nomme = jeton + " Alice";
        fixture.ligne(Ligne.de(JOUR.atTime(8, 0), jeton).avecUtilisateur(nomme));
        fixture.ligne(Ligne.de(JOUR.atTime(9, 0), jeton).avecUtilisateur(""));
        fixture.sansUtilisateur(fixture.ligne(Ligne.de(JOUR.atTime(10, 0), jeton)));

        assertThat(appels.utilisateurs(jour())).contains(nomme).doesNotContainNull().doesNotContain("");
    }

    @Test
    void r9_utilisateurs_bornesParLaPeriode() throws Exception {
        String dedans = jeton + " Dedans";
        String avant = jeton + " Avant";
        String apres = jeton + " Apres";
        fixture.ligne(Ligne.de(JOUR.atTime(23, 59, 59), jeton).avecUtilisateur(dedans));
        fixture.ligne(Ligne.de(JOUR.minusDays(1).atTime(23, 59, 59), jeton).avecUtilisateur(avant));
        fixture.ligne(Ligne.de(JOUR.plusDays(1).atStartOfDay(), jeton).avecUtilisateur(apres));

        assertThat(appels.utilisateurs(jour())).contains(dedans).doesNotContain(avant, apres);
    }

    @Test
    void r9_utilisateurs_sansDates_trenteDerniersJours() throws Exception {
        String recent = jeton + " Recent";
        String ancien = jeton + " Ancien";
        fixture.ligne(Ligne.de(ilYa(AGE_DANS_LA_PERIODE), jeton).avecUtilisateur(recent));
        fixture.ligne(Ligne.de(ilYa(AGE_HORS_PERIODE), jeton).avecUtilisateur(ancien));

        assertThat(appels.utilisateurs(Map.of())).contains(recent).doesNotContain(ancien);
    }
}

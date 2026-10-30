package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureHistorique;
import com.tsix.immocrm.fixtures.FixtureHistorique.Ligne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsHistorique.ACTION;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.LISTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.PAGE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.SIZE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.TEXTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.UTILISATEUR;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.UTILISATEURS;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.periode;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureHistorique.AJOUT;
import static com.tsix.immocrm.fixtures.FixtureHistorique.LIBELLE_NEUTRE;
import static com.tsix.immocrm.fixtures.FixtureHistorique.MODIFICATION;
import static com.tsix.immocrm.fixtures.FixtureHistorique.SUPPRESSION;
import static com.tsix.immocrm.fixtures.FixtureHistorique.TYPE_NEUTRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Filtres et validations de la liste : règles 2 à 6 et 8. FEATURE-admin-historique.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoriqueFiltresTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final LocalDate JOUR = LocalDate.of(2012, 3, 10);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureHistorique fixture;

    private final String jeton = FixtureHistorique.jeton();
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

    // ------------------------------------------------------------------ règle 2

    @Test
    void r2_dateFinIncluse_finDeJournee_sort_lendemainMinuit_non() throws Exception {
        UUID finDeJournee = fixture.ligne(Ligne.de(JOUR.atTime(23, 59, 59), jeton));
        fixture.ligne(Ligne.de(JOUR.plusDays(1).atStartOfDay(), jeton));

        assertThat(appels.ids(periode(JOUR.minusDays(1), JOUR, TEXTE, jeton))).containsExactly(finDeJournee.toString());
    }

    @Test
    void r2_dateDebutIncluse_minuit_sort_veille_non() throws Exception {
        UUID minuit = fixture.ligne(Ligne.de(JOUR.atStartOfDay(), jeton));
        fixture.ligne(Ligne.de(JOUR.minusDays(1).atTime(23, 59, 59), jeton));

        assertThat(appels.ids(periode(JOUR, JOUR.plusDays(1), TEXTE, jeton))).containsExactly(minuit.toString());
    }

    // ------------------------------------------------------------------ règle 3

    @Test
    void r3_texte_trouveDansObjectType_parSousChaine() throws Exception {
        UUID ligne = fixture.ligne(ligneDuJour().avecType("Yes.Adm." + jeton + ".Fiche").avecLibelle(LIBELLE_NEUTRE));

        assertThat(appels.ids(jour(TEXTE, jeton))).containsExactly(ligne.toString());
    }

    @Test
    void r3_texte_trouveDansObjectLabel_parSousChaine() throws Exception {
        UUID ligne = fixture.ligne(ligneDuJour().avecType(TYPE_NEUTRE).avecLibelle("Avant " + jeton + " apres"));

        assertThat(appels.ids(jour(TEXTE, jeton))).containsExactly(ligne.toString());
    }

    @Test
    void r3_texte_sansTenirCompteDeLaCasse() throws Exception {
        UUID ligne = fixture.ligne(ligneDuJour());

        assertThat(appels.ids(jour(TEXTE, jeton.toLowerCase(Locale.ROOT)))).containsExactly(ligne.toString());
    }

    @Test
    void r3_texte_aucunAutreChamp() throws Exception {
        Ligne neutre = ligneDuJour().avecType(TYPE_NEUTRE).avecLibelle(LIBELLE_NEUTRE);
        fixture.ligne(neutre.avecUtilisateur("Compte " + jeton));
        fixture.ligne(neutre.avecLibelleType("Type " + jeton));
        fixture.ligne(neutre.avecDescription("Description " + jeton));
        fixture.ligne(neutre.avecDetails("Details " + jeton));

        assertThat(appels.ids(jour(TEXTE, jeton))).isEmpty();
    }

    // ------------------------------------------------------------------ règle 4

    @Test
    void r4_utilisateur_egaliteExacte_niPrefixeNiSuffixe() throws Exception {
        String nom = jeton + " Alice";
        UUID exacte = fixture.ligne(ligneDuJour().avecUtilisateur(nom));
        fixture.ligne(ligneDuJour().avecUtilisateur(nom + " Martin"));
        fixture.ligne(ligneDuJour().avecUtilisateur("Mme " + nom));

        assertThat(appels.ids(jour(UTILISATEUR, nom))).containsExactly(exacte.toString());
    }

    // ------------------------------------------------------------------ règle 5

    @ParameterizedTest
    @ValueSource(strings = {AJOUT, MODIFICATION, SUPPRESSION})
    void r5_action_filtreExact(String action) throws Exception {
        fixture.ligne(ligneDuJour().avecAction(AJOUT));
        fixture.ligne(ligneDuJour().avecAction(MODIFICATION));
        fixture.ligne(ligneDuJour().avecAction(SUPPRESSION));

        String corps = appels.json(LISTE, jour(TEXTE, jeton, ACTION, action));

        List<String> actions = JsonPath.read(corps, "$.content[*].action");
        assertThat(actions).containsExactly(action);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Viewed", "Add", "Added,Deleted"})
    void r5_action_horsDesTrois_400(String action) throws Exception {
        appels.appeler(LISTE, jour(ACTION, action)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ règle 6

    @Test
    void r6_dateDebutApresDateFin_liste_400() throws Exception {
        appels.appeler(LISTE, periode(JOUR.plusDays(1), JOUR)).andExpect(status().isBadRequest());
    }

    @Test
    void r6_dateDebutApresDateFin_utilisateurs_400() throws Exception {
        appels.appeler(UTILISATEURS, periode(JOUR.plusDays(1), JOUR)).andExpect(status().isBadRequest());
    }

    @Test
    void r6_dateDebutEgaleDateFin_accepte() throws Exception {
        UUID ligne = fixture.ligne(ligneDuJour());

        assertThat(appels.ids(jour(TEXTE, jeton))).containsExactly(ligne.toString());
    }

    @Test
    void r6_pageNegative_400() throws Exception {
        appels.appeler(LISTE, jour(PAGE, "-1")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 201})
    void r6_sizeHorsBornes_400(int taille) throws Exception {
        appels.appeler(LISTE, jour(SIZE, String.valueOf(taille))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 200})
    void r6_sizeAuxBornes_accepte(int taille) throws Exception {
        String corps = appels.json(LISTE, jour(SIZE, String.valueOf(taille)));

        Integer lue = JsonPath.read(corps, "$.size");
        assertThat(lue).isEqualTo(taille);
    }

    // ------------------------------------------------------------------ règle 8

    @Test
    void r8_totalElements_refleteChaqueFiltre_etNonLaPage() throws Exception {
        String alice = jeton + " Alice";
        String bruno = jeton + " Bruno";
        fixture.ligne(Ligne.de(JOUR.atTime(8, 0), jeton).avecUtilisateur(alice));
        fixture.ligne(Ligne.de(JOUR.atTime(9, 0), jeton).avecUtilisateur(alice).avecAction(MODIFICATION));
        fixture.ligne(Ligne.de(JOUR.atTime(10, 0), jeton).avecUtilisateur(bruno));
        fixture.ligne(Ligne.de(JOUR.plusDays(1).atTime(10, 0), jeton).avecUtilisateur(bruno));
        LocalDate lendemain = JOUR.plusDays(1);

        assertAll(
                () -> assertThat(appels.total(periode(JOUR, lendemain, TEXTE, jeton, SIZE, "1"))).isEqualTo(4),
                () -> assertThat(appels.total(jour(TEXTE, jeton, SIZE, "1"))).isEqualTo(3),
                () -> assertThat(appels.total(periode(JOUR, lendemain, UTILISATEUR, alice, SIZE, "1"))).isEqualTo(2),
                () -> assertThat(appels.total(periode(JOUR, lendemain, TEXTE, jeton, ACTION, AJOUT, SIZE, "1")))
                        .isEqualTo(3),
                () -> assertThat(appels.total(jour(TEXTE, jeton, UTILISATEUR, bruno, ACTION, AJOUT, SIZE, "1")))
                        .isEqualTo(1),
                () -> assertThat(appels.total(jour(TEXTE, jeton, ACTION, SUPPRESSION))).isZero());
    }
}

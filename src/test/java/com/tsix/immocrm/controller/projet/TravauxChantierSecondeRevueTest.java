package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUTES_LES_TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_ANNULATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.majuscules;

/**
 * Règle 6, seconde revue N2b (FEATURE-n2-travaux-chantier.md, « Tests à écrire ») : un restreint ne peut ni exécuter une
 * transition sans intervenant, ni agir sur un jour invisible, ni supprimer hors {@code CanDelete}, ni lire l'historique d'un
 * jour invisible. Chaque refus a son témoin accepté.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierSecondeRevueTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String PAR_PKEY = "SELECT COUNT(*) FROM GrhWorkedDay WHERE Pkey = ?";
    private static final String TEMOIN = "témoin : ";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GSR-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TR-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
    }

    private UUID jour(UUID projet, UUID statut, String code) {
        return fixture.jour(new Jour(chantier.societe(), projet, type, zone, statut, DATE.atStartOfDay(), code + "-" + jeton,
                null));
    }

    private UUID jour(UUID statut, String code) {
        return jour(chantier.projet(), statut, code);
    }

    private RequestPostProcessor restreint(String suffixe, List<UUID> statuts, List<UUID> transitions) {
        return fixture.restreint(jeton + suffixe, chantier.projet(), statuts, transitions, DROIT_LIRE, DROIT_SUPPRIMER)
                .connexion();
    }

    private void inchange(SoftAssertions s, UUID jour, UUID statut) {
        s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(statut));
        s.assertThat(fixture.historique(jour)).isEmpty();
    }

    @Test
    void restreint_sansIntervenantSurLaTransition_409_libreTemoin200() throws Exception {
        RequestPostProcessor annuleur = restreint("a", List.of(EN_COURS, DEMANDE_VALIDATION), List.of(T_ANNULATION));
        RequestPostProcessor libre = fixture.compte(jeton + "l", false, List.of(chantier.projet()), DROIT_LIRE).connexion();
        UUID jour = jour(EN_COURS, "RV1");
        UUID temoin = jour(EN_COURS, "RV1T");

        Reponse refus = appels.executer(jour, T_DEMANDE, annuleur);
        Reponse ok = appels.executer(temoin, T_DEMANDE, libre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            inchange(s, jour, EN_COURS);
            s.assertThat(ok.statut()).as(TEMOIN + ok.corps()).isEqualTo(200);
        });
    }

    @Test
    void restreint_jourAuStatutInvisible_intervenantSurLaTransition_404_rienNEstEcrit() throws Exception {
        RequestPostProcessor voitEnCours = restreint("b", List.of(EN_COURS), TOUTES_LES_TRANSITIONS);
        UUID invisible = jour(DEMANDE_VALIDATION, "RV2");
        UUID temoin = jour(EN_COURS, "RV2T");

        Reponse refus = appels.executer(invisible, T_VALIDATION, voitEnCours);
        Reponse ok = appels.executer(temoin, T_DEMANDE, voitEnCours);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(404);
            inchange(s, invisible, DEMANDE_VALIDATION);
            s.assertThat(ok.statut()).as(TEMOIN + ok.corps()).isEqualTo(200);
        });
    }

    @Test
    void restreint_suppressionHorsCanDelete_409_enCoursTemoin204() throws Exception {
        RequestPostProcessor qui = restreint("c", List.of(EN_COURS, DEMANDE_VALIDATION), List.of());
        UUID bloque = jour(DEMANDE_VALIDATION, "RV3");
        UUID temoin = jour(EN_COURS, "RV3T");

        Reponse refus = appels.supprimer(bloque, qui);
        Reponse ok = appels.supprimer(temoin, qui);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(fixture.compter(PAR_PKEY, bloque)).isEqualTo(1);
            s.assertThat(ok.statut()).as(TEMOIN + ok.corps()).isEqualTo(204);
        });
    }

    @Test
    void restreint_historiqueDUnJourInvisible_404_statutOuProjet() throws Exception {
        RequestPostProcessor voitEnCours = restreint("d", List.of(EN_COURS), List.of());
        UUID statutInvisible = jour(DEMANDE_VALIDATION, "RV4");
        UUID projetInvisible = jour(fixture.projet(chantier.societe(), "GRN-" + jeton), EN_COURS, "RV4P");
        UUID temoin = jour(EN_COURS, "RV4T");
        for (UUID j : List.of(statutInvisible, projetInvisible, temoin)) {
            fixture.ligneHistorique(j, EN_COURS, "Secret " + jeton, LocalDateTime.of(2077, 3, 1, 9, 0));
        }

        Reponse parStatut = appels.historique(statutInvisible, voitEnCours);
        Reponse parProjet = appels.historique(projetInvisible, voitEnCours);
        Reponse ok = appels.historique(temoin, voitEnCours);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(parStatut.statut()).as(parStatut.corps()).isEqualTo(404);
            s.assertThat(parStatut.corps()).doesNotContain("Secret " + jeton);
            s.assertThat(parProjet.statut()).as(parProjet.corps()).isEqualTo(404);
            s.assertThat(parProjet.corps()).doesNotContain("Secret " + jeton);
            s.assertThat(ok.statut()).as(TEMOIN + ok.corps()).isEqualTo(200);
        });
    }

    @Test
    void restreint_transitionsDUnJourInvisible_404_aucuneFuite() throws Exception {
        RequestPostProcessor voitEnCours = restreint("e", List.of(EN_COURS), TOUTES_LES_TRANSITIONS);
        UUID projetInvisible = jour(fixture.projet(chantier.societe(), "GRM-" + jeton), EN_COURS, "RV5");
        UUID temoin = jour(EN_COURS, "RV5T");

        Reponse refus = appels.transitions(projetInvisible, voitEnCours);
        Reponse ok = appels.transitions(temoin, voitEnCours);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(404);
            s.assertThat(refus.corps()).doesNotContain(T_DEMANDE.toString());
            s.assertThat(ok.statut()).as(TEMOIN + ok.corps()).isEqualTo(200);
        });
    }
}

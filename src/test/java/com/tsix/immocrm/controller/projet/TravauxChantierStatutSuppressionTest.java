package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.ANNULEE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.REJETEE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUS_LES_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.VALIDEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * N2b R-N2b-7 (FEATURE-n2-travaux-chantier.md, {@code RessourceWithWorkFlow.cs:BeforeDelete} l.465-472) : statut sans
 * {@code CanDelete} et appelant restreint → 409 et message OMRAN ; libre ou « En cours » ({@code CanDelete = 1}) → 204.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierStatutSuppressionTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String PAR_PKEY = "SELECT COUNT(*) FROM GrhWorkedDay WHERE Pkey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;
    private RequestPostProcessor restreint;
    private RequestPostProcessor libre;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GSS-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TX-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        restreint = fixture.restreint(jeton + "r", chantier.projet(), TOUS_LES_STATUTS, List.of(T_DEMANDE), DROIT_LIRE,
                DROIT_SUPPRIMER).connexion();
        libre = fixture.compte(jeton + "l", false, List.of(chantier.projet()), DROIT_LIRE, DROIT_SUPPRIMER).connexion();
    }

    private UUID jour(UUID statut, String code) {
        return fixture.jour(new Jour(chantier.societe(), chantier.projet(), type, zone, statut, DATE.atStartOfDay(),
                code + "-" + jeton, null));
    }

    static Stream<Arguments> statutsNonSupprimables() {
        return Stream.of(Arguments.of(DEMANDE_VALIDATION, "Demande de validation"), Arguments.of(VALIDEE, "Validée"),
                Arguments.of(ANNULEE, "Annulée"), Arguments.of(REJETEE, "Rejetée"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("statutsNonSupprimables")
    void r7_restreint_statutSansCanDelete_409_messageOmran_jourConserve(UUID statut, String libelle) throws Exception {
        UUID jour = jour(statut, "S7" + statut.toString().substring(0, 4));

        Reponse r = appels.supprimer(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertAll(
                () -> assertThat(r.<String>lire("$.detail"))
                        .isEqualTo("Vous ne pouvez pas supprimer cette entité dans le statut : [" + libelle + "] !"),
                () -> assertThat(fixture.compter(PAR_PKEY, jour)).isEqualTo(1));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("statutsNonSupprimables")
    void r7_libre_statutSansCanDelete_204(UUID statut, String libelle) throws Exception {
        UUID jour = jour(statut, "L7" + statut.toString().substring(0, 4));

        Reponse r = appels.supprimer(jour, libre);

        assertThat(r.statut()).as(libelle + " : " + r.corps()).isEqualTo(204);
        assertThat(fixture.compter(PAR_PKEY, jour)).isZero();
    }

    @Test
    void r7_restreint_enCours_204() throws Exception {
        UUID jour = jour(EN_COURS, "E7");

        Reponse r = appels.supprimer(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(fixture.compter(PAR_PKEY, jour)).isZero();
    }

    @Test
    void r7_restreint_apresTransitionVersDemandeDeValidation_409() throws Exception {
        UUID jour = jour(EN_COURS, "T7");
        assertThat(appels.executer(jour, T_DEMANDE, restreint).statut()).isEqualTo(200);

        Reponse r = appels.supprimer(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fixture.compter(PAR_PKEY, jour)).isEqualTo(1);
    }
}

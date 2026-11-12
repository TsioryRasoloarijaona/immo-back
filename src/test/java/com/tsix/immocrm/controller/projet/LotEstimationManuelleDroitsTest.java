package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotFiche.DROIT_ESTIMATION;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E6, estimation manuelle (règle 6, droit) — FEATURE-lot-fiche-parite.md : {@code montantForfaitaireHT/TTC}, {@code dateDebutBase},
 * {@code delaiBaseJours}, {@code dateFinBase} ne changent qu'avec le droit OMRAN {@code ManualEstimationAdministrator} ; sinon 403
 * si le corps les change ({@code DroitsOmran.exiger}). Administrateur = tous les droits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotEstimationManuelleDroitsTest {

    private static final String MONTANT_HT = "montantForfaitaireHT";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private LotFicheSupport appels;
    private String jeton;
    private UUID projet;
    private UUID lot;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("EMP-" + jeton, parite.entiteJuridique("EME-" + jeton));
        lot = parite.lot(projet, "EML-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "EMS-" + jeton));
    }

    private RequestPostProcessor sansDroit() {
        return connecte(parite.avecDroits("lots.em.n." + jeton, List.of(), projet), "Commercial");
    }

    private RequestPostProcessor avecDroitSeul() {
        return connecte(parite.avecDroits("lots.em.d." + jeton, List.of(DROIT_ESTIMATION), projet), "Commercial");
    }

    private RequestPostProcessor administrateur() {
        return connecte(parite.administrateur("lots.em.a." + jeton, projet), "Administrateur");
    }

    /** Champ du corps, valeur qui change l'estimation (lot de fixture à 0 / NULL), colonne {@code PrjMarket}. */
    static Stream<Arguments> champsEstimation() {
        return Stream.of(
                Arguments.of(MONTANT_HT, new BigDecimal("1000"), "EstimatedPriceHTManual"),
                Arguments.of("montantForfaitaireTTC", new BigDecimal("1200"), "EstimatedPriceTTCManual"),
                Arguments.of("dateDebutBase", "2026-03-01", "StartDateManual"),
                Arguments.of("delaiBaseJours", 30, "DelayManual"),
                Arguments.of("dateFinBase", "2026-03-31", "EndDateManual"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("champsEstimation")
    void e6_estimationManuelle_sansDroit_403_baseInchangee(String champ, Object valeur, String colonne) throws Exception {
        Object avant = fiche.lot(lot).get(colonne);

        Reponse r = appels.modifier(sansDroit(), lot, corps().avec(champ, valeur));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(fiche.lotApresRefus(lot).get(colonne)).as(colonne).isEqualTo(avant);
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("champsEstimation")
    void e6_estimationManuelle_droitSeul_200_ecrit(String champ, Object valeur, String colonne) throws Exception {
        Reponse r = appels.modifier(avecDroitSeul(), lot, corps().avec(champ, valeur));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fiche.lot(lot).get(colonne)).as(colonne).isNotNull();
    }

    @Test
    void e6_estimationManuelle_droitSeul_montantEcritALaValeur() throws Exception {
        Reponse r = appels.modifier(avecDroitSeul(), lot, corps().avec(MONTANT_HT, new BigDecimal("1000")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(LotsPariteSupport.nombre(fiche.lot(lot).get("EstimatedPriceHTManual"))).isEqualByComparingTo("1000");
    }

    @Test
    void e6_estimationManuelle_administrateur_200() throws Exception {
        Reponse r = appels.modifier(administrateur(), lot, corps().avec(MONTANT_HT, new BigDecimal("1000"))
                .avec("dateDebutBase", "2026-03-01"));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(LotsPariteSupport.nombre(enBase.get("EstimatedPriceHTManual"))).isEqualByComparingTo("1000");
            s.assertThat(enBase.get("StartDateManual")).isNotNull();
        });
    }

    // « sinon 403 si le corps change l'un de ces champs » : la même valeur renvoyée (formulaire complet) n'est pas un changement.
    @Test
    void e6_estimationManuelle_memeValeur_sansDroit_200() throws Exception {
        CorpsJson corps = corps().avec(MONTANT_HT, BigDecimal.ZERO).avec("description", "Lot EML-" + jeton + " bis");

        Reponse r = appels.modifier(sansDroit(), lot, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void e6_autresChamps_sansDroit_200() throws Exception {
        Reponse r = appels.modifier(sansDroit(), lot, corps().avec("description", "Sans droit " + jeton));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fiche.lot(lot)).containsEntry("Description", "Sans droit " + jeton);
    }
}

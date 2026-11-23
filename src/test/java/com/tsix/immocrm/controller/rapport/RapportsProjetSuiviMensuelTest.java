package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.MESSAGE_UN_PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.MOIS;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.REEL;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.SUIVI;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * R2 {@code PrjMarketTresorieByMonth} (FEATURE-n5b-b-rapports-projet.md) : 12 mois de l'année {@code annee}, réel = cumul
 * depuis janvier des {@code PrjWorkingOut.AmountTTC} des lots mono-fournisseur du projet. Lot mono M dans PA1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetSuiviMensuelTest {

    private static final BigDecimal PRECISION = new BigDecimal("0.01");
    private static final int OK = 200;
    private static final int REFUS = 400;
    private static final String AN = "2025";
    private static final List<String> MOIS_FR = List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août",
            "Septembre", "Octobre", "Novembre", "Décembre");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureBilansProjet bilans;
    @Autowired private FixtureProjetLots lots;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private AppelsRapports appels;
    private Cadre c;
    private RapportsProjetBilanSupport s;
    private UUID lot;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        RapportsProjetBilanSupport.rapports(rapports);
        s = new RapportsProjetBilanSupport(c, achat, fixture, parite, bilans, lots, jdbc, em);
        lot = s.lot(c.pa1(), "M");
    }

    private MockHttpServletRequestBuilder requete(UUID projet, String annee) {
        MockHttpServletRequestBuilder r = get(AppelsRapports.resultat(PROJET, SUIVI))
                .with(connecte(c.compte(), FixtureRapportsAchat.role()));
        if (projet != null) {
            r.param(PROJET, projet.toString());
        }
        return r.param(ANNEE, annee);
    }

    private Map<String, Object> section(UUID projet) throws Exception {
        return premiere(appels.sections(requete(projet, AN)));
    }

    /** Réel de chaque mois, janvier → décembre. */
    private static List<BigDecimal> reels(Map<String, Object> section) {
        return AppelsRapports.lignes(section).stream().map(l -> montant(l, REEL)).toList();
    }

    private static void reels(Map<String, Object> section, String... attendus) {
        List<BigDecimal> lus = reels(section);
        assertThat(lus).hasSize(12);
        SoftAssertions sa = new SoftAssertions();
        for (int i = 0; i < 12; i++) {
            sa.assertThat(lus.get(i)).as("réel %s", MOIS_FR.get(i)).isCloseTo(new BigDecimal(attendus[i]), within(PRECISION));
        }
        sa.assertAll();
    }

    private String detail(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andReturn().getResponse().getContentAsString();
        return corps.isBlank() ? null : JsonPath.read(corps, "$.detail");
    }

    @Test
    void r2_douzeLignes_janvierADecembre_zeroSansDecompte() throws Exception {
        Map<String, Object> r = section(c.pa1());

        assertThat(AppelsRapports.lignes(r).stream().map(l -> l.get(MOIS)).toList()).containsExactlyElementsOf(MOIS_FR);
        reels(r, "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");
    }

    @Test
    void r2_reel_cumulDepuisJanvier_amountTtcDesDecomptes() throws Exception {
        s.decompte(lot, "D1", 1, "100", LocalDate.of(2025, 2, 10));
        s.decompte(lot, "D2", 2, "50", LocalDate.of(2025, 2, 25));
        s.decompte(lot, "D3", 3, "1000", LocalDate.of(2025, 7, 1));

        reels(section(c.pa1()), "0", "150", "150", "150", "150", "150", "1150", "1150", "1150", "1150", "1150", "1150");
    }

    @Test
    void r2_decomptesDUneAutreAnnee_exclus() throws Exception {
        s.decompte(lot, "D1", 1, "300", LocalDate.of(2024, 12, 31));
        s.decompte(lot, "D2", 2, "400", LocalDate.of(2026, 1, 5));
        s.decompte(lot, "D3", 3, "100", LocalDate.of(2025, 3, 15));

        reels(section(c.pa1()), "0", "0", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100");
    }

    @Test
    void r2_lotMultiFournisseur_exclu() throws Exception {
        UUID multi = s.lotMulti(c.pa1(), "MM");
        s.decompte(multi, "DM", 1, "999", LocalDate.of(2025, 3, 1));
        s.decompte(lot, "D1", 1, "100", LocalDate.of(2025, 3, 1));

        reels(section(c.pa1()), "0", "0", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100");
    }

    @Test
    void r2_decompteSansDate_ignore() throws Exception {
        s.decompte(lot, "DN", 1, "777", null);
        s.decompte(lot, "D1", 2, "100", LocalDate.of(2025, 12, 1));

        reels(section(c.pa1()), "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "100");
    }

    @Test
    void r2_lotDUnAutreProjet_exclu() throws Exception {
        UUID autre = s.lot(c.pa2(), "M2");
        s.decompte(autre, "DA", 1, "500", LocalDate.of(2025, 1, 10));

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(reels(section(c.pa1())).getLast()).as("PA1").isCloseTo(BigDecimal.ZERO, within(PRECISION));
        sa.assertThat(reels(section(c.pa2())).getFirst()).as("PA2 janvier").isCloseTo(new BigDecimal("500"), within(PRECISION));
        sa.assertAll();
    }

    @Test
    void r2_aucuneLigneDeTotaux() throws Exception {
        s.decompte(lot, "D1", 1, "100", LocalDate.of(2025, 2, 10));

        Map<String, Object> totaux = AppelsRapports.totaux(section(c.pa1()));

        assertThat(totaux == null || totaux.isEmpty()).as("totaux %s", totaux).isTrue();
    }

    @Test
    void r2_sansProjet_400_veuillezSelectionnerUnProjet() throws Exception {
        int temoin = appels.statut(requete(c.pa1(), AN));
        int statut = appels.statut(requete(null, AN));
        String message = detail(requete(null, AN));

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(temoin).as("témoin PA1").isEqualTo(OK);
            sa.assertThat(statut).isEqualTo(REFUS);
            sa.assertThat(message).isEqualTo(MESSAGE_UN_PROJET);
        });
    }

    /** R2 / R3a : projet hors périmètre → même 400 que l'absence de projet. */
    @Test
    void r2_r3a_projetHorsPerimetre_400_memeMessage() throws Exception {
        int temoin = appels.statut(requete(c.pa1(), AN));
        int statut = appels.statut(requete(c.pb(), AN));
        String message = detail(requete(c.pb(), AN));

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(temoin).as("témoin PA1").isEqualTo(OK);
            sa.assertThat(statut).isEqualTo(REFUS);
            sa.assertThat(message).isEqualTo(MESSAGE_UN_PROJET);
        });
    }

    @Test
    void r2_descripteur_projetUnique_anneeObligatoireAvecDefaut() throws Exception {
        String corps = appels.corps(get(AppelsRapports.descripteur(PROJET, SUIVI))
                .with(connecte(c.compte(), FixtureRapportsAchat.role())));

        List<Boolean> multiple = JsonPath.read(corps, "$.parametres[?(@.nom == 'projet')].multiple");
        List<String> type = JsonPath.read(corps, "$.parametres[?(@.nom == 'annee')].type");
        List<Boolean> obligatoire = JsonPath.read(corps, "$.parametres[?(@.nom == 'annee')].obligatoire");
        List<Object> defaut = JsonPath.read(corps, "$.parametres[?(@.nom == 'annee')].valeurParDefaut");

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(multiple).as("projet multiple").containsExactly(false);
        sa.assertThat(type).as("annee type").containsExactly("ANNEE");
        sa.assertThat(obligatoire).as("annee obligatoire").containsExactly(true);
        sa.assertThat(defaut).as("annee défaut").hasSize(1);
        sa.assertThat(String.valueOf(defaut.isEmpty() ? null : defaut.getFirst())).as("annee défaut").matches("\\d{4}");
        sa.assertAll();
    }
}

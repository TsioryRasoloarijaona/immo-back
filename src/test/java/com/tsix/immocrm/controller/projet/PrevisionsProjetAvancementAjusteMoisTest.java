package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.HorlogeReglable;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.INVOICE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * R7-ter (FEATURE-n4-previsions-chantier.md, correctif fix/n4-avancement-ajuste-mois) : chaque lot a une valeur d'avancement
 * ajusté à tout mois de la plage du projet (0 avant son début, 1 après sa fin) → aucun {@code mois[].avancementPrevision} null.
 * Aujourd'hui = 15/07/2026 ; A = 01/01/2026 + 30 j, B = 01/06/2026 + 30 j, HT égaux (poids 0,5 / 0,5) ; pièces réelles
 * en 01/2026 et 10/2026 pour que la plage couvre 03/2026 et des mois après la fin des deux lots.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsProjetAvancementAjusteMoisTest {

    private static final HorlogeReglable HORLOGE = new HorlogeReglable();
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 7, 15);
    private static final double PRECISION = 0.0001;
    private static final String HT = "1000";
    private static final String CHAMP = "avancementPrevision";
    /** B finit le 01/07/2026 : tout mois à partir d'août est après la fin des deux lots. */
    private static final String APRES_FIN = "2026-08";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return HORLOGE;
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixtureLotSousFenetres sf;

    private PrevisionsProjetSupport appels;
    private String jeton;
    private UUID societe;
    private UUID statut;
    private UUID projet;
    private int rang;

    @BeforeEach
    void setUp() {
        HORLOGE.regler(AUJOURDHUI);
        appels = new PrevisionsProjetSupport(mockMvc, jdbc, em);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        societe = parite.entiteJuridique("PPAM-" + jeton);
        statut = parite.statut(ENTREPRENEURS, "PPAMS-" + jeton);
        projet = parite.projet("PPAM-" + jeton, societe);
    }

    // ------------------------------------------------------------------ fixtures

    /** Lot inclus, non multi, planning {@code debut} + 30 j, estimation de base HT 1000 (TVA 20 %), sans retenue. */
    private UUID lot(LocalDate debut) {
        rang++;
        UUID lot = parite.lot(projet, "PPAM" + rang + "-" + jeton, ENTREPRENEURS, statut);
        prev.planning(lot, debut, 30, debut, 30);
        prev.estimationBase(lot, HT, "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        appels.inclure(lot, true);
        return lot;
    }

    private void piece(UUID lot, LocalDate echeance) {
        rang++;
        sf.piece(lot, projet, societe, "PPAMD" + rang + "-" + jeton, INVOICE, "", echeance, "100");
    }

    /** Cas (e)/(f) : A fini au 01/02/2026, B du 01/06 au 01/07/2026 ; réel en 01/2026 (A) et 10/2026 (B). */
    private Reponse deuxLotsEspaces() throws Exception {
        UUID a = lot(LocalDate.of(2026, 1, 1));
        UUID b = lot(LocalDate.of(2026, 6, 1));
        piece(a, LocalDate.of(2026, 1, 10));
        piece(b, LocalDate.of(2026, 10, 10));
        rang++;
        RequestPostProcessor qui = connecte(parite.administrateur("ppam.adm" + rang + "." + jeton, projet), "Administrateur");
        Reponse r = appels.lire(qui, projet);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static List<Map<?, ?>> mois(Reponse r) {
        return ((List<?>) r.valeur("$.mois")).stream().<Map<?, ?>>map(p -> (Map<?, ?>) p).toList();
    }

    private static String libelle(Map<?, ?> point) {
        return String.valueOf(point.get("mois"));
    }

    // ------------------------------------------------------------------ R7-ter (e) mois sans lot dans sa fenêtre

    /** R7-ter (e) : 03/2026, A fini (1) et B pas commencé (0) → 0,5 × 1 + 0,5 × 0 = 0,5, pas null. */
    @Test
    void r7ter_e_mars2026_aucunLotDansSaFenetre_avancementAjuste05() throws Exception {
        Reponse r = deuxLotsEspaces();

        Map<?, ?> mars = mois(r).stream().filter(p -> libelle(p).startsWith("2026-03")).findFirst().orElse(null);

        assertThat(mars).as("point mois 2026-03 dans " + r.corps()).isNotNull();
        assertThat(mars.get(CHAMP)).as(CHAMP + " de 2026-03").isNotNull();
        assertThat(new BigDecimal(String.valueOf(mars.get(CHAMP))).doubleValue()).isCloseTo(0.5, within(PRECISION));
    }

    /** R7-ter (e) : aucun point mois[] du premier au dernier mois du projet n'a d'avancement ajusté null. */
    @Test
    void r7ter_e_aucunPointMoisAvecAvancementAjusteNull() throws Exception {
        Reponse r = deuxLotsEspaces();

        List<Map<?, ?>> points = mois(r);
        List<String> nuls = points.stream().filter(p -> p.get(CHAMP) == null).map(PrevisionsProjetAvancementAjusteMoisTest::libelle)
                .toList();

        SoftAssertions s = new SoftAssertions();
        s.assertThat(points).as("points mois").isNotEmpty();
        s.assertThat(nuls).as("mois dont " + CHAMP + " est null").isEmpty();
        s.assertAll();
    }

    // ------------------------------------------------------------------ R7-ter (f) après la fin de tous les lots

    /** R7-ter (f) : dernier point mois[] (10/2026, après la fin des deux lots) → 1, pas null. */
    @Test
    void r7ter_f_dernierMoisApresFinDeTousLesLots_avancementAjuste1() throws Exception {
        Reponse r = deuxLotsEspaces();

        List<Map<?, ?>> points = mois(r);
        assertThat(points).as("points mois").isNotEmpty();
        Map<?, ?> dernier = points.get(points.size() - 1);

        assertThat(libelle(dernier)).as("dernier mois après la fin des lots").isGreaterThanOrEqualTo(APRES_FIN);
        assertThat(dernier.get(CHAMP)).as(CHAMP + " de " + libelle(dernier)).isNotNull();
        assertThat(new BigDecimal(String.valueOf(dernier.get(CHAMP))).doubleValue()).isCloseTo(1.0, within(PRECISION));
    }
}

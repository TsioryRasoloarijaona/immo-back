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

import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.AJUSTEE;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.AVANCEMENT;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.somme;
import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * R7-bis (FEATURE-n4-previsions-chantier.md, correctif fix/n4-avancement-ajuste) : avancement « Prévision ajustée » du projet
 * = pour chaque lot l'avancement prévu au 1ᵉʳ du mois (0 avant le début du lot, 1 après sa fin), non plus
 * {@code PrjMarketPaymentPrevision.Achievement}. Aujourd'hui = 15/07/2026 ; lot « terminé » = planning 01/01/2026 + 90 j,
 * « non commencé » = 01/01/2027 + 90 j ; poids HT 4000 / 1000 → 0,8 / 0,2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsProjetAvancementAjusteTest {

    private static final HorlogeReglable HORLOGE = new HorlogeReglable();
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 7, 15);
    private static final LocalDate DEBUT_TERMINE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DEBUT_NON_COMMENCE = LocalDate.of(2027, 1, 1);
    private static final double PRECISION = 0.0001;
    private static final String HT_LOURD = "4000";
    private static final String HT_LEGER = "1000";

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
        societe = parite.entiteJuridique("PPAJ-" + jeton);
        statut = parite.statut(ENTREPRENEURS, "PPAJS-" + jeton);
        projet = parite.projet("PPAJ-" + jeton, societe);
    }

    // ------------------------------------------------------------------ fixtures

    /** Lot inclus, non multi, planning {@code debut} + 90 j, estimation de base HT (TVA 20 %), sans retenue. */
    private UUID lot(LocalDate debut, String ht) {
        rang++;
        UUID lot = parite.lot(projet, "PPAJ" + rang + "-" + jeton, ENTREPRENEURS, statut);
        prev.planning(lot, debut, 90, debut, 90);
        BigDecimal h = new BigDecimal(ht);
        BigDecimal tva = h.multiply(new BigDecimal("0.2"));
        prev.estimationBase(lot, ht, tva.toPlainString(), h.add(tva).toPlainString(), "0.2");
        prev.retenues(lot, "0", "0", "0");
        appels.inclure(lot, true);
        return lot;
    }

    private Reponse ok() throws Exception {
        rang++;
        RequestPostProcessor qui = connecte(parite.administrateur("ppaj.adm" + rang + "." + jeton, projet), "Administrateur");
        Reponse r = appels.lire(qui, projet);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static double ajustee(Reponse r) {
        return r.nombre(totaux(AJUSTEE, AVANCEMENT)).doubleValue();
    }

    /** Valeur du dernier point de {@code liste} dont le mois est antérieur à {@code moisExclu} (texte « aaaa-mm… »). */
    private static double dernierAvant(Reponse r, String liste, String champ, String moisExclu) {
        double valeur = Double.NaN;
        for (Object p : (List<?>) r.valeur(liste)) {
            Map<?, ?> point = (Map<?, ?>) p;
            if (String.valueOf(point.get("mois")).compareTo(moisExclu) < 0 && point.get(champ) != null) {
                valeur = new BigDecimal(String.valueOf(point.get(champ))).doubleValue();
            }
        }
        return valeur;
    }

    /** Fixture du cas (c) : lignes P aux avancements trompeurs (0,3 sur le lot terminé, 0,4 sur le non commencé). */
    private void deuxLotsAvecAvancementsTrompeurs() {
        UUID termine = lot(DEBUT_TERMINE, HT_LOURD);
        UUID nonCommence = lot(DEBUT_NON_COMMENCE, HT_LEGER);
        sf.prevision(termine, LocalDate.of(2026, 2, 1), "500", "0.3");
        sf.prevision(nonCommence, LocalDate.of(2026, 6, 1), "200", "0.4");
    }

    // ------------------------------------------------------------------ R7-bis (a) lot terminé → 1

    /** R7-bis (a) : lot terminé sans aucune ligne PrjMarketPaymentPrevision compte 1 → 0,8 × 1 + 0,2 × 0. */
    @Test
    void r7bis_a_lotTermineSansLignePrevision_compte1() throws Exception {
        lot(DEBUT_TERMINE, HT_LOURD);
        lot(DEBUT_NON_COMMENCE, HT_LEGER);

        Reponse r = ok();

        assertThat(ajustee(r)).isCloseTo(0.8, within(PRECISION));
    }

    /** R7-bis (a) : lot terminé dont les lignes portent Achievement = -1 compte 1 → 0,8. */
    @Test
    void r7bis_a_lotTermineAchievementMoinsUn_compte1() throws Exception {
        UUID termine = lot(DEBUT_TERMINE, HT_LOURD);
        lot(DEBUT_NON_COMMENCE, HT_LEGER);
        sf.prevision(termine, LocalDate.of(2026, 2, 1), "500", "-1");
        sf.prevision(termine, LocalDate.of(2026, 3, 1), "700", "-1");

        Reponse r = ok();

        assertThat(ajustee(r)).isCloseTo(0.8, within(PRECISION));
    }

    // ------------------------------------------------------------------ R7-bis (b) lot non commencé → 0

    /**
     * R7-bis (b) : lot non commencé (0,8) compte 0 malgré une ligne P antérieure à Achievement 0,5 ; lot terminé (0,2) à 1
     * → 0,2 (l'ancienne lecture donnait 0,8 × 0,5 + 0,2 × 1 = 0,6).
     */
    @Test
    void r7bis_b_lotNonCommence_compte0_memeAvecAchievementAnterieur() throws Exception {
        UUID nonCommence = lot(DEBUT_NON_COMMENCE, HT_LOURD);
        UUID termine = lot(DEBUT_TERMINE, HT_LEGER);
        sf.prevision(nonCommence, LocalDate.of(2026, 3, 1), "800", "0.5");
        sf.prevision(termine, LocalDate.of(2026, 3, 1), "1200", "1");

        Reponse r = ok();

        assertThat(ajustee(r)).isCloseTo(0.2, within(PRECISION));
    }

    // ------------------------------------------------------------------ R7-bis (c) deux lots 0,8 / 0,2

    /** R7-bis (c) : 0,8 terminé + 0,2 non commencé → 0,8 (lecture Achievement : 0,8 × 0,3 + 0,2 × 0,4 = 0,32). */
    @Test
    void r7bis_c_termine08_nonCommence02_totaux08() throws Exception {
        deuxLotsAvecAvancementsTrompeurs();

        Reponse r = ok();

        assertThat(ajustee(r)).isCloseTo(0.8, within(PRECISION));
    }

    /** R7-bis (c) : la liste avancements.previsionAjustee vaut 0,8 au dernier point jusqu'à juillet 2026 inclus. */
    @Test
    void r7bis_c_listeAvancementsAjustee_08AuMoisDAujourdhui() throws Exception {
        deuxLotsAvecAvancementsTrompeurs();

        Reponse r = ok();

        assertThat(dernierAvant(r, "$.avancements.previsionAjustee", AVANCEMENT, "2026-08"))
                .isCloseTo(0.8, within(PRECISION));
    }

    /** R7-bis (c) : point du graphe de juillet 2026, avancement ajusté ({@code mois[].avancementPrevision}) = 0,8. */
    @Test
    void r7bis_c_moisDuGraphe_avancementPrevisionAjuste08() throws Exception {
        deuxLotsAvecAvancementsTrompeurs();

        Reponse r = ok();

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.ids("$.mois[*].mois")).anyMatch(m -> m.startsWith("2026-07"));
        s.assertThat(dernierAvant(r, "$.mois", "avancementPrevision", "2026-08")).isCloseTo(0.8, within(PRECISION));
        s.assertAll();
    }

    // ------------------------------------------------------------------ R7-bis « Inchangé » : fiche Prévisions du lot

    /** R7-bis inchangé : la fiche du lot terminé garde avancementPrevision = max Achievement du mois (0,3 en février). */
    @Test
    void r7bis_inchange_ficheDuLot_avancementPrevisionMaxAchievementDuMois() throws Exception {
        UUID termine = lot(DEBUT_TERMINE, HT_LOURD);
        sf.prevision(termine, LocalDate.of(2026, 2, 1), "500", "0.3");
        sf.prevision(termine, LocalDate.of(2026, 3, 10), "300", "0.4");
        sf.prevision(termine, LocalDate.of(2026, 3, 20), "200", "0.45");
        rang++;
        RequestPostProcessor qui = connecte(parite.administrateur("ppaj.lot" + rang + "." + jeton, projet), "Administrateur");

        Reponse fiche = new AppelsProjet(mockMvc).lire(qui, AppelsProjet.LOTS + "/" + termine + "/previsions");

        assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(somme(fiche, "$.mois[?(@.mois == '2026-02-01')].avancementPrevision")).isEqualByComparingTo("0.3");
        s.assertThat(somme(fiche, "$.mois[?(@.mois == '2026-03-01')].avancementPrevision")).isEqualByComparingTo("0.45");
        s.assertAll();
    }
}

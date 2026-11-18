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
import java.util.TreeMap;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.somme;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.INVOICE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Revue N4a (R3, Q4) : la lecture groupée de plusieurs lots (une requête par table) rend pour chaque lot les mêmes séries
 * que sa fiche lue seule — lots aux plannings, retenues, points de base et pièces différents, plus un multi-fournisseurs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsProjetMultiLotsRevueTest {

    private static final HorlogeReglable HORLOGE = new HorlogeReglable();
    private static final BigDecimal CENTIME = new BigDecimal("0.01");

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

    private String jeton;
    private UUID societe;
    private int rang;

    @BeforeEach
    void setUp() {
        HORLOGE.regler(LocalDate.of(2026, 3, 15));
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        societe = parite.entiteJuridique("PPMR-" + jeton);
    }

    private UUID lot(UUID projet, String genre, UUID statut, LocalDate debut, int duree, String ttc, String avance, String rg) {
        rang++;
        UUID lot = parite.lot(projet, "PMR" + rang + "-" + jeton, genre, statut);
        prev.planning(lot, debut, duree, debut, duree);
        BigDecimal t = new BigDecimal(ttc);
        prev.estimationBase(lot, t.divide(new BigDecimal("1.2")).toPlainString(),
                t.subtract(t.divide(new BigDecimal("1.2"))).toPlainString(), ttc, "0.2");
        prev.retenues(lot, avance, rg, "12");
        new PrevisionsProjetSupport(mockMvc, jdbc, em).inclure(lot, true);
        return lot;
    }

    private void piece(UUID lot, UUID projet, LocalDate echeance, String montant) {
        rang++;
        sf.piece(lot, projet, societe, "PMRD" + rang + "-" + jeton, INVOICE, "", echeance, montant);
    }

    @Test
    void plusieursLots_chaqueLigneEtChaqueMoisEgauxALaFicheDuLot() throws Exception {
        UUID projet = parite.projet("PMR-" + jeton, societe);
        UUID statut = parite.statut(ENTREPRENEURS, "PMRS-" + jeton);
        UUID a = lot(projet, ENTREPRENEURS, statut, LocalDate.of(2026, 1, 1), 90, "1200", "0.1", "0.05");
        UUID b = lot(projet, ENTREPRENEURS, statut, LocalDate.of(2026, 2, 10), 200, "3600", "0", "0.1");
        UUID c = lot(projet, FOURNISSEURS, parite.statut(FOURNISSEURS, "PMRM-" + jeton), LocalDate.of(2026, 3, 1), 60,
                "2400", "0", "0");
        prev.pointBase(a, LocalDate.of(2026, 1, 1), "0", "0", "0", "0");
        prev.pointBase(a, LocalDate.of(2026, 2, 15), "0.7", "0.5", "0.7", "840");
        prev.pointBase(a, LocalDate.of(2026, 4, 1), "1", "1", "1", "1200");
        sf.prevision(a, LocalDate.of(2026, 2, 1), "500", "0.5");
        sf.prevision(b, LocalDate.of(2026, 5, 1), "1800", "0.5");
        sf.prevision(c, LocalDate.of(2026, 4, 1), "2400", "1");
        piece(a, projet, LocalDate.of(2026, 2, 10), "1000");
        piece(b, projet, LocalDate.of(2026, 3, 5), "400");
        piece(c, projet, LocalDate.of(2026, 3, 20), "700");
        RequestPostProcessor qui = connecte(parite.administrateur("pmr.adm." + jeton, projet), "Administrateur");

        Reponse r = new PrevisionsProjetSupport(mockMvc, jdbc, em).lire(qui, projet);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);

        SoftAssertions s = new SoftAssertions();
        Map<String, BigDecimal[]> sommeDesFiches = new TreeMap<>();
        for (UUID lot : List.of(a, b, c)) {
            Reponse fiche = new AppelsProjet(mockMvc).lire(qui, AppelsProjet.LOTS + "/" + lot + "/previsions");
            assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
            BigDecimal estimation = somme(fiche, "$.mois[*].estimation");
            BigDecimal prevision = somme(fiche, "$.mois[*].prevision");
            BigDecimal reel = somme(fiche, "$.mois[*].reel");
            String ligne = "$.lots[?(@.lotId == '" + lot + "')].";
            s.assertThat(somme(r, ligne + "previsionNetAPayerCalculee")).as("estimation " + lot)
                    .isCloseTo(estimation, within(CENTIME));
            s.assertThat(somme(r, ligne + "coutPrevisionnelNetAPayerCalcule")).as("P + R " + lot)
                    .isCloseTo(prevision.add(reel), within(CENTIME));
            s.assertThat(somme(r, ligne + "positionNetAPayerCalculee")).as("R " + lot).isCloseTo(reel, within(CENTIME));
            for (Object p : (List<?>) fiche.valeur("$.mois")) {
                Map<?, ?> point = (Map<?, ?>) p;
                BigDecimal[] v = sommeDesFiches.computeIfAbsent(String.valueOf(point.get("mois")),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                v[0] = v[0].add(new BigDecimal(String.valueOf(point.get("estimation"))));
                v[1] = v[1].add(new BigDecimal(String.valueOf(point.get("prevision"))));
                v[2] = v[2].add(new BigDecimal(String.valueOf(point.get("reel"))));
            }
        }
        for (Object p : (List<?>) r.valeur("$.mois")) {
            Map<?, ?> point = (Map<?, ?>) p;
            String mois = String.valueOf(point.get("mois"));
            BigDecimal[] attendu = sommeDesFiches.getOrDefault(mois,
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            s.assertThat(new BigDecimal(String.valueOf(point.get("estimation")))).as("estimation " + mois)
                    .isCloseTo(attendu[0], within(CENTIME));
            s.assertThat(new BigDecimal(String.valueOf(point.get("prevision")))).as("prevision " + mois)
                    .isCloseTo(attendu[1], within(CENTIME));
            s.assertThat(new BigDecimal(String.valueOf(point.get("reel")))).as("reel " + mois)
                    .isCloseTo(attendu[2], within(CENTIME));
        }
        s.assertThat(somme(r, "$.mois[*].estimation")).isPositive();
        s.assertAll();
    }
}

package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePrevisionsReel;
import com.tsix.immocrm.fixtures.FixturePrevisionsReel.TauxStockes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.POINTS;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.nombre;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.point;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.texte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * FEATURE-lot-previsions-reel.md §4 C1-C4, par HTTP (PUT des prévisions puis lecture des prévisions de paiement) : net du
 * premier décompte simulé quand le lot a un décompte réel. Chiffres faits à la main, communs à tous les tests :
 * <ul>
 *   <li>lot 01/01/2026 + 90 j, taux du lot : avance 20 %, RG 10 %, autres 0 ; bordereau 1000 HT → montant 1200 TTC ;</li>
 *   <li>décompte réel du 01/03/2026 : HT 500 (avancement 0,5), TTC 600 ; taux stockés propres à chaque test ;</li>
 *   <li>points saisis (01/01, 0) et (01/04, 1) → un seul décompte simulé, TTC 1200 − 600 = 600 ;</li>
 *   <li>Σ des avances {@code MARKET_ADVANCE} = 240 dans tous les tests → plafond des simulés 240 ; net cumulé aux taux du lot
 *       sur 1200 = 1200 − min(240, 240) − 120 = 840.</li>
 * </ul>
 * Net du décompte simulé = 840 − net cumulé du dernier réel, calculé avec <b>ses</b> taux stockés (C2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecompteSimuleTest {

    private static final String DECOMPTE_N = "Décompte prévisionnel N° ";
    private static final String DESCRIPTION = "Description";
    private static final String MONTANT = "Amount";
    private static final BigDecimal CENTIME = new BigDecimal("0.01");
    private static final LocalDate DATE_REEL = LocalDate.of(2026, 3, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixturePrevisionsReel reel;

    private PrevisionsLotSupport appels;
    private String jeton;
    private UUID projet;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new PrevisionsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        projet = parite.projet("PDS-" + jeton, parite.entiteJuridique("PDE-" + jeton));
        admin = connecte(parite.administrateur("lot.pds." + jeton, projet), "Administrateur");
    }

    private UUID lot() {
        UUID lot = parite.lot(projet, "PDL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "PDU-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0.2", "0.1", "0");
        reel.bordereau(lot, "1000");
        return lot;
    }

    private UUID decompteReel(UUID lot, TauxStockes taux) {
        return reel.decompteFacture(lot, "PDD-" + jeton, 1, DATE_REEL, "500", "600", "600", taux);
    }

    private void avanceMarche(UUID lot, String suffixe, LocalDate date, String ttc) {
        reel.avance(lot, "PDA" + suffixe + "-" + jeton, FixtureLotSousFenetres.MARKET_ADVANCE, date, ttc);
    }

    /** PUT des points (01/01, 0) et (01/04, 1), puis net du seul décompte simulé. */
    private BigDecimal netDuDecompteSimule(UUID lot) throws Exception {
        Reponse r = appels.enregistrer(admin, lot, inchange().avec(POINTS,
                List.of(point(DEBUT_2026, "0"), point(FIN_2026, "1"))));
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> simules = prev.lignesPaiement(lot).stream()
                .filter(l -> texte(l, DESCRIPTION).startsWith(DECOMPTE_N)).toList();
        assertThat(simules).as("un seul décompte simulé après le réel").hasSize(1);
        return nombre(simules.getFirst(), MONTANT);
    }

    // C1 + C2 : taux stockés avance 25 %, RG 7 % (≠ lot) ; avance 240 datée avant le réel.
    // Réel : 600 − min(0,25 × 600 = 150, 240) − 0,07 × 600 = 42 → 408 ; simulé 840 − 408 = 432 (taux du lot : 420).
    @Test
    void c2_netAvant_tauxStockesDuDernierReel() throws Exception {
        UUID lot = lot();
        avanceMarche(lot, "1", LocalDate.of(2026, 2, 1), "240");
        decompteReel(lot, TauxStockes.de("0.25", "0.07"));

        assertThat(netDuDecompteSimule(lot)).isCloseTo(new BigDecimal("432"), within(CENTIME));
    }

    // C3 : avances 100 au 01/03 (= date du réel, comptée) et 140 au 15/03 (après, exclue du plafond du réel).
    // Réel : 600 − min(150, 100) − 42 = 458 ; simulé 840 − 458 = 382 (sans C3 : 432 ; filtre strict « < » : 282).
    @Test
    void c3_avanceDuReel_plafonneeParLesAvancesDateesAuPlusTardAuDecompte() throws Exception {
        UUID lot = lot();
        avanceMarche(lot, "1", DATE_REEL, "100");
        avanceMarche(lot, "2", LocalDate.of(2026, 3, 15), "140");
        decompteReel(lot, TauxStockes.de("0.25", "0.07"));

        assertThat(netDuDecompteSimule(lot)).isCloseTo(new BigDecimal("382"), within(CENTIME));
    }

    // C1 : DEC stocké 2 % plafonné à 1000 (lot : 0 %). Réel : 600 − 150 − 42 − 12 = 396 ; simulé 840 − 396 = 444.
    @Test
    void c1_retenueDecStockee_compteeDansLeNetDuReel() throws Exception {
        UUID lot = lot();
        avanceMarche(lot, "1", LocalDate.of(2026, 2, 1), "240");
        decompteReel(lot, new TauxStockes("0.25", "0.07", "0", "0.02", "1000", "0", "0"));

        assertThat(netDuDecompteSimule(lot)).isCloseTo(new BigDecimal("444"), within(CENTIME));
    }

    // C4 : avance sur décompte 100, dont 30 déjà amortis par le réel → reste 70 retenu sur le simulé : 432 − 70 = 362.
    @Test
    void c4_avanceSurDecompte_resteAAmortirApresLeReel() throws Exception {
        UUID lot = lot();
        avanceMarche(lot, "1", LocalDate.of(2026, 2, 1), "240");
        reel.avance(lot, "PDW-" + jeton, FixtureLotSousFenetres.WORKINGOUT_ADVANCE, LocalDate.of(2026, 2, 1), "100");
        UUID decompte = decompteReel(lot, TauxStockes.de("0.25", "0.07"));
        reel.avanceAmortie(decompte, "30");

        assertThat(netDuDecompteSimule(lot)).isCloseTo(new BigDecimal("362"), within(CENTIME));
    }

    // Sans décompte réel : comportement L3a-bis inchangé (S2-S5) — 1200 − 240 − 120 = 840 en un seul décompte.
    @Test
    void c2_sansReel_comportementActuel() throws Exception {
        UUID lot = lot();
        avanceMarche(lot, "1", LocalDate.of(2026, 2, 1), "240");

        assertThat(netDuDecompteSimule(lot)).isCloseTo(new BigDecimal("840"), within(CENTIME));
    }
}

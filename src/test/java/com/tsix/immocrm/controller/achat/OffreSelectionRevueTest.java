package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * FEATURE-lc7-appel-offres.md D1 (égalité des ensembles de codes DIA / offre, code fautif nommé), R3 (PU HT après remise à 6 décimales),
 * R4 (codes uniques, DIA et offre) à la génération « Fin succès », et invariant « une seule offre sélectionnée » (règle 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreSelectionRevueTest {

    private static final String CODE_INTROUVABLE = "Il y a une erreur au niveaux du bordereau de dépouillement !";
    private static final String A1_DOUBLON = "Le code A1 doit être unique pour les lignes du bordereaux !";
    private static final String LIGNES_LOT = "SELECT * FROM PrjMarketLine WHERE PrjMarketId = ? ORDER BY Code ASC";
    private static final String OFFRE = "SELECT * FROM BuyDocument WHERE PKey = ?";
    private static final String SELECTIONNEES = "SELECT COUNT(*) FROM BuyDocument WHERE BuySelectionId = ? AND IsSelected = 1";
    private static final String A1 = "A1";
    private static final String A2 = "A2";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;
    private UUID cloturer;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("SR");
        qLibre = qui(b.libre(c, "lc7.sr.libre." + c.jeton()));
        cloturer = demandes.transition(c.entite(), c.depouil(), c.clot(), "Clôturer-" + c.jeton(), 1);
    }

    /** DIA en Dépouillement sur un lot non multi (bordereau définitif ancien « OLD ») ; bordereau DIA G1 ▸ (A1, G2 ▸ A2) ; offre sélectionnée. */
    private record Scenario(UUID dia, UUID lot, UUID offre, UUID g1, UUID categorie) {
    }

    private Scenario scenario(String suffixe) {
        UUID dia = demandes.demande(c, c.code(suffixe), c.depouil());
        UUID lot = b.lotDe(c, dia, "L" + suffixe + "-" + c.jeton(), false);
        b.groupeDefinitif(lot, "OLDG-" + suffixe);
        b.ligneDefinitive(lot, "OLD-" + suffixe);
        UUID categorie = achat.categorie("CAT" + suffixe + "-" + c.jeton());
        UUID g1 = b.groupe(dia, "G1", null, 1);
        UUID g2 = b.groupe(dia, "G2", g1, 2);
        b.ligne(dia, g1, 1, A1, "10", categorie);
        b.ligne(dia, g2, 2, A2, "20", categorie);
        UUID t = achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
        demandes.poser(DEMANDE, dia, "CfgTierId", t);
        UUID offre = b.offre(dia, t, c.code("O" + suffixe), true);
        return new Scenario(dia, lot, offre, g1, categorie);
    }

    private List<Object> codesLotApresRefus(UUID lot) {
        return b.lignesApresRefus(LIGNES_LOT, lot.toString()).stream().map(l -> l.get("Code")).toList();
    }

    private static String detail(Reponse r) {
        return r.statut() == 409 ? String.valueOf(r.valeur(DETAIL)) : "";
    }

    // ------------------------------------------------------------------ D1 : égalité des ensembles de codes

    @Test
    void d1_ligneDeDiaAbsenteDeLOffre_409_codeNomme_bordereauDuLotIntact() throws Exception {
        Scenario s = scenario("DAB");
        b.ligneOffre(s.offre(), 1, A1, "200", "0");

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);
        List<Object> lot = codesLotApresRefus(s.lot());

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            a.assertThat(detail(r)).startsWith(CODE_INTROUVABLE).contains(A2);
            a.assertThat(lot).containsExactly("OLD-DAB");
        });
    }

    @Test
    void d1_codeDOffreInconnuDeLaDia_409_codeNomme_bordereauDuLotIntact() throws Exception {
        Scenario s = scenario("DIN");
        b.ligneOffre(s.offre(), 1, A1, "200", "0");
        b.ligneOffre(s.offre(), 2, A2, "50", "0");
        b.ligneOffre(s.offre(), 3, "Z9", "70", "0");

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);
        List<Object> lot = codesLotApresRefus(s.lot());

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            a.assertThat(detail(r)).startsWith(CODE_INTROUVABLE).contains("Z9");
            a.assertThat(lot).containsExactly("OLD-DIN");
        });
    }

    // ------------------------------------------------------------------ R3 : PU HT après remise, 6 décimales

    @Test
    void r3_puHTDuDefinitif_egalPuFoisUnMoinsRemise_aSixDecimales() throws Exception {
        Scenario s = scenario("PU6");
        b.ligneOffre(s.offre(), 1, A1, "123.456789", "0.125");
        b.ligneOffre(s.offre(), 2, A2, "50", "0");

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> lignes = b.lignes(LIGNES_LOT, s.lot().toString());
        assertThat(lignes).extracting(l -> l.get("Code")).containsExactly(A1, A2);
        assertThat((BigDecimal) lignes.getFirst().get("UnitPriceHT")).as("123,456789 × 0,875 = 108,024690375, juste à 6 décimales")
                .isCloseTo(new BigDecimal("108.024690"), within(new BigDecimal("0.000001")));
    }

    // ------------------------------------------------------------------ R4 : codes uniques

    @Test
    void r4_codeEnDoubleDansLaDia_409_message_bordereauDuLotIntact() throws Exception {
        Scenario s = scenario("DDI");
        b.ligne(s.dia(), s.g1(), 3, A1, "30", s.categorie());
        b.ligneOffre(s.offre(), 1, A1, "200", "0");
        b.ligneOffre(s.offre(), 2, A2, "50", "0");

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);
        List<Object> lot = codesLotApresRefus(s.lot());

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            a.assertThat(r.corps()).contains(A1_DOUBLON);
            a.assertThat(lot).containsExactly("OLD-DDI");
        });
    }

    @Test
    void r4_codeEnDoubleDansLOffre_409_message_bordereauDuLotIntact() throws Exception {
        Scenario s = scenario("DOF");
        b.ligneOffre(s.offre(), 1, A1, "200", "0");
        b.ligneOffre(s.offre(), 2, A2, "50", "0");
        b.ligneOffre(s.offre(), 3, A1, "210", "0");

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);
        List<Object> lot = codesLotApresRefus(s.lot());

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            a.assertThat(r.corps()).contains(A1_DOUBLON);
            a.assertThat(lot).containsExactly("OLD-DOF");
        });
    }

    // ------------------------------------------------------------------ invariant : une seule offre sélectionnée par DIA

    @Test
    void invariant_selectionDUneOffreNonMaximale_uneSeuleOffreSelectionneeSurLaDia() throws Exception {
        UUID dia = demandes.demande(c, c.code("UNQ"), c.prep());
        b.lotDe(c, dia, "LUNQ-" + c.jeton(), false);
        UUID ancienne = b.offre(dia, achat.fournisseur("TUNQA-" + c.jeton(), "Fournisseur UNQA"), c.code("OUNQA"), true);
        achat.montants(ancienne, "900", "1000", "1000");
        UUID autre = b.offre(dia, achat.fournisseur("TUNQB-" + c.jeton(), "Fournisseur UNQB"), c.code("OUNQB"), false);
        achat.montants(autre, "700", "800", "800");
        UUID choisie = b.offre(dia, achat.fournisseur("TUNQC-" + c.jeton(), "Fournisseur UNQC"), c.code("OUNQC"), false);
        achat.montants(choisie, "500", "600", "600");

        Reponse r = appels.poster(qLibre, dia, "/offres/" + choisie + "/selection", corps());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(b.compter(SELECTIONNEES, dia.toString())).isEqualTo(1);
            a.assertThat(b.lignes(OFFRE, choisie.toString()).getFirst()).containsEntry("IsSelected", true);
            a.assertThat(b.lignes(OFFRE, ancienne.toString()).getFirst()).containsEntry("IsSelected", false);
        });
    }
}

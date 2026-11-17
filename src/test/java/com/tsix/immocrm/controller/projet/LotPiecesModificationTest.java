package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_MODIFICATION;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_REGLEMENTS;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_SUPPRESSION;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.NOTES;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.PENALITE_RG;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.decimal;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.idCree;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.saisie;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.DATE_PIECE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_ADVANCE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_PROPORTION;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_WARRANTY;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.WORKINGOUT_ADVANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Modification et suppression des pièces à payer du lot (FEATURE-lot-pieces-a-payer.md R8 bis, R8 ter, R10, R11), lot cité
 * (R14) et prévisions recalculées (R15).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPiecesModificationTest {

    private static final String TTC = "AmountTTC";
    private static final String CENT = "100";
    private static final String MODIFIABLE_FAUX = "\"modifiable\":false";
    private static final String LOT_CITE = "Ce lot est utilisé ailleurs (décomptes, achats…) et ne peut pas être supprimé.";
    private static final LocalDate AVRIL = LocalDate.of(2026, 4, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureLotSousFenetres sf;

    private PiecesLotSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        c = pf.cadre("PM");
        appels = new PiecesLotSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "pm.admin"), "Administrateur");
    }

    private String code() {
        rang++;
        return "PM" + rang;
    }

    private UUID lot(UUID statut) {
        return pf.lot(c, code(), statut);
    }

    private UUID piece(UUID lot, String nature, UUID statut) {
        return pf.piece(c, lot, nature, code(), CENT, statut);
    }

    /** Corps de modification : 1er avril, échéance 1er mai, 600, notes « Modifiée ». */
    private static CorpsJson modification(String nature) {
        return saisie(nature, AVRIL, AVRIL.plusMonths(1), "600").avec(NOTES, "Modifiée");
    }

    private static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : new BigDecimal(v.toString());
    }

    private Reponse creerOk(UUID lot, CorpsJson corps) throws Exception {
        Reponse r = appels.creer(admin, lot, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return r;
    }

    // ------------------------------------------------------------------ R10 modification

    // R10 : AVA en cours d'un lot libre → 200, date / échéance / montant / notes modifiés, lignes recalculées (R7).
    @Test
    void r10_lotLibre_200_champsModifies_lignesRecalculees() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());

        Reponse r = appels.modifier(admin, lot, ava, modification(MARKET_ADVANCE));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.date")).isEqualTo(AVRIL.toString());
            s.assertThat(r.valeur("$.echeance")).isEqualTo(AVRIL.plusMonths(1).toString());
            s.assertThat(r.nombre("$.montantTTC")).isEqualByComparingTo("600");
            s.assertThat(r.valeur("$.notes")).isEqualTo("Modifiée");
            s.assertThat(montant(pf.pieceEnBase(ava), TTC)).isEqualByComparingTo("600");
            s.assertThat(pf.lignes(ava)).singleElement()
                    .satisfies(l -> assertThat(montant(l, "ttc")).isCloseTo(new BigDecimal("600"), within(new BigDecimal("0.00001"))));
        });
    }

    // R10 : drapeau de statut du lot exigé par la nature absent → 409 texte Arpent, pièce inchangée, fiche non modifiable.
    @Test
    void r10_drapeauDuLotAbsent_409_avaWavaRrg() throws Exception {
        Map<String, UUID> statutSansSonDrapeau = Map.of(MARKET_ADVANCE, c.statutAvanceDecompte(),
                WORKINGOUT_ADVANCE, c.statutAvanceMarche(), MARKET_WARRANTY, c.statutAvanceDecompte());
        SoftAssertions s = new SoftAssertions();

        for (Map.Entry<String, UUID> e : statutSansSonDrapeau.entrySet()) {
            UUID lot = lot(e.getValue());
            UUID p = piece(lot, e.getKey(), c.enCours());
            Reponse r = appels.modifier(admin, lot, p, modification(e.getKey()));
            Reponse fiche = appels.lire(admin, lot, p);
            s.assertThat(r.statut()).as(e.getKey() + " " + r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).as(e.getKey()).isEqualTo(MSG_MODIFICATION);
            s.assertThat(montant(pf.pieceEnBase(p), TTC)).as(e.getKey() + " inchangée").isEqualByComparingTo(CENT);
            s.assertThat(fiche.corps()).as(e.getKey() + " fiche").contains(MODIFIABLE_FAUX);
        }
        s.assertAll();
    }

    // R10 : RCP n'exige aucun drapeau → 200 sur un lot sans drapeau.
    @Test
    void r10_rcp_lotSansDrapeau_200() throws Exception {
        UUID lot = lot(c.statutSansDrapeau());
        UUID rcp = piece(lot, MARKET_PROPORTION, c.enCours());

        Reponse r = appels.modifier(admin, lot, rcp, modification(MARKET_PROPORTION));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(montant(pf.pieceEnBase(rcp), TTC)).isEqualByComparingTo("600");
        });
    }

    // R10 : statut de pièce en lecture seule → 409 texte Arpent, pièce inchangée, fiche non modifiable.
    @Test
    void r10_statutLectureSeule_409() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = piece(lot, MARKET_ADVANCE, c.verrouille());

        Reponse r = appels.modifier(admin, lot, ava, modification(MARKET_ADVANCE));
        Reponse fiche = appels.lire(admin, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_MODIFICATION);
            s.assertThat(montant(pf.pieceEnBase(ava), TTC)).isEqualByComparingTo(CENT);
            s.assertThat(fiche.corps()).contains(MODIFIABLE_FAUX);
        });
    }

    // R8 ter / R10 : pénalité RG modifiable → AmountRegul = −pénalité.
    @Test
    void r8ter_modification_penaliteRrg_amountRegulNegatif() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID rrg = piece(lot, MARKET_WARRANTY, c.enCours());

        Reponse r = appels.modifier(admin, lot, rrg, modification(MARKET_WARRANTY).avec(PENALITE_RG, decimal("30")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(montant(pf.pieceEnBase(rrg), "AmountRegul")).isEqualByComparingTo("-30");
        });
    }

    // ------------------------------------------------------------------ R11 suppression

    // R11 : statut de pièce en lecture seule → 409 message OMRAN exact (+ statut), pièce conservée.
    @Test
    void r11_statutLectureSeule_409_messageExact() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = piece(lot, MARKET_ADVANCE, c.verrouille());

        Reponse r = appels.supprimer(admin, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_SUPPRESSION + "VALIDE DT-" + c.jeton());
            s.assertThat(pf.existe(ava)).isEqualTo(1);
        });
    }

    // R11 : règlement lié → 409 message existant, pièce conservée.
    @Test
    void r11_reglementsLies_409() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());
        pf.reglement(ava, code(), "40");

        Reponse r = appels.supprimer(admin, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_REGLEMENTS);
            s.assertThat(pf.existe(ava)).isEqualTo(1);
        });
    }

    // R11 : pièce créée puis supprimée → 204 ; pièce, lignes et historique de statut supprimés ; relecture 404.
    @Test
    void r11_suppression_204_pieceLignesHistoriqueSupprimes() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = idCree(creerOk(lot, saisie(MARKET_ADVANCE, DATE_PIECE, null, CENT)));

        Reponse r = appels.supprimer(admin, lot, ava);
        Reponse relecture = appels.lire(admin, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(pf.existe(ava)).isZero();
            s.assertThat(pf.nombreDeLignes(ava)).isZero();
            s.assertThat(pf.historique(ava)).isEmpty();
            s.assertThat(relecture.statut()).isEqualTo(404);
        });
    }

    // R8 bis : suppression d'une W.AVA rattachée → reste du décompte recalculé : W.AVA restantes (100) − autres décomptes (40) = 60.
    @Test
    void r8bis_suppression_resteDuDecompteRecalcule() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.decompte(c, lot, 1, true, null, "40");
        UUID ouvert = pf.decompte(c, lot, 2, false, null, null);
        pf.piece(c, lot, WORKINGOUT_ADVANCE, code(), CENT, c.enCours());
        UUID wava = idCree(creerOk(lot, saisie(WORKINGOUT_ADVANCE, DATE_PIECE, null, "250")));

        Reponse r = appels.supprimer(admin, lot, wava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(montant(pf.decompteEnBase(ouvert), "reste")).isEqualByComparingTo("60");
        });
    }

    // ------------------------------------------------------------------ R14 lot cité

    // R14 : lot cité par un avenant, une échéance de RG ou une réception → 409 LOT_CITE, lot conservé ; témoin : lot libre → 204.
    @Test
    void r14_lotCiteParAvenantEcheanceReception_409() throws Exception {
        UUID libre = pf.lotSansVentilation(c, code(), c.statutLibre());
        UUID avecAvenant = pf.lotSansVentilation(c, code(), c.statutLibre());
        sf.avenant(avecAvenant, 1, code(), DATE_PIECE, CENT, "20", "120", "120");
        UUID avecEcheance = pf.lotSansVentilation(c, code(), c.statutLibre());
        sf.echeanceRg(avecEcheance, 1, "Échéance", 30, DATE_PIECE, "1");
        UUID avecReception = pf.lotSansVentilation(c, code(), c.statutLibre());
        sf.reception(avecReception, 1, code(), DATE_PIECE, false, "Chantier", sf.typeReception(code()),
                sf.genreReception(code(), true));
        LotsPariteSupport lots = new LotsPariteSupport(mockMvc);
        SoftAssertions s = new SoftAssertions();

        Reponse temoin = lots.supprimer(admin, libre);
        s.assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(204);
        for (UUID lot : List.of(avecAvenant, avecEcheance, avecReception)) {
            Reponse r = lots.supprimer(admin, lot);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(r.corps()).contains(LOT_CITE);
            s.assertThat(pf.compter("SELECT COUNT(*) FROM PrjMarket WHERE PKey = ?", lot.toString())).isEqualTo(1);
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R15 prévisions

    // R15 : création d'une pièce → prévisions du lot recalculées (la ligne périmée disparaît).
    @Test
    void r15_creation_previsionsRecalculees() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.previsionsCalculables(lot);
        assertThat(pf.libellesPrevisions(lot)).as("avant").contains(LIGNE_PERIMEE);

        creerOk(lot, saisie(MARKET_ADVANCE, DATE_PIECE, null, CENT));

        assertThat(pf.libellesPrevisions(lot)).isNotEmpty().doesNotContain(LIGNE_PERIMEE);
    }

    // R15 : modification d'une pièce → prévisions recalculées.
    @Test
    void r15_modification_previsionsRecalculees() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());
        pf.previsionsCalculables(lot);

        Reponse r = appels.modifier(admin, lot, ava, modification(MARKET_ADVANCE));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(pf.libellesPrevisions(lot)).isNotEmpty().doesNotContain(LIGNE_PERIMEE);
    }

    // R15 : suppression d'une pièce → prévisions recalculées.
    @Test
    void r15_suppression_previsionsRecalculees() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());
        pf.previsionsCalculables(lot);

        Reponse r = appels.supprimer(admin, lot, ava);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(pf.libellesPrevisions(lot)).isNotEmpty().doesNotContain(LIGNE_PERIMEE);
    }

    // R15 : suppression d'un avoir → prévisions recalculées.
    @Test
    void r15_suppressionAvoir_previsionsRecalculees() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID avoir = pf.avoir(c, lot, code(), c.enCours());
        pf.previsionsCalculables(lot);

        Reponse r = appels.supprimerAvoir(admin, lot, avoir);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(pf.libellesPrevisions(lot)).isNotEmpty().doesNotContain(LIGNE_PERIMEE);
    }
}

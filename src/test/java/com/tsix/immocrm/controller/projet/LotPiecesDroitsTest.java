package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
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

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.AVANCES;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.AVOIRS;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.DATE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.DROIT_AVANCE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.DROIT_SUPPRIMER_AVOIR;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_AVOIR;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_ETAT_AVA;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.REMBOURSEMENTS;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.saisie;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.DATE_PIECE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_ADVANCE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_PROPORTION;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_WARRANTY;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.WORKINGOUT_ADVANCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pièces à payer du lot et avoirs (FEATURE-lot-pieces-a-payer.md §3 codes de retour, R2, R12, R13, R17). Ordre : 404 lot → 404
 * pièce / avoir → 403 → 400 → 409. Toute attente 404 a un témoin 2xx / 403 / 400 dans le même test (route absente = 404 avant le code).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPiecesDroitsTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String TEMOIN = "témoin : ";
    private static final String MONTANT = "100";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;

    private PiecesLotSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        c = pf.cadre("PD");
        appels = new PiecesLotSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "pd.admin"), "Administrateur");
    }

    private String code() {
        rang++;
        return "PD" + rang;
    }

    private UUID lotLibre() {
        return pf.lot(c, code(), c.statutLibre());
    }

    private UUID piece(UUID lot, String nature, UUID statut) {
        return pf.piece(c, lot, nature, code(), MONTANT, statut);
    }

    /** Compte non Administrateur habilité sur le projet et la société, portant exactement ces droits. */
    private RequestPostProcessor avec(String... droits) {
        return connecte(pf.compte(c, "pd" + code(), List.of(droits)), COMMERCIAL);
    }

    private static CorpsJson corpsValide(String nature) {
        return saisie(nature, DATE_PIECE, null, MONTANT);
    }

    // ------------------------------------------------------------------ ordre 1 : 404 lot

    // Ordre 1 : lot inconnu → 404, même détail (LOT_INTROUVABLE) que la lecture existante des avances.
    @Test
    void ordre1_lotInconnu_404_memeDetailQueLaLecture() throws Exception {
        UUID inconnu = UUID.randomUUID();
        Reponse lecture = appels.lecture(admin, inconnu, AVANCES);

        Reponse creation = appels.creer(admin, inconnu, corpsValide(MARKET_ADVANCE));
        Reponse modele = appels.modele(admin, inconnu, MARKET_ADVANCE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(404);
            s.assertThat(detail(creation)).isNotBlank().isEqualTo(detail(lecture));
            s.assertThat(modele.statut()).as(modele.corps()).isEqualTo(404);
            s.assertThat(detail(modele)).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    // Ordre 1 avant 3 : lot d'un projet non habilité, compte sans droit → 404 ; témoin : 403 sur un lot visible.
    @Test
    void ordre1_lotHorsPerimetre_compteSansDroit_404AvantLe403() throws Exception {
        UUID invisible = pf.lotHorsPerimetre(c, code());
        UUID visible = lotLibre();
        RequestPostProcessor sansDroit = avec();

        Reponse temoin = appels.creer(sansDroit, visible, corpsValide(MARKET_ADVANCE));
        Reponse creation = appels.creer(sansDroit, invisible, corpsValide(MARKET_ADVANCE));
        Reponse modele = appels.modele(sansDroit, invisible, MARKET_ADVANCE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(403);
            s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(404);
            s.assertThat(modele.statut()).as(modele.corps()).isEqualTo(404);
            s.assertThat(pf.piecesDuLot(invisible, MARKET_ADVANCE)).isEmpty();
        });
    }

    // ------------------------------------------------------------------ ordre 2 : 404 pièce / avoir

    // Ordre 2 : pièce d'un autre lot → 404 en GET, PUT, DELETE ; témoin : GET sur son propre lot → 200.
    @Test
    void ordre2_pieceDUnAutreLot_404_getPutDelete() throws Exception {
        UUID lot = lotLibre();
        UUID autreLot = lotLibre();
        UUID etrangere = piece(autreLot, MARKET_ADVANCE, c.enCours());

        Reponse temoin = appels.lire(admin, autreLot, etrangere);
        Reponse lecture = appels.lire(admin, lot, etrangere);
        Reponse modification = appels.modifier(admin, lot, etrangere, corpsValide(MARKET_ADVANCE));
        Reponse suppression = appels.supprimer(admin, lot, etrangere);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(200);
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(404);
            s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(404);
            s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(404);
            s.assertThat(pf.existe(etrangere)).as("pièce étrangère intacte").isEqualTo(1);
        });
    }

    // Ordre 2 : pièce du lot hors des 4 natures (facture) ou id inconnu → 404 ; témoin : une AVA du lot → 200.
    @Test
    void ordre2_pieceHorsDesQuatreNaturesOuInconnue_404() throws Exception {
        UUID lot = lotLibre();
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());
        UUID facture = pf.facture(c, lot, code(), FixturePiecesLot.GESTION_FACTURE_DECOMPTE, "0", "0");

        Reponse temoin = appels.lire(admin, lot, ava);
        Reponse lecture = appels.lire(admin, lot, facture);
        Reponse suppression = appels.supprimer(admin, lot, facture);
        Reponse inconnue = appels.lire(admin, lot, UUID.randomUUID());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(200);
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(404);
            s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(404);
            s.assertThat(inconnue.statut()).as(inconnue.corps()).isEqualTo(404);
            s.assertThat(pf.existe(facture)).as("facture intacte").isEqualTo(1);
        });
    }

    // Ordre 2 avant 3 : pièce d'un autre lot, compte sans toolAdvance → 404 ; témoin : 403 sur son propre lot.
    @Test
    void ordre2_pieceDUnAutreLot_compteSansDroit_404AvantLe403() throws Exception {
        UUID lot = lotLibre();
        UUID autreLot = lotLibre();
        UUID etrangere = piece(autreLot, MARKET_ADVANCE, c.enCours());
        RequestPostProcessor sansDroit = avec();

        Reponse temoin = appels.supprimer(sansDroit, autreLot, etrangere);
        Reponse r = appels.supprimer(sansDroit, lot, etrangere);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(403);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        });
    }

    // Ordre 2 : avoir d'un autre lot, ou pièce qui n'est pas un REFUND → 404 ; témoin : l'avoir supprimé par son lot → 204.
    @Test
    void ordre2_avoirDUnAutreLotOuPasUnAvoir_404() throws Exception {
        UUID lot = lotLibre();
        UUID autreLot = lotLibre();
        UUID avoir = pf.avoir(c, autreLot, code(), c.enCours());
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());

        Reponse r = appels.supprimerAvoir(admin, lot, avoir);
        Reponse pasUnAvoir = appels.supprimerAvoir(admin, lot, ava);
        int avoirApresRefus = pf.existe(avoir);
        Reponse temoin = appels.supprimerAvoir(admin, autreLot, avoir);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(avoirApresRefus).as("avoir intact après le 404").isEqualTo(1);
            s.assertThat(pasUnAvoir.statut()).as(pasUnAvoir.corps()).isEqualTo(404);
            s.assertThat(pf.existe(ava)).as("AVA intacte").isEqualTo(1);
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(204);
        });
    }

    // ------------------------------------------------------------------ ordre 3 → 4 → 5

    // Ordre 3 avant 4 : corps invalide (date absente), compte sans toolAdvance → 403 ; témoin : Administrateur → 400.
    @Test
    void ordre3_corpsInvalide_sansToolAdvance_403AvantLe400() throws Exception {
        UUID lot = lotLibre();
        CorpsJson sansDate = corpsValide(MARKET_ADVANCE).avec(DATE, null);

        Reponse temoin = appels.creer(admin, lot, sansDate);
        Reponse r = appels.creer(avec(), lot, sansDate);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(400);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        });
    }

    // Ordre 4 avant 5 : lot sans drapeau avance marché, corps invalide → 400 ; témoin : corps valide → 409 R4.
    @Test
    void ordre4_corpsInvalide_lotSansDrapeau_400AvantLe409() throws Exception {
        UUID lot = pf.lot(c, code(), c.statutSansDrapeau());

        Reponse temoin = appels.creer(admin, lot, corpsValide(MARKET_ADVANCE));
        Reponse r = appels.creer(admin, lot, corpsValide(MARKET_ADVANCE).avec(DATE, null));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(TEMOIN + temoin.corps()).isEqualTo(409);
            s.assertThat(detail(temoin)).isEqualTo(MSG_ETAT_AVA);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        });
    }

    // ------------------------------------------------------------------ R2 droit toolAdvance

    // R2 : sans toolAdvance, AVA / W.AVA / RRG refusées (403) au POST et au modèle ; rien créé.
    @Test
    void r2_sansToolAdvance_avaWavaRrg_403_postEtModele() throws Exception {
        UUID lot = lotLibre();
        RequestPostProcessor sansDroit = avec();
        SoftAssertions s = new SoftAssertions();

        for (String nature : List.of(MARKET_ADVANCE, WORKINGOUT_ADVANCE, MARKET_WARRANTY)) {
            Reponse creation = appels.creer(sansDroit, lot, corpsValide(nature));
            Reponse modele = appels.modele(sansDroit, lot, nature);
            s.assertThat(creation.statut()).as(nature + " POST " + creation.corps()).isEqualTo(403);
            s.assertThat(modele.statut()).as(nature + " modèle " + modele.corps()).isEqualTo(403);
            s.assertThat(pf.piecesDuLot(lot, nature)).as(nature + " rien créé").isEmpty();
        }
        s.assertAll();
    }

    // R2 : avec toolAdvance seul, AVA créée (201).
    @Test
    void r2_avecToolAdvance_ava_201() throws Exception {
        UUID lot = lotLibre();

        Reponse r = appels.creer(avec(DROIT_AVANCE), lot, corpsValide(MARKET_ADVANCE));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(pf.piecesDuLot(lot, MARKET_ADVANCE)).hasSize(1);
        });
    }

    // R2 : le prorata (RCP) n'a pas de droit propre → compte sans aucun droit : modèle 200, création 201.
    @Test
    void r2_rcp_sansDroit_201() throws Exception {
        UUID lot = lotLibre();
        RequestPostProcessor sansDroit = avec();

        Reponse modele = appels.modele(sansDroit, lot, MARKET_PROPORTION);
        Reponse r = appels.creer(sansDroit, lot, corpsValide(MARKET_PROPORTION));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(modele.statut()).as(modele.corps()).isEqualTo(200);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(pf.piecesDuLot(lot, MARKET_PROPORTION)).hasSize(1);
        });
    }

    // R2 : supprimer une AVA / W.AVA / RRG sans toolAdvance → 403, pièce conservée ; une RCP → 204.
    @Test
    void r2_supprimer_sansToolAdvance_403_saufRcp() throws Exception {
        UUID lot = lotLibre();
        RequestPostProcessor sansDroit = avec();
        UUID rcp = piece(lot, MARKET_PROPORTION, c.enCours());
        SoftAssertions s = new SoftAssertions();

        for (String nature : List.of(MARKET_ADVANCE, WORKINGOUT_ADVANCE, MARKET_WARRANTY)) {
            UUID p = piece(lot, nature, c.enCours());
            Reponse r = appels.supprimer(sansDroit, lot, p);
            s.assertThat(r.statut()).as(nature + " " + r.corps()).isEqualTo(403);
            s.assertThat(pf.existe(p)).as(nature + " conservée").isEqualTo(1);
        }
        Reponse r = appels.supprimer(sansDroit, lot, rcp);
        s.assertThat(r.statut()).as("RCP " + r.corps()).isEqualTo(204);
        s.assertThat(pf.existe(rcp)).as("RCP supprimée").isZero();
        s.assertAll();
    }

    // R2 : supprimer une AVA avec toolAdvance seul → 204.
    @Test
    void r2_supprimer_avecToolAdvance_204() throws Exception {
        UUID lot = lotLibre();
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());

        Reponse r = appels.supprimer(avec(DROIT_AVANCE), lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(pf.existe(ava)).isZero();
        });
    }

    // R2 : modifier n'a pas de droit propre → compte sans aucun droit, AVA d'un lot libre en cours : 200.
    @Test
    void r2_modifier_sansDroit_200() throws Exception {
        UUID lot = lotLibre();
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());

        Reponse r = appels.modifier(avec(), lot, ava, saisie(MARKET_ADVANCE, DATE_PIECE, null, "250"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ R12 / R13 avoirs

    // R12 : supprimer un avoir sans RefundList.toolDelete → 403, avoir conservé (toolAdvance ne suffit pas).
    @Test
    void r12_avoir_sansToolDelete_403() throws Exception {
        UUID lot = lotLibre();
        UUID avoir = pf.avoir(c, lot, code(), c.enCours());

        Reponse r = appels.supprimerAvoir(avec(DROIT_AVANCE), lot, avoir);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(pf.existe(avoir)).isEqualTo(1);
        });
    }

    // R12 : avec RefundList.toolDelete seul, avoir en cours supprimé → 204.
    @Test
    void r12_avoir_avecToolDelete_204() throws Exception {
        UUID lot = lotLibre();
        UUID avoir = pf.avoir(c, lot, code(), c.enCours());

        Reponse r = appels.supprimerAvoir(avec(DROIT_SUPPRIMER_AVOIR), lot, avoir);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(pf.existe(avoir)).isZero();
        });
    }

    // R13 : avoir au statut lecture seule → 409 « … avec un statut <statut> », avoir conservé.
    @Test
    void r13_avoirVerrouille_409_messageExact() throws Exception {
        UUID lot = lotLibre();
        UUID avoir = pf.avoir(c, lot, code(), c.verrouille());

        Reponse r = appels.supprimerAvoir(admin, lot, avoir);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_AVOIR + "VALIDE DT-" + c.jeton());
            s.assertThat(pf.existe(avoir)).isEqualTo(1);
        });
    }

    // ------------------------------------------------------------------ R17 lectures cohérentes

    // R17 : droitPieces des avances et des remboursements = droit toolAdvance (R2).
    @Test
    void r17_droitPieces_suitToolAdvance() throws Exception {
        UUID lot = lotLibre();
        RequestPostProcessor avecDroit = avec(DROIT_AVANCE);
        RequestPostProcessor sansDroit = avec();

        Reponse avancesAvec = appels.lecture(avecDroit, lot, AVANCES);
        Reponse avancesSans = appels.lecture(sansDroit, lot, AVANCES);
        Reponse rembAvec = appels.lecture(avecDroit, lot, REMBOURSEMENTS);
        Reponse rembSans = appels.lecture(sansDroit, lot, REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avancesAvec.valeur("$.droitPieces")).as(avancesAvec.corps()).isEqualTo(true);
            s.assertThat(avancesSans.valeur("$.droitPieces")).as(avancesSans.corps()).isEqualTo(false);
            s.assertThat(rembAvec.valeur("$.droitPieces")).as(rembAvec.corps()).isEqualTo(true);
            s.assertThat(rembSans.valeur("$.droitPieces")).as(rembSans.corps()).isEqualTo(false);
        });
    }

    // R17 : AVA en cours d'un lot libre → modifiable et supprimable (liste et fiche), comme PUT 200 / DELETE 204.
    @Test
    void r17_avaLibre_modifiableEtSupprimable_listeEtFiche() throws Exception {
        UUID lot = lotLibre();
        UUID ava = piece(lot, MARKET_ADVANCE, c.enCours());

        Reponse liste = appels.lecture(admin, lot, AVANCES);
        Reponse fiche = appels.lire(admin, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.valeur("$.surMarche[0].modifiable")).as(liste.corps()).isEqualTo(true);
            s.assertThat(liste.valeur("$.surMarche[0].supprimable")).as(liste.corps()).isEqualTo(true);
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
            s.assertThat(fiche.corps()).contains("\"modifiable\":true", "\"supprimable\":true");
        });
    }

    // R17 : AVA au statut lecture seule → ni modifiable (R10) ni supprimable (R11), liste et fiche.
    @Test
    void r17_avaVerrouillee_niModifiableNiSupprimable() throws Exception {
        UUID lot = lotLibre();
        UUID ava = piece(lot, MARKET_ADVANCE, c.verrouille());

        Reponse liste = appels.lecture(admin, lot, AVANCES);
        Reponse fiche = appels.lire(admin, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.valeur("$.surMarche[0].modifiable")).as(liste.corps()).isEqualTo(false);
            s.assertThat(liste.valeur("$.surMarche[0].supprimable")).as(liste.corps()).isEqualTo(false);
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
            s.assertThat(fiche.corps()).contains("\"modifiable\":false", "\"supprimable\":false");
        });
    }

    // R17 : W.AVA d'un lot sans drapeau avance décompte → non modifiable (R10) ; sa suppression n'a pas de garde de lot.
    @Test
    void r17_wavaLotSansDrapeau_nonModifiable() throws Exception {
        UUID lot = pf.lot(c, code(), c.statutAvanceMarche());
        piece(lot, WORKINGOUT_ADVANCE, c.enCours());

        Reponse liste = appels.lecture(admin, lot, AVANCES);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.valeur("$.surDecompte[0].modifiable")).as(liste.corps()).isEqualTo(false);
            s.assertThat(liste.valeur("$.surDecompte[0].supprimable")).as(liste.corps()).isEqualTo(true);
        });
    }

    // R17 : compte sans toolAdvance → AVA non supprimable (R2), RCP supprimable (pas de droit) ; avec règlement → non supprimable (R11).
    @Test
    void r17_supprimable_suitDroitEtReglements() throws Exception {
        UUID lot = lotLibre();
        piece(lot, MARKET_ADVANCE, c.enCours());
        piece(lot, MARKET_PROPORTION, c.enCours());
        UUID autreLot = lotLibre();
        UUID regle = piece(autreLot, MARKET_ADVANCE, c.enCours());
        pf.reglement(regle, code(), "50");
        RequestPostProcessor sansDroit = avec();

        Reponse avances = appels.lecture(sansDroit, lot, AVANCES);
        Reponse remboursements = appels.lecture(sansDroit, lot, REMBOURSEMENTS);
        Reponse avancesRegle = appels.lecture(admin, autreLot, AVANCES);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avances.valeur("$.surMarche[0].supprimable")).as(avances.corps()).isEqualTo(false);
            s.assertThat(remboursements.valeur("$.remboursementsProrata[0].supprimable")).as(remboursements.corps())
                    .isEqualTo(true);
            s.assertThat(avancesRegle.valeur("$.surMarche[0].supprimable")).as(avancesRegle.corps()).isEqualTo(false);
        });
    }

    // R17 : avoir supprimable selon RefundList.toolDelete (R12) et le statut (R13).
    @Test
    void r17_avoirs_supprimable_suitDroitEtStatut() throws Exception {
        UUID lot = lotLibre();
        pf.avoir(c, lot, code(), c.enCours());
        UUID lotVerrouille = lotLibre();
        pf.avoir(c, lotVerrouille, code(), c.verrouille());

        Reponse avecDroit = appels.lecture(avec(DROIT_SUPPRIMER_AVOIR), lot, AVOIRS);
        Reponse sansDroit = appels.lecture(avec(), lot, AVOIRS);
        Reponse verrouille = appels.lecture(admin, lotVerrouille, AVOIRS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avecDroit.valeur("$[0].supprimable")).as(avecDroit.corps()).isEqualTo(true);
            s.assertThat(sansDroit.valeur("$[0].supprimable")).as(sansDroit.corps()).isEqualTo(false);
            s.assertThat(verrouille.valeur("$[0].supprimable")).as(verrouille.corps()).isEqualTo(false);
        });
    }
}

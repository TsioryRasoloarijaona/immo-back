package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.DATE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.ECHEANCE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_ETAT_AVA;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_ETAT_RRG;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_ETAT_WAVA;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_FOURNISSEUR;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.MSG_RG_BLOQUEE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.NATURE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.PENALITE_RG;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.decimal;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.idCree;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.saisie;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.DATE_PIECE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.DELAI_PAIEMENT;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.GESTIONS;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_ADVANCE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_PROPORTION;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_WARRANTY;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.WORKINGOUT_ADVANCE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Création des pièces à payer du lot (FEATURE-lot-pieces-a-payer.md R1, R3-R9, statut initial) : modèle (GET …/modele) et POST.
 * Montants attendus calculés à la main depuis les fixtures (devis, factures, règlements, décomptes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPiecesCreationTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
    private static final String TTC = "AmountTTC";
    private static final String PROJET = " Projet : ";
    private static final String MILLE = "1000";
    private static final String CENT = "100";
    private static final String MONTANT_MODELE = "$.montantTTC";
    private static final String MAXIMUM_MODELE = "$.montantMaximum";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private Clock clock;

    private PiecesLotSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        c = pf.cadre("PC");
        appels = new PiecesLotSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "pc.admin"), "Administrateur");
    }

    private String code() {
        rang++;
        return "PC" + rang;
    }

    private UUID lot(UUID statut) {
        return pf.lot(c, code(), statut);
    }

    private UUID piece(UUID lot, String nature, String ttc) {
        return pf.piece(c, lot, nature, code(), ttc, c.enCours());
    }

    private static CorpsJson corps(String nature, String montant) {
        return saisie(nature, DATE_PIECE, null, montant);
    }

    /** POST attendu 201 ; renvoie la pièce créée. */
    private Reponse creerOk(UUID lot, CorpsJson corps) throws Exception {
        Reponse r = appels.creer(admin, lot, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return r;
    }

    private Map<String, Object> enBase(Reponse r) {
        return pf.pieceEnBase(idCree(r));
    }

    private static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : new BigDecimal(v.toString());
    }

    private String notes(String prefixe, UUID lot) {
        return prefixe + " " + pf.descriptionLot(c, codeDuLot(lot)) + PROJET + c.projetDescription();
    }

    private final Map<UUID, String> codes = new java.util.HashMap<>();

    private UUID lotNomme(String code, UUID statut) {
        UUID lot = pf.lot(c, code, statut);
        codes.put(lot, code);
        return lot;
    }

    private String codeDuLot(UUID lot) {
        return codes.get(lot);
    }

    // ------------------------------------------------------------------ R1 natures, devise, dates

    // R1 : AVA → DOCUMENT_TO_PAY, ReferenceType, gestion OMRAN, module PRJ, société, projet, lot, fournisseur, devise du lot.
    @Test
    void r1_ava_colonnesDeLaPiece_deviseDuLot() throws Exception {
        UUID lot = lot(c.statutLibre());

        Map<String, Object> d = enBase(creerOk(lot, corps(MARKET_ADVANCE, MILLE)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(d).containsEntry("DocType", "DOCUMENT_TO_PAY").containsEntry("ReferenceType", MARKET_ADVANCE);
            s.assertThat(guid(d.get("StrEntityId"))).isEqualTo(guid(GESTIONS.get(MARKET_ADVANCE)));
            s.assertThat(guid(d.get("AdmModuleId"))).isEqualTo(guid(c.modulePrj()));
            s.assertThat(guid(d.get("CfgCompanyId"))).isEqualTo(guid(c.societe()));
            s.assertThat(guid(d.get("PrjMarketId"))).isEqualTo(guid(lot));
            s.assertThat(guid(d.get("CfgTierId"))).isEqualTo(guid(c.fournisseur()));
            s.assertThat(guid(d.get("StlCurrencyId"))).isEqualTo(guid(c.deviseLot()));
        });
    }

    // R1 : W.AVA → devise du lot ; RRG et RCP → devise par défaut ; gestion OMRAN de chaque nature.
    @Test
    void r1_devise_lotPourAvances_parDefautPourRemboursements() throws Exception {
        UUID lot = lot(c.statutLibre());
        SoftAssertions s = new SoftAssertions();

        for (String nature : List.of(WORKINGOUT_ADVANCE, MARKET_WARRANTY, MARKET_PROPORTION)) {
            Map<String, Object> d = enBase(creerOk(lot, corps(nature, CENT)));
            UUID devise = WORKINGOUT_ADVANCE.equals(nature) ? c.deviseLot() : c.deviseParDefaut();
            s.assertThat(guid(d.get("StlCurrencyId"))).as(nature).isEqualTo(guid(devise));
            s.assertThat(guid(d.get("StrEntityId"))).as(nature).isEqualTo(guid(GESTIONS.get(nature)));
            s.assertThat(d).as(nature).containsEntry("ReferenceType", nature);
        }
        s.assertAll();
    }

    // R1 : échéance absente → date + PaymentDelay du lot (30 j) ; réponse = PieceLotDetailDto.
    @Test
    void r1_echeanceAbsente_dateplusDelaiDuLot_reponseDetail() throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, MILLE));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.nature")).isEqualTo(MARKET_ADVANCE);
            s.assertThat(r.valeur("$.date")).isEqualTo(DATE_PIECE.toString());
            s.assertThat(r.valeur("$.echeance")).isEqualTo(DATE_PIECE.plusDays(DELAI_PAIEMENT).toString());
            s.assertThat(r.nombre("$.montantTTC")).isEqualByComparingTo(MILLE);
            s.assertThat(r.valeur("$.fournisseur")).isEqualTo("Fournisseur " + c.jeton());
            s.assertThat(r.valeur("$.notes")).isEqualTo("Notes de test");
        });
    }

    // R1 : échéance fournie → conservée.
    @Test
    void r1_echeanceFournie_conservee() throws Exception {
        UUID lot = lot(c.statutLibre());
        LocalDate echeance = DATE_PIECE.plusDays(10);

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, MILLE).avec(ECHEANCE, echeance));

        assertThat(r.valeur("$.echeance")).isEqualTo(echeance.toString());
    }

    // R1 : modèle → date du jour (horloge Casablanca), échéance = jour + 30, nature, fournisseur du lot.
    @Test
    void r1_modele_dateDuJour_echeanceSelonDelai() throws Exception {
        UUID lot = lot(c.statutLibre());
        LocalDate aujourdhui = LocalDate.now(clock);

        Reponse m = appels.modele(admin, lot, MARKET_ADVANCE);

        assertThat(m.statut()).as(m.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(m.valeur("$.nature")).isEqualTo(MARKET_ADVANCE);
            s.assertThat(m.valeur("$.date")).isEqualTo(aujourdhui.toString());
            s.assertThat(m.valeur("$.echeance")).isEqualTo(aujourdhui.plusDays(DELAI_PAIEMENT).toString());
            s.assertThat(m.valeur("$.fournisseur")).isEqualTo("Fournisseur " + c.jeton());
        });
    }

    // Statut initial : statut « Nouveau » du workflow de la gestion (EN COURS), une ligne d'historique à ce statut.
    @Test
    void statutInitial_enCours_uneLigneDHistorique() throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, MILLE));
        UUID piece = idCree(r);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(pf.pieceEnBase(piece).get("BuyDocumentStatusId"))).isEqualTo(guid(c.enCours()));
            s.assertThat(pf.historique(piece)).singleElement()
                    .satisfies(h -> assertThat(guid(h.get("statut"))).isEqualTo(guid(c.enCours())));
        });
    }

    // ------------------------------------------------------------------ R3 fournisseur

    // R3 : lot mono-fournisseur sans fournisseur → 409 message OMRAN exact (modèle et POST), rien créé.
    @Test
    void r3_lotMonoSansFournisseur_409_messageExact() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.sansFournisseur(lot);

        Reponse modele = appels.modele(admin, lot, MARKET_PROPORTION);
        Reponse r = appels.creer(admin, lot, corps(MARKET_PROPORTION, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(modele.statut()).as(modele.corps()).isEqualTo(409);
            s.assertThat(detail(modele)).isEqualTo(MSG_FOURNISSEUR);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_FOURNISSEUR);
            s.assertThat(pf.piecesDuLot(lot, MARKET_PROPORTION)).isEmpty();
        });
    }

    // R3 : lot multi-fournisseur sans fournisseur → 201.
    @Test
    void r3_lotMultiSansFournisseur_201() throws Exception {
        UUID lot = pf.lotMultiSansFournisseur(c, code());

        creerOk(lot, corps(MARKET_ADVANCE, CENT));

        assertThat(pf.piecesDuLot(lot, MARKET_ADVANCE)).hasSize(1);
    }

    // R3 avant R4 : lot mono sans fournisseur ET sans drapeau → message fournisseur.
    @Test
    void r3_avantR4_messageFournisseur() throws Exception {
        UUID lot = lot(c.statutSansDrapeau());
        pf.sansFournisseur(lot);

        Reponse r = appels.creer(admin, lot, corps(MARKET_ADVANCE, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_FOURNISSEUR);
        });
    }

    // ------------------------------------------------------------------ R4 refus par nature

    // R4 : AVA sur un statut sans BoolField010 (008 seul) → 409, modèle et POST, rien créé.
    @Test
    void r4_ava_sansDrapeauAvanceMarche_409() throws Exception {
        assertRefusR4(lot(c.statutAvanceDecompte()), MARKET_ADVANCE, MSG_ETAT_AVA);
    }

    // R4 : W.AVA sur un statut sans BoolField008 (010 seul) → 409, modèle et POST, rien créé.
    @Test
    void r4_wava_sansDrapeauAvanceDecompte_409() throws Exception {
        assertRefusR4(lot(c.statutAvanceMarche()), WORKINGOUT_ADVANCE, MSG_ETAT_WAVA);
    }

    // R4 : RRG sur un statut sans BoolField010 (008 seul) → 409, modèle et POST, rien créé.
    @Test
    void r4_rrg_sansDrapeauAvanceMarche_409() throws Exception {
        assertRefusR4(lot(c.statutAvanceDecompte()), MARKET_WARRANTY, MSG_ETAT_RRG);
    }

    // R4 : RRG, restitution bloquée (BlokWarrantyRestitution = 1) sur un statut libre → 409, modèle et POST, rien créé.
    @Test
    void r4_rrg_restitutionBloquee_409() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.rgBloquee(lot);

        assertRefusR4(lot, MARKET_WARRANTY, MSG_RG_BLOQUEE);
    }

    // R4 : RRG, drapeau absent ET bloquée → le message d'état d'abord.
    @Test
    void r4_rrg_drapeauAvantBlocage() throws Exception {
        UUID lot = lot(c.statutAvanceDecompte());
        pf.rgBloquee(lot);

        Reponse r = appels.creer(admin, lot, corps(MARKET_WARRANTY, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(MSG_ETAT_RRG);
        });
    }

    private void assertRefusR4(UUID lot, String nature, String message) throws Exception {
        Reponse modele = appels.modele(admin, lot, nature);
        Reponse r = appels.creer(admin, lot, corps(nature, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(modele.statut()).as(modele.corps()).isEqualTo(409);
            s.assertThat(detail(modele)).isEqualTo(message);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).isEqualTo(message);
            s.assertThat(pf.piecesDuLot(lot, nature)).isEmpty();
        });
    }

    // R4 : chaque nature lit son propre drapeau — AVA passe avec 010 seul, W.AVA avec 008 seul.
    @Test
    void r4_drapeauxCroises_201() throws Exception {
        UUID lotMarche = lot(c.statutAvanceMarche());
        UUID lotDecompte = lot(c.statutAvanceDecompte());

        Reponse ava = appels.creer(admin, lotMarche, corps(MARKET_ADVANCE, CENT));
        Reponse wava = appels.creer(admin, lotDecompte, corps(WORKINGOUT_ADVANCE, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ava.statut()).as(ava.corps()).isEqualTo(201);
            s.assertThat(wava.statut()).as(wava.corps()).isEqualTo(201);
        });
    }

    // R4 : RCP n'a ni refus de statut ni blocage → 201 sur un statut sans drapeau, RG bloquée.
    @Test
    void r4_rcp_aucunRefus_201() throws Exception {
        UUID lot = lot(c.statutSansDrapeau());
        pf.rgBloquee(lot);

        creerOk(lot, corps(MARKET_PROPORTION, CENT));

        assertThat(pf.piecesDuLot(lot, MARKET_PROPORTION)).hasSize(1);
    }

    // ------------------------------------------------------------------ R5 DocRef

    // R5 : AVA n + 1, n = toutes les MARKET_ADVANCE du lot quel que soit leur statut ; un autre lot ne compte pas.
    @Test
    void r5_ava_numeroteeSansFiltreDeStatut() throws Exception {
        UUID lot = lot(c.statutLibre());
        piece(lot, MARKET_ADVANCE, CENT);
        pf.piece(c, lot, MARKET_ADVANCE, code(), CENT, c.verrouille());
        piece(lot(c.statutLibre()), MARKET_ADVANCE, CENT);

        Reponse modele = appels.modele(admin, lot, MARKET_ADVANCE);
        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(modele.valeur("$.reference")).as(modele.corps()).isEqualTo("AVA3");
            s.assertThat(r.valeur("$.reference")).isEqualTo("AVA3");
            s.assertThat(enBase(r)).containsEntry("DocRef", "AVA3");
        });
    }

    // R5 : lot sans pièce → AVA1 ; le code (séquence « Document à régler entrant ») est renseigné.
    @Test
    void r5_premiereAva_ava1_codeRenseigne() throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, CENT));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(enBase(r)).containsEntry("DocRef", "AVA1");
            s.assertThat(String.valueOf(r.valeur("$.code"))).isNotBlank().isNotEqualTo("null");
        });
    }

    // R5 : lot multi-fournisseur → n compte les ORDER_ADVANCE (pas les MARKET_ADVANCE).
    @Test
    void r5_ava_lotMulti_compteLesOrderAdvance() throws Exception {
        UUID lot = pf.lotMultiSansFournisseur(c, code());
        piece(lot, "ORDER_ADVANCE", CENT);
        piece(lot, "ORDER_ADVANCE", CENT);
        piece(lot, MARKET_ADVANCE, CENT);

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, CENT));

        assertThat(enBase(r)).containsEntry("DocRef", "AVA3");
    }

    // R5 : W.AVA n + 1, n = WORKINGOUT_ADVANCE du lot.
    @Test
    void r5_wava_numerotee() throws Exception {
        UUID lot = lot(c.statutLibre());
        piece(lot, WORKINGOUT_ADVANCE, CENT);

        Reponse r = creerOk(lot, corps(WORKINGOUT_ADVANCE, CENT));

        assertThat(enBase(r)).containsEntry("DocRef", "W.AVA2");
    }

    // R5 : RRG n + 1, n = pièces du lot de StrEntityId B602 ou A71B4F91 (multi-fournisseur), quelle que soit leur nature.
    @Test
    void r5_rrg_compteLesDeuxGestions() throws Exception {
        UUID lot = lot(c.statutLibre());
        piece(lot, MARKET_WARRANTY, CENT);
        UUID multi = piece(lot, "ORDER_WARRANTY", CENT);
        pf.entite(FixturePiecesLot.GESTION_RRG_MULTI, "PrjDocumentDebitOrderWarrantyMultiSupplier");
        pf.gestion(multi, FixturePiecesLot.GESTION_RRG_MULTI);
        UUID autreGestion = piece(lot, MARKET_WARRANTY, CENT);
        pf.gestion(autreGestion, FixturePiecesLot.GESTION_AVA);

        Reponse r = creerOk(lot, corps(MARKET_WARRANTY, CENT));

        assertThat(enBase(r)).containsEntry("DocRef", "RRG3");
    }

    // R5 : RCP sans numéro, même avec une RCP existante.
    @Test
    void r5_rcp_sansNumero() throws Exception {
        UUID lot = lot(c.statutLibre());
        piece(lot, MARKET_PROPORTION, CENT);

        Reponse r = creerOk(lot, corps(MARKET_PROPORTION, CENT));

        assertThat(enBase(r)).containsEntry("DocRef", "RCP");
    }

    // ------------------------------------------------------------------ R6 montant par défaut

    // Lot à avance 10 % d'un devis retenu de 10 000 TTC : avance prévue 1 000.
    private UUID lotAvance1000() {
        UUID lot = lot(c.statutLibre());
        pf.tauxAvance(lot, "0.1");
        pf.devisRetenu(c, lot, "8000", "10000");
        return lot;
    }

    // R6 AVA : min(prévue − réglée, prévue − validée), plancher 0 ; montantMaximum = même reste.
    @Test
    void r6_ava_minDesDeuxRestes_plancherZero() throws Exception {
        UUID sansAvance = lotAvance1000();
        UUID valideeLimite = lotAvance1000();
        UUID regleeLimite = lotAvance1000();
        UUID depassee = lotAvance1000();
        pf.reglement(piece(valideeLimite, MARKET_ADVANCE, "300"), code(), CENT);
        pf.reglement(piece(regleeLimite, MARKET_ADVANCE, "300"), code(), "500");
        piece(depassee, MARKET_ADVANCE, "1500");
        Map<UUID, String> attendus = Map.of(sansAvance, MILLE, valideeLimite, "700", regleeLimite, "500", depassee, "0");
        SoftAssertions s = new SoftAssertions();

        for (Map.Entry<UUID, String> e : attendus.entrySet()) {
            Reponse m = appels.modele(admin, e.getKey(), MARKET_ADVANCE);
            s.assertThat(m.statut()).as(m.corps()).isEqualTo(200);
            if (m.statut() == 200) {
                s.assertThat(m.nombre(MONTANT_MODELE)).as("défaut " + e.getValue()).isEqualByComparingTo(e.getValue());
                s.assertThat(m.nombre(MAXIMUM_MODELE)).as("maximum " + e.getValue()).isEqualByComparingTo(e.getValue());
            }
        }
        s.assertAll();
    }

    // R6 : POST sans montant → montant par défaut (même calcul que le modèle).
    @Test
    void r6_ava_postSansMontant_montantParDefaut() throws Exception {
        UUID lot = lotAvance1000();
        pf.reglement(piece(lot, MARKET_ADVANCE, "300"), code(), CENT);

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, null));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.montantTTC")).isEqualByComparingTo("700");
            s.assertThat(montant(enBase(r), TTC)).isEqualByComparingTo("700");
        });
    }

    // R6 W.AVA : 0.
    @Test
    void r6_wava_zero() throws Exception {
        UUID lot = lot(c.statutLibre());
        piece(lot, WORKINGOUT_ADVANCE, CENT);

        Reponse m = appels.modele(admin, lot, WORKINGOUT_ADVANCE);

        assertThat(m.statut()).as(m.corps()).isEqualTo(200);
        assertThat(m.nombre(MONTANT_MODELE)).isEqualByComparingTo("0");
    }

    // R6 RRG : RG des factures (500) + RG des avenants facturés (200) − RG déjà validée (150) = 550.
    @Test
    void r6_rrg_resteDeRgValidee() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.facture(c, lot, code(), FixturePiecesLot.GESTION_FACTURE_DECOMPTE, "500", "0");
        pf.avenantFacture(c, lot, "200");
        piece(lot, MARKET_WARRANTY, "150");

        Reponse m = appels.modele(admin, lot, MARKET_WARRANTY);

        assertThat(m.statut()).as(m.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(m.nombre(MONTANT_MODELE)).isEqualByComparingTo("550");
            s.assertThat(m.nombre(MAXIMUM_MODELE)).isEqualByComparingTo("550");
        });
    }

    // R6 RCP : lot gestionnaire du prorata, prorata facturé sur le projet (300) − déjà remboursé (100) = 200.
    @Test
    void r6_rcp_lotGestionnaire_prorataFactureMoinsRembourse() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.lotProrataDuProjet(c, lot);
        pf.facture(c, lot(c.statutLibre()), code(), FixturePiecesLot.GESTION_FACTURE_DECOMPTE, "0", "300");
        piece(lot, MARKET_PROPORTION, CENT);

        Reponse m = appels.modele(admin, lot, MARKET_PROPORTION);

        assertThat(m.statut()).as(m.corps()).isEqualTo(200);
        assertThat(m.nombre(MONTANT_MODELE)).isEqualByComparingTo("200");
    }

    // R6 RCP : plancher 0 (lot non gestionnaire, RCP déjà saisie : OMRAN proposerait −100).
    @Test
    void r6_rcp_plancherZero() throws Exception {
        UUID lot = lot(c.statutLibre());
        piece(lot, MARKET_PROPORTION, CENT);

        Reponse m = appels.modele(admin, lot, MARKET_PROPORTION);

        assertThat(m.statut()).as(m.corps()).isEqualTo(200);
        assertThat(m.nombre(MONTANT_MODELE)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ R7 lignes

    // R7 : 2 catégories de ventilation 60 / 40, AVA de 1 200 → 2 lignes TTC 720 / 480, HT 600 / 400, qté 1, U, TVA du lot ;
    // Σ TTC des lignes = montant ; en-tête = Σ lignes.
    @Test
    void r7_deuxCategories_60_40_deuxLignes_enteteEgaleSommeDesLignes() throws Exception {
        UUID lot = pf.lotSansVentilation(c, code(), c.statutLibre());
        UUID soixante = pf.categorie("PCV6-" + c.jeton());
        UUID quarante = pf.categorie("PCV4-" + c.jeton());
        pf.ventilation(lot, soixante, "0.6");
        pf.ventilation(lot, quarante, "0.4");

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, "1200"));
        List<Map<String, Object>> lignes = pf.lignes(idCree(r));
        Map<String, Object> d = enBase(r);

        assertThat(lignes).hasSize(2);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).extracting(l -> guid(l.get("categorie"))).containsExactly(guid(soixante), guid(quarante));
            s.assertThat(montant(lignes.get(0), "ttc")).isCloseTo(new BigDecimal("720"), within(TOLERANCE));
            s.assertThat(montant(lignes.get(1), "ttc")).isCloseTo(new BigDecimal("480"), within(TOLERANCE));
            s.assertThat(montant(lignes.get(0), "ht")).isCloseTo(new BigDecimal("600"), within(TOLERANCE));
            s.assertThat(montant(lignes.get(1), "ht")).isCloseTo(new BigDecimal("400"), within(TOLERANCE));
            s.assertThat(lignes).isNotEmpty().allSatisfy(l -> {
                assertThat(montant(l, "Quantity")).isEqualByComparingTo("1");
                assertThat(montant(l, "VatRatio")).isEqualByComparingTo(FixturePiecesLot.TVA_LOT);
                assertThat(l).containsEntry("Unity", "U").containsEntry("Description", "Avance sur lot");
            });
            s.assertThat(montant(lignes.get(0), "ttc").add(montant(lignes.get(1), "ttc")))
                    .isCloseTo(new BigDecimal("1200"), within(TOLERANCE));
            s.assertThat(montant(d, TTC)).isCloseTo(new BigDecimal("1200"), within(TOLERANCE));
            s.assertThat(montant(d, "AmountHT")).isCloseTo(new BigDecimal(MILLE), within(TOLERANCE));
        });
    }

    // R7 : lot sans catégorie de ventilation → une seule ligne, sans catégorie, TTC = montant.
    @Test
    void r7_lotSansVentilation_uneLigneSansCategorie() throws Exception {
        UUID lot = pf.lotSansVentilation(c, code(), c.statutLibre());

        Reponse r = creerOk(lot, corps(MARKET_PROPORTION, "600"));

        assertThat(pf.lignes(idCree(r))).singleElement().satisfies(l -> SoftAssertions.assertSoftly(s -> {
            s.assertThat(l.get("categorie")).isNull();
            s.assertThat(montant(l, "ttc")).isCloseTo(new BigDecimal("600"), within(TOLERANCE));
            s.assertThat(l).containsEntry("Description", "Remboursement de compte prorata");
        }));
    }

    // R7 : libellé de ligne par nature.
    @Test
    void r7_libelleDeLigneParNature() throws Exception {
        UUID lot = lot(c.statutLibre());
        Map<String, String> libelles = Map.of(MARKET_ADVANCE, "Avance sur lot", WORKINGOUT_ADVANCE, "Avance sur décompte",
                MARKET_WARRANTY, "Remboursement de retenue de garantie", MARKET_PROPORTION, "Remboursement de compte prorata");
        SoftAssertions s = new SoftAssertions();

        for (Map.Entry<String, String> e : libelles.entrySet()) {
            List<Map<String, Object>> lignes = pf.lignes(idCree(creerOk(lot, corps(e.getKey(), CENT))));
            s.assertThat(lignes).as(e.getKey()).singleElement()
                    .satisfies(l -> assertThat(l).containsEntry("Description", e.getValue()));
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R8 notes par défaut

    // R8 : notes du modèle = libellé + Description du lot + « Projet : » + Description du projet.
    @Test
    void r8_notesParDefaut_descriptionsDuLotEtDuProjet() throws Exception {
        UUID lot = lotNomme(code(), c.statutLibre());
        Map<String, String> prefixes = Map.of(MARKET_ADVANCE, "Avance sur lot", WORKINGOUT_ADVANCE, "Avance sur Décompte du lot",
                MARKET_WARRANTY, "Remboursement de retenue de garantie", MARKET_PROPORTION, "Remboursement de compte prorata");
        SoftAssertions s = new SoftAssertions();

        for (Map.Entry<String, String> e : prefixes.entrySet()) {
            Reponse m = appels.modele(admin, lot, e.getKey());
            s.assertThat(m.statut()).as(e.getKey() + " " + m.corps()).isEqualTo(200);
            if (m.statut() == 200) {
                s.assertThat(m.valeur("$.notes")).as(e.getKey()).isEqualTo(notes(e.getValue(), lot));
            }
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R8 bis W.AVA rattachée

    // R8 bis : dernier décompte (rang 2) ouvert → PrjWorkingOutId = lui ; reste recalculé :
    // W.AVA du lot (100 + 250) − avance imputée aux autres décomptes (40) = 310.
    @Test
    void r8bis_rattacheeAuDernierDecompteOuvert_resteRecalcule() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.decompte(c, lot, 1, true, null, "40");
        UUID ouvert = pf.decompte(c, lot, 2, false, null, null);
        piece(lot, WORKINGOUT_ADVANCE, CENT);

        Reponse r = creerOk(lot, corps(WORKINGOUT_ADVANCE, "250"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(enBase(r).get("PrjWorkingOutId"))).isEqualTo(guid(ouvert));
            s.assertThat(montant(pf.decompteEnBase(ouvert), "reste")).isEqualByComparingTo("310");
        });
    }

    // R8 bis : dernier décompte final → non rattachée (pas de repli sur un décompte ouvert plus ancien).
    @Test
    void r8bis_dernierDecompteFinal_nonRattachee() throws Exception {
        UUID lot = lot(c.statutLibre());
        pf.decompte(c, lot, 1, false, null, null);
        pf.decompte(c, lot, 2, true, null, null);

        Reponse r = creerOk(lot, corps(WORKINGOUT_ADVANCE, CENT));

        assertThat(enBase(r)).containsEntry("PrjWorkingOutId", null);
    }

    // R8 bis : dernier décompte facturé → non rattachée.
    @Test
    void r8bis_dernierDecompteFacture_nonRattachee() throws Exception {
        UUID lot = lot(c.statutLibre());
        UUID facture = pf.facture(c, lot, code(), FixturePiecesLot.GESTION_FACTURE_DECOMPTE, "0", "0");
        pf.decompte(c, lot, 1, false, facture, null);

        Reponse r = creerOk(lot, corps(WORKINGOUT_ADVANCE, CENT));

        assertThat(enBase(r)).containsEntry("PrjWorkingOutId", null);
    }

    // ------------------------------------------------------------------ R8 ter pénalité RG

    // R8 ter : RRG avec pénalité 50 → AmountRegul = −50, pénalité relue dans la réponse.
    @Test
    void r8ter_penaliteRrg_amountRegulNegatif() throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = creerOk(lot, corps(MARKET_WARRANTY, "500").avec(PENALITE_RG, decimal("50")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(enBase(r), "AmountRegul")).isEqualByComparingTo("-50");
            s.assertThat(r.nombre("$.penaliteRg")).isEqualByComparingTo("50");
        });
    }

    // R8 ter : pénalité ignorée hors RRG (AVA) → AmountRegul nul ou 0.
    @Test
    void r8ter_penaliteIgnoreeHorsRrg() throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = creerOk(lot, corps(MARKET_ADVANCE, "500").avec(PENALITE_RG, decimal("50")));

        BigDecimal regul = montant(enBase(r), "AmountRegul");
        assertThat(regul == null ? BigDecimal.ZERO : regul).isZero();
    }

    // ------------------------------------------------------------------ R9 validation

    static Stream<Arguments> corpsInvalides() {
        return Stream.of(
                Arguments.of("date absente", corps(MARKET_ADVANCE, CENT).avec(DATE, null)),
                Arguments.of("montant négatif", corps(MARKET_ADVANCE, "-1")),
                Arguments.of("échéance avant la date", corps(MARKET_ADVANCE, CENT).avec(ECHEANCE, DATE_PIECE.minusDays(1))),
                Arguments.of("pénalité négative", corps(MARKET_WARRANTY, CENT).avec(PENALITE_RG, decimal("-1"))),
                Arguments.of("nature absente", corps(MARKET_ADVANCE, CENT).avec(NATURE, null)),
                Arguments.of("nature hors enum", corps(MARKET_ADVANCE, CENT).avec(NATURE, "FACTURE")));
    }

    // R9 : corps invalide → 400, rien créé.
    @ParameterizedTest(name = "{0}")
    @MethodSource("corpsInvalides")
    void r9_corpsInvalide_400_rienCree(String cas, CorpsJson corps) throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = appels.creer(admin, lot, corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(cas + " : " + r.corps()).isEqualTo(400);
            for (String nature : List.of(MARKET_ADVANCE, MARKET_WARRANTY)) {
                s.assertThat(pf.piecesDuLot(lot, nature)).as(cas).isEmpty();
            }
        });
    }

    // R9 : montant 0 accepté (≥ 0) ; échéance = date acceptée.
    @Test
    void r9_montantZeroEtEcheanceEgaleDate_201() throws Exception {
        UUID lot = lot(c.statutLibre());

        Reponse r = appels.creer(admin, lot, corps(MARKET_ADVANCE, "0").avec(ECHEANCE, DATE_PIECE));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }
}

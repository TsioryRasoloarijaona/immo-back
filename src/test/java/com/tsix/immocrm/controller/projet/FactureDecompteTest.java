package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureFactureDecompte;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
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

import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.CODE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.DETAIL;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.MSG_DEJA_FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.NET;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.factureId;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFactureDecompte.date;
import static com.tsix.immocrm.fixtures.FixtureFactureDecompte.docRef;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.DELAI_PAIEMENT;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.GESTION_FACTURE_DECOMPTE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.guid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Facture d'un décompte (FEATURE-lc8-facture-decompte.md R1, R2, D2, R4 refus métier, R5, R7, R8) :
 * {@code POST /api/projet/decomptes/{id}/facture}, lu en base et par la fiche décompte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FactureDecompteTest {

    private static final String NOTES = "Notes";
    private static final String PU = "UnitPriceHT";
    private static final String CENT = "100";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureFactureDecompte fd;

    private FactureDecompteSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fd.cadre("FD");
        appels = new FactureDecompteSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "fd.admin"), "Administrateur");
    }

    private UUID lot() {
        rang++;
        return pf.lot(c, "FD" + rang, c.statutLibre());
    }

    private Reponse facturer(UUID decompte) throws Exception {
        fd.flush();
        return appels.generer(admin, decompte, FACTURE);
    }

    private Reponse facturerOk(UUID decompte) throws Exception {
        Reponse r = facturer(decompte);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return r;
    }

    private static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : new BigDecimal(v.toString());
    }

    private static String jour(Object horodatage) {
        return String.valueOf(horodatage).substring(0, 10);
    }

    // ------------------------------------------------------------------ R1 création

    // R1 + Contrat : 201 FactureGenereeDto(factureId, code, netAPayer) = la pièce créée (code de la séquence « Facture fournisseur »).
    @Test
    void r1_201_factureGeneree_idCodeNetDeLaPiece() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 1);

        Reponse r = facturerOk(d);

        Map<String, Object> f = pf.pieceEnBase(factureId(r));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(CODE)).isEqualTo(f.get("Code"))
                    .isEqualTo(FixtureAchat.SEQUENCES_SAISIE.get(FixtureAchat.FACTURE).codeSuivant());
            s.assertThat(r.nombre(NET)).isEqualByComparingTo(montant(f, "AmountToPay"));
        });
    }

    // R1 : INVOICE, gestion PrjInvoiceDebitWorkingOut (B604), module PRJ, tiers, société, devise, lot, projet du lot ;
    // date = date du décompte ; échéance = date + PaymentDelay (30).
    @Test
    void r1_enTete_colonnesDuLotEtDuDecompte() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompteSimple(c, lot, 2);

        Map<String, Object> f = pf.pieceEnBase(factureId(facturerOk(d)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f).containsEntry("DocType", FixtureAchat.FACTURE);
            s.assertThat(guid(f.get("StrEntityId"))).isEqualTo(guid(GESTION_FACTURE_DECOMPTE));
            s.assertThat(guid(f.get("AdmModuleId"))).isEqualTo(guid(c.modulePrj()));
            s.assertThat(guid(f.get("CfgTierId"))).isEqualTo(guid(c.fournisseur()));
            s.assertThat(guid(f.get("CfgCompanyId"))).isEqualTo(guid(c.societe()));
            s.assertThat(guid(f.get("StlCurrencyId"))).isEqualTo(guid(c.deviseLot()));
            s.assertThat(guid(f.get("PrjMarketId"))).isEqualTo(guid(lot));
            s.assertThat(guid(f.get("PrjProjectId"))).isEqualTo(guid(c.projet()));
            s.assertThat(jour(f.get("DocDate"))).isEqualTo(date(2).toString());
            s.assertThat(jour(f.get("MaturityDate"))).isEqualTo(date(2).plusDays(DELAI_PAIEMENT).toString());
        });
    }

    // R1 (Invoice.cs l.794) : note « Décompte N° <DocRef> du jj/mm/aaaa », sans mention de stock ni d'avance s'ils sont nuls.
    @Test
    void r1_note_decompteNumeroDocRefDuJour() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 3);

        String notes = String.valueOf(pf.pieceEnBase(factureId(facturerOk(d))).get(NOTES));

        assertThat(notes).contains("Décompte N° " + docRef(c, 3) + " du 03/03/2026")
                .doesNotContain("La Valeur du stock est de").doesNotContain("Avance sur décompte à déduire");
    }

    // R1 (Invoice.cs l.798-806) : stock et avance sur décompte non nuls → mentionnés dans la note.
    @Test
    void r1_note_stockEtAvanceSurDecompte_siNonNuls() throws Exception {
        UUID d = fd.decompteComplet(c, lot(), 4);

        String notes = String.valueOf(pf.pieceEnBase(factureId(facturerOk(d))).get(NOTES));

        assertThat(notes).contains("Décompte N° " + docRef(c, 4) + " du 04/03/2026")
                .contains("La Valeur du stock est de").contains("Avance sur décompte à déduire");
    }

    // R1 : PrjWorkingOut.BuyDocumentId = la facture créée.
    @Test
    void r1_lienPose_buyDocumentIdDuDecompte() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 5);

        UUID facture = factureId(facturerOk(d));

        assertThat(fd.factureDuDecompte(d)).isEqualTo(guid(facture));
    }

    // R1 / Q7 : la fiche décompte renvoie ensuite la facture (DecompteDetailDto.facture).
    @Test
    void r1_ficheDecompte_porteLaFacture() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 6);
        Reponse cree = facturerOk(d);

        fd.flush();
        Reponse fiche = new AppelsProjet(mockMvc).lire(admin, AppelsProjet.DECOMPTES + "/" + d);

        assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fiche.valeur("$.facture.id")).isEqualTo(cree.valeur(FactureDecompteSupport.FACTURE_ID));
            s.assertThat(fiche.valeur("$.facture.code")).isEqualTo(cree.valeur(CODE));
        });
    }

    // ------------------------------------------------------------------ R2 lignes

    // R2 (Invoice.cs l.719-724) : lignes de même ligne de bordereau groupées, quantités sommées (2 + 3 = 5).
    @Test
    void r2_groupage_parLigneDeBordereau_quantitesSommees() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompte(c, lot, 7, Map.of());
        UUID marche = fd.ligneMarche(lot, 1, CENT);
        fd.ligne(d, marche, 1, "2", CENT);
        fd.ligne(d, marche, 2, "3", CENT);

        List<Map<String, Object>> lignes = pf.lignes(factureId(facturerOk(d)));

        assertThat(lignes).singleElement().satisfies(l -> SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, "Quantity")).isEqualByComparingTo("5");
            s.assertThat(montant(l, PU)).isEqualByComparingTo(CENT);
        }));
    }

    // R2 (Invoice.cs l.719-724) : groupe de quantité totale 0 ignoré.
    @Test
    void r2_quantiteZero_groupeIgnore() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompte(c, lot, 8, Map.of());
        fd.ligne(d, fd.ligneMarche(lot, 1, CENT), 1, "2", CENT);
        fd.ligne(d, fd.ligneMarche(lot, 2, "70"), 2, "0", "70");

        List<Map<String, Object>> lignes = pf.lignes(factureId(facturerOk(d)));

        assertThat(lignes).singleElement().satisfies(l -> assertThat(montant(l, PU)).isEqualByComparingTo(CENT));
    }

    // R2 (Invoice.cs l.771-790) : chaque extra du décompte recopié en ligne de facture.
    @Test
    void r2_extras_recopiesEnLignes() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompte(c, lot, 9, Map.of());
        fd.ligne(d, fd.ligneMarche(lot, 1, CENT), 1, "2", CENT);
        fd.extra(d, "3", "40");

        List<Map<String, Object>> lignes = pf.lignes(factureId(facturerOk(d)));

        assertThat(lignes).hasSize(2).anySatisfy(l -> SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, PU)).isEqualByComparingTo("40");
            s.assertThat(montant(l, "Quantity")).isEqualByComparingTo("3");
            s.assertThat(l).containsEntry("Description", "Extra");
        }));
    }

    // D2 : décompte sans ligne ni extra → facture créée (201), sans ligne.
    @Test
    void d2_sansLigneNiExtra_201_factureSansLigne() throws Exception {
        UUID d = fd.decompte(c, lot(), 10, Map.of(FixtureProjetLots.TTC, new BigDecimal("600"),
                FixtureProjetLots.A_PAYER, new BigDecimal("600")));

        UUID facture = factureId(facturerOk(d));

        assertThat(pf.nombreDeLignes(facture)).isZero();
    }

    // ------------------------------------------------------------------ R4 refus métier

    // R4 : décompte déjà facturé → 409, message exact, aucune nouvelle facture, lien inchangé.
    @Test
    void r4_dejaFacture_409_message_rienCree() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompteSimple(c, lot, 11);
        UUID existante = fd.dejaFacture(c, d);
        int avant = pf.compter("SELECT COUNT(*) FROM BuyDocument WHERE PrjMarketId = ?", lot.toString());

        Reponse r = facturer(d);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(MSG_DEJA_FACTURE);
            s.assertThat(pf.compter("SELECT COUNT(*) FROM BuyDocument WHERE PrjMarketId = ?", lot.toString())).isEqualTo(avant);
            s.assertThat(fd.factureDuDecompte(d)).isEqualTo(guid(existante));
        });
    }

    // R4 : décompte inconnu → 404.
    @Test
    void r4_decompteInconnu_404() throws Exception {
        Reponse r = facturer(UUID.randomUUID());

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // R4 : décompte d'un projet hors périmètre → 404, rien n'est posé.
    @Test
    void r4_horsPerimetre_404() throws Exception {
        UUID lot = pf.lotHorsPerimetre(c, "FDX");
        UUID d = fd.decompteSimple(c, lot, 12);

        Reponse r = facturer(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(fd.factureDuDecompte(d)).isNull();
        });
    }

    // R4 / Q4 : lot sans ventilation catégorie → 400, décompte non facturé.
    @Test
    void r4_lotSansVentilation_400() throws Exception {
        UUID lot = pf.lotSansVentilation(c, "FDV", c.statutLibre());
        UUID d = fd.decompteSimple(c, lot, 13);

        Reponse r = facturer(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(fd.factureDuDecompte(d)).isNull();
        });
    }

    // R4 ordre / Q4 : déjà facturé ET lot sans ventilation → 409 (409 avant 400).
    @Test
    void r4_ordre_dejaFactureEtSansVentilation_409() throws Exception {
        UUID lot = pf.lotSansVentilation(c, "FDW", c.statutLibre());
        UUID d = fd.decompteSimple(c, lot, 14);
        fd.dejaFacture(c, d);

        Reponse r = facturer(d);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
    }

    // ------------------------------------------------------------------ R5 statut de départ

    // R5 : statut de départ du workflow de la gestion B604 (EN COURS), une ligne d'historique à ce statut.
    @Test
    void r5_statutDeDepart_duWorkflowDeLaGestion() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 15);

        UUID facture = factureId(facturerOk(d));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(pf.pieceEnBase(facture).get("BuyDocumentStatusId"))).isEqualTo(guid(c.enCours()));
            s.assertThat(pf.historique(facture)).singleElement()
                    .satisfies(h -> assertThat(guid(h.get("statut"))).isEqualTo(guid(c.enCours())));
        });
    }

    // ------------------------------------------------------------------ R7 verrou

    // R7 : après la facture, suppression du décompte → 409, décompte conservé.
    @Test
    void r7_apresFacture_suppression409() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 16);
        facturerOk(d);

        fd.flush();
        Reponse r = appels.supprimer(admin, d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(pf.compter("SELECT COUNT(*) FROM PrjWorkingOut WHERE PKey = ?", d.toString())).isEqualTo(1);
        });
    }

    // R7 : après la facture, recalcul du décompte → 409.
    @Test
    void r7_apresFacture_recalcul409() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 17);
        facturerOk(d);

        fd.flush();
        Reponse r = appels.generer(admin, d, DecomptesPariteEcrituresTest.RECALCUL);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
    }

    // ------------------------------------------------------------------ R8 prévisions

    // R8 : après la facture, prévisions de paiement du lot recalculées (la ligne périmée disparaît).
    @Test
    void r8_apresFacture_previsionsRecalculees() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompteSimple(c, lot, 18);
        pf.previsionsCalculables(lot);
        assertThat(pf.libellesPrevisions(lot)).as("avant").contains(LIGNE_PERIMEE);

        facturerOk(d);

        assertThat(pf.libellesPrevisions(lot)).isNotEmpty().doesNotContain(LIGNE_PERIMEE);
    }
}

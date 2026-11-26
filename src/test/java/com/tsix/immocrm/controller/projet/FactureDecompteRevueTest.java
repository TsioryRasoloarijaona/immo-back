package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureFactureDecompte;
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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.AVOIR;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.CODE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.DETAIL;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.MSG_SANS_FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.NET;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.factureId;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFactureDecompte.GESTION_AVOIR;
import static com.tsix.immocrm.fixtures.FixtureFactureDecompte.NET_DECOMPTE_COMPLET;
import static com.tsix.immocrm.fixtures.FixtureFactureDecompte.RETENUES_RECOPIEES;
import static com.tsix.immocrm.fixtures.FixtureFactureDecompte.RETENUE_FORFAITAIRE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Montants (R3, règle 6) et avoir (R6) de FEATURE-lc8-facture-decompte.md : la facture recopie les retenues du décompte calculé,
 * son net = {@code PrjWorkingOut.AmountToPay} ; l'avoir clone la facture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FactureDecompteRevueTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");
    private static final String A_PAYER = "AmountToPay";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureFactureDecompte fd;

    private FactureDecompteSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fd.cadre("FR");
        appels = new FactureDecompteSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "fr.admin"), "Administrateur");
    }

    private UUID lot() {
        rang++;
        return pf.lot(c, "FR" + rang, c.statutLibre());
    }

    private Reponse generer(UUID decompte, String route) throws Exception {
        fd.flush();
        return appels.generer(admin, decompte, route);
    }

    private Reponse genererOk(UUID decompte, String route) throws Exception {
        Reponse r = generer(decompte, route);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return r;
    }

    private static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    // ------------------------------------------------------------------ R3 montants

    // R3 invariant : chaque composante ≠ 0 ; net de la facture (BuyDocument.AmountToPay et $.netAPayer)
    // = PrjWorkingOut.AmountToPay à ±0,02.
    @Test
    void r3_invariant_netFacture_egalNetDecompte() throws Exception {
        UUID d = fd.decompteComplet(c, lot(), 1);

        Reponse r = genererOk(d, FACTURE);

        BigDecimal netDecompte = montant(fd.decompteEnBase(d), A_PAYER);
        Map<String, Object> f = pf.pieceEnBase(factureId(r));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(netDecompte).isEqualByComparingTo(NET_DECOMPTE_COMPLET);
            s.assertThat(montant(f, A_PAYER)).isCloseTo(netDecompte, within(TOLERANCE));
            s.assertThat(r.nombre(NET)).isCloseTo(netDecompte, within(TOLERANCE));
        });
    }

    // R3 : retenues et montants recopiés du décompte (pas recalculés), colonne par colonne.
    @Test
    void r3_retenuesRecopiees_duDecompte() throws Exception {
        UUID d = fd.decompteComplet(c, lot(), 2);

        Map<String, Object> f = pf.pieceEnBase(factureId(genererOk(d, FACTURE)));

        Map<String, Object> w = fd.decompteEnBase(d);
        SoftAssertions s = new SoftAssertions();
        RETENUES_RECOPIEES.forEach((decompte, facture) -> s.assertThat(montant(f, facture)).as(facture)
                .isNotZero().isEqualByComparingTo(montant(w, decompte)));
        s.assertAll();
    }

    // R3 (WorkingOut.cs l.1439, l.2299) : BuyDocument.RetainedAmount = Σ PrjWorkingOutRetainedLine.Amount du décompte,
    // pas nonRefundableRetainer (18 ici).
    @Test
    void r3_retainedAmount_sommeDesRetenuesForfaitaires() throws Exception {
        UUID d = fd.decompteComplet(c, lot(), 7);

        Map<String, Object> f = pf.pieceEnBase(factureId(genererOk(d, FACTURE)));

        assertThat(montant(f, "RetainedAmount")).isEqualByComparingTo(RETENUE_FORFAITAIRE);
    }

    // ------------------------------------------------------------------ R6 avoir

    // R6 : avoir REFUND (gestion BuyRefundDebit), InvoiceSourceId = la facture, lignes clonées ;
    // réponse FactureGenereeDto = l'avoir (code « Avoir fournisseur », netAPayer = son AmountToPay).
    @Test
    void r6_avoir_201_sourceFacture_lignesClonees() throws Exception {
        UUID d = fd.decompteComplet(c, lot(), 3);
        UUID facture = factureId(genererOk(d, FACTURE));

        Reponse r = genererOk(d, AVOIR);

        UUID avoir = factureId(r);
        Map<String, Object> a = pf.pieceEnBase(avoir);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avoir).isNotEqualTo(facture);
            s.assertThat(a).containsEntry("DocType", FixtureAchat.AVOIR);
            s.assertThat(guid(a.get("InvoiceSourceId"))).isEqualTo(guid(facture));
            s.assertThat(guid(a.get("StrEntityId"))).isEqualTo(guid(GESTION_AVOIR));
            s.assertThat(pf.nombreDeLignes(avoir)).isEqualTo(pf.nombreDeLignes(facture)).isPositive();
            s.assertThat(r.valeur(CODE)).isEqualTo(a.get("Code"))
                    .isEqualTo(FixtureFactureDecompte.SEQUENCE_AVOIR.codeSuivant());
            s.assertThat(r.nombre(NET)).isEqualByComparingTo(montant(a, A_PAYER));
        });
    }

    // R6 : décompte non facturé → 409, message exact, aucun avoir.
    @Test
    void r6_sansFacture_409_message() throws Exception {
        UUID lot = lot();
        UUID d = fd.decompteSimple(c, lot, 4);

        Reponse r = generer(d, AVOIR);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(MSG_SANS_FACTURE);
            s.assertThat(pf.compter("SELECT COUNT(*) FROM BuyDocument WHERE PrjMarketId = ?", lot.toString())).isZero();
        });
    }

    // R6 : plusieurs avoirs autorisés sur la même facture.
    @Test
    void r6_deuxAvoirs_autorises() throws Exception {
        UUID d = fd.decompteSimple(c, lot(), 5);
        UUID facture = factureId(genererOk(d, FACTURE));

        UUID premier = factureId(genererOk(d, AVOIR));
        UUID second = factureId(genererOk(d, AVOIR));

        assertThat(second).isNotEqualTo(premier);
        assertThat(pf.compter("SELECT COUNT(*) FROM BuyDocument WHERE InvoiceSourceId = ?", facture.toString())).isEqualTo(2);
    }

    // R6 / R4 : décompte inconnu → 404.
    @Test
    void r6_decompteInconnu_404() throws Exception {
        Reponse r = generer(UUID.randomUUID(), AVOIR);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // R6 / R4 : décompte facturé d'un projet hors périmètre → 404.
    @Test
    void r6_horsPerimetre_404() throws Exception {
        UUID d = fd.decompteSimple(c, pf.lotHorsPerimetre(c, "FRX"), 6);
        fd.dejaFacture(c, d);

        Reponse r = generer(d, AVOIR);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }
}

package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Cadre;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.AFFECTATIONS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.BUY;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.CREDIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DEBIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.SENS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Affectations (FEATURE-n3-recettes-depenses.md) : une ligne = une {@code StlSettlementLine} ; R4 module, R5 {@code dateSur},
 * {@code sens}. Jeu : RA (PRJ, 300, ligne LA 300 → FA), RBUY (BUY, 70, ligne LBUY → FBUY),
 * RD (POINTED, pièce 10/01/2031, échéance 10/02/2031, pointage 10/03/2031, ligne LD 10 → FA).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AffectationsTest {

    private static final String DATE_SUR = "dateSur";
    private static final String DU = "du";
    private static final String AU = "au";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;
    private UUID fa;
    private UUID ra;
    private UUID la;
    private UUID lbuy;
    private UUID ld;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        Cadre c = fixture.cadre("TRF");
        fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(), c.modulePrj(),
                "1000");
        UUID fbuy = fixture.piece(c.code("-FBUY"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.moduleBuy(), "70");
        ra = fixture.reglement(Reglement.debit(c.code("-RA"), c, "300"));
        la = fixture.ligne(ra, fa, "300");
        UUID rbuy = fixture.reglement(Reglement.debit(c.code("-RBUY"), c, "70").module(c.moduleBuy()));
        lbuy = fixture.ligne(rbuy, fbuy, "70");
        UUID rd = fixture.reglement(Reglement.debit(c.code("-RD"), c, "10").statut(FixtureTresorerie.POINTE)
                .dates(LocalDate.of(2031, 1, 10), LocalDate.of(2031, 2, 10), LocalDate.of(2031, 3, 10)));
        ld = fixture.ligne(rd, fa, "10");
        a = connecte(fixture.compte(c, "TRFA", FixtureTresorerie.DROIT_AFFECTATIONS,
                FixtureTresorerie.DROIT_AFFECTATIONS_RECETTE), FixtureTresorerie.ROLE);
    }

    private Reponse debitPrj(String... params) throws Exception {
        String[] tous = new String[params.length + 4];
        tous[0] = MODULE;
        tous[1] = PRJ;
        tous[2] = SENS;
        tous[3] = DEBIT;
        System.arraycopy(params, 0, tous, 4, params.length);
        return appels.ok(a, AFFECTATIONS, tous);
    }

    @Test
    void ligne_porteReglementPieceLieeEtMontants() throws Exception {
        Map<String, Object> l = debitPrj().ligne(la);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(l).containsEntry("reglementId", ra.toString());
            s.assertThat(nombre(l, "montantAffecte")).isEqualByComparingTo("300");
            s.assertThat(nombre(l, "montantReglement")).isEqualByComparingTo("300");
            s.assertThat(String.valueOf((Object) JsonPath.read(l, "$.pieceLiee.id"))).isEqualTo(fa.toString());
        });
    }

    // ------------------------------------------------------------------ R4 module

    @Test
    void r4_affectationBuy_absenteSousPrj() throws Exception {
        assertThat(debitPrj().ids()).contains(la.toString()).doesNotContain(lbuy.toString());
    }

    @Test
    void r4_affectationPrj_absenteSousBuy() throws Exception {
        assertThat(appels.ok(a, AFFECTATIONS, MODULE, BUY, SENS, DEBIT).ids()).contains(lbuy.toString())
                .doesNotContain(la.toString());
    }

    // ------------------------------------------------------------------ R5 dateSur

    @Test
    void r5_dateSurPiece_filtreSurDatePiece() throws Exception {
        List<String> janvier = debitPrj(DATE_SUR, "PIECE", DU, "2031-01-01", AU, "2031-01-31").ids();
        List<String> mars = debitPrj(DATE_SUR, "PIECE", DU, "2031-03-01", AU, "2031-03-31").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(janvier).containsExactly(ld.toString());
            s.assertThat(mars).doesNotContain(ld.toString());
        });
    }

    @Test
    void r5_dateSurEcheance_filtreSurEcheance() throws Exception {
        List<String> fevrier = debitPrj(DATE_SUR, "ECHEANCE", DU, "2031-02-01", AU, "2031-02-28").ids();

        assertThat(fevrier).containsExactly(ld.toString());
    }

    @Test
    void r5_dateSurPointage_filtreSurDatePointage() throws Exception {
        List<String> mars = debitPrj(DATE_SUR, "POINTAGE", DU, "2031-03-01", AU, "2031-03-31").ids();
        List<String> fevrier = debitPrj(DATE_SUR, "POINTAGE", DU, "2031-02-01", AU, "2031-02-28").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mars).containsExactly(ld.toString());
            s.assertThat(fevrier).doesNotContain(ld.toString());
        });
    }

    // ------------------------------------------------------------------ sens

    @Test
    void sensCredit_neRendPasLesAffectationsDeDecaissement() throws Exception {
        List<String> credit = appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, CREDIT).ids();
        List<String> debit = debitPrj().ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(debit).as("témoin").contains(la.toString());
            s.assertThat(credit).doesNotContain(la.toString(), ld.toString());
        });
    }
}

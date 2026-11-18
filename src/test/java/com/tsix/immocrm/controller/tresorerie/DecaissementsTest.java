package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.domain.FuseauMetier;
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

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.BUY;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DECAISSEMENTS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Décaissements (FEATURE-n3-recettes-depenses.md) : R3 montants et reste à affecter, R4 module, R5 {@code dateSur} et
 * {@code sansProjet}, R6 {@code enRetard}. Jeu (une société, un projet habilité, FA au module PRJ, FBUY au module BUY) :
 * <pre>
 *   RA    montant 1010, base 1000, conversion 1/1, lignes 300 + 200 → FA     reste 500
 *   RCONV montant 100,  base 100,  conversion 2/1, ligne 50 → FA            reste 200 − 50 = 150
 *   RU0   montant 100,  base 100,  conversion 3/0 (unité 0 → ×1), sans ligne reste 100
 *   RNEG  montant −469 999,97 = base, sans ligne                             reste −469 999,97
 *   RBUY  module BUY, 70, ligne → FBUY
 *   RD    POINTED, pièce 10/01/2031, échéance 10/02/2031, pointage 10/03/2031, ligne 10 → FA
 *   RRET  échéance 15/01/2020, IN_WORK, non pointé ; RPTE échéance 15/01/2020, POINTED le 20/01/2020 ;
 *   RAUJ  échéance aujourd'hui, IN_WORK, non pointé ; les autres : échéance 2078
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementsTest {

    private static final String DATE_SUR = "dateSur";
    private static final String DU = "du";
    private static final String AU = "au";
    private static final String EN_RETARD = "enRetard";
    private static final String RESTE = "resteAAffecter";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;
    private UUID ra;
    private UUID rconv;
    private UUID ru0;
    private UUID rneg;
    private UUID rbuy;
    private UUID rd;
    private UUID rret;
    private UUID rpte;
    private UUID raujourdhui;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        Cadre c = fixture.cadre("TRD");
        UUID fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "5000");
        UUID fbuy = fixture.piece(c.code("-FBUY"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.moduleBuy(), "70");

        ra = fixture.reglement(Reglement.debit(c.code("-RA"), c, "1010").base("1000"));
        fixture.ligne(ra, fa, "300");
        fixture.ligne(ra, fa, "200");
        rconv = fixture.reglement(Reglement.debit(c.code("-RCONV"), c, "100").conversion("2", 1));
        fixture.ligne(rconv, fa, "50");
        ru0 = fixture.reglement(Reglement.debit(c.code("-RU0"), c, "100").conversion("3", 0));
        rneg = fixture.reglement(Reglement.debit(c.code("-RNEG"), c, "-469999.97"));
        rbuy = fixture.reglement(Reglement.debit(c.code("-RBUY"), c, "70").module(c.moduleBuy()));
        fixture.ligne(rbuy, fbuy, "70");
        rd = fixture.reglement(Reglement.debit(c.code("-RD"), c, "10").statut(FixtureTresorerie.POINTE)
                .dates(LocalDate.of(2031, 1, 10), LocalDate.of(2031, 2, 10), LocalDate.of(2031, 3, 10)));
        fixture.ligne(rd, fa, "10");
        LocalDate passe = LocalDate.of(2020, 1, 15);
        rret = fixture.reglement(Reglement.debit(c.code("-RRET"), c, "20").dates(passe, passe, null));
        rpte = fixture.reglement(Reglement.debit(c.code("-RPTE"), c, "30").statut(FixtureTresorerie.POINTE)
                .dates(passe, passe, LocalDate.of(2020, 1, 20)));
        LocalDate aujourdhui = LocalDate.now(FuseauMetier.ZONE);
        raujourdhui = fixture.reglement(Reglement.debit(c.code("-RAUJ"), c, "40")
                .dates(LocalDate.of(2020, 1, 1), aujourdhui, null));

        a = connecte(fixture.compte(c, "TRDA", FixtureTresorerie.DROIT_DECAISSEMENTS), FixtureTresorerie.ROLE);
    }

    private Reponse liste(String... params) throws Exception {
        String[] avecModule = new String[params.length + 2];
        avecModule[0] = MODULE;
        avecModule[1] = PRJ;
        System.arraycopy(params, 0, avecModule, 2, params.length);
        return appels.ok(a, DECAISSEMENTS, avecModule);
    }

    // ------------------------------------------------------------------ R3 / D6 montants

    @Test
    void r3_resteAAffecter_baseConvertieMoinsSommeDesDeuxLignes() throws Exception {
        Map<String, Object> l = liste().ligne(ra);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(l, "montant")).as("Amount tel que stocké").isEqualByComparingTo("1010");
            s.assertThat(nombre(l, "montantAffecte")).as("300 + 200").isEqualByComparingTo("500");
            s.assertThat(nombre(l, RESTE)).as("AmountBase 1000 × 1 − 500").isEqualByComparingTo("500");
        });
    }

    @Test
    void r3_resteAAffecter_multiplicateurConvertValueSurConvertUnit() throws Exception {
        assertThat(nombre(liste().ligne(rconv), RESTE)).as("100 × 2/1 − 50").isEqualByComparingTo("150");
    }

    @Test
    void r3_resteAAffecter_convertUnitNul_multiplicateurUn() throws Exception {
        assertThat(nombre(liste().ligne(ru0), RESTE)).as("100 × 1 − 0").isEqualByComparingTo("100");
    }

    @Test
    void r3_montantNegatif_telQueStocke() throws Exception {
        Map<String, Object> l = liste().ligne(rneg);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(l, "montant")).isEqualByComparingTo("-469999.97");
            s.assertThat(nombre(l, RESTE)).isEqualByComparingTo("-469999.97");
        });
    }

    // ------------------------------------------------------------------ R4 module

    @Test
    void r4_reglementBuy_absentSousPrj() throws Exception {
        assertThat(liste().ids()).contains(ra.toString()).doesNotContain(rbuy.toString());
    }

    @Test
    void r4_reglementPrj_absentSousBuy() throws Exception {
        assertThat(appels.ok(a, DECAISSEMENTS, MODULE, BUY).ids()).contains(rbuy.toString())
                .doesNotContain(ra.toString());
    }

    // ------------------------------------------------------------------ R5 dateSur

    @Test
    void r5_dateSurPiece_filtreSurDatePiece() throws Exception {
        List<String> janvier = liste(DATE_SUR, "PIECE", DU, "2031-01-01", AU, "2031-01-31").ids();
        List<String> mars = liste(DATE_SUR, "PIECE", DU, "2031-03-01", AU, "2031-03-31").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(janvier).containsExactly(rd.toString());
            s.assertThat(mars).doesNotContain(rd.toString());
        });
    }

    @Test
    void r5_dateSurEcheance_filtreSurEcheance() throws Exception {
        List<String> fevrier = liste(DATE_SUR, "ECHEANCE", DU, "2031-02-01", AU, "2031-02-28").ids();
        List<String> janvier = liste(DATE_SUR, "ECHEANCE", DU, "2031-01-01", AU, "2031-01-31").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fevrier).containsExactly(rd.toString());
            s.assertThat(janvier).doesNotContain(rd.toString());
        });
    }

    @Test
    void r5_dateSurPointage_filtreSurDatePointage() throws Exception {
        List<String> mars = liste(DATE_SUR, "POINTAGE", DU, "2031-03-01", AU, "2031-03-31").ids();
        List<String> janvier = liste(DATE_SUR, "POINTAGE", DU, "2031-01-01", AU, "2031-01-31").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mars).containsExactly(rd.toString());
            s.assertThat(janvier).doesNotContain(rd.toString());
        });
    }

    @Test
    void r5_dateSurPointageReporte_reglementPointe_prendSaDateDePointage() throws Exception {
        List<String> mars = liste(DATE_SUR, "POINTAGE_REPORTE", DU, "2031-03-01", AU, "2031-03-31").ids();
        List<String> janvier = liste(DATE_SUR, "POINTAGE_REPORTE", DU, "2031-01-01", AU, "2031-01-31").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mars).contains(rd.toString());
            s.assertThat(janvier).doesNotContain(rd.toString());
        });
    }

    @Test
    void r5_sansProjet_rameneLesReglementsSansLigne() throws Exception {
        assertThat(liste("sansProjet", "true").ids()).contains(rneg.toString(), ru0.toString())
                .doesNotContain(ra.toString(), rd.toString());
    }

    // ------------------------------------------------------------------ R6 enRetard

    @Test
    void r6_enRetard_echeancePasseeNonPointe_vrai_pointeOuFutur_faux() throws Exception {
        Reponse r = liste();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ligne(rret)).as("échéance passée, non pointé").containsEntry(EN_RETARD, true);
            s.assertThat(r.ligne(raujourdhui)).as("échéance = aujourd'hui (≤)").containsEntry(EN_RETARD, true);
            s.assertThat(r.ligne(rpte)).as("échéance passée mais pointé").containsEntry(EN_RETARD, false);
            s.assertThat(r.ligne(ra)).as("échéance future").containsEntry(EN_RETARD, false);
        });
    }
}

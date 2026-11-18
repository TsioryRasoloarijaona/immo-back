package com.tsix.immocrm.controller.tresorerie;

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

import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.AFFECTATIONS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DEBIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DECAISSEMENTS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.SENS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R2 / D5 (FEATURE-n3-recettes-depenses.md) : CANCELED listé par défaut, absent avec {@code inclureAnnules=false}, jamais
 * dans les totaux ; statut NULL compté. Jeu (une société, un projet habilité, une facture FP) :
 * RV IN_WORK 100 (ligne 40), RC CANCELED 200 (ligne 200), RX statut NULL 50 (ligne 50).
 * Totaux attendus hors annulés : montant 150, affecté 90, reste (100 − 40) + (50 − 50) = 60.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieAnnulesTest {

    private static final String INCLURE_ANNULES = "inclureAnnules";
    private static final String FAUX = "false";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;
    private UUID rv;
    private UUID rc;
    private UUID rx;
    private UUID lv;
    private UUID lc;
    private UUID lx;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        Cadre c = fixture.cadre("TRA");
        UUID fp = fixture.piece(c.code("-FP"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        rv = fixture.reglement(Reglement.debit(c.code("-RV"), c, "100"));
        lv = fixture.ligne(rv, fp, "40");
        rc = fixture.reglement(Reglement.debit(c.code("-RC"), c, "200").statut(FixtureTresorerie.ANNULE));
        lc = fixture.ligne(rc, fp, "200");
        rx = fixture.reglement(Reglement.debit(c.code("-RX"), c, "50").statut(null));
        lx = fixture.ligne(rx, fp, "50");
        a = connecte(fixture.compte(c, "TRAA", FixtureTresorerie.DROIT_DECAISSEMENTS,
                FixtureTresorerie.DROIT_AFFECTATIONS), FixtureTresorerie.ROLE);
    }

    // ------------------------------------------------------------------ décaissements

    @Test
    void r2_decaissements_annuleListeParDefaut() throws Exception {
        assertThat(appels.ok(a, DECAISSEMENTS, MODULE, PRJ).ids())
                .contains(rv.toString(), rc.toString(), rx.toString());
    }

    @Test
    void r2_decaissements_inclureAnnulesFaux_annuleAbsent_statutNullPresent() throws Exception {
        assertThat(appels.ok(a, DECAISSEMENTS, MODULE, PRJ, INCLURE_ANNULES, FAUX).ids())
                .contains(rv.toString(), rx.toString()).doesNotContain(rc.toString());
    }

    @Test
    void r2_decaissements_totauxHorsAnnules_statutNullCompte_parDefaut() throws Exception {
        verifierTotauxDecaissements(appels.ok(a, DECAISSEMENTS, MODULE, PRJ));
    }

    @Test
    void r2_decaissements_totauxHorsAnnules_inclureAnnulesFaux_memesTotaux() throws Exception {
        verifierTotauxDecaissements(appels.ok(a, DECAISSEMENTS, MODULE, PRJ, INCLURE_ANNULES, FAUX));
    }

    private static void verifierTotauxDecaissements(Reponse r) {
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.totaux.montant")).isEqualByComparingTo("150");
            s.assertThat(r.nombre("$.totaux.montantAffecte")).isEqualByComparingTo("90");
            s.assertThat(r.nombre("$.totaux.resteAAffecter")).isEqualByComparingTo("60");
        });
    }

    // ------------------------------------------------------------------ affectations

    @Test
    void r2_affectations_ligneAnnuleeListeeParDefaut() throws Exception {
        assertThat(appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT).ids())
                .contains(lv.toString(), lc.toString(), lx.toString());
    }

    @Test
    void r2_affectations_inclureAnnulesFaux_ligneAnnuleeAbsente_statutNullPresente() throws Exception {
        assertThat(appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT, INCLURE_ANNULES, FAUX).ids())
                .contains(lv.toString(), lx.toString()).doesNotContain(lc.toString());
    }

    @Test
    void r2_affectations_totauxHorsAnnules_statutNullCompte() throws Exception {
        Reponse r = appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.totaux.montantAffecte")).as("40 + 50").isEqualByComparingTo("90");
            s.assertThat(r.nombre("$.totaux.montantReglement")).as("100 + 50, une ligne par règlement")
                    .isEqualByComparingTo("150");
        });
    }
}

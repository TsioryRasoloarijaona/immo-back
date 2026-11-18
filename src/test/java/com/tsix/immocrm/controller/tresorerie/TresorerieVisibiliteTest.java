package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
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

import java.util.List;
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
 * R1 / D4 (FEATURE-n3-recettes-depenses.md) : société habilitée ET (projet habilité OU sans projet), projet lu à la ligne.
 * <pre>
 *   société S habilitée : projet A habilité ← FA ; projet B non habilité ← FB
 *   société T non habilitée : projet T ← FT
 *   RA  : LA 300 → FA                      (visible)
 *   RAB : LABA 150 → FA + LABB 250 → FB     (à cheval : visible, LABB absente des affectations)
 *   RB  : LB 800 → FB                      (seulement projet non habilité : absent)
 *   RN  : sans ligne, société S             (visible, nonAffecte)
 *   RT  : LT → FT, société T ; RT0 : sans ligne, société T (absents)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieVisibiliteTest {

    private static final String NON_AFFECTE = "nonAffecte";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;
    @Autowired private FixtureAchat achat;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;
    private UUID ra;
    private UUID rab;
    private UUID rb;
    private UUID rn;
    private UUID rt;
    private UUID rt0;
    private UUID la;
    private UUID labA;
    private UUID labB;
    private UUID lb;
    private UUID lt;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        Cadre c = fixture.cadre("TRV");
        UUID societeT = achat.societe("SOCT-" + c.jeton());
        UUID projetT = achat.projet(c.code("-PT"), societeT);
        UUID fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        UUID fb = fixture.piece(c.code("-FB"), c.societe(), c.projetNonHabilite(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        UUID ft = fixture.piece(c.code("-FT"), societeT, projetT, c.fournisseur(), c.fournisseurNom(), c.modulePrj(),
                "1000");

        ra = fixture.reglement(Reglement.debit(c.code("-RA"), c, "300"));
        la = fixture.ligne(ra, fa, "300");
        rab = fixture.reglement(Reglement.debit(c.code("-RAB"), c, "400"));
        labA = fixture.ligne(rab, fa, "150");
        labB = fixture.ligne(rab, fb, "250");
        rb = fixture.reglement(Reglement.debit(c.code("-RB"), c, "800"));
        lb = fixture.ligne(rb, fb, "800");
        rn = fixture.reglement(Reglement.debit(c.code("-RN"), c, "700"));
        rt = fixture.reglement(Reglement.debit(c.code("-RT"), c, "900").societe(societeT));
        lt = fixture.ligne(rt, ft, "900");
        rt0 = fixture.reglement(Reglement.debit(c.code("-RT0"), c, "500").societe(societeT));

        a = connecte(fixture.compte(c, "TRVA", FixtureTresorerie.DROIT_DECAISSEMENTS,
                FixtureTresorerie.DROIT_AFFECTATIONS), FixtureTresorerie.ROLE);
    }

    private List<String> decaissements() throws Exception {
        return appels.ok(a, DECAISSEMENTS, MODULE, PRJ).ids();
    }

    @Test
    void r1_decaissementACheval_projetHabiliteEtNonHabilite_visible() throws Exception {
        assertThat(decaissements()).contains(rab.toString(), ra.toString());
    }

    @Test
    void r1_decaissementSeulementSurProjetNonHabilite_absent() throws Exception {
        assertThat(decaissements()).contains(ra.toString()).doesNotContain(rb.toString());
    }

    @Test
    void r1_decaissementSansLigne_societeHabilitee_visibleEtNonAffecte() throws Exception {
        Reponse r = appels.ok(a, DECAISSEMENTS, MODULE, PRJ);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ligne(rn)).containsEntry(NON_AFFECTE, true);
            s.assertThat(r.ligne(ra)).containsEntry(NON_AFFECTE, false);
        });
    }

    @Test
    void r1_societeNonHabilitee_decaissementsAbsents_avecOuSansLigne() throws Exception {
        assertThat(decaissements()).contains(ra.toString()).doesNotContain(rt.toString(), rt0.toString());
    }

    @Test
    void r1_affectation_projetNonHabiliteOuSocieteNonHabilitee_absente() throws Exception {
        List<String> ids = appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT).ids();

        assertThat(ids).contains(la.toString(), labA.toString())
                .doesNotContain(labB.toString(), lb.toString(), lt.toString());
    }

    @Test
    void r1_ficheDecaissementInvisible_404_visible_200() throws Exception {
        Reponse visible = appels.lire(a, DECAISSEMENTS + "/" + ra);
        Reponse projetNonHabilite = appels.lire(a, DECAISSEMENTS + "/" + rb);
        Reponse societeNonHabilitee = appels.lire(a, DECAISSEMENTS + "/" + rt0);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).as("témoin : %s", visible.corps()).isEqualTo(200);
            s.assertThat(projetNonHabilite.statut()).isEqualTo(404);
            s.assertThat(societeNonHabilitee.statut()).isEqualTo(404);
        });
    }

    @Test
    void r1_ficheDecaissementVisible_porteSesLignes() throws Exception {
        Reponse r = appels.lire(a, DECAISSEMENTS + "/" + ra);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat((List<?>) r.valeur("$.lignes")).hasSize(1);
    }
}

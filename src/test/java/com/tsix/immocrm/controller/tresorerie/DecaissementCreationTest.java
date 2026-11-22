package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.PIECES_A_AFFECTER;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.nombre;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.texte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md : création d'un décaissement ({@code POST /decaissements}), valeurs d'insertion D2 /
 * D10 / D12, statut NOTPAYED (réponse Q1), liste des pièces à affecter (§3, §6 « même fournisseur, reste à régler »).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementCreationTest {

    private static final String MONTANT = "montant";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private FixtureAchat achat;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID pa;
    private UUID pb;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DCR");
        qui = connecte(fixture.compte(s, "DCRA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU),
                FixtureTresorerie.ROLE);
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
        pb = fixture.piece(s, "-FB", s.c().projet(), "500");
    }

    private Map<String, Object> deuxLignes() {
        return corps(s, s.code("-R1"), "800", List.of(ligne(pa, "300"), ligne(pb, "200")));
    }

    // ------------------------------------------------------------------ §3 POST 201, D3 reste à affecter

    @Test
    void post_201_ficheAvecMontantAffecteEtResteAAffecter() throws Exception {
        Reponse r = appels.creer(qui, deuxLignes());

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(r.nombre("$.decaissement.montantAffecte")).as("300 + 200").isEqualByComparingTo("500");
            soft.assertThat(r.nombre("$.decaissement.resteAAffecter")).as("800 − 500").isEqualByComparingTo("300");
            soft.assertThat(r.valeur("$.decaissement.statut")).isEqualTo(AppelsSaisieDecaissement.IN_WORK);
            soft.assertThat(r.valeur("$.decaissement.comptabilise")).isEqualTo(false);
            soft.assertThat((List<?>) r.valeur("$.lignes")).hasSize(2);
        });
    }

    @Test
    void post_relectureParLaFiche_memesMontants() throws Exception {
        UUID id = appels.creerOk(qui, deuxLignes());

        Reponse fiche = appels.fiche(qui, id);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(fiche.nombre("$.decaissement.montant")).isEqualByComparingTo("800");
            soft.assertThat(fiche.nombre("$.decaissement.montantAffecte")).isEqualByComparingTo("500");
            soft.assertThat(fiche.nombre("$.decaissement.resteAAffecter")).isEqualByComparingTo("300");
            soft.assertThat(fiche.nombre("$.lignesMasquees")).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ D2 / D12 valeurs d'insertion

    @Test
    void d12_debitModulePrjNonComptabilise_montantEgalBase_payeurEgalTiers() throws Exception {
        UUID id = appels.creerOk(qui, deuxLignes());

        Map<String, Object> enBase = fixture.reglementEnBase(id).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(enBase).as("IsCredit").containsEntry("credit", false);
            soft.assertThat(enBase).as("IsAccounted").containsEntry("comptabilise", false);
            soft.assertThat(enBase).as("AdmModuleId").containsEntry("module", texte(s.c().modulePrj()));
            soft.assertThat(enBase).as("TierId").containsEntry("tiers", texte(s.c().fournisseur()));
            soft.assertThat(enBase).as("TierPayerId = TierId si non saisi").containsEntry("payeur", texte(s.c().fournisseur()));
            soft.assertThat(nombre(enBase, "base")).as("AmountBase = montant").isEqualByComparingTo("800");
            soft.assertThat(nombre(enBase, MONTANT)).as("D2 Amount = AmountBase").isEqualByComparingTo("800");
        });
    }

    @Test
    void d12_payeurSaisi_ecritTierPayerId() throws Exception {
        UUID payeur = achat.fournisseur("PAY-" + s.c().jeton(), "Payeur " + s.c().jeton());
        Map<String, Object> corps = deuxLignes();
        corps.put("payeurId", payeur);

        UUID id = appels.creerOk(qui, corps);

        assertThat(fixture.reglementEnBase(id).get(0)).containsEntry("payeur", texte(payeur))
                .containsEntry("tiers", texte(s.c().fournisseur()));
    }

    @Test
    void d12_moduleBuy_ecritAdmModuleIdBuy() throws Exception {
        UUID piece = fixture.piece(s, "-FBUY", s.c().projet(), s.c().fournisseur(), s.c().moduleBuy(), "300");
        Map<String, Object> corps = corps(s, s.code("-RB"), "300", List.of(ligne(piece, "300")));
        corps.put("module", AppelsTresorerie.BUY);

        UUID id = appels.creerOk(qui, corps);

        assertThat(fixture.reglementEnBase(id).get(0)).containsEntry("module", texte(s.c().moduleBuy()));
    }

    @Test
    void d10_ligne_porteSeulementBuyDocumentId() throws Exception {
        UUID id = appels.creerOk(qui, corps(s, s.code("-RL"), "300", List.of(ligne(pa, "300"))));

        List<Map<String, Object>> lignes = fixture.lignesEnBase(id);

        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0)).containsEntry("piece", texte(pa)).containsEntry("com", null).containsEntry("stl", null);
        assertThat(nombre(lignes.get(0), MONTANT)).isEqualByComparingTo("300");
    }

    // ------------------------------------------------------------------ Q1 : statuts de la fiche OMRAN

    @Test
    void q1_statutNotPayed_accepte() throws Exception {
        Map<String, Object> corps = deuxLignes();
        corps.put("statut", "NOTPAYED");

        UUID id = appels.creerOk(qui, corps);

        assertThat(fixture.reglementEnBase(id).get(0)).containsEntry("statut", "NOTPAYED");
    }

    // ------------------------------------------------------------------ §3 / §6 pièces à affecter

    @Test
    void piecesAAffecter_memeFournisseur_netRegleReste_horsAnnules() throws Exception {
        UUID r = tresorerie.reglement(Reglement.debit(s.code("-RX"), s.c(), "400"));
        tresorerie.ligne(r, pa, "400");
        UUID annule = tresorerie.reglement(Reglement.debit(s.code("-RAN"), s.c(), "300").statut(FixtureTresorerie.ANNULE));
        tresorerie.ligne(annule, pa, "300");

        Reponse rep = piecesAAffecter();

        assertThat(rep.statut()).as(rep.corps()).isEqualTo(200);
        Map<String, Object> l = piece(rep, pa);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(nombre(l, "netAPayer")).isEqualByComparingTo("1000");
            soft.assertThat(nombre(l, "regle")).as("règlement annulé non compté").isEqualByComparingTo("400");
            soft.assertThat(nombre(l, "resteAPayer")).isEqualByComparingTo("600");
        });
    }

    @Test
    void piecesAAffecter_exclutSoldeeAutreFournisseurEtInvisible() throws Exception {
        UUID r = tresorerie.reglement(Reglement.debit(s.code("-RS"), s.c(), "500"));
        tresorerie.ligne(r, pb, "500");
        UUID autre = achat.fournisseur("FOU2-" + s.c().jeton(), "Autre " + s.c().jeton());
        UUID pAutre = fixture.piece(s, "-FAUT", s.c().projet(), autre, s.c().modulePrj(), "100");
        UUID pInvisible = fixture.piece(s, "-FINV", s.c().projetNonHabilite(), "100");

        Reponse rep = piecesAAffecter();

        assertThat(rep.statut()).as(rep.corps()).isEqualTo(200);
        List<String> ids = JsonPath.read(rep.corps(), "$[*].id");
        assertThat(ids).contains(pa.toString()).doesNotContain(pb.toString(), pAutre.toString(), pInvisible.toString());
    }

    private Reponse piecesAAffecter() throws Exception {
        return appels.lire(qui, PIECES_A_AFFECTER, AppelsTresorerie.MODULE, AppelsTresorerie.PRJ,
                "fournisseurId", s.c().fournisseur().toString(), "societeId", s.c().societe().toString());
    }

    private static Map<String, Object> piece(Reponse r, UUID id) {
        List<Map<String, Object>> trouvees = JsonPath.read(r.corps(), "$[?(@.id == '" + id + "')]");
        assertThat(trouvees).as("pièce %s dans %s", id, r.corps()).hasSize(1);
        return trouvees.get(0);
    }
}

package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
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

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_CONFIDENTIEL;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6b onglets Modalités de paiement, Bons de commande et bordereau de la DIA (FEATURE-n6-demandes-achat.md « Endpoints N6b »),
 * avec R7, R9 et D6 sur ces routes. Corps d'ajout de modalité : {@code {ordre, code, ratio}} (O.D.T., Code, Ratio du Brief front).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatBordereauTest {

    private static final String MODALITES = "/modalites";
    private static final String BONS_COMMANDE = "/bons-commande";
    private static final String BORDEREAU = "/bordereau";
    private static final String MODALITES_EN_BASE = "SELECT * FROM BuySelectionPaymentTerm WHERE BuySelectionId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("BO");
        qLibre = qui(b.libre(c, "n6b.bo.libre." + c.jeton()));
    }

    private UUID dia(String suffixe, UUID statut) {
        return demandes.demande(c, c.code(suffixe), statut);
    }

    private UUID diaConfidentielle(String suffixe) {
        UUID procedure = demandes.procedure("PCONF" + suffixe + "-" + c.jeton(), c.entite());
        demandes.confidentielle(procedure);
        return demandes.demande(c, c.code(suffixe), c.prep(), procedure, LocalDate.of(2026, 3, 10));
    }

    private RequestPostProcessor sansConfidentiel() {
        return qui(demandes.libre(c, "n6b.bo.sansconf." + c.jeton(), sauf(DROIT_CONFIDENTIEL)));
    }

    // ------------------------------------------------------------------ modalités de paiement

    @Test
    void modalites_lecture_listeLesModalitesDeLaDia() throws Exception {
        UUID dia = dia("MLE", c.prep());
        b.modalite(dia, 1, "AVANCE-" + c.jeton(), "0.3");

        Reponse r = appels.lire(qLibre, dia, MODALITES);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.corps()).contains("AVANCE-" + c.jeton());
    }

    @Test
    void modalites_ajout_ecritLaLigneSurLaDia() throws Exception {
        UUID dia = dia("MAJ", c.prep());

        Reponse r = appels.poster(qLibre, dia, MODALITES, corps().avec("ordre", 2).avec("code", "SOLDE-" + c.jeton())
                .avec("ratio", 70));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        List<Map<String, Object>> modalites = b.lignes(MODALITES_EN_BASE, dia.toString());
        assertThat(modalites).singleElement()
                .satisfies(m -> assertThat(m).containsEntry("Code", "SOLDE-" + c.jeton()).containsEntry("Sorting", 2));
    }

    @Test
    void modalites_suppression_retireLaLigne() throws Exception {
        UUID dia = dia("MSU", c.prep());
        UUID retiree = b.modalite(dia, 1, "RET-" + c.jeton(), "0.5");
        b.modalite(dia, 2, "GARDE-" + c.jeton(), "0.5");

        Reponse r = appels.retirer(qLibre, dia, MODALITES + "/" + retiree);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(b.lignes(MODALITES_EN_BASE, dia.toString())).extracting(m -> m.get("Code"))
                .containsExactly("GARDE-" + c.jeton());
    }

    @Test
    void r7_demandeNonDerniere_ajoutModalite_409() throws Exception {
        UUID ancienne = dia("MR7A", c.prep());
        demandes.poser(DEMANDE, ancienne, "IsTheLast", false);
        UUID derniere = dia("MR7B", c.prep());

        Reponse refus = appels.poster(qLibre, ancienne, MODALITES, corps().avec("ordre", 1).avec("code", "R7").avec("ratio", 100));
        Reponse temoin = appels.poster(qLibre, derniere, MODALITES, corps().avec("ordre", 1).avec("code", "R7").avec("ratio", 100));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(b.lignesApresRefus(MODALITES_EN_BASE, ancienne.toString())).isEmpty();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void d6_procedureConfidentielle_sansLeDroit_modalites_403() throws Exception {
        UUID diaConf = diaConfidentielle("MCF");
        RequestPostProcessor sansConf = sansConfidentiel();

        Reponse refus = appels.lire(sansConf, diaConf, MODALITES);
        Reponse temoinOrdinaire = appels.lire(sansConf, dia("MOR", c.prep()), MODALITES);
        Reponse temoinAvecDroit = appels.lire(qLibre, diaConf, MODALITES);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoinOrdinaire.statut()).as(temoinOrdinaire.corps()).isEqualTo(200);
            s.assertThat(temoinAvecDroit.statut()).as(temoinAvecDroit.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ bons de commande

    @Test
    void bonsCommande_listeLesBonsDeCommandeLies_pasLesDevis() throws Exception {
        UUID dia = dia("BC", c.prep());
        b.pieceNonDevis(dia, "BC-" + c.jeton(), FixtureAchat.BON_COMMANDE);
        b.offre(dia, achat.fournisseur("TBC-" + c.jeton(), "Fournisseur BC " + c.jeton()), "DV-" + c.jeton(), false);

        Reponse r = appels.lire(qLibre, dia, BONS_COMMANDE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.corps()).contains("BC-" + c.jeton()).doesNotContain("DV-" + c.jeton());
    }

    @Test
    void d6_procedureConfidentielle_sansLeDroit_bonsCommande_403() throws Exception {
        UUID diaConf = diaConfidentielle("BCF");
        RequestPostProcessor sansConf = sansConfidentiel();

        Reponse refus = appels.lire(sansConf, diaConf, BONS_COMMANDE);
        Reponse temoinOrdinaire = appels.lire(sansConf, dia("BOR", c.prep()), BONS_COMMANDE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoinOrdinaire.statut()).as(temoinOrdinaire.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ bordereau

    @Test
    void bordereau_lecture_groupesEtLignesDeLaDia() throws Exception {
        UUID dia = dia("BDL", c.prep());
        UUID categorie = achat.categorie("CATBDL-" + c.jeton());
        UUID g1 = b.groupe(dia, "GRP1-" + c.jeton(), null, 1);
        UUID g2 = b.groupe(dia, "GRP2-" + c.jeton(), g1, 2);
        b.ligne(dia, g1, 1, "LIG1-" + c.jeton(), "10", categorie);
        b.ligne(dia, g2, 2, "LIG2-" + c.jeton(), "20", categorie);

        Reponse r = appels.lire(qLibre, dia, BORDEREAU);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.corps()).contains("GRP1-" + c.jeton(), "GRP2-" + c.jeton(), "LIG1-" + c.jeton(), "LIG2-" + c.jeton());
    }

    @Test
    void r9_bordereau_horsPerimetre_404() throws Exception {
        UUID horsPerimetre = dia("BHP", c.prep());
        demandes.poser(DEMANDE, horsPerimetre, "PrjProjectId", demandes.projetDe(c, "PRJ-BHP-" + c.jeton()));

        Reponse refus = appels.lire(qLibre, horsPerimetre, BORDEREAU);
        Reponse temoin = appels.lire(qLibre, dia("BVI", c.prep()), BORDEREAU);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(404);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }
}

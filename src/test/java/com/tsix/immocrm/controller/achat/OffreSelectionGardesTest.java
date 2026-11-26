package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
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

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;

/** FEATURE-lc7-appel-offres.md R1 (lot multi-fournisseur : seule garde = pièce non devis rattachée) et R5 (désélection sans offre). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreSelectionGardesTest {

    private static final String DESELECTION = "/offres/selection";
    private static final String OFFRE = "SELECT * FROM BuyDocument WHERE PKey = ?";
    private static final String SELECTIONNEE = "IsSelected";
    private static final String LIGNES_DIA = "SELECT COUNT(*) FROM BuySelectionLine WHERE BuySelectionId = ?";
    private static final String MULTI_AVEC_DOCUMENTS = "lot multi-fournisseur contenant des documents affectés à cet appel d'offre !";
    private static final String NON_SELECTIONNEE = "Vous ne pouvez pas désélectionner une offre non sélectionnée.";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureLotsParite parite;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("GA");
        qLibre = qui(b.libre(c, "lc7.ga.libre." + c.jeton()));
    }

    private UUID dia(String suffixe, boolean multi) {
        UUID dia = demandes.demande(c, c.code(suffixe), c.prep());
        b.lotDe(c, dia, "L" + suffixe + "-" + c.jeton(), multi);
        return dia;
    }

    private UUID tiers(String suffixe) {
        return achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
    }

    private UUID offre(UUID dia, String suffixe, boolean selectionnee) {
        return b.offre(dia, tiers(suffixe), c.code("O" + suffixe), selectionnee);
    }

    private static String selection(UUID offre) {
        return "/offres/" + offre + "/selection";
    }

    private Object selectionneeApresRefus(UUID offre) {
        return b.lignesApresRefus(OFFRE, offre.toString()).getFirst().get(SELECTIONNEE);
    }

    // ------------------------------------------------------------------ R1 lot multi-fournisseur

    @Test
    void r1_lotMulti_selectionAvecBonDeCommande_409_temoinSansBonDeCommande_200() throws Exception {
        UUID dia = dia("MBC", true);
        b.pieceNonDevis(dia, c.code("BCMBC"), FixtureAchat.BON_COMMANDE);
        UUID o = offre(dia, "MBC", false);
        UUID diaTemoin = dia("MSB", true);
        UUID oTemoin = offre(diaTemoin, "MSB", false);

        Reponse refus = appels.poster(qLibre, dia, selection(o), corps());
        Object apresRefus = selectionneeApresRefus(o);
        Reponse temoin = appels.poster(qLibre, diaTemoin, selection(oTemoin), corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(MULTI_AVEC_DOCUMENTS);
            s.assertThat(apresRefus).isEqualTo(false);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r1_lotMulti_deselectionAvecBonDeCommande_409_offreResteSelectionnee_temoinSansBonDeCommande_200() throws Exception {
        UUID dia = dia("MDB", true);
        b.pieceNonDevis(dia, c.code("BCMDB"), FixtureAchat.BON_COMMANDE);
        UUID o = offre(dia, "MDB", true);
        UUID diaTemoin = dia("MDS", true);
        offre(diaTemoin, "MDS", true);

        Reponse refus = appels.retirer(qLibre, dia, DESELECTION);
        Object apresRefus = selectionneeApresRefus(o);
        Reponse temoin = appels.retirer(qLibre, diaTemoin, DESELECTION);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(MULTI_AVEC_DOCUMENTS);
            s.assertThat(apresRefus).isEqualTo(true);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r1_lotMulti_avecDecompteSansBonDeCommande_selection_200_puisDeselection_200() throws Exception {
        UUID dia = demandes.demande(c, c.code("MDC"), c.prep());
        UUID lot = b.lotDe(c, dia, "LMDC-" + c.jeton(), true);
        parite.decompteDuLot(lot, "DC-MDC-" + c.jeton());
        UUID o = offre(dia, "MDC", false);

        Reponse selection = appels.poster(qLibre, dia, selection(o), corps());
        Object apresSelection = b.lignes(OFFRE, o.toString()).getFirst().get(SELECTIONNEE);
        Reponse deselection = appels.retirer(qLibre, dia, DESELECTION);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(selection.statut()).as("multi : pas de contrôle des décomptes " + selection.corps()).isEqualTo(200);
            s.assertThat(apresSelection).isEqualTo(true);
            s.assertThat(deselection.statut()).as(deselection.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ R5 désélection sans offre sélectionnée

    @Test
    void r5_deselection_offresNonSelectionnees_409_detailExact_rienNEstEcrit_temoin200() throws Exception {
        UUID dia = dia("R5N", false);
        UUID t = tiers("R5F");
        demandes.poser(DEMANDE, dia, "CfgTierId", t);
        b.ligne(dia, b.groupe(dia, "G1", null, 1), 1, "A1", "10", achat.categorie("CATR5N-" + c.jeton()));
        offre(dia, "R5N", false);
        UUID diaTemoin = dia("R5T", false);
        offre(diaTemoin, "R5T", true);

        Reponse refus = appels.retirer(qLibre, dia, DESELECTION);
        Object fournisseur = b.lignesApresRefus("SELECT CfgTierId FROM BuySelection WHERE PKey = ?", dia.toString())
                .getFirst().get("CfgTierId");
        int lignesDia = b.compterApresRefus(LIGNES_DIA, dia.toString());
        Reponse temoin = appels.retirer(qLibre, diaTemoin, DESELECTION);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.statut() == 409 ? refus.valeur(DETAIL) : null).isEqualTo(NON_SELECTIONNEE);
            s.assertThat(String.valueOf(fournisseur)).isEqualToIgnoringCase(t.toString());
            s.assertThat(lignesDia).isEqualTo(1);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r5_deselection_aucuneOffre_409() throws Exception {
        UUID dia = dia("R5A", false);

        Reponse refus = appels.retirer(qLibre, dia, DESELECTION);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(NON_SELECTIONNEE);
        });
    }
}

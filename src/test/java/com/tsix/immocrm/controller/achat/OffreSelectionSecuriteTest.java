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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.RACINE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_SELECTION_OFFRE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** FEATURE-lc7-appel-offres.md : le droit de sélection d'offre passe avant R6 (confirmation) et R5 (désélection sans offre). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreSelectionSecuriteTest {

    private static final String OFFRE = "SELECT * FROM BuyDocument WHERE PKey = ?";
    private static final String SELECTIONNEE = "IsSelected";
    private static final String CODE_PLUS_DISANTE = "OFFRE_LA_PLUS_DISANTE";
    private static final String DESELECTION = "/offres/selection";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;
    private RequestPostProcessor qSansSelection;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("SS");
        qLibre = qui(b.libre(c, "lc7.ss.libre." + c.jeton()));
        qSansSelection = qui(demandes.libre(c, "lc7.ss.sans." + c.jeton(), sauf(DROIT_SELECTION_OFFRE)));
    }

    /** DIA sur un lot sans décompte, deux offres non sélectionnées ; rend l'offre au net à payer maximal. */
    private UUID offreMaximale(UUID dia, String suffixe) {
        UUID chere = b.offre(dia, achat.fournisseur("T" + suffixe + "A-" + c.jeton(), "Fournisseur " + suffixe + "A"),
                c.code("O" + suffixe + "A"), false);
        achat.montants(chere, "900", "1000", "1000");
        UUID autre = b.offre(dia, achat.fournisseur("T" + suffixe + "B-" + c.jeton(), "Fournisseur " + suffixe + "B"),
                c.code("O" + suffixe + "B"), false);
        achat.montants(autre, "700", "800", "800");
        return chere;
    }

    private UUID dia(String suffixe) {
        UUID dia = demandes.demande(c, c.code(suffixe), c.prep());
        b.lotDe(c, dia, "L" + suffixe + "-" + c.jeton(), false);
        return dia;
    }

    private Reponse selectionner(RequestPostProcessor qui, UUID dia, UUID offre, String confirme) throws Exception {
        MockHttpServletRequestBuilder requete = post(RACINE + "/" + dia + "/offres/" + offre + "/selection").with(qui);
        if (confirme != null) {
            requete = requete.param("confirme", confirme);
        }
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private Object selectionneeApresRefus(UUID offre) {
        return b.lignesApresRefus(OFFRE, offre.toString()).getFirst().get(SELECTIONNEE);
    }

    @Test
    void droit_sansDroitSelection_confirmeTrue_403_rienNEstEcrit_temoin200() throws Exception {
        UUID dia = dia("CT");
        UUID chere = offreMaximale(dia, "CT");
        UUID diaTemoin = dia("CTT");
        UUID chereTemoin = offreMaximale(diaTemoin, "CTT");

        Reponse refus = selectionner(qSansSelection, dia, chere, "true");
        Object apresRefus = selectionneeApresRefus(chere);
        Reponse temoin = selectionner(qLibre, diaTemoin, chereTemoin, "true");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(apresRefus).isEqualTo(false);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void droit_sansDroitSelection_offreMaximaleSansConfirme_403_sansCodePlusDisante_temoin409() throws Exception {
        UUID dia = dia("SC");
        UUID chere = offreMaximale(dia, "SC");

        Reponse refus = selectionner(qSansSelection, dia, chere, null);
        Reponse temoin = selectionner(qLibre, dia, chere, null);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(refus.corps()).doesNotContain(CODE_PLUS_DISANTE);
            s.assertThat(temoin.statut()).as("le compte habilité reçoit la demande de confirmation " + temoin.corps()).isEqualTo(409);
            s.assertThat(temoin.corps()).contains(CODE_PLUS_DISANTE);
        });
    }

    @Test
    void droit_sansDroitSelection_deselectionSansOffreSelectionnee_403AvantR5_temoin409() throws Exception {
        UUID dia = dia("DS");
        offreMaximale(dia, "DS");

        Reponse refus = appels.retirer(qSansSelection, dia, DESELECTION);
        Reponse temoin = appels.retirer(qLibre, dia, DESELECTION);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as("R5 pour le compte habilité " + temoin.corps()).isEqualTo(409);
        });
    }
}

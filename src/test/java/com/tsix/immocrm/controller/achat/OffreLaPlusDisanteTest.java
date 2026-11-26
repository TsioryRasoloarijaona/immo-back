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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.RACINE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FEATURE-lc7-appel-offres.md R6 (D3) : confirmation « offre la plus disante » quand la DIA a au moins 2 offres et que le
 * {@code BuyDocument.AmountToPay} de l'offre choisie égale le maximum des offres de la DIA, écartées comprises.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreLaPlusDisanteTest {

    private static final String OFFRE = "SELECT * FROM BuyDocument WHERE PKey = ?";
    private static final String SELECTIONNEE = "IsSelected";
    private static final String CODE_PLUS_DISANTE = "OFFRE_LA_PLUS_DISANTE";
    private static final String MESSAGE_PLUS_DISANTE = "Voulez vous continuer avec l'offre la plus disante ?";
    private static final String CONFIRME = "confirme";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureLotsParite parite;

    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        c = b.cadre("PD");
        qLibre = qui(b.libre(c, "lc7.pd.libre." + c.jeton()));
    }

    private UUID dia(String suffixe) {
        UUID dia = demandes.demande(c, c.code(suffixe), c.prep());
        b.lotDe(c, dia, "L" + suffixe + "-" + c.jeton(), false);
        return dia;
    }

    /** Offre non sélectionnée ; TTC volontairement dans l'ordre inverse du net à payer (D3 : on compare AmountToPay). */
    private UUID offre(UUID dia, String suffixe, String ttc, String aPayer) {
        UUID tiers = achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
        UUID o = b.offre(dia, tiers, c.code("O" + suffixe), false);
        achat.montants(o, ttc, ttc, aPayer);
        return o;
    }

    private Reponse selectionner(UUID dia, UUID offre, String confirme) throws Exception {
        MockHttpServletRequestBuilder requete = post(RACINE + "/" + dia + "/offres/" + offre + "/selection").with(qLibre);
        if (confirme != null) {
            requete = requete.param(CONFIRME, confirme);
        }
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private Object selectionneeApresRefus(UUID offre) {
        return b.lignesApresRefus(OFFRE, offre.toString()).getFirst().get(SELECTIONNEE);
    }

    private Object selectionnee(UUID offre) {
        return b.lignes(OFFRE, offre.toString()).getFirst().get(SELECTIONNEE);
    }

    @Test
    void r6_offreAuNetAPayerMaximal_sansConfirme_409_codeEtMessage_rienNEstEcrit() throws Exception {
        UUID dia = dia("MAX");
        UUID chere = offre(dia, "MAXA", "500", "1000");
        offre(dia, "MAXB", "900", "800");

        Reponse refus = selectionner(dia, chere, null);
        Object apresRefus = selectionneeApresRefus(chere);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.statut() == 409 ? refus.valeur("$.code") : null).isEqualTo(CODE_PLUS_DISANTE);
            s.assertThat(refus.statut() == 409 ? refus.valeur(DETAIL) : null).isEqualTo(MESSAGE_PLUS_DISANTE);
            s.assertThat(apresRefus).isEqualTo(false);
        });
    }

    @Test
    void r6_offreAuNetAPayerMaximal_confirmeFalse_409() throws Exception {
        UUID dia = dia("MXF");
        UUID chere = offre(dia, "MXFA", "500", "1000");
        offre(dia, "MXFB", "900", "800");

        Reponse refus = selectionner(dia, chere, "false");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(CODE_PLUS_DISANTE);
        });
    }

    @Test
    void r6_offreAuNetAPayerMaximal_confirmeTrue_200_selectionnee() throws Exception {
        UUID dia = dia("MXT");
        UUID chere = offre(dia, "MXTA", "500", "1000");
        UUID autre = offre(dia, "MXTB", "900", "800");

        Reponse r = selectionner(dia, chere, "true");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(selectionnee(chere)).isEqualTo(true);
            s.assertThat(selectionnee(autre)).isEqualTo(false);
        });
    }

    @Test
    void r6_offreNonMaximale_sansConfirme_200_memeSiSonTTCEstLePlusHaut() throws Exception {
        UUID dia = dia("NMX");
        offre(dia, "NMXA", "500", "1000");
        UUID moinsChere = offre(dia, "NMXB", "900", "800");

        Reponse r = selectionner(dia, moinsChere, null);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(selectionnee(moinsChere)).isEqualTo(true);
        });
    }

    @Test
    void r6_uneSeuleOffre_sansConfirme_200() throws Exception {
        UUID dia = dia("UNE");
        UUID seule = offre(dia, "UNE", "500", "1000");

        Reponse r = selectionner(dia, seule, null);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(selectionnee(seule)).isEqualTo(true);
        });
    }

    @Test
    void r6_egaliteAvecLeMaximum_sansConfirme_409() throws Exception {
        UUID dia = dia("EGA");
        UUID a = offre(dia, "EGAA", "500", "1000");
        offre(dia, "EGAB", "900", "1000");

        Reponse refus = selectionner(dia, a, null);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(CODE_PLUS_DISANTE);
        });
    }

    @Test
    void r6_offreEcarteeCompteePourLeNombre_409() throws Exception {
        UUID dia = dia("ECN");
        UUID chere = offre(dia, "ECNA", "500", "1000");
        UUID ecartee = offre(dia, "ECNB", "900", "500");
        achat.poser(ecartee, "IsRejected", true);

        Reponse refus = selectionner(dia, chere, null);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as("2 offres, l'écartée comprise " + refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(CODE_PLUS_DISANTE);
        });
    }

    @Test
    void r6_offreEcarteeCompteePourLeMaximum_200() throws Exception {
        UUID dia = dia("ECM");
        UUID retenue = offre(dia, "ECMA", "500", "800");
        UUID ecartee = offre(dia, "ECMB", "900", "1000");
        achat.poser(ecartee, "IsRejected", true);

        Reponse r = selectionner(dia, retenue, null);

        assertThat(r.statut()).as("l'écartée à 1000 est le maximum " + r.corps()).isEqualTo(200);
    }

    @Test
    void r6_r1PasseAvant_lotAvecDecompte_409SansCodePlusDisante() throws Exception {
        UUID dia = demandes.demande(c, c.code("R1A"), c.prep());
        UUID lot = b.lotDe(c, dia, "LR1A-" + c.jeton(), false);
        parite.decompteDuLot(lot, "DC-R1A-" + c.jeton());
        UUID chere = offre(dia, "R1AA", "500", "1000");
        offre(dia, "R1AB", "900", "800");

        Reponse refus = selectionner(dia, chere, null);
        Reponse confirme = selectionner(dia, chere, "true");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).doesNotContain(CODE_PLUS_DISANTE);
            s.assertThat(confirme.statut()).as("confirme ne lève pas R1 " + confirme.corps()).isEqualTo(409);
        });
    }

    @Test
    void r6_r2PasseAvant_offreEcarteeMaximale_409SansCodePlusDisante() throws Exception {
        UUID dia = dia("R2A");
        UUID ecartee = offre(dia, "R2AA", "500", "1000");
        achat.poser(ecartee, "IsRejected", true);
        offre(dia, "R2AB", "900", "800");

        Reponse refus = selectionner(dia, ecartee, null);
        Reponse confirme = selectionner(dia, ecartee, "true");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).doesNotContain(CODE_PLUS_DISANTE);
            s.assertThat(confirme.statut()).as("confirme ne lève pas R2 " + confirme.corps()).isEqualTo(409);
        });
    }
}

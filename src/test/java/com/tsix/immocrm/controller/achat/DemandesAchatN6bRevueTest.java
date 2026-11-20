package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
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

import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;

/**
 * Revue N6b : R8 / R16, la sélection et la désélection d'offre exigent un lot non multi (OMRAN {@code FilterToolsByRights}
 * l.256-257 : {@code PrjMarketId.HasValue && !IsMultiSupplier}). Une DIA sans lot ne doit ni sélectionner ni vider son bordereau.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatN6bRevueTest {

    private static final String SELECTION = "/offres/selection";
    private static final String OFFRE = "SELECT IsSelected FROM BuyDocument WHERE PKey = ?";
    private static final String LIGNES_DIA = "SELECT COUNT(*) FROM BuySelectionLine WHERE BuySelectionId = ?";
    private static final String SELECTIONNEE = "IsSelected";

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
        c = b.cadre("RV");
        qLibre = qui(b.libre(c, "n6b.rv.libre." + c.jeton()));
    }

    private UUID tiers(String suffixe) {
        return achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
    }

    @Test
    void r8_selection_demandeSansLot_409_rienNEstEcrit() throws Exception {
        UUID dia = demandes.demande(c, c.code("SSL"), c.prep());
        UUID t = tiers("SSL");
        UUID o = b.offre(dia, t, c.code("OSSL"), false);
        UUID diaTemoin = demandes.demande(c, c.code("SST"), c.prep());
        b.lotDe(c, diaTemoin, "LSST-" + c.jeton(), false);
        UUID oTemoin = b.offre(diaTemoin, tiers("SST"), c.code("OSST"), false);

        Reponse refus = appels.poster(qLibre, dia, "/offres/" + o + "/selection", CorpsJson.corps());
        Object apresRefus = b.lignesApresRefus(OFFRE, o.toString()).getFirst().get(SELECTIONNEE);
        Object fournisseur = b.lignesApresRefus("SELECT CfgTierId FROM BuySelection WHERE PKey = ?", dia.toString()).getFirst()
                .get("CfgTierId");
        Reponse temoin = appels.poster(qLibre, diaTemoin, "/offres/" + oTemoin + "/selection", CorpsJson.corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(apresRefus).isEqualTo(false);
            s.assertThat(fournisseur).isNull();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r16_deselection_demandeSansLot_409_bordereauIntact() throws Exception {
        UUID dia = demandes.demande(c, c.code("DSL"), c.prep());
        UUID categorie = achat.categorie("CATDSL-" + c.jeton());
        UUID g1 = b.groupe(dia, "G1", null, 1);
        b.ligne(dia, g1, 1, "A1", "10", categorie);
        b.ligne(dia, g1, 2, "A2", "20", categorie);
        UUID t = tiers("DSL");
        demandes.poser(DEMANDE, dia, "CfgTierId", t);
        UUID retenue = b.offre(dia, t, c.code("ODSL"), true);

        Reponse refus = appels.retirer(qLibre, dia, SELECTION);
        Object apresRefus = b.lignesApresRefus(OFFRE, retenue.toString()).getFirst().get(SELECTIONNEE);
        int lignesDia = b.compterApresRefus(LIGNES_DIA, dia.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(apresRefus).isEqualTo(true);
            s.assertThat(lignesDia).as("bordereau de la DIA sans lot vidé par la désélection").isEqualTo(2);
        });
    }
}

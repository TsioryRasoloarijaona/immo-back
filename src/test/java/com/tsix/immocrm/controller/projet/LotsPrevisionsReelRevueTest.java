package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.INVOICE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue L3a : le « Réel » d'OMRAN (Market.cs:GetPaymentReal) lit BuyDocumentToPayView, dont la clause est
 * {@code WHERE BuyDocument.CanBePayed = 1} : devis, commandes et bons de livraison (CanBePayed = 0) n'y entrent pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsPrevisionsReelRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureLotSousFenetres sf;
    @Autowired private FixtureVisibiliteOmran visibilite;

    @Test
    void previsions_reel_excluPiecesNonPayables_commeBuyDocumentToPayView() throws Exception {
        String jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        sf.tables();
        UUID societe = parite.entiteJuridique("RVE-" + jeton);
        UUID projet = parite.projet("RVP-" + jeton, societe);
        UUID statutLot = parite.statut(ENTREPRENEURS, "RVS-" + jeton);
        AdmUser compte = parite.administrateur("lot.rv." + jeton, projet);
        visibilite.habiliterSociete(compte, societe);
        UUID lot = parite.lot(projet, "RV-" + jeton, ENTREPRENEURS, statutLot);
        LocalDate avril = LocalDate.of(2026, 4, 15);
        sf.piece(lot, projet, societe, "RVF-" + jeton, INVOICE, "", avril, "1000");
        UUID devis = sf.piece(lot, projet, societe, "RVQ-" + jeton, "QUOTE", "", avril, "9000");
        sf.nonPayable(devis);

        Reponse r = new AppelsProjet(mockMvc).lire(connecte(compte, "Administrateur"), LOTS + "/" + lot + "/previsions");

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids("$.mois[*].mois")).containsExactly("2026-03-01", "2026-04-01");
        assertThat(r.nombre("$.mois[1].reel")).as("facture payable seule, devis CanBePayed = 0 exclu")
                .isEqualByComparingTo("1000");
    }
}

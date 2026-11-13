package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.Ligne;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.PU_HT;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.TVA;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.groupe;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue règle 6 du lot L3b-1 (FEATURE-lot-ecritures.md) : cas limites hors des tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotBordereauxRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;

    private BordereauxEcritureSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statutOuvert;
    private UUID categorie;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new BordereauxEcritureSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        projet = parite.projet("BRP-" + jeton, parite.entiteJuridique("BRE-" + jeton));
        statutOuvert = parite.statut(ENTREPRENEURS, "BRO-" + jeton);
        b.ouvrirEstimatif(statutOuvert);
        categorie = b.categorie("BRA-" + jeton);
        admin = connecte(parite.administrateur("lot.br." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code) {
        return parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statutOuvert);
    }

    private Reponse ecrire(UUID lot, String route, CorpsJson corps) throws Exception {
        return appels.ecrire(admin, lot, route, corps);
    }

    // §11.6 : l'id d'une ligne IsStock = 1 du même lot et du même bordereau → 400, la ligne de stock reste intacte.
    @Test
    void r7_idDUneLigneDeStockDuLot_400() throws Exception {
        UUID lot = lot("BR1");
        UUID stock = b.ligne(LIGNES_DEFINITIF, new Ligne(lot, null, 1, "S1", "3", "10", categorie, true));

        Reponse r = ecrire(lot, DEFINITIF, bordereau(List.of(), List.of(ligne(stock, null, 1, "1", "9", "999", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(new BigDecimal(String.valueOf(b.parId(LIGNES_DEFINITIF, stock).get(0).get("UnitPriceHT"))))
                .isEqualByComparingTo("10");
    }

    // R4 « ids uniques dans l'envoi » : un même UUID pour un groupe et une ligne est accepté aujourd'hui.
    @Test
    void r4_memeIdPourUnGroupeEtUneLigne_400() throws Exception {
        UUID id = UUID.randomUUID();
        CorpsJson corps = bordereau(List.of(groupe(id, null, 1, "1")), List.of(ligne(id, id, 1, "1.1", "1", "10", categorie)));

        Reponse r = ecrire(lot("BR2"), ESTIMATIF, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // R4 : un PU HT au-delà de decimal(26,16) (UnitPriceHT du définitif) doit être un 400, pas une erreur SQL (500).
    @Test
    void r4_prixHorsCapaciteDeLaColonne_400() throws Exception {
        CorpsJson l = ligne(UUID.randomUUID(), null, 1, "1", "1", "1", categorie).avec(PU_HT, new BigDecimal("10000000000"));

        Reponse r = ecrire(lot("BR3"), DEFINITIF, bordereau(List.of(), List.of(l)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // R6 : le PU TTC stocké doit être celui de la TVA stockée (VatRatio, 6 décimales), sinon HT + TVA ≠ TTC à la relecture.
    @Test
    void r6_tvaAPlusDeQuatreDecimales_puTTCCoherentAvecLaTvaStockee() throws Exception {
        UUID id = UUID.randomUUID();
        CorpsJson l = ligne(id, null, 1, "1", "1", "1000000", categorie).avec(TVA, new BigDecimal("19.12345678"));

        Reponse r = ecrire(lot("BR4"), ESTIMATIF, bordereau(List.of(), List.of(l)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> enBase = b.parId(LIGNES_ESTIMATIF, id).get(0);
        BigDecimal ht = new BigDecimal(String.valueOf(enBase.get("UnitPriceHT")));
        BigDecimal tva = new BigDecimal(String.valueOf(enBase.get("VatRatio")));
        BigDecimal ttc = new BigDecimal(String.valueOf(enBase.get("UnitPriceTTC")));
        assertThat(ttc).as("VatRatio stocké " + tva).isEqualByComparingTo(
                ht.multiply(BigDecimal.ONE.add(tva)).setScale(6, RoundingMode.HALF_UP));
    }
}

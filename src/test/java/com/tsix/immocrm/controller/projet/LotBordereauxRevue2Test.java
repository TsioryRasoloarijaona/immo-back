package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.Ligne;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DESIGNATION;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.NOTES;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.PU_HT;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Second tour de revue (règle 6) du lot L3b-1 (FEATURE-lot-ecritures.md) : données réelles. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotBordereauxRevue2Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;

    // R14 : 879 lignes définitives (32 lots, yesRemsProd 13/11) ont un PU HT à 16 décimales (HT = TTC / 1,2 saisi par OMRAN).
    // Le front les relit en double JS et renvoie 178666.66666666666 : une ligne non touchée ne doit être ni réécrite ni
    // marquée UserUpdate.
    @Test
    void r14_ligneIntacteRenvoyeeParLeFront_puHTSeizeDecimales_nonModifiee() throws Exception {
        String jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        UUID projet = parite.projet("BRV-" + jeton, parite.entiteJuridique("BRW-" + jeton));
        UUID statut = parite.statut(ENTREPRENEURS, "BRX-" + jeton);
        UUID categorie = b.categorie("BRY-" + jeton);
        UUID lot = parite.lot(projet, "BRZ-" + jeton, ENTREPRENEURS, statut);
        String puOmran = "178666.6666666666666667";
        UUID id = b.ligne(LIGNES_DEFINITIF, new Ligne(lot, null, 1, "1", "1", puOmran, categorie, false));
        double puLuParLeFront = Double.parseDouble(puOmran);

        Reponse r = new BordereauxEcritureSupport(mockMvc).ecrire(
                connecte(parite.administrateur("lot.brv." + jeton, projet), "Administrateur"), lot, DEFINITIF,
                bordereau(List.of(), List.of(ligne(id, null, 1, "1", "1", "1", categorie)
                        .avec(PU_HT, new BigDecimal(Double.toString(puLuParLeFront)))
                        .avec(DESIGNATION, "Ligne 1").avec(NOTES, "Note 1"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> enBase = b.parId(LIGNES_DEFINITIF, id).get(0);
        assertThat(new BigDecimal(String.valueOf(enBase.get("UnitPriceHT")))).isEqualByComparingTo(puOmran);
        assertThat(enBase.get("UserUpdate")).as("ligne non touchée marquée modifiée").isNull();
    }
}

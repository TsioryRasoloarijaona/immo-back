package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.AVENANTS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.GARANTIE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.NOTES;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.avenant;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ligne;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.retenue;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue L3b-2a : une retenue par montant supérieure au TTC donne un taux renvoyé &gt; 100 ; renvoyer la fiche telle que lue
 * (seules les notes changent) doit rester possible, le taux étant ignoré et recalculé en mode montant (§4.1, §11.6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotAvenantsRevueTest {

    private static final LocalDate MARS = LocalDate.of(2026, 3, 5);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;
    @Autowired private FixtureLotAvenants av;

    private AvenantsLotSupport appels;
    private UUID projet;
    private UUID statut;
    private UUID categorie;
    private String jeton;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        av.tables();
        appels = new AvenantsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        av.sequenceAvenant(41);
        UUID societe = parite.entiteJuridique("RVE-" + jeton);
        projet = parite.projet("RVP-" + jeton, societe);
        statut = parite.statut(ENTREPRENEURS, "RVS-" + jeton);
        categorie = b.categorie("RVC-" + jeton);
        admin = connecte(parite.administrateur("lot.rv." + jeton, projet), "Administrateur");
    }

    @Test
    void r4_retenueParMontantSuperieureAuTTC_ficheRelueRenvoyee_200() throws Exception {
        UUID lot = parite.lot(projet, "RV1-" + jeton, ENTREPRENEURS, statut);
        UUID l = UUID.randomUUID();
        // TTC = 1 × 100 HT, TVA 0, FODEC 0 = 100 ; garantie par montant 150.
        CorpsJson corps = avenant(MARS, List.of(ligne(l, 1, "1", "1", "100", "0", "0", categorie)))
                .avec(GARANTIE, retenue(true, "0", "150"));
        Reponse cree = appels.creer(admin, lot, AVENANTS, corps);
        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);
        UUID avenant = UUID.fromString(String.valueOf(cree.valeur("$.id")));
        String tauxRenvoye = String.valueOf(cree.valeur("$.garantie.taux"));

        // Le front renvoie la retenue telle que lue (taux recalculé par le serveur) et ne change que les notes.
        Reponse r = appels.modifier(admin, lot, AVENANTS, avenant,
                corps.avec(GARANTIE, retenue(true, tauxRenvoye, "150")).avec(NOTES, "note"));

        assertThat(r.statut()).as("taux renvoyé par le serveur = " + tauxRenvoye + " ; " + r.corps()).isEqualTo(200);
    }
}

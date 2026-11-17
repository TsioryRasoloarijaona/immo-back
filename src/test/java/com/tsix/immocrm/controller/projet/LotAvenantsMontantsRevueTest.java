package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.AVENANTS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.avenant;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue L3b-2a (2e tour) : un PU HT copié du bordereau définitif (decimal(26,16), 880 / 4 129 lignes de yesRemsProd au-delà de
 * 6 décimales) est stocké arrondi à 6 décimales dans PrjClauseLine.UnitPriceHT ; les totaux de l'avenant doivent rester
 * égaux à la somme des lignes relues (R5 : calcul unique, totaux persistés cohérents avec les lignes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotAvenantsMontantsRevueTest {

    private static final LocalDate MARS = LocalDate.of(2026, 3, 5);

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
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
        av.sequenceAvenant(43);
        UUID societe = parite.entiteJuridique("RME-" + jeton);
        projet = parite.projet("RMP-" + jeton, societe);
        statut = parite.statut(ENTREPRENEURS, "RMS-" + jeton);
        categorie = b.categorie("RMC-" + jeton);
        admin = connecte(parite.administrateur("lot.rm." + jeton, projet), "Administrateur");
    }

    @Test
    void r5_puHTAuDelaDe6Decimales_totauxRelusEgauxALaSommeDesLignes() throws Exception {
        UUID lot = parite.lot(projet, "RM1-" + jeton, ENTREPRENEURS, statut);
        // PU HT à 7 décimales (copié d'une ligne de bordereau), quantité 1000, TVA 0, FODEC 0.
        Reponse cree = appels.creer(admin, lot, AVENANTS,
                avenant(MARS, List.of(ligne(UUID.randomUUID(), 1, "1", "1000", "123.4567894", "0", "0", categorie))));
        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);
        UUID id = UUID.fromString(String.valueOf(cree.valeur("$.id")));

        em.flush();
        em.clear();
        Reponse relu = appels.lire(admin, lot, AVENANTS, id);

        assertThat(relu.statut()).as(relu.corps()).isEqualTo(200);
        assertThat(nombre(relu.valeur("$.montantHT")))
                .as("montantHT de l'en-tête vs totalHT de l'unique ligne relue ; " + relu.corps())
                .isEqualByComparingTo(nombre(relu.valeur("$.lignes[0].totalHT")));
        assertThat(nombre(relu.valeur("$.montantTTC")))
                .as("montantTTC de l'en-tête vs totalTTC de l'unique ligne relue")
                .isEqualByComparingTo(nombre(relu.valeur("$.lignes[0].totalTTC")));
    }

    private static BigDecimal nombre(Object v) {
        return new BigDecimal(String.valueOf(v)).setScale(6, RoundingMode.HALF_UP);
    }
}

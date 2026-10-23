package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Statut;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue adverse du lot P7b : écarts au spec constatés sur le code livré (FEATURE-patrimoine-prospections-terrain.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RevueP7bProspectionsTerrainTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private FixtureTerrains terrains;

    private ApiProspectionsTerrain api;
    private Contexte c;

    @BeforeEach
    void setUp() {
        api = new ApiProspectionsTerrain(mockMvc, patrimoine.administrateur());
        c = fixture.contexte("P7R");
    }

    /** §5.7 : SalePrice = SalePriceFloor = prix proposé exactement (3 m², prix proposé 10 → 10, pas 9,999999). */
    @Test
    void r5_7_prixDeVente_egalExactementLePrixPropose() throws Exception {
        patrimoine.referentielArticle();
        terrains.configurationTerrain();
        CfgCompany societe = patrimoine.entite("ENT-" + c.jeton(), true);
        UUID categorieTerrain = patrimoine.categorie("CTE-" + c.jeton(), terrains.natureTer(), false).getId();
        UUID transformable = fixture.statut("TRF-" + c.jeton(), 250, TYPE_SUCCES, new Statut(false, true, true, 0));
        UUID p = fixture.prospection(c, "PRX-" + c.jeton(), transformable);
        fixture.definir(p, "Area", d("3"));
        fixture.definir(p, "ProspectionPriceProposed", d("10"));

        CorpsJson dialogue = corps().avec(ENTITE_ID, societe.getId()).avec(VOCATION_ID, FixturePatrimoine.VOCATION_PROJET)
                .avec(CATEGORIE_ID, categorieTerrain);
        api.transformer(p, dialogue).andExpect(status().isCreated());

        Tuple t = fixture.lignes("SELECT * FROM StkItem WHERE LndLandId = :id", p).get(0);
        assertThat(decimal(t.get("SalePrice"))).isEqualByComparingTo("10");
        assertThat(decimal(t.get("SalePriceFloor"))).isEqualByComparingTo("10");
    }

    /** §3.3 n° 6 : superficies >= 0 ; seule la prospection exige > 0, une zone de 0 m² est acceptée (LndLandZone.cs sans Verif). */
    @Test
    void r3_3_zoneDeSuperficieNulle_acceptee() throws Exception {
        UUID p = fixture.prospection(c, "ZNU-" + c.jeton());
        fixture.definir(p, "IsFinished", false);

        api.zones(p, List.of(corps().avec(IDENTIFICATION, "Z0").avec(SUPERFICIE, d("0")))).andExpect(status().isOk());
    }

    /** §3.3 n° 6 : valeur hors bornes decimal(26,6) → 400, jamais 500 (ici une valeur dérivée : superficie × CUF). */
    @Test
    void r3_3_6_valeurDeriveeHorsBornes_400() throws Exception {
        UUID p = fixture.prospection(c, "OVF-" + c.jeton());

        api.modifier(p, corps().avec(SUPERFICIE, d("99999999999999999999")).avec(CUF, d("99999999999999999999")))
                .andExpect(status().isBadRequest());
    }

    /** Création : même contrôle de bornes sur une valeur dérivée. */
    @Test
    void r3_3_6_creation_valeurDeriveeHorsBornes_400() throws Exception {
        api.creer(ApiProspectionsTerrain.corpsProspection(c, "OVC-" + c.jeton())
                        .avec(SUPERFICIE, d("99999999999999999999")).avec(CUF, d("99999999999999999999")))
                .andExpect(status().isBadRequest());
    }
}

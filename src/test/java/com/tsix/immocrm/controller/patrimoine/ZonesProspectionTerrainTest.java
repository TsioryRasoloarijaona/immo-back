package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
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
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Zones d'un terrain brut — FEATURE-patrimoine-prospections-terrain.md §3.2, §3.3 n° 2 et §5.4 (ZonesProspectionTerrainTest) :
 * grille réconciliée (créer / modifier / supprimer), identification obligatoire, calculs de zone sans voies d'aménagement
 * (piège OMRAN du plancher reproduit, décision 4), total des zones ≤ superficie nette, id d'une autre prospection → 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ZonesProspectionTerrainTest {

    private static final String ZONES_SQL = "SELECT * FROM LndLandZone WHERE ProspectionId = :id ORDER BY Identification ASC";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;

    private ApiProspectionsTerrain api;
    private Contexte c;
    private UUID prospection;

    @BeforeEach
    void setUp() {
        api = new ApiProspectionsTerrain(mockMvc, patrimoine.administrateur());
        c = fixture.contexte("P7Z");
        prospection = fixture.prospection(c, "ZON-" + c.jeton());
        fixture.definir(prospection, "IsFinished", false);
    }

    private static CorpsJson zone(String identification, String superficie) {
        return corps().avec(IDENTIFICATION, identification).avec(SUPERFICIE, d(superficie));
    }

    private List<Tuple> zones() {
        return fixture.lignes(ZONES_SQL, prospection);
    }

    // ------------------------------------------------------------------ réconciliation

    /** §5.4 : lignes sans id → créées, rattachées à la prospection ; réponse = ZoneTerrainDto[]. */
    @Test
    void r5_4_creation_200_lignesRattachees() throws Exception {
        api.zones(prospection, List.of(zone("Z1", "400"), zone("Z2", "500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        List<Tuple> lignes = zones();
        assertAll(
                () -> assertThat(lignes).extracting(t -> t.get("Identification")).containsExactly("Z1", "Z2"),
                () -> assertThat(lignes).extracting(t -> guid(t.get("ProspectionId")))
                        .containsOnly(guid(prospection)));
    }

    /** §5.4 : id connu = mise à jour, ligne absente = suppression. */
    @Test
    void r5_4_miseAJourEtSuppression() throws Exception {
        UUID gardee = fixture.zone(prospection, "Z1", d("300"));
        fixture.zone(prospection, "Z2", d("200"));

        api.zones(prospection, List.of(zone("Z1-bis", "350").avec(ID, gardee))).andExpect(status().isOk());

        List<Tuple> lignes = zones();
        assertThat(lignes).hasSize(1);
        Tuple t = lignes.get(0);
        assertAll(
                () -> assertThat(guid(t.get("PKey"))).isEqualTo(guid(gardee)),
                () -> assertThat(t.get("Identification")).isEqualTo("Z1-bis"),
                () -> assertThat(decimal(t.get("Area"))).isEqualByComparingTo("350"));
    }

    /** §5.4 : grille vide → toutes les zones supprimées. */
    @Test
    void r5_4_grilleVide_toutSupprime() throws Exception {
        fixture.zone(prospection, "Z1", d("300"));

        api.zones(prospection, List.of()).andExpect(status().isOk());

        assertThat(zones()).isEmpty();
    }

    /** §5.4 : id d'une zone d'une autre prospection → 400, rien ne bouge. */
    @Test
    void r5_4_idDUneAutreProspection_400() throws Exception {
        UUID autre = fixture.prospection(c, "ZOA-" + c.jeton());
        UUID zoneAilleurs = fixture.zone(autre, "ZA", d("100"));

        api.zones(prospection, List.of(zone("Vol", "100").avec(ID, zoneAilleurs))).andExpect(status().isBadRequest());

        Tuple t = fixture.lignes(ZONES_SQL, autre).get(0);
        assertAll(
                () -> assertThat(t.get("Identification")).isEqualTo("ZA"),
                () -> assertThat(zones()).isEmpty());
    }

    /** §3.3 n° 3 : identification obligatoire (LndLandZone.cs:61). */
    @Test
    void r3_3_identificationVide_400() throws Exception {
        api.zones(prospection, List.of(zone("", "100"))).andExpect(status().isBadRequest());

        assertThat(zones()).isEmpty();
    }

    @Test
    void r3_3_identificationAbsente_400() throws Exception {
        api.zones(prospection, List.of(corps().avec(SUPERFICIE, d("100")))).andExpect(status().isBadRequest());

        assertThat(zones()).isEmpty();
    }

    // ------------------------------------------------------------------ total ≤ nette

    /** §3.3 n° 2 : Σ zones (400 + 800) > nette (1 100) → 400 « zones », rien d'écrit. */
    @Test
    void r3_3_2_totalSuperieurALaNette_400() throws Exception {
        api.zones(prospection, List.of(zone("Z1", "400"), zone("Z2", "800")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_ZONES)));

        assertThat(zones()).isEmpty();
    }

    /** §3.3 n° 2 : évalué sur la valeur résultante — une zone existante agrandie fait dépasser le total. */
    @Test
    void r3_3_2_valeurResultante_zoneAgrandie_400() throws Exception {
        UUID z1 = fixture.zone(prospection, "Z1", d("500"));
        fixture.zone(prospection, "Z2", d("500"));

        api.zones(prospection, List.of(zone("Z1", "700").avec(ID, z1), zone("Z2", "500")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_ZONES)));
    }

    // ------------------------------------------------------------------ calculs §3.2

    /** §3.2 exemple : Area 300, Cos 0,5, Cuf 2 → emprise 150, plancher couvert 600 (sans voies d'aménagement). */
    @Test
    void r3_2_calculsDeZone_sensDirect() throws Exception {
        api.zones(prospection, List.of(zone("Z1", "300").avec(COS, d("0.5")).avec(CUF, d("2"))
                        .avec(COEF_VENDABLE, d("0.8")).avec(COEF_COMMERCE, d("0.1")).avec(COEF_BUREAU, d("0.05"))
                        .avec(COEF_PARKING, d("0.2"))))
                .andExpect(status().isOk());

        Tuple t = zones().get(0);
        assertAll(
                () -> assertThat(decimal(t.get("AreaCos"))).isEqualByComparingTo("150"),
                () -> assertThat(decimal(t.get("AreaCuf"))).isEqualByComparingTo("600"),
                () -> assertThat(decimal(t.get("SalableArea"))).isEqualByComparingTo("480"),
                () -> assertThat(decimal(t.get("CommerceArea"))).isEqualByComparingTo("30"),
                () -> assertThat(decimal(t.get("OfficeArea"))).isEqualByComparingTo("15"),
                () -> assertThat(decimal(t.get("UndergroundParkingArea"))).isEqualByComparingTo("60"),
                () -> assertThat(decimal(t.get("HabitatArea"))).isEqualByComparingTo("435"));
    }

    /** §3.2 sens inverse : emprise donnée → Cos = AreaCos / Area. */
    @Test
    void r3_2_sensInverse_emprise_cosDerive() throws Exception {
        api.zones(prospection, List.of(zone("Z1", "300").avec(SUPERFICIE_COS, d("120")))).andExpect(status().isOk());

        assertThat(decimal(zones().get(0).get("Cos"))).isEqualByComparingTo("0.4");
    }

    /** §3.2 piège OMRAN reproduit (décision 4) : plancher donné → Cuf = AreaCuf / (Area × Cos). */
    @Test
    void d13_4_sensInverse_plancher_cufSurAreaFoisCos() throws Exception {
        api.zones(prospection, List.of(zone("Z1", "300").avec(COS, d("0.5")).avec(SUPERFICIE_CUF, d("600"))))
                .andExpect(status().isOk());

        Tuple t = zones().get(0);
        assertAll(
                () -> assertThat(decimal(t.get("Cuf"))).isEqualByComparingTo("4"),
                () -> assertThat(decimal(t.get("AreaCuf"))).isEqualByComparingTo("600"));
    }

    /** §5.4 : la réponse porte superficieHabitat calculée. */
    @Test
    void r5_4_reponse_superficieHabitat() throws Exception {
        api.zones(prospection, List.of(zone("Z1", "300").avec(CUF, d("2")).avec(COEF_VENDABLE, d("1"))
                        .avec(COEF_BUREAU, d("0.1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]." + SUPERFICIE_HABITAT).value(570.0));
    }
}

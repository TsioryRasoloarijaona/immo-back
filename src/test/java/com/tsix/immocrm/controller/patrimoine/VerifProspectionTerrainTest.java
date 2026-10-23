package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.corpsProspection;
import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrôles {@code Verif} et saisie — FEATURE-patrimoine-prospections-terrain.md §3.3 (VerifProspectionTerrainTest) : 400
 * avec le message OMRAN exact quand le spec le donne, rien d'écrit ; obligatoires, références de la bonne nature, ville du
 * pays, lien Google, longueurs, décimaux hors bornes et négatifs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VerifProspectionTerrainTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private FixtureTerrains terrains;

    private ApiProspectionsTerrain api;
    private Contexte c;

    @BeforeEach
    void setUp() {
        api = new ApiProspectionsTerrain(mockMvc, patrimoine.administrateur());
        c = fixture.contexte("P7V");
        fixture.formule("MA");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private void refuseRienEcrit(String code, CorpsJson corps) throws Exception {
        api.creer(corps).andExpect(status().isBadRequest());
        assertThat(fixture.prospectionsDeCode(code)).isZero();
    }

    // ------------------------------------------------------------------ n° 1 superficie

    /** §3.3 n° 1 : Area ≤ 0 → « La valeur de la supérficie doit être strictement supérieure à 0 ! ». */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"0", "-5"})
    void r3_3_1_superficieNonPositive_400_messageOmran(String superficie) throws Exception {
        String code = code("SUP");

        api.creer(corpsProspection(c, code).avec(SUPERFICIE, new BigDecimal(superficie)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SUPERFICIE)));

        assertThat(fixture.prospectionsDeCode(code)).isZero();
    }

    @Test
    void r3_3_1_patch_superficieZero_400_ligneInchangee() throws Exception {
        UUID id = fixture.prospection(c, code("PSZ"));

        api.modifier(id, corps().avec(SUPERFICIE, d("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SUPERFICIE)));

        assertThat(decimal(fixture.ligne(id).get("Area"))).isEqualByComparingTo("1200");
    }

    // ------------------------------------------------------------------ n° 2 zones (valeur résultante)

    /** §3.3 n° 2 : PATCH qui ramène la nette sous le total des zones → 400 « zones » évalué sur la valeur résultante. */
    @Test
    void r3_3_2_patch_netteSousLeTotalDesZones_400() throws Exception {
        UUID id = fixture.prospection(c, code("PZN"));
        fixture.zone(id, "Z1", d("600"));
        fixture.zone(id, "Z2", d("400"));

        api.modifier(id, corps().avec(SUPERFICIE_EQUIPEMENTS, d("300")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_ZONES)));

        assertThat(decimal(fixture.ligne(id).get("EquipementsArea"))).isEqualByComparingTo("100");
    }

    /** §3.3 n° 2 : total des zones égal ou inférieur à la nette résultante → accepté. */
    @Test
    void r3_3_2_patch_netteAuDessusDuTotal_200() throws Exception {
        UUID id = fixture.prospection(c, code("PZO"));
        fixture.zone(id, "Z1", d("600"));
        fixture.zone(id, "Z2", d("400"));

        api.modifier(id, corps().avec(SUPERFICIE_EQUIPEMENTS, d("150"))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ n° 3 obligatoires

    /** §3.3 n° 3 : code, description, pays, ville, catégorie, type exigés. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CODE, DESCRIPTION, VILLE_ID, CATEGORIE_ID, TYPE_ID})
    void r3_3_3_champObligatoireAbsent_400_rienEcrit(String champ) throws Exception {
        String code = code("OBL");

        refuseRienEcrit(code, corpsProspection(c, code).sans(champ));
    }

    /** §3.3 n° 3 : code et description vides (chaînes) → 400. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CODE, DESCRIPTION})
    void r3_3_3_texteObligatoireVide_400(String champ) throws Exception {
        api.creer(corpsProspection(c, code("VID")).avec(champ, "")).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ n° 5 références

    /** §3.3 n° 5 : ville d'un autre pays → 400. */
    @Test
    void r3_3_5_villeDUnAutrePays_400() throws Exception {
        String code = code("VAP");
        UUID autrePays = patrimoine.pays("PAYS2-" + c.jeton()).getId();
        UUID villeAilleurs = terrains.ville("VIL2-" + c.jeton(), autrePays);

        refuseRienEcrit(code, corpsProspection(c, code).avec(VILLE_ID, villeAilleurs));
    }

    /** §3.3 n° 5 : catégorie de nature TER (et non PROS TER) → 400. */
    @Test
    void r3_3_5_categorieDUneAutreNature_400() throws Exception {
        String code = code("CAN");
        UUID categorieTerrain = patrimoine.categorie("CTER-" + c.jeton(), terrains.natureTer(), false).getId();

        refuseRienEcrit(code, corpsProspection(c, code).avec(CATEGORIE_ID, categorieTerrain));
    }

    /** §3.3 n° 5 : type de nature PROS TER (et non TER) → 400. */
    @Test
    void r3_3_5_typeDUneAutreNature_400() throws Exception {
        String code = code("TAN");
        UUID typeProspection = patrimoine.type("TPRO-" + c.jeton(), fixture.natureProsTer(), false).getId();

        refuseRienEcrit(code, corpsProspection(c, code).avec(TYPE_ID, typeProspection));
    }

    /** §3.3 n° 5 : référence inconnue → 400 nommant le champ (pas 500). */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIE_ID, TYPE_ID, NIVEAU_ID, VOCATION_ID, ORIENTATION_ID, COURTIER_ID, NOTAIRE_ID,
            VILLE_ID})
    void r3_3_5_referenceInconnue_400_nommantLeChamp(String champ) throws Exception {
        String code = code("RIN");

        api.creer(corpsProspection(c, code).avec(champ, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(champ)));

        assertThat(fixture.prospectionsDeCode(code)).isZero();
    }

    /** §3.3 n° 5 : lien Google saisi sans le préfixe https://www.google → 400 (message OMRAN front l.304). */
    @Test
    void r3_3_5_lienGoogleInvalide_400() throws Exception {
        String code = code("GMP");

        api.creer(corpsProspection(c, code).avec(LIEN_GOOGLE, "https://maps.example.com/x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_GMAP)));

        assertThat(fixture.prospectionsDeCode(code)).isZero();
    }

    /** §3.3 n° 5 : lien Google vide accepté (contrôle seulement s'il est saisi). */
    @Test
    void r3_3_5_lienGoogleVide_201() throws Exception {
        api.creer(corpsProspection(c, code("GMV")).avec(LIEN_GOOGLE, "")).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ n° 6 textes et décimaux

    /** §3.3 n° 6 : Notes, Anomalies, ZoneIdentification, DealerConnectionConditions, LandBase ≤ 1 000. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {NOTES, ANOMALIES, IDENTIFICATION_ZONE, CONDITIONS_RACCORDEMENT, ASSIETTE_FONCIERE})
    void r3_3_6_texteLongPlusDe1000_400(String champ) throws Exception {
        String code = code("TL1");

        refuseRienEcrit(code, corpsProspection(c, code).avec(champ, "x".repeat(1001)));
    }

    /** §3.3 n° 6 : Adress, Owner, Registry*, ImmatriculationCPF ≤ 255. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ADRESSE, PROPRIETAIRES, NUMERO_ENREGISTREMENT, LIEU_ENREGISTREMENT, CPF_IMMATRICULATION})
    void r3_3_6_texteCourtPlusDe255_400(String champ) throws Exception {
        String code = code("TL2");

        refuseRienEcrit(code, corpsProspection(c, code).avec(champ, "x".repeat(256)));
    }

    /** §3.3 n° 6 : superficies, coefficients, prix ≥ 0. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SUPERFICIE_EQUIPEMENTS, COS, CUF, COEF_VENDABLE, COEF_COMMERCE, COEF_BUREAU, COEF_PARKING,
            PRIX_M2, PRIX_PROPOSE})
    void r3_3_6_valeurNegative_400(String champ) throws Exception {
        String code = code("NEG");

        refuseRienEcrit(code, corpsProspection(c, code).avec(champ, d("-1")));
    }

    /** §3.3 n° 6 : décimal hors bornes de decimal(26,6) → 400, pas 500 (écart P6 à ne pas reproduire). */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SUPERFICIE, PRIX_PROPOSE, COS})
    void r3_3_6_decimalHorsBornes_400(String champ) throws Exception {
        String code = code("HB");

        refuseRienEcrit(code, corpsProspection(c, code).avec(champ, d("123456789012345678901234567")));
    }
}

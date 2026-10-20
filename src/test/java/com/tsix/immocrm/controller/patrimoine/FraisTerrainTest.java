package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains.TypeFrais;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.DATE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.FOURNISSEUR_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.FRAIS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.MONTANT;
import static com.tsix.immocrm.fixtures.FixtureTerrains.NOTES;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TYPE_FRAIS_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Frais d'un terrain ({@code StkExpense}) — FEATURE-patrimoine-terrains.md §2 et §6.2 (FraisTerrainTest) : PUT liste
 * réconciliée (créer / modifier / supprimer), Amount > 0, fournisseur par défaut = celui du terrain (NULL des deux côtés
 * → 400), ligne validée ni modifiable ni supprimable (409).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FraisTerrainTest {

    private static final String FRAIS_DU_TERRAIN = "SELECT * FROM StkExpense WHERE StkItemId = :id";
    private static final String FRAIS_PAR_ID = "SELECT * FROM StkExpense WHERE Pkey = :id";
    private static final String MESSAGE_SANS_FOURNISSEUR = "Renseignez le fournisseur du frais ou du terrain";
    private static final LocalDate DATE_FRAIS = LocalDate.of(2026, 4, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;
    private UUID terrain;
    private TypeFrais type;
    private UUID fournisseur;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = terrains.contexte("P7F");
        terrain = terrains.terrain(c, "TER-" + c.jeton());
        type = terrains.typeFrais(c.jeton(), true, true);
        fournisseur = terrains.fournisseur("FRN-" + c.jeton(), null);
    }

    private CorpsJson ligne(BigDecimal montant) {
        return corps().avec(DATE, DATE_FRAIS).avec(MONTANT, montant).avec(TYPE_FRAIS_ID, type.id())
                .avec(FOURNISSEUR_ID, fournisseur);
    }

    /** Ligne existante renvoyée telle que la fixture l'a écrite (date 15/03/2026, notes vides). */
    private CorpsJson ligneExistante(UUID id, BigDecimal montant) {
        return corps().avec(ID, id).avec(DATE, LocalDate.of(2026, 3, 15)).avec(MONTANT, montant).avec(NOTES, "")
                .avec(TYPE_FRAIS_ID, type.id()).avec(FOURNISSEUR_ID, fournisseur);
    }

    private ResultActions enregistrer(String corps) throws Exception {
        return mockMvc.perform(put(FRAIS, terrain).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private List<Tuple> fraisDuTerrain() {
        return terrains.lignes(FRAIS_DU_TERRAIN, terrain);
    }

    // ------------------------------------------------------------------ créer / modifier / supprimer

    @Test
    void r2_put_creeLaLigne_colonnesEcrites() throws Exception {
        enregistrer(lignes(ligne(new BigDecimal("1500.50")).avec(NOTES, "Honoraires"))).andExpect(status().isOk());

        assertThat(fraisDuTerrain()).singleElement().satisfies(f -> assertAll(
                () -> assertThat(horodatage(f.get("Date")).toLocalDate()).isEqualTo(DATE_FRAIS),
                () -> assertThat(decimal(f.get("Amount"))).isEqualByComparingTo("1500.50"),
                () -> assertThat(f.get("Notes")).isEqualTo("Honoraires"),
                () -> assertThat(f.get("IsValidated")).isEqualTo(false),
                () -> assertThat(guid(f.get("StkExpenseTypeId"))).isEqualTo(guid(type.id())),
                () -> assertThat(guid(f.get("CfgSupplierId"))).isEqualTo(guid(fournisseur))));
    }

    /** §2 : Notes vaut '' (jamais NULL) quand elle n'est pas saisie. */
    @Test
    void r2_put_notesAbsentes_chaineVide() throws Exception {
        enregistrer(lignes(ligne(new BigDecimal("100")))).andExpect(status().isOk());

        assertThat(fraisDuTerrain()).singleElement().satisfies(f -> assertThat(f.get("Notes")).isEqualTo(""));
    }

    @Test
    void r2_put_ligneConnue_modifiee() throws Exception {
        UUID frais = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("1000"), false);

        enregistrer(lignes(ligneExistante(frais, new BigDecimal("2000")))).andExpect(status().isOk());

        assertThat(decimal(terrains.ligne(FRAIS_PAR_ID, frais).get("Amount"))).isEqualByComparingTo("2000");
    }

    @Test
    void r2_put_ligneAbsente_supprimee() throws Exception {
        UUID garde = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("1000"), false);
        UUID retire = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("3000"), false);

        enregistrer(lignes(ligneExistante(garde, new BigDecimal("1000")))).andExpect(status().isOk());

        assertThat(fraisDuTerrain()).extracting(f -> guid(f.get("Pkey")))
                .containsExactly(guid(garde))
                .doesNotContain(guid(retire));
    }

    // ------------------------------------------------------------------ refus 400

    /** §2 : Amount obligatoire et > 0. */
    @ParameterizedTest(name = "montant {0}")
    @ValueSource(strings = {"0", "-5"})
    void r2_montantNulOuNegatif_400_rienEcrit(String montant) throws Exception {
        enregistrer(lignes(ligne(new BigDecimal(montant)))).andExpect(status().isBadRequest());

        assertThat(fraisDuTerrain()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {DATE, MONTANT, TYPE_FRAIS_ID})
    void r2_champObligatoireAbsent_400_rienEcrit(String champ) throws Exception {
        enregistrer(lignes(ligne(new BigDecimal("100")).sans(champ))).andExpect(status().isBadRequest());

        assertThat(fraisDuTerrain()).isEmpty();
    }

    @Test
    void r2_notesDePlusDe1000Caracteres_400() throws Exception {
        enregistrer(lignes(ligne(new BigDecimal("100")).avec(NOTES, "n".repeat(1001))))
                .andExpect(status().isBadRequest());

        assertThat(fraisDuTerrain()).isEmpty();
    }

    /** §2 : fournisseur NULL sur le frais et sur le terrain (5 terrains sur 5 en prod) → 400 « Renseignez… ». */
    @Test
    void r2_fournisseurAbsentDuFraisEtDuTerrain_400_message() throws Exception {
        enregistrer(lignes(ligne(new BigDecimal("100")).sans(FOURNISSEUR_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SANS_FOURNISSEUR)));

        assertThat(fraisDuTerrain()).isEmpty();
    }

    /** §2 : défaut = CfgSupplierId du terrain. */
    @Test
    void r2_fournisseurAbsent_defautFournisseurDuTerrain() throws Exception {
        UUID proprietaire = terrains.fournisseur("PRO-" + c.jeton(), null);
        terrains.definir(terrain, "CfgSupplierId", proprietaire);

        enregistrer(lignes(ligne(new BigDecimal("100")).sans(FOURNISSEUR_ID))).andExpect(status().isOk());

        assertThat(fraisDuTerrain()).singleElement()
                .satisfies(f -> assertThat(guid(f.get("CfgSupplierId"))).isEqualTo(guid(proprietaire)));
    }

    // ------------------------------------------------------------------ ligne validée intouchable (409)

    @Test
    void r2_ligneValidee_modifiee_409_inchangee() throws Exception {
        UUID valide = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("8000"), true);

        enregistrer(lignes(ligneExistante(valide, new BigDecimal("9000")))).andExpect(status().isConflict());

        assertThat(decimal(terrains.ligne(FRAIS_PAR_ID, valide).get("Amount"))).isEqualByComparingTo("8000");
    }

    @Test
    void r2_ligneValidee_absenteDuPut_409_conservee() throws Exception {
        UUID valide = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("8000"), true);

        enregistrer(lignes(ligne(new BigDecimal("100")))).andExpect(status().isConflict());

        assertThat(fraisDuTerrain()).extracting(f -> guid(f.get("Pkey"))).containsExactly(guid(valide));
    }

    /** §2 : une ligne validée renvoyée inchangée n'est ni modifiée ni supprimée — le reste du PUT s'applique. */
    @Test
    void r2_ligneValidee_renvoyeeInchangee_200_nouvelleLigneCreee() throws Exception {
        UUID valide = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("8000"), true);

        enregistrer(lignes(ligneExistante(valide, new BigDecimal("8000")), ligne(new BigDecimal("100"))))
                .andExpect(status().isOk());

        assertThat(fraisDuTerrain()).hasSize(2).extracting(f -> guid(f.get("Pkey"))).contains(guid(valide));
    }

    // ------------------------------------------------------------------ visibilité

    @Test
    void r1_2_terrainDUneSocieteInactive_404_rienEcrit() throws Exception {
        mockMvc.perform(get("/api/terrains/{id}", terrain).with(connecte(admin))).andExpect(status().isOk());
        entites.actif(c.entite().getId(), false);

        enregistrer(lignes(ligne(new BigDecimal("100")))).andExpect(status().isNotFound());

        assertThat(fraisDuTerrain()).isEmpty();
    }
}

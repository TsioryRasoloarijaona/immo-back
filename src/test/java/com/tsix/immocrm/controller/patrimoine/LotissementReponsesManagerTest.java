package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.VOCATION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENTS;
import static com.tsix.immocrm.fixtures.FixtureLotissements.PRESENTATION_ID;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TYPE_IMPORT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.corpsLot;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Réponses 3 à 6 du manager au test-writer (FEATURE-patrimoine-lotissements.md, 27/10) : présentation d'une autre tranche,
 * tranche inconnue à la création, frais validé à la suppression, colonne Nature de l'import. Jeu validé pour de bon.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotissementReponsesManagerTest {

    private static final String NATURE = "Nature";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;
    @Autowired private DataSource dataSource;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CM");
    }

    @AfterEach
    void nettoyer() {
        new JdbcTemplate(dataSource).update("DELETE FROM StkExpense WHERE Notes = ?", j.jeton());
        fixture.nettoyer(j);
    }

    @Test
    void reponse3_patchPresentationDUneAutreTranche_400_presentationInchangee() throws Exception {
        UUID lot = fixture.lot(j, j.code("MPR"));
        UUID ailleurs = fixture.presentation(j, j.autreTranche(), "PRA-" + j.jeton());

        mockMvc.perform(patch(LOTISSEMENT, lot).with(connecte(j.admin())).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(PRESENTATION_ID, ailleurs).json()))
                .andExpect(status().isBadRequest());

        assertThat(fixture.ligne(lot)).containsEntry("StkPresentationTypeId", null);
    }

    @Test
    void reponse3_patchPresentationDeSaTranche_200() throws Exception {
        UUID lot = fixture.lot(j, j.code("MPT"));
        UUID ici = fixture.presentation(j, j.tranche(), "PRT-" + j.jeton());

        mockMvc.perform(patch(LOTISSEMENT, lot).with(connecte(j.admin())).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(PRESENTATION_ID, ici).json()))
                .andExpect(status().isOk());

        assertThat(guid(fixture.ligne(lot).get("StkPresentationTypeId"))).isEqualTo(guid(ici));
    }

    @Test
    void reponse4_creationSurTrancheInconnue_400_rienEcrit() throws Exception {
        String code = j.code("MTI");

        mockMvc.perform(post(LOTISSEMENTS).with(connecte(j.admin())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLot(j, code).avec(TRANCHE_ID, UUID.randomUUID()).json()))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItem WHERE Code = ?", code)).isZero();
    }

    @Test
    void reponse5_lotPortantUnFraisValide_409_lotEtFraisGardes() throws Exception {
        UUID lot = fixture.lot(j, j.code("MFV"));
        new JdbcTemplate(dataSource).update("""
                        INSERT INTO StkExpense (Pkey, [Date], Amount, IsValidated, Notes, StkItemId)
                        VALUES (?, '20260315', 1000, 1, ?, ?)""",
                UUID.randomUUID().toString(), j.jeton(), lot.toString());

        mockMvc.perform(delete(LOTISSEMENT, lot).with(connecte(j.admin()))).andExpect(status().isConflict());

        assertThat(fixture.existe(lot)).isTrue();
    }

    @ParameterizedTest(name = "Nature « {0} » → {1}")
    @CsvSource({"ter,CREATION", "TER,CREATION", "bat,ERREUR"})
    void reponse6_colonneNature_codeSansCasse(String nature, String attendu) throws Exception {
        assertThat(resultatNature(nature)).isEqualTo(attendu);
    }

    @Test
    void reponse6_colonneNature_libelleSansCasse() throws Exception {
        String libelle = new JdbcTemplate(dataSource).queryForObject(
                "SELECT Description FROM StkKind WHERE Code = N'TER'", String.class);

        assertThat(resultatNature(Objects.requireNonNull(libelle).toLowerCase(Locale.ROOT))).isEqualTo(CREATION);
    }

    private String resultatNature(String nature) throws Exception {
        String code = j.code("MNA");
        Map<String, Object> ligne = colonnes(PIVOT, code, CODE, code, DESCRIPTION, "Lot " + code, NATURE, nature,
                VOCATION, j.codeVocation(), CATEGORIE, j.ter().codeCategorie(), TYPE, j.ter().codeType(),
                SURFACE_PROV, new BigDecimal("1000"));
        String bilan = corps(mockMvc.perform(requete(TYPE_IMPORT, j.tranche(), true, fichier(List.of(ligne)),
                connecte(j.admin()))).andExpect(status().isOk()).andReturn());
        assertThat(bilan).isNotBlank();
        return resultat(bilan, 2);
    }
}

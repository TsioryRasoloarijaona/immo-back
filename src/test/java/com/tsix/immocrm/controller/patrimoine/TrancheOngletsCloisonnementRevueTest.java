package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue règle 6 — cloisonnement entre tranches (FEATURE-patrimoine-tranche-onglets.md §1, §4, §8.6) : un PUT ne
 * touche jamais une ligne d'une autre tranche (id de ligne étranger = 400, ligne intacte), un DELETE n'efface rien
 * d'une tranche voisine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheOngletsCloisonnementRevueTest {

    private static final String NIVEAU_ID = "niveauTerrainId";
    private static final String TERRAIN_ID = "terrainId";
    private static final String ETAGE_DE = "SELECT COUNT(*) FROM CfgTrancheLevel WHERE PKey = :id AND CfgTrancheId = :autre";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private FixtureTrancheOnglets onglets;

    private AdmUser admin;
    private CfgTranche cible;
    private CfgTranche voisine;
    private CfgTranche inactive;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1BR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        cible = fixture.tranche("TR-C-" + jeton, fixture.projet("PRJ-C-" + jeton, fixture.entite("ENT-C-" + jeton, true)));
        voisine = fixture.tranche("TR-V-" + jeton, fixture.projet("PRJ-V-" + jeton, fixture.entite("ENT-V-" + jeton, true)));
        inactive = fixture.tranche("TR-I-" + jeton,
                fixture.projet("PRJ-I-" + jeton, fixture.entite("ENT-I-" + jeton, false)));
    }

    @Test
    void put_etages_idDeLigneDUneAutreTranche_400_ligneIntacte() throws Exception {
        UUID niveau = fiche.niveauTerrain("NIV-" + jeton);
        UUID autreNiveau = fiche.niveauTerrain("NIV2-" + jeton);
        UUID ligneVoisine = onglets.ligneEtage(voisine.getId(), niveau);

        mockMvc.perform(put("/api/tranches/{id}/etages", cible.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lignes(corps().avec("id", ligneVoisine).avec(NIVEAU_ID, autreNiveau))))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(onglets.compter(ETAGE_DE, ligneVoisine, voisine.getId())).isEqualTo(1),
                () -> assertThat(onglets.compter(
                        "SELECT COUNT(*) FROM CfgTrancheLevel WHERE PKey = :id AND LndLevelId = :autre",
                        ligneVoisine, niveau)).isEqualTo(1));
    }

    @Test
    void put_terrains_idDeLigneDUneTrancheDEntiteInactive_400_ligneIntacte() throws Exception {
        UUID terrain = onglets.terrain("TER-" + jeton);
        UUID ligneInactive = onglets.lienTerrain(inactive.getId(), terrain, BigDecimal.ONE, 1);

        mockMvc.perform(put("/api/tranches/{id}/terrains", cible.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lignes(corps().avec("id", ligneInactive).avec(TERRAIN_ID, terrain)
                                .avec("ratio", new BigDecimal("0.5")))))
                .andExpect(status().isBadRequest());

        assertThat(onglets.compter("SELECT COUNT(*) FROM CfgTrancheStkItem WHERE PKey = :id AND CfgTrancheId = :autre "
                + "AND Ratio = 1", ligneInactive, inactive.getId())).isEqualTo(1);
    }

    @Test
    void put_listeVide_neSupprimePasLesLignesDUneAutreTranche() throws Exception {
        UUID ligneVoisine = onglets.ligneEtage(voisine.getId(), fiche.niveauTerrain("NIV3-" + jeton));

        mockMvc.perform(put("/api/tranches/{id}/etages", cible.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lignes\":[]}"))
                .andExpect(status().isOk());

        assertThat(onglets.compter(ETAGE_DE, ligneVoisine, voisine.getId())).isEqualTo(1);
    }

    @Test
    void delete_neTouchePasAuxOngletsNiHabilitationsDUneTrancheVoisine() throws Exception {
        UUID terrain = onglets.terrain("TER2-" + jeton);
        UUID lienVoisin = onglets.lienTerrain(voisine.getId(), terrain, BigDecimal.ONE, 1);
        onglets.lienTerrain(cible.getId(), terrain, BigDecimal.ONE, 1);
        UUID etageVoisin = onglets.ligneEtage(voisine.getId(), fiche.niveauTerrain("NIV4-" + jeton));
        onglets.habilitationTranche(admin.getId(), voisine.getId());

        mockMvc.perform(delete("/api/tranches/{id}", cible.getId()).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id", voisine.getId()))
                        .isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheStkItem WHERE PKey = :id", lienVoisin))
                        .isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheLevel WHERE PKey = :id", etageVoisin))
                        .isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM StkItem WHERE PKey = :id", terrain)).isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM AdmUserHabilitation WHERE CfgTrancheId = :id",
                        voisine.getId())).isEqualTo(1));
    }
}

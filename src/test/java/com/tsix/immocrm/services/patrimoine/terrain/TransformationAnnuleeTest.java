package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.lnd.LndLand;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Statut;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.NettoyageProspectionsTerrain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.CATEGORIE_ID;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.ENTITE_ID;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.TYPE_SUCCES;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.VOCATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Même scénario que TransformationProspectionTerrainRollbackTest (§5.7, rollback complet), stub posé dans l'ordre que
 * Mockito exige : {@code doThrow().when(spy)} avant les matchers (voir RAPPORT, « Tests contestés »).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransformationAnnuleeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private EcritureTerrain ecritureTerrain;

    private NettoyageProspectionsTerrain nettoyage;
    private AdmUser admin;
    private CfgCompany societe;
    private UUID categorieTerrain;
    private UUID prospection;
    private String code;

    @BeforeEach
    void setUp() {
        nettoyage = new NettoyageProspectionsTerrain(dataSource, transactionManager);
        nettoyage.noterConfigurationAbsente();
        admin = nettoyage.valider(patrimoine::administrateur);
        nettoyage.suivre("AdmUser", admin.getId());
        Contexte c = nettoyage.commeAdmin(admin, () -> fixture.contexte("P7Q"));
        nettoyage.suivre(c);
        nettoyage.commeAdmin(admin, () -> {
            patrimoine.referentielArticle();
            terrains.configurationTerrain();
            return null;
        });
        societe = nettoyage.commeAdmin(admin, () -> patrimoine.entite("ENT-" + c.jeton(), true));
        nettoyage.suivre("CfgCompany", societe.getId());
        categorieTerrain = nettoyage.commeAdmin(admin,
                () -> patrimoine.categorie("CTE-" + c.jeton(), terrains.natureTer(), false).getId());
        nettoyage.suivre("StkItemCategory", categorieTerrain);
        UUID transformable = nettoyage.commeAdmin(admin,
                () -> fixture.statut("TRF-" + c.jeton(), 250, TYPE_SUCCES, new Statut(false, true, true, 0)));
        nettoyage.suivre("StrStatus", transformable);
        code = "RBQ-" + c.jeton();
        prospection = nettoyage.commeAdmin(admin, () -> fixture.prospection(c, code, transformable));
        nettoyage.suivreProspection(prospection);
    }

    @AfterEach
    void nettoyer() {
        Mockito.reset(ecritureTerrain);
        nettoyage.nettoyer();
    }

    @Test
    void echecDeLaCompletion_aucunTerrain_prospectionNiArchiveeNiTransformee() throws Exception {
        Mockito.doThrow(new IllegalStateException("Echec simule de la completion")).when(ecritureTerrain)
                .completerDepuisProspection(ArgumentMatchers.any(StkItem.class), ArgumentMatchers.any(LndLand.class));

        int statut = new ApiProspectionsTerrain(mockMvc, admin).transformer(prospection, corps()
                        .avec(ENTITE_ID, societe.getId()).avec(VOCATION_ID, FixturePatrimoine.VOCATION_PROJET)
                        .avec(CATEGORIE_ID, categorieTerrain))
                .andReturn().getResponse().getStatus();

        Integer terrainsIssus = nettoyage.jdbc().queryForObject("SELECT COUNT(*) FROM StkItem WHERE LndLandId = ? OR Code = ?",
                Integer.class, prospection.toString(), code);
        Map<String, Object> ligne = nettoyage.jdbc().queryForMap(
                "SELECT IsInArchive, IsTransformedToLand FROM LndLand WHERE PKey = ?", prospection.toString());
        assertAll(
                () -> assertThat(statut).isEqualTo(500),
                () -> assertThat(terrainsIssus).isZero(),
                () -> assertThat(ligne).containsEntry("IsInArchive", false).containsEntry("IsTransformedToLand", false));
    }
}

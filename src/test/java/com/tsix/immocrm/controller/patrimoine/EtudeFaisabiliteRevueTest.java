package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P8a : une écriture sans rapport avec le bloc A (libellé seul) ne doit pas changer les surfaces ni les montants
 * d'une étude saisie par ses montants (FEATURE-patrimoine-faisabilite.md §8 bis.6, §8 ter.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudeFaisabiliteRevueTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private EntityManager entityManager;

    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
    }

    private static BigDecimal valeur(String json, String champ) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$.valeurs." + champ)));
    }

    /** ASM TERRAIN 1 saisi par la superficie des équipements (8 404) et un prix de terrain global non multiple de la superficie. */
    @Test
    void patchDuLibelle_neRecalculePasLesMontantsSaisis() throws Exception {
        CorpsJson corps = FixtureFaisabilite.fiche("REV-" + jeton, societe.getId())
                .avec(SUPERFICIE, d("24404")).avec(SUPERFICIE_EQUIPEMENTS, d("8404"))
                .avec(COEF_EMPRISE, d("0.148172")).avec(CUF, d("0.717751")).avec(COEF_VENDABLE, d("1"))
                .avec(PRIX_TERRAIN, d("1000000")).avec(TAUX_TVA_TERRAIN, BigDecimal.ZERO);
        String cree = api.creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(cree, "$.id"));

        String apres = api.modifier(id, corps().avec(DESCRIPTION, "Libelle seul " + jeton).json())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(valeur(cree, SUPERFICIE_NETTE)).as("création").isCloseTo(d("16000"), within(TOLERANCE)),
                () -> assertThat(valeur(apres, SUPERFICIE_EQUIPEMENTS)).as(SUPERFICIE_EQUIPEMENTS)
                        .isCloseTo(d("8404"), within(TOLERANCE)),
                () -> assertThat(valeur(apres, SUPERFICIE_NETTE)).as(SUPERFICIE_NETTE)
                        .isCloseTo(d("16000"), within(TOLERANCE)),
                () -> assertThat(valeur(apres, EMPRISE_AU_SOL)).as(EMPRISE_AU_SOL)
                        .isCloseTo(d("2370.752"), within(TOLERANCE)),
                () -> assertThat(valeur(apres, PRIX_TERRAIN)).as(PRIX_TERRAIN)
                        .isCloseTo(d("1000000"), within(TOLERANCE)),
                () -> assertThat(valeur(apres, COUT_TOTAL_TERRAIN)).as(COUT_TOTAL_TERRAIN)
                        .isCloseTo(d("1000000"), within(TOLERANCE)));
    }

    private UUID etudeAvecTranche(String suffixe) throws Exception {
        UUID id = api.etude(yestest(FixtureFaisabilite.fiche(suffixe + "-" + jeton, societe.getId())));
        UUID tranche = patrimoine.tranche("T" + suffixe + "-" + jeton,
                patrimoine.projet("P" + suffixe + "-" + jeton, societe)).getId();
        api.tranches(id, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, tranche, d("1"))))).andExpect(status().isOk());
        return id;
    }

    /**
     * P8b-1 (FEATURE-patrimoine-faisabilite-workflow.md §2.3) : retirer une tranche avec lignes par tranche devient permis ;
     * ces lignes dérivées sont reconstruites par le recalcul (aucune sans tranche), plus de 409.
     */
    @Test
    void retraitDUneTrancheAvecPrevision_permis_lignesParTrancheEffacees() throws Exception {
        UUID id = etudeAvecTranche("PRV");
        UUID ligne = fixture.ligneTranche(id, "Tranche 1");
        entityManager.createNativeQuery("INSERT INTO StkFSPrevisionStkTranche (PKey, Amount, FeasibilityStudyId, StkTrancheId) "
                + "VALUES (NEWID(), 10, :etude, :tranche)").setParameter("etude", id).setParameter("tranche", ligne)
                .executeUpdate();

        api.tranches(id, lignes(new String[0])).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyCfgTranche WHERE FeasibilityStudyId = :id",
                        id)).isZero(),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkFSPrevisionStkTranche WHERE FeasibilityStudyId = :id",
                        id)).isZero());
    }

    /** §2.4 : changer de société laisserait des tranches d'une autre société : 400. */
    @Test
    void changementDeSociete_avecTranches_400() throws Exception {
        UUID id = etudeAvecTranche("SOC");
        CfgCompany autre = patrimoine.entite("AUT-" + jeton, true);

        api.modifier(id, corps().avec(SOCIETE_ID, autre.getId()).json()).andExpect(status().isBadRequest());
    }
}

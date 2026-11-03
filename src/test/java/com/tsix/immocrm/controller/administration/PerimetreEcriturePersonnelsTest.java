package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixturePersonnels;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsPhysique;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePersonnels.employe;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Décision Q2 de FEATURE-admin-personnels.md : un PUT ne touche aucune colonne hors de la liste de la famille. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreEcriturePersonnelsTest {

    private static final List<String> COLONNES_HORS_PERIMETRE = List.of("Ice", "FiscalCode", "FiscalRegistry", "FiscalDouane",
            "CodeCountable", "CodeCountableCollective", "RsRatio", "VATRatio", "PaymentDelay", "AdmLiabilityId", "IsExtern");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;
    @Autowired private FixturePersonnels personnels;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private UUID employeRempli() {
        return employe(j, "E1").avec("Ice", "ICE-1").avec("FiscalCode", "IF-1").avec("FiscalRegistry", "RC-1")
                .avec("FiscalDouane", "DOU-1").avec("CodeCountable", "411").avec("CodeCountableCollective", "4111")
                .avec("RsRatio", new BigDecimal("0.1")).avec("VATRatio", new BigDecimal("0.2")).avec("PaymentDelay", 30)
                .avec("AdmLiabilityId", j.parent("AdmLiability")).avec("IsExtern", true).inserer();
    }

    private void verifierInchangees(UUID id, Map<String, Object> corps) throws Exception {
        Map<String, Object> avant = personnels.colonnes(id);

        mockMvc.perform(modifier(PERSONNELS.famille(), id, corps, connecte(j.admin()))).andExpect(status().isOk());

        Map<String, Object> apres = personnels.colonnes(id);
        assertThat(COLONNES_HORS_PERIMETRE).hasSize(11)
                .allSatisfy(c -> assertThat(apres.get(c)).as(c).isNotNull().isEqualTo(avant.get(c)));
    }

    @Test
    void put_champsOmis_colonnesHorsPerimetreGardees() throws Exception {
        verifierInchangees(employeRempli(), corpsPhysique(j, "Amal", "Benali"));
    }

    @Test
    void put_champsEnvoyes_ignores() throws Exception {
        Map<String, Object> corps = corpsPhysique(j, "Amal", "Benali");
        corps.putAll(Map.of("ice", "AUTRE", "matriculeFiscal", "AUTRE", "registreCommerce", "AUTRE", "douane", "AUTRE",
                "codeComptable", "AUTRE", "codeComptableCollectif", "AUTRE", "tauxRetenue", 9, "tauxTva", 9, "delaiPaiement", 9,
                "estPrestataire", false));
        corps.put("assujettissementId", null);

        verifierInchangees(employeRempli(), corps);
    }
}

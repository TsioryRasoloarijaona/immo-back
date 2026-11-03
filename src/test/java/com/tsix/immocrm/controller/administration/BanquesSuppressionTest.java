package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureBanques;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.BANQUES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Suppression — FEATURE-admin-banques-organismes.md, règle 9. FK de la CI posées dans la transaction du test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BanquesSuppressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBanques banques;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = banques.jeu();
        fixture.poserClesEtrangeres();
    }

    private String refus(String famille, UUID id) throws Exception {
        String corps = mockMvc.perform(supprimer(famille, id, connecte(j.admin())))
                .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        fixture.oublierEcritureRefusee();
        return corps;
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r9_referenceNoAction_409_messageMetier_rienModifie(TypeTiers type) throws Exception {
        UUID id = j.tiers(type, "T1").inserer();
        j.referencer(id);
        Map<String, Object> avant = fixture.ligne(id);

        String corps = refus(type.famille(), id);

        String detail = JsonPath.read(corps, "$.detail");
        assertAll(
                () -> assertThat(detail).isNotBlank().doesNotContainIgnoringCase("constraint").doesNotContainIgnoringCase("sql"),
                () -> assertThat(fixture.ligne(id)).isEqualTo(avant));
    }

    @Test
    void r9_banqueDUnCompteBancaire_409_ligneGardee() throws Exception {
        banques.poserCle("StlAccount", "CfgBankId");
        UUID banque = j.tiers(BANQUES, "B1").inserer();
        banques.compteBancaire(banque);

        refus(BANQUES.famille(), banque);

        assertThat(fixture.existe(banque)).isTrue();
    }

    @Test
    void r9_banqueDUnCreditDeDossier_409_ligneGardee() throws Exception {
        banques.poserCle("ComFolder", "CreditBankId");
        UUID banque = j.tiers(BANQUES, "B2").inserer();
        banques.dossierACredit(j, banque);

        refus(BANQUES.famille(), banque);

        assertThat(fixture.existe(banque)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r9_tiersLibre_204_ligneSupprimee(TypeTiers type) throws Exception {
        UUID id = j.tiers(type, "T2").inserer();

        mockMvc.perform(supprimer(type.famille(), id, connecte(j.admin()))).andExpect(status().isNoContent());

        assertThat(fixture.existe(id)).isFalse();
    }
}

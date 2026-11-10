package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 400 et 401 des routes du lot — FEATURE-achat-impayes.md §4, §8 test 7.
 * {@code /documents/totaux} répond déjà 400 sur un paramètre faux tant qu'elle tombe sur {@code /documents/{id}} :
 * ses cas 400 ne deviennent probants qu'avec les tests 200 d'{@code ImpayesAchatTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpayesAchatSecuriteTest {

    private static final String ROLE = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private AdmUser compte;

    @BeforeEach
    void setUp() {
        String jeton = FixtureAchat.jeton("ASE");
        UUID projet = fixture.projet("P-" + jeton, fixture.societe("SOC-" + jeton));
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliter(compte, projet);
    }

    @ParameterizedTest(name = "{0}?{1}={2} → 400")
    @CsvSource({
            "/api/achat/documents, okPourPaiement, peut-être",
            "/api/achat/documents/export, okPourPaiement, peut-être",
            "/api/achat/documents/totaux, okPourPaiement, peut-être",
            "/api/achat/documents/totaux, projetIds, pas-un-uuid",
            "/api/achat/fournisseurs/soldes, projetIds, pas-un-uuid",
            "/api/achat/fournisseurs/soldes/export, projetIds, pas-un-uuid"})
    void s87_parametreMalForme_400(String chemin, String parametre, String valeur) throws Exception {
        mockMvc.perform(get(chemin).param(parametre, valeur).with(connecte(compte, ROLE)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0} sans jeton → 401")
    @ValueSource(strings = {
            AppelsImpayes.TOTAUX,
            AppelsImpayes.SOLDES,
            AppelsImpayes.SOLDES_EXPORT})
    void s87_sansJeton_401(String chemin) throws Exception {
        mockMvc.perform(get(chemin)).andExpect(status().isUnauthorized());
    }
}

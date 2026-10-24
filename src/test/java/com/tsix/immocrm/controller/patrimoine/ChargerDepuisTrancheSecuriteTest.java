package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureChargementTranche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.ChargerDepuisTrancheTest.CHARGER;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Refus de « Charger à partir d'une autre tranche » — FEATURE-patrimoine-reliquat-v2.md §A.4, §A.6 règle 7 : 404 source ou
 * cible inconnue ou d'une entité inactive (200 une fois réactivée), 400 source = cible, 403 Commercial, 401 anonyme ; aucune
 * écriture sur la cible quand un contrôle échoue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChargerDepuisTrancheSecuriteTest {

    private static final String ADRESSE = "Address";
    private static final String ADRESSE_SOURCE = "1 rue Source";
    private static final String ADRESSE_CIBLE = "Adresse de la cible";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureEntites entites;
    @Autowired private FixtureChargementTranche chargement;

    private AdmUser admin;
    private CfgCompany entiteSource;
    private CfgCompany entiteCible;
    private CfgTranche source;
    private CfgTranche cible;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        String jeton = "P5BS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entiteSource = patrimoine.entite("ENT-S-" + jeton, true);
        entiteCible = patrimoine.entite("ENT-C-" + jeton, true);
        source = patrimoine.tranche("TR-S-" + jeton, patrimoine.projet("PRJ-S-" + jeton, entiteSource));
        cible = patrimoine.tranche("TR-C-" + jeton, patrimoine.projet("PRJ-C-" + jeton, entiteCible));
        chargement.definir(source.getId(), Map.of(ADRESSE, ADRESSE_SOURCE));
        chargement.definir(cible.getId(), Map.of(ADRESSE, ADRESSE_CIBLE));
    }

    private ResultActions charger(UUID cibleId, UUID sourceId, RequestPostProcessor appelant) throws Exception {
        return mockMvc.perform(post(CHARGER, cibleId, sourceId).with(appelant));
    }

    private String adresseDeLaCible() {
        return chargement.valeurs(cible.getId(), List.of(ADRESSE)).get(ADRESSE);
    }

    private static RequestPostProcessor commercial(AdmUser compte) {
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), compte.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @Test
    void r7_sourceInconnue_404() throws Exception {
        charger(cible.getId(), UUID.randomUUID(), connecte(admin)).andExpect(status().isNotFound());

        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_CIBLE);
    }

    @Test
    void r7_cibleInconnue_404() throws Exception {
        charger(UUID.randomUUID(), source.getId(), connecte(admin)).andExpect(status().isNotFound());
    }

    @Test
    void r7_sourceEgaleCible_400_rienEcrit() throws Exception {
        charger(cible.getId(), cible.getId(), connecte(admin)).andExpect(status().isBadRequest());

        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_CIBLE);
    }

    @Test
    void r7_commercial_403_rienEcrit() throws Exception {
        charger(cible.getId(), source.getId(), commercial(patrimoine.administrateur())).andExpect(status().isForbidden());

        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_CIBLE);
    }

    @Test
    void r7_anonyme_401() throws Exception {
        mockMvc.perform(post(CHARGER, cible.getId(), source.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void r7_sourceDUneEntiteInactive_404_rienEcrit_puis200UneFoisReactivee() throws Exception {
        entites.actif(entiteSource.getId(), false);

        charger(cible.getId(), source.getId(), connecte(admin)).andExpect(status().isNotFound());
        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_CIBLE);

        entites.actif(entiteSource.getId(), true);
        charger(cible.getId(), source.getId(), connecte(admin)).andExpect(status().isOk());
        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_SOURCE);
    }

    @Test
    void r7_cibleDUneEntiteInactive_404_rienEcrit_puis200UneFoisReactivee() throws Exception {
        entites.actif(entiteCible.getId(), false);

        charger(cible.getId(), source.getId(), connecte(admin)).andExpect(status().isNotFound());
        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_CIBLE);

        entites.actif(entiteCible.getId(), true);
        charger(cible.getId(), source.getId(), connecte(admin)).andExpect(status().isOk());
        assertThat(adresseDeLaCible()).isEqualToIgnoringCase(ADRESSE_SOURCE);
    }
}

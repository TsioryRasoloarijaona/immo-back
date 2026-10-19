package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lectures de tranche exclues de REFERENTIELS_LIRE (clients, commissions, stock valorisé), compte Administrateur. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheReglesCleApiExclusionsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    @ParameterizedTest
    @ValueSource(strings = {"mettre-a-jour-schemas/apercu", "commissions", "previsions-base", "previsions-base/stock"})
    void cleApiReferentiels_compteAdministrateur_lectureExclue_horsPortee_403(String sousRoute) throws Exception {
        String jeton = "P1CX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgTranche tranche = patrimoine.tranche("TR-" + jeton,
                patrimoine.projet("PRJ-" + jeton, patrimoine.entite("ENT-" + jeton, true)));

        AdmUser compte = dossiers.compte("N8N-" + jeton, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-exclusions-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(get("/api/tranches/" + tranche.getId() + "/" + sousRoute)
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

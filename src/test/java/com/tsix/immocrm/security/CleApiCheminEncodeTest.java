package com.tsix.immocrm.security;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Portée d'une clé d'API comparée au chemin décodé : un encodage ne contourne aucune exclusion. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleApiCheminEncodeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private CfgTranche tranche;
    private String cle;

    @BeforeEach
    void setUp() {
        String jeton = "CAPE" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = patrimoine.tranche("TR-" + jeton,
                patrimoine.projet("PRJ-" + jeton, patrimoine.entite("ENT-" + jeton, true)));
        AdmUser compte = dossiers.compte("N8N-" + jeton, "Administrateur");
        cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-encode-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/%63ommissions", "/commission%73", "/previsions%2Dbase", "/pieces%2djointes/x"})
    void cleApiReferentiels_exclusionEncodee_horsPortee_403(String suffixe) throws Exception {
        mockMvc.perform(get(URI.create("/api/tranches/" + tranche.getId() + suffixe))
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // Le pare-feu Spring Security (StrictHttpFirewall) les rejette en 400 avant le filtre de clé, qui les refuserait aussi.
    @ParameterizedTest
    @ValueSource(strings = {"/commissions;jsessionid=abc", "%2Fcommissions", "/x/..%2Fcommissions", "/%2E%2E/x",
            "//commissions", "/commissions%253B"})
    void cleApiReferentiels_cheminAmbigu_refuse(String suffixe) throws Exception {
        mockMvc.perform(get(URI.create("/api/tranches/" + tranche.getId() + suffixe))
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(400, 403));
    }

    @Test
    void cleApiReferentiels_cheminLegitime_200() throws Exception {
        mockMvc.perform(get("/api/tranches/" + tranche.getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isOk());
    }
}

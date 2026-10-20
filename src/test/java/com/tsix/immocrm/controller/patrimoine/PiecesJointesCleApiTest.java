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

/**
 * FEATURE-patrimoine-article-onglets.md §8.2, §11 : les lectures de pièces jointes de tranche et de programme sont dans
 * les {@code exclues} de {@code REFERENTIELS_LIRE} (accès aux fichiers refusé à un compte de service, même Administrateur).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PiecesJointesCleApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/api/tranches/{T}/pieces-jointes",
            "/api/tranches/{T}/pieces-jointes/{P}/fichier",
            "/api/projets/{R}/pieces-jointes",
            "/api/projets/{R}/pieces-jointes/{P}/fichier"})
    void r8_2_cleApiReferentiels_lecturePiecesJointes_horsPortee403(String route) throws Exception {
        String jeton = "P2BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgTranche tranche = patrimoine.tranche("TR-" + jeton,
                patrimoine.projet("PRJ-" + jeton, patrimoine.entite("ENT-" + jeton, true)));
        AdmUser compte = dossiers.compte("N8N-" + jeton, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-pj-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        String url = route.replace("{T}", tranche.getId().toString())
                .replace("{R}", tranche.getCfgProject().getId().toString())
                .replace("{P}", UUID.randomUUID().toString());

        mockMvc.perform(get(url).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

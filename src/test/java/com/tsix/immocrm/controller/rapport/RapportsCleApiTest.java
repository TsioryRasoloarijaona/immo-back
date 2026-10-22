package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RD5 (FEATURE-tdb-vente-rapports-socle.md §2) : aucune portée de clé d'API ne couvre {@code /api/rapports/**}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsCleApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureRapports rapports;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    @ParameterizedTest
    @ValueSource(strings = {"/api/rapports/vente", "/api/rapports/vente/" + FixtureRapports.SUIVI_GLOBAL,
            "/api/rapports/vente/" + FixtureRapports.SUIVI_GLOBAL + "/resultat",
            "/api/rapports/vente/" + FixtureRapports.SUIVI_GLOBAL + "/export?format=xlsx"})
    void rd5_cleAvecToutesLesPortees_compteAdministrateur_403HorsPortee(String route) throws Exception {
        String jeton = FixtureEtatVentes.prefixe("RCA");
        rapports.tables();
        rapports.rapportVente(FixtureRapports.SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        AdmUser compte = dossiers.compte("N8N-" + jeton, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("rapports-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(get(route).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

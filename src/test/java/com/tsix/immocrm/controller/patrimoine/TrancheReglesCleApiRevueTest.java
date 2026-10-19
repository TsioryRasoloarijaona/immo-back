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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue P1c (FEATURE-patrimoine-tranche-regles.md §4, §11.5) : l'aperçu de la mise à jour des schémas expose
 * clients et restes dus ; comme le réajustement des superficies, une clé REFERENTIELS_LIRE ne doit pas le lire, même
 * sur un compte de service Administrateur ({@code GET /api/tranches/**} la couvre sinon).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheReglesCleApiRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    @Test
    void cleApiReferentiels_compteAdministrateur_apercuSchemas_horsPortee_403() throws Exception {
        String jeton = "P1CK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgTranche tranche = patrimoine.tranche("TR-" + jeton,
                patrimoine.projet("PRJ-" + jeton, patrimoine.entite("ENT-" + jeton, true)));
        tranche.setStartDateFinalisation(LocalDate.of(2026, 6, 1).atStartOfDay());

        AdmUser compte = dossiers.compte("N8N-" + jeton, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-schemas-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(get("/api/tranches/" + tranche.getId() + "/mettre-a-jour-schemas/apercu")
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

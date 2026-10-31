package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R1 : aucune portée de clé d'API ne couvre ces routes, même une clé qui les a toutes. Vert avant le code, verrou de non-régression. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UtilisateursCleApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CleApiService cleApiService;
    @Autowired private AppApiClientRepository clientRepository;

    private String cleToutesPortees() {
        AdmUser admin = fixture.administrateur();
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-utilisateurs-" + UUID.randomUUID());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(admin);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"GET,/api/utilisateurs", "GET,/api/utilisateurs/export", "GET,/api/utilisateurs/references",
            "GET,/api/utilisateurs/5B6C7D8E-9FA0-4B1C-82D3-E4F506172839",
            "GET,/api/utilisateurs/5B6C7D8E-9FA0-4B1C-82D3-E4F506172839/habilitations",
            "POST,/api/utilisateurs", "PATCH,/api/utilisateurs/5B6C7D8E-9FA0-4B1C-82D3-E4F506172839",
            "PUT,/api/utilisateurs/5B6C7D8E-9FA0-4B1C-82D3-E4F506172839/roles",
            "PUT,/api/utilisateurs/5B6C7D8E-9FA0-4B1C-82D3-E4F506172839/comptes-depot",
            "PUT,/api/utilisateurs/5B6C7D8E-9FA0-4B1C-82D3-E4F506172839/habilitations"})
    void r1_cleApi_toutesPortees_403(String methode, String url) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(methode), url).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

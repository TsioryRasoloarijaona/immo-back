package com.tsix.immocrm.controller.auth;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /api/cles-api} — {@code instructions/FEATURE-cle-api-n8n.md} §5. Réservé au rôle
 * Administrateur ; le secret n'est renvoyé qu'à la création et à la régénération.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class CleApiAdminTest {

    private static final String URL = "/api/cles-api";

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AppApiClientRepository clientRepository;

    private static String nomUnique() {
        return "cle-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String creer(String nom) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"%s","portees":["PRE_PROSPECTIONS_ECRIRE","REFERENTIELS_LIRE"]}""".formatted(nom)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void creer_renvoieLeSecretUneFois_etCreeLeCompteDeService() throws Exception {
        String nom = nomUnique();
        String json = creer(nom);
        String cle = JsonPath.read(json, "$.cle");
        String id = JsonPath.read(json, "$.cleApi.id");

        assertThat(cle).startsWith("ak_").hasSize(46);
        assertThat(JsonPath.<String>read(json, "$.cleApi.clePrefixe")).isEqualTo(cle.substring(0, 11));
        assertThat(JsonPath.<String>read(json, "$.cleApi.compteCode")).isEqualTo(nom);
        assertThat(JsonPath.<Boolean>read(json, "$.cleApi.actif")).isTrue();

        AdmUser compte = userRepository.findByCode(nom).orElseThrow();
        assertThat(compte.getDescription()).isEqualTo(nom);
        assertThat(compte.getDisabled()).isFalse();
        assertThat(compte.getCfgEmployee()).isNull();
        assertThat(clientRepository.findById(UUID.fromString(id)).orElseThrow().getCleHash())
                .as("seul le hash est stocké").doesNotContain(cle);

        // La liste ne montre jamais le secret.
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].clePrefixe").value(cle.substring(0, 11)))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].cle").doesNotExist())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].portees[*]", hasSize(2)));

        // Et la clé fonctionne de bout en bout.
        mockMvc.perform(post("/api/pre-prospections").header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Lead " + nom + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preProspection.code").value(matchesPattern("\\d{2}/\\d{5}")));
    }

    @Test
    void creer_avecCompteExistant_neCreePasDeCompte() throws Exception {
        AdmUser existant = new AdmUser();
        existant.setId(UUID.randomUUID());
        existant.setCode("svc-" + UUID.randomUUID().toString().substring(0, 8));
        existant.setDescription("Compte existant");
        existant.setDisabled(false);
        userRepository.save(existant);
        long comptesAvant = userRepository.count();

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"%s","portees":["PRE_PROSPECTIONS_LIRE"],"compteCode":"%s"}"""
                        .formatted(nomUnique(), existant.getCode())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cleApi.compteCode").value(existant.getCode()))
                .andExpect(jsonPath("$.cleApi.compteNom").value("Compte existant"));
        assertThat(userRepository.count()).isEqualTo(comptesAvant);
    }

    @Test
    void creer_400_surNomVide_porteesVides_porteeInconnue_compteInconnu() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"  \",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nomUnique() + "\",\"portees\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nomUnique() + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nomUnique() + "\",\"portees\":[\"TOUT\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nomUnique() + "\",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\"],\"compteCode\":\"inconnu-"
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creer_409_surNomDejaPris() throws Exception {
        String nom = nomUnique();
        creer(nom);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"%s","portees":["PRE_PROSPECTIONS_ECRIRE"]}""".formatted(nom.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void regenerer_remplaceLeSecret_lAncienCesseImmediatement() throws Exception {
        String json = creer(nomUnique());
        String ancienne = JsonPath.read(json, "$.cle");
        String id = JsonPath.read(json, "$.cleApi.id");

        String nouvelle = JsonPath.read(mockMvc.perform(post(URL + "/{id}/regenerer", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.cle");
        assertThat(nouvelle).startsWith("ak_").isNotEqualTo(ancienne);

        mockMvc.perform(get("/api/commerciaux").header(ApiKeyAuthenticationFilter.EN_TETE, ancienne))
                .andExpect(status().isUnauthorized());
        // En dernier : sous MockMvc le contexte de sécurité est partagé entre les requêtes d'un
        // même test, et cet appel accepté remplace l'administrateur simulé par le compte de service.
        mockMvc.perform(get("/api/commerciaux").header(ApiKeyAuthenticationFilter.EN_TETE, nouvelle))
                .andExpect(status().isOk());
    }

    @Test
    void regenerer_404_surCleInconnue() throws Exception {
        mockMvc.perform(post(URL + "/{id}/regenerer", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void revoquer_204_idempotent_puisLaCleEstRefusee_etNeSeRegenerePlus() throws Exception {
        String json = creer(nomUnique());
        String cle = JsonPath.read(json, "$.cle");
        String id = JsonPath.read(json, "$.cleApi.id");

        mockMvc.perform(delete(URL + "/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete(URL + "/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete(URL + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/commerciaux").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].actif").value(false))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].revoqueLe").exists());
        mockMvc.perform(post(URL + "/{id}/regenerer", id))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "Commercial")
    void sansLeRoleAdministrateur_403() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nomUnique() + "\",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\"]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(URL + "/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
    }
}

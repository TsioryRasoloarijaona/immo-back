package com.tsix.immocrm.security;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code ApiKeyAuthenticationFilter} — {@code instructions/FEATURE-cle-api-n8n.md} §5. Pas de
 * {@code @WithMockUser} sur la classe : c'est l'authentification elle-même qu'on teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleApiAuthentificationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CleApiService cleApiService;
    @Autowired private EntityManager entityManager;

    private String nomClient;
    private AdmUser compte;
    private AppApiClient client;
    private String cle;

    @BeforeEach
    void setUp() {
        nomClient = "n8n-" + UUID.randomUUID().toString().substring(0, 8);
        compte = compteDeService(nomClient);
        cle = cleApiService.genererCle();
        client = cle(nomClient, compte, cle, EnumSet.of(PorteeCleApi.PRE_PROSPECTIONS_ECRIRE, PorteeCleApi.REFERENTIELS_LIRE));
    }

    private AdmUser compteDeService(String code) {
        AdmUser u = new AdmUser();
        u.setId(UUID.randomUUID());
        u.setCode(code);
        u.setDescription(code);
        u.setDisabled(false);
        return userRepository.save(u);
    }

    private AppApiClient cle(String nom, AdmUser compte, String cle, Set<PorteeCleApi> portees) {
        AppApiClient c = new AppApiClient();
        c.setId(UUID.randomUUID());
        c.setNom(nom);
        c.setCleHash(CleApiService.hacher(cle));
        c.setClePrefixe(CleApiService.prefixeAffiche(cle));
        c.setAdmUser(compte);
        c.setPortees(PorteeCleApi.versColonne(portees));
        c.setActif(true);
        c.setUserCreateDateTime(LocalDateTime.now());
        return clientRepository.save(c);
    }

    private String corpsLead() {
        return """
                {"nom":"Lead n8n %s","telephone":"0612345678","notes":"Formulaire Facebook"}"""
                .formatted(UUID.randomUUID());
    }

    // ---------------------------------------------------------------- clé valide

    @Test
    void cleValide_creeUnePreProspection_auNomDuCompteDeService() throws Exception {
        String json = mockMvc.perform(post("/api/pre-prospections")
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content(corpsLead()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.preProspection.id"));

        ComProspection ligne = prospectionRepository.findById(id).orElseThrow();
        assertThat(ligne.getUserCreate()).as("l'auteur est le compte de service, plus jamais NULL")
                .isEqualTo(nomClient);
    }

    @Test
    void cleValide_litLesReferentiels_dansSaPortee() throws Exception {
        mockMvc.perform(get("/api/referentiels/origines-prospection").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/commerciaux").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isOk());
    }

    @Test
    void cleValide_traceLeDernierUsage() throws Exception {
        assertThat(client.getDernierUsage()).isNull();
        mockMvc.perform(get("/api/referentiels/origines-prospection").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isOk());

        entityManager.refresh(client);
        assertThat(client.getDernierUsage()).isNotNull();
    }

    // ---------------------------------------------------------------- hors portée : 403

    @Test
    void routeHorsPortee_403_horsPortee_memeAvecUneCleValide() throws Exception {
        mockMvc.perform(get("/api/dossiers").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(nomClient)));
        // Une clé ne crée jamais de clé : /api/cles-api n'est dans aucune portée.
        mockMvc.perform(post("/api/cles-api").header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"x\",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        // La bonne route avec la mauvaise méthode ne passe pas non plus.
        mockMvc.perform(get("/api/pre-prospections").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden());
    }

    @Test
    void porteeLecture_ouvreLaListeEtLaFiche_maisPasLaCreation() throws Exception {
        String cleLecture = cleApiService.genererCle();
        cle("lecture-" + nomClient, compte, cleLecture, EnumSet.of(PorteeCleApi.PRE_PROSPECTIONS_LIRE));

        mockMvc.perform(get("/api/pre-prospections").header(ApiKeyAuthenticationFilter.EN_TETE, cleLecture))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/prospections/{id}", UUID.randomUUID()).header(ApiKeyAuthenticationFilter.EN_TETE, cleLecture))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/pre-prospections").header(ApiKeyAuthenticationFilter.EN_TETE, cleLecture)
                        .contentType(MediaType.APPLICATION_JSON).content(corpsLead()))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- refus : 401

    @Test
    void cleInconnue_401() throws Exception {
        mockMvc.perform(post("/api/pre-prospections")
                        .header(ApiKeyAuthenticationFilter.EN_TETE, "ak_" + "x".repeat(43))
                        .contentType(MediaType.APPLICATION_JSON).content(corpsLead()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLE_INVALIDE"));
        mockMvc.perform(get("/api/referentiels/origines-prospection").header(ApiKeyAuthenticationFilter.EN_TETE, "pas-une-cle"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cleRevoquee_401() throws Exception {
        client.setActif(false);
        clientRepository.save(client);

        mockMvc.perform(get("/api/referentiels/origines-prospection").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void compteDeServiceDesactive_401() throws Exception {
        compte.setDisabled(true);
        userRepository.save(compte);

        mockMvc.perform(get("/api/referentiels/origines-prospection").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isUnauthorized());
    }

    /** La clé présente fait foi : une session valide ne rattrape pas une clé refusée. */
    @Test
    @WithMockUser(roles = "Administrateur")
    void cleInvalide_prime_surUneSessionValide() throws Exception {
        mockMvc.perform(get("/api/cles-api"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cles-api").header(ApiKeyAuthenticationFilter.EN_TETE, "ak_" + "x".repeat(43)))
                .andExpect(status().isUnauthorized());
    }

    /** Non-régression : sans en-tête ni cookie, rien ne change. */
    @Test
    void sansEnTete_401_commeAvant() throws Exception {
        mockMvc.perform(post("/api/pre-prospections")
                        .contentType(MediaType.APPLICATION_JSON).content(corpsLead()))
                .andExpect(status().isUnauthorized());
    }
}

package com.tsix.immocrm.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verrouille la seule exception non applicative à {@code anyRequest().authenticated()}.
 *
 * <p>Pas de {@code @WithMockUser} ici, contrairement au reste de la suite : c'est précisément
 * l'accès <b>anonyme</b> qui est sous test. Une sonde de déploiement — HEALTHCHECK Docker,
 * vérification post-déploiement du pipeline — n'a pas de cookie JWT à présenter.
 *
 * <p>Les deux assertions vont par paire et doivent le rester : ouvrir {@code /actuator/health} ne
 * doit jamais ouvrir {@code /actuator/metrics}, qui exposerait l'inventaire des URI et les
 * latences par endpoint.
 *
 * <p><b>Ce que ce test ne couvre pas.</b> En production, {@code management.server.port=9091}
 * sert l'actuator sur un port dédié. Or l'environnement web {@code MOCK} de
 * {@code @SpringBootTest} n'ouvre aucun port réel : {@code management.server.port} y est
 * <b>ignoré</b>, et MockMvc voit donc l'actuator dans le contexte principal. Ce test valide
 * l'état de repli — celui qui s'appliquerait si le port de management disparaissait — et non
 * la configuration réellement servie en production. Vérifier celle-ci demande un appel réseau
 * contre l'instance déployée, pas un test MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActuatorSecuriteTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_estPublic_etNeDivulgueQueLeStatut() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // show-details=never : aucune sous-clé ne doit apparaître, sinon l'état du
                // DataSource et du stockage objet fuiterait à un appelant anonyme.
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("dataSource"))));
    }

    @Test
    void metrics_resteAuthentifie() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }
}

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
 * <p>Les trois assertions forment un tout et doivent le rester : ouvrir {@code health} et
 * {@code prometheus} ne doit jamais ouvrir {@code metrics}, qui exposerait l'inventaire des URI
 * et les latences par endpoint.
 *
 * <p><b>Portée réelle.</b> En production, {@code management.server.port=9091} sert l'actuator
 * sur un port dédié, alors que l'environnement web {@code MOCK} n'ouvre aucun port et ignore
 * cette propriété. La différence est sans effet <b>ici</b> : la chaîne de filtres de
 * {@code SecurityConfig} s'applique aux deux ports, et c'est elle qui est sous test. Vérifié en
 * production sur 9091 — {@code health} en {@code 200} anonyme, {@code metrics} en {@code 401}.
 *
 * <p>L'exposition des trois endpoints est forcée dans
 * {@code src/test/resources/application.properties} : ce fichier remplace celui de {@code main},
 * et sans cette ligne seul {@code health} serait monté — le test passerait au vert en
 * n'interrogeant rien.
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

    /**
     * Prometheus scrape sans identifiant : derrière l'authentification, la cible ressort
     * {@code down} avec un {@code 401} — c'est exactement le défaut qui a motivé ce test.
     */
    @Test
    void prometheus_estPublic_etRendLeFormatTexte() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                // Le format d'exposition Prometheus, pas du JSON : la présence d'une ligne
                // HELP/TYPE prouve que le registre est bien branché et pas seulement l'endpoint.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# TYPE")));
    }

    @Test
    void metrics_resteAuthentifie() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }
}

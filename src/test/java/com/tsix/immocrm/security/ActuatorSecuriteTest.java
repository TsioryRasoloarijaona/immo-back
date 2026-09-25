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

    /**
     * Ce test est aussi le garde-fou de {@code management.health.mail.enabled=false}.
     * {@code spring-boot-starter-mail} enregistre un {@code MailHealthIndicator} qui ouvre une
     * connexion SMTP à chaque appel : retirer cette propriété fait répondre {@code 503} ici au
     * bout de sept secondes, et en production ferait échouer la sonde de déploiement dès que la
     * boîte Hostinger est injoignable. Voir {@code FEATURE-service-envoi-email.md}.
     */
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

    /** La version qui tourne, lisible par Grafana : labels tag/commit, valeur 1 (InfosBuild). */
    @Test
    void prometheus_porteLaVersionEmbarquee() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("immo_back_build_info{")))
                // Hors image (tests), les deux valeurs valent "inconnu" — la jauge existe quand même.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tag=\"inconnu\"")));
    }

    @Test
    void info_estPublic_etNeRendQueLaVersion() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.tag").value("inconnu"))
                .andExpect(jsonPath("$.build.commit").value("inconnu"))
                .andExpect(jsonPath("$.java").doesNotExist())
                .andExpect(jsonPath("$.os").doesNotExist());
    }

    @Test
    void metrics_resteAuthentifie() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }
}

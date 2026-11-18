package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsReunions.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_PLAGES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_TYPES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.PLAGES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.STATUTS;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.TYPES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revue adverse de la configuration des réunions : droits D7 sur les deux routes, R2, valeurs de prod, unicité. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierConfigRevueTest {

    private static final String MOTEUR = "/api/referentiels-admin/";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRV-" + jeton);
        admin = fixture.admin(jeton, projet);
    }

    private static List<String> routes(String slug) {
        return List.of("/api/projet/config/" + slug, MOTEUR + slug);
    }

    @Test
    void d7_sansLeDroitDeLEcran_lectureEtEcriture403_surLesDeuxRoutes() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "rev.a." + jeton, DROIT_LIRE);
        UUID type = fixture.type("RV-" + jeton);
        UUID statut = fixture.statut("SV-" + jeton, false);
        UUID plage = fixture.plage("PV-" + jeton);
        String corps = "{\"description\":\"x\"}";

        for (String base : List.of("types-reunion", "statuts-reserves", "plages-couleurs")) {
            UUID id = switch (base) {
                case "types-reunion" -> type;
                case "statuts-reserves" -> statut;
                default -> plage;
            };
            for (String route : routes(base)) {
                assertThat(appels.appeler(get(route), lecteur).statut()).as("GET " + route).isEqualTo(403);
                assertThat(appels.appeler(get(route + "/" + id), lecteur).statut()).as("GET id " + route).isIn(403, 404, 405);
                assertThat(appels.envoyer(post(route), corps, lecteur).statut()).as("POST " + route).isEqualTo(403);
                assertThat(appels.envoyer(put(route + "/" + id), corps, lecteur).statut()).as("PUT " + route).isIn(403, 405);
                assertThat(appels.envoyer(patch(route + "/" + id), corps, lecteur).statut()).as("PATCH " + route)
                        .isIn(403, 405);
                assertThat(appels.appeler(delete(route + "/" + id), lecteur).statut()).as("DELETE " + route).isEqualTo(403);
            }
        }
    }

    @Test
    void d7_droitDUnEcran_neDonnePasAccesAuxAutres_viaLeMoteur() throws Exception {
        RequestPostProcessor typesSeul = fixture.compte(projet, "rev.b." + jeton, DROIT_TYPES);

        assertThat(appels.appeler(get(MOTEUR + "types-reunion"), typesSeul).statut()).isEqualTo(200);
        assertThat(appels.appeler(get(MOTEUR + "statuts-reserves"), typesSeul).statut()).isEqualTo(403);
        assertThat(appels.appeler(get(MOTEUR + "plages-couleurs"), typesSeul).statut()).isEqualTo(403);
        assertThat(appels.envoyer(post(MOTEUR + "plages-couleurs"), "{}", typesSeul).statut()).isEqualTo(403);
    }

    @Test
    void d7_avecLeDroitDeLEcran_ecritureViaLeMoteurAccepteeSansRoleAdministrateur() throws Exception {
        RequestPostProcessor plagesSeul = fixture.compte(projet, "rev.c." + jeton, DROIT_PLAGES);
        String corps = "{\"sorting\":1,\"couleur\":\"RED\",\"description\":\"M-" + jeton + "\"}";

        Reponse viaAlias = appels.envoyer(post("/api/projet/config/plages-couleurs"), corps, plagesSeul);
        Reponse viaMoteur = appels.envoyer(post(MOTEUR + "plages-couleurs"), corps, plagesSeul);

        assertThat(viaAlias.statut()).as(viaAlias.corps()).isEqualTo(201);
        assertThat(viaMoteur.statut()).as(viaMoteur.corps()).isEqualTo(201);
    }

    @Test
    void plages_valeursDeProd_fractionsConserveesEtHorsBornes400() throws Exception {
        String ok = "{\"sorting\":1,\"couleur\":\"GREEN\",\"description\":\"H-" + jeton + "\",\"de\":0.6,\"a\":1.0}";
        Reponse cree = appels.envoyer(post(PLAGES), ok, admin);

        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);
        assertThat(cree.corps()).contains("\"couleur\":\"GREEN\"").contains("\"de\":0.6");
        String pourcent = "{\"sorting\":1,\"couleur\":\"GREEN\",\"description\":\"I-" + jeton + "\",\"de\":60,\"a\":100}";
        assertThat(appels.envoyer(post(PLAGES), pourcent, admin).statut()).isEqualTo(400);
    }

    @Test
    void plages_deuxPlagesDeMemeDescription_pasDeContrainteDUnicite() throws Exception {
        String corps = "{\"sorting\":1,\"couleur\":\"RED\",\"description\":\"D-" + jeton + "\"}";

        assertThat(appels.envoyer(post(PLAGES), corps, admin).statut()).isEqualTo(201);
        assertThat(appels.envoyer(post(PLAGES), corps, admin).statut()).isEqualTo(201);
    }

    @Test
    void types_codeDeLongueur26_400_et_doublonDeCode409() throws Exception {
        String trop = "{\"sorting\":1,\"code\":\"" + "C".repeat(26) + "\",\"description\":\"d\"}";
        assertThat(appels.envoyer(post(TYPES), trop, admin).statut()).isEqualTo(400);

        String code = "T" + jeton;
        String corps = "{\"sorting\":1,\"code\":\"" + code + "\",\"description\":\"d\"}";
        assertThat(appels.envoyer(post(TYPES), corps, admin).statut()).isEqualTo(201);
        assertThat(appels.envoyer(post(TYPES), corps.replace(code, code.toLowerCase()), admin).statut()).isEqualTo(409);
    }

    @Test
    void statuts_creationSansEstFerme_puisModificationPartielle() throws Exception {
        String corps = "{\"sorting\":1,\"code\":\"S" + jeton + "\",\"description\":\"d\",\"couleur\":\"RED\"}";
        Reponse cree = appels.envoyer(post(STATUTS), corps, admin);
        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);

        Reponse modifie = appels.envoyer(put(STATUTS + "/" + cree.id()), "{\"estFerme\":true}", admin);

        assertThat(modifie.statut()).as(modifie.corps()).isEqualTo(200);
        assertThat(modifie.corps()).contains("\"estFerme\":true").contains("\"code\":\"S" + jeton + "\"");
    }

    @Test
    void routesDuProjet_neSontPasMasqueesParLAlias() throws Exception {
        assertThat(appels.appeler(get("/api/projet/config/types-lot/defaut"), admin).statut()).isNotEqualTo(404);
        assertThat(appels.appeler(get("/api/projet/config/referentiels"), admin).statut()).isEqualTo(200);
    }
}

package com.tsix.immocrm.controller.chantier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureChantier.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité du module Chantier (FEATURE-projet-chantier-lecture.md §2) : exempté → tout ; habilité → ses projets,
 * 404 ailleurs ; sans habilitation → listes vides et 404 partout (pas « aucune restriction » comme au commercial).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreChantierTest {

    private static final String PROJETS = "/api/chantier/projets";
    private static final String DECOMPTES = "/api/chantier/decomptes";
    private static final String IDS_PROJETS = "$[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureChantier fixture;

    private FixtureChantier.Chaine chaine;

    @BeforeEach
    void setUp() {
        chaine = fixture.creer();
    }

    private String lire(MockHttpServletRequestBuilder requete, AdmUser user) throws Exception {
        return mockMvc.perform(requete.with(connecte(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private int statut(String uri, UUID id, AdmUser user) throws Exception {
        return mockMvc.perform(get(uri, id).with(connecte(user))).andReturn().getResponse().getStatus();
    }

    /** Toutes les routes par id d'une ressource du projet B. */
    private List<Integer> statutsSurLeProjetB(AdmUser user) throws Exception {
        return List.of(
                statut("/api/chantier/projets/{id}", chaine.projetB().getId(), user),
                statut("/api/chantier/projets/{id}/lots", chaine.projetB().getId(), user),
                statut("/api/chantier/lots/{id}", chaine.lotB().getId(), user),
                statut("/api/chantier/lots/{id}/bordereau", chaine.lotB().getId(), user),
                statut("/api/chantier/lots/{id}/decomptes", chaine.lotB().getId(), user),
                statut("/api/chantier/decomptes/{id}", chaine.decompteB().getId(), user));
    }

    // ------------------------------------------------------------------ exemptés

    @Test
    void administrateur_voitToutMemeHabiliteSurUnSeulProjet() throws Exception {
        AdmUser admin = fixture.utilisateur("chantier.admin");
        fixture.habiliter(admin, chaine.projetA());
        fixture.donnerRole(admin, "Administrateur");

        List<String> projets = JsonPath.read(lire(get(PROJETS), admin), IDS_PROJETS);

        assertAll(
                () -> assertThat(projets).contains(chaine.projetA().getId().toString(),
                        chaine.projetB().getId().toString()),
                () -> assertThat(statutsSurLeProjetB(admin)).containsOnly(200));
    }

    @Test
    void directionGenerale_voitTout() throws Exception {
        AdmUser dg = fixture.utilisateur("chantier.dg");
        fixture.donnerRole(dg, "Direction Générale");

        assertThat(statutsSurLeProjetB(dg)).containsOnly(200);
    }

    // ------------------------------------------------------------------ habilité sur A

    @Test
    void habiliteSurA_voitA_404SurToutesLesRoutesDeB() throws Exception {
        AdmUser conducteur = fixture.utilisateur("chantier.conducteur");
        fixture.habiliter(conducteur, chaine.projetA());

        List<String> projets = JsonPath.read(lire(get(PROJETS), conducteur), IDS_PROJETS);

        assertAll(
                () -> assertThat(projets).containsExactly(chaine.projetA().getId().toString()),
                () -> assertThat(statutsSurLeProjetB(conducteur)).containsOnly(404),
                () -> assertThat(statut("/api/chantier/lots/{id}", chaine.lotBordereau().getId(), conducteur))
                        .isEqualTo(200),
                () -> assertThat(statut("/api/chantier/decomptes/{id}", chaine.decompte1().getId(), conducteur))
                        .isEqualTo(200));
    }

    @Test
    void habiliteSurA_listeGlobaleDesDecomptesSansB_memeFiltreeSurB() throws Exception {
        AdmUser conducteur = fixture.utilisateur("chantier.global");
        fixture.habiliter(conducteur, chaine.projetA());

        List<String> tous = JsonPath.read(lire(get(DECOMPTES), conducteur), "$.content[*].id");
        String filtreSurB = lire(get(DECOMPTES).param("projetId", chaine.projetB().getId().toString()), conducteur);

        assertAll(
                () -> assertThat(tous).contains(chaine.decompte1().getId().toString())
                        .doesNotContain(chaine.decompteB().getId().toString()),
                () -> assertThat((Integer) JsonPath.read(filtreSurB, "$.totalElements")).isZero());
    }

    @Test
    void habiliteSurA_categoriesDesReferentiels_limiteesAuPerimetre() throws Exception {
        AdmUser conducteur = fixture.utilisateur("chantier.ref");
        fixture.habiliter(conducteur, chaine.projetA());

        List<String> categories = JsonPath.read(lire(get("/api/chantier/referentiels"), conducteur),
                "$.categories[*].id");

        assertThat(categories).contains(chaine.categorie().getId().toString())
                .doesNotContain(chaine.categorieB().getId().toString());
    }

    // ------------------------------------------------------------------ sans habilitation

    @Test
    void sansHabilitation_listesVides_et404SurToutDetail() throws Exception {
        AdmUser sansDroit = fixture.utilisateur("chantier.sansdroit");

        String projets = lire(get(PROJETS), sansDroit);
        String decomptes = lire(get(DECOMPTES), sansDroit);
        String referentiels = lire(get("/api/chantier/referentiels"), sansDroit);

        assertAll(
                () -> assertThat((Integer) JsonPath.read(projets, "$.length()")).isZero(),
                () -> assertThat((Integer) JsonPath.read(decomptes, "$.totalElements")).isZero(),
                () -> assertThat((Integer) JsonPath.read(referentiels, "$.categories.length()")).isZero(),
                () -> assertThat(statutsSurLeProjetB(sansDroit)).containsOnly(404),
                () -> assertThat(statut("/api/chantier/projets/{id}", chaine.projetA().getId(), sansDroit))
                        .isEqualTo(404),
                () -> assertThat(statut("/api/chantier/decomptes/{id}", chaine.decompte1().getId(), sansDroit))
                        .isEqualTo(404));
    }

    @Test
    @WithMockUser
    void appelantNonIdentifiable_neVoitRien() throws Exception {
        mockMvc.perform(get(PROJETS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

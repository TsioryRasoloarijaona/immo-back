package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Lien;
import com.tsix.immocrm.fixtures.FixtureRoles.Restriction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureRoles.ARBRE;
import static com.tsix.immocrm.fixtures.FixtureRoles.KIND_ACTION;
import static com.tsix.immocrm.fixtures.FixtureRoles.RACINE;
import static com.tsix.immocrm.fixtures.FixtureRoles.TEXTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste, fiche, arbre — FEATURE-admin-roles.md, routes de lecture, DTO et règle 4 (affichage). */
@SpringBootTest
@AutoConfigureMockMvc
class RolesListeTest {

    private static final String LIGNES = "$.content[*].code";
    private static final String RESTRICTION = "$.restrictions.";
    private static final UUID INCONNU = UUID.fromString("0D1E2F3A-4B5C-4D6E-8F70-8192A3B4C5D6");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AppelsRoles appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsRoles(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    @Test
    void liste_texte_chercheDansLeCode() throws Exception {
        String cherche = "COD-" + jeton;
        fixture.role(cherche);
        fixture.role("AUT-" + jeton);

        appels.lire(RACINE, TEXTE, cherche).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath(LIGNES, contains(cherche)));
    }

    @Test
    void liste_texte_chercheDansLaDescription() throws Exception {
        String mot = "MOT" + jeton;
        fixture.role("DES-" + jeton, "Libelle " + mot);
        fixture.role("AUT-" + jeton, "Autre");

        appels.lire(RACINE, TEXTE, mot).andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES, contains("DES-" + jeton)));
    }

    @Test
    void liste_triParDefaut_parCode() throws Exception {
        fixture.role("C-" + jeton);
        fixture.role("A-" + jeton);
        fixture.role("B-" + jeton);

        appels.lire(RACINE, TEXTE, jeton).andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES, contains("A-" + jeton, "B-" + jeton, "C-" + jeton)));
    }

    @Test
    void liste_paginee_totalSurTouteLaRecherche() throws Exception {
        fixture.role("A-" + jeton);
        fixture.role("B-" + jeton);

        appels.lire(RACINE, TEXTE, jeton, "size", "1", "page", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath(LIGNES, contains("B-" + jeton)));
    }

    @Test
    void liste_sansResultat_pageVide() throws Exception {
        fixture.role("A-" + jeton);

        appels.lire(RACINE, TEXTE, "ABSENT" + jeton).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", empty()));
    }

    /** {@code utilisateurs} = lignes AdmUserRole (WebRoleId n'y entre pas), {@code droits} = lignes AdmRight, hors arbre compris. */
    @Test
    void liste_compteUtilisateursEtDroits() throws Exception {
        UUID role = fixture.role("CPT-" + jeton, "Compteurs");
        fixture.lier(Lien.UTILISATEUR, role);
        fixture.lier(Lien.UTILISATEUR, role);
        fixture.lier(Lien.UTILISATEUR_WEB, role);
        String dansArbre = "ARB-" + jeton;
        fixture.noeud(dansArbre);
        fixture.droit(role, dansArbre);
        fixture.droit(role, "HORS1-" + jeton);
        fixture.droit(role, "HORS2-" + jeton);

        appels.lire(RACINE, TEXTE, jeton).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(role.toString()))
                .andExpect(jsonPath("$.content[0].code").value("CPT-" + jeton))
                .andExpect(jsonPath("$.content[0].description").value("Compteurs"))
                .andExpect(jsonPath("$.content[0].utilisateurs").value(2))
                .andExpect(jsonPath("$.content[0].droits").value(3));
    }

    @Test
    void liste_roleSansLien_compteursAZero() throws Exception {
        fixture.role("VID-" + jeton);

        appels.lire(RACINE, TEXTE, jeton).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].utilisateurs").value(0))
                .andExpect(jsonPath("$.content[0].droits").value(0));
    }

    @Test
    void fiche_rendCodesDeLArbreEtCompteHorsArbre() throws Exception {
        UUID role = fixture.role("FIC-" + jeton, "Ma fiche");
        String a = "ARB-A-" + jeton;
        String b = "ARB-B-" + jeton;
        fixture.noeud(a);
        fixture.noeud(b);
        fixture.droit(role, a);
        fixture.droit(role, b);
        fixture.droit(role, "HORS-" + jeton);

        appels.fiche(role).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(role.toString()))
                .andExpect(jsonPath("$.code").value("FIC-" + jeton))
                .andExpect(jsonPath("$.description").value("Ma fiche"))
                .andExpect(jsonPath("$.codesDroits", containsInAnyOrder(a, b)))
                .andExpect(jsonPath("$.droitsHorsArbre").value(1))
                .andExpect(jsonPath("$.verrouille").value(false));
    }

    @Test
    void fiche_idInconnu_404() throws Exception {
        UUID existant = fixture.role("EXI-" + jeton);
        appels.fiche(existant).andExpect(status().isOk());

        appels.fiche(INCONNU).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Restriction.class)
    void dto_restrictionNullEnBase_luTrue(Restriction restriction) throws Exception {
        UUID role = fixture.role("NUL-" + jeton);

        appels.fiche(role).andExpect(status().isOk())
                .andExpect(jsonPath(RESTRICTION + restriction.champ()).value(true));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Restriction.class)
    void dto_restriction_luDeSaPropreColonne(Restriction restriction) throws Exception {
        UUID role = fixture.role("COL-" + jeton);
        for (Restriction r : Restriction.values()) {
            fixture.restriction(role, r, true);
        }
        fixture.restriction(role, restriction, false);

        appels.fiche(role).andExpect(status().isOk())
                .andExpect(jsonPath(RESTRICTION + restriction.champ()).value(false));
    }

    @Test
    void r4_valeurExistante_afficheeTelleQuelle() throws Exception {
        UUID role = fixture.role("PNT-" + jeton, ".");

        appels.fiche(role).andExpect(status().isOk()).andExpect(jsonPath("$.description").value("."));

        appels.lire(RACINE, TEXTE, jeton).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("."));
    }

    @Test
    void arbre_trieParSorting_tousLesChamps() throws Exception {
        String premier = "N1-" + jeton;
        String deuxieme = "N2-" + jeton;
        String troisieme = "N3-" + jeton;
        fixture.noeud(troisieme, "Troisieme", 2_000_003, 3, KIND_ACTION);
        fixture.noeud(premier, "Premier", 2_000_001, 0, "Module");
        fixture.noeud(deuxieme, "Deuxieme", 2_000_002, 1, "Form");

        String json = appels.corpsLu(ARBRE);

        List<String> codes = JsonPath.read(json, "$[*].code");
        assertThat(codes.stream().filter(c -> c.contains(jeton)).toList()).containsExactly(premier, deuxieme, troisieme);
        assertThat(JsonPath.<List<Object>>read(json, "$[?(@.code == '" + deuxieme + "')]['description','profondeur','type']"))
                .hasSize(1).first().asString().contains("Deuxieme", "1", "Form");
    }
}

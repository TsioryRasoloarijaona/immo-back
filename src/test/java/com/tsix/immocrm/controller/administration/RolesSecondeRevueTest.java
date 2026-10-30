package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsRoles.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureRoles.DROITS;
import static com.tsix.immocrm.fixtures.FixtureRoles.FICHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Second tour de revue de FEATURE-admin-roles.md : variantes de code, clés d'API, lecture du journal, contenu des traces. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesSecondeRevueTest {

    private static final String JOURNAL = "/api/audit";
    private static final String OBJET = "objetId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AdmUser admin;
    private AppelsRoles appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        admin = fixture.compte();
        appels = new AppelsRoles(mockMvc, connecte(admin));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    /** R2 : espace insécable (U+00A0), cadratin (U+2003), insécable fine (U+202F). {@code trim()} ne les retire pas. */
    @ParameterizedTest(name = "U+{0}")
    @ValueSource(ints = {0x00A0, 0x2003, 0x202F})
    void r2_espaceUnicodeDeBord_retireeDuCode(int espace) throws Exception {
        String code = "ESP-" + jeton;

        appels.creer(corps().avec(CODE, code + (char) espace)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code));
    }

    /** R2 : même mécanisme, le sosie d'un code existant passe la garde d'unicité (ainsi « Administrateur » + U+00A0). */
    @Test
    void r2_sosieParEspaceInsecable_409() throws Exception {
        String code = "DBL-" + jeton;
        fixture.role(code);

        appels.creer(corps().avec(CODE, code + (char) 0x00A0)).andExpect(status().isConflict());
    }

    @Test
    void r7_varianteDeCasse_verrouillee() throws Exception {
        UUID variante = fixture.role("administrateur");
        Etat avant = fixture.etat(variante);

        appels.fiche(variante).andExpect(jsonPath("$.verrouille").value(true));
        appels.modifier(variante, corps().avec(DESCRIPTION, "Pirate")).andExpect(status().isConflict());
        appels.droits(variante, List.of()).andExpect(status().isConflict());
        appels.supprimer(variante).andExpect(status().isConflict());

        assertThat(fixture.etat(variante)).isEqualTo(avant);
    }

    /** Le corps se lit avant le service : un JSON illisible répond 400, pas 409, mais n'écrit rien. */
    @Test
    void r7_corpsIllisible_400_sansEcriture() throws Exception {
        UUID administrateur = fixture.administrateur();
        Etat avant = fixture.etat(administrateur);

        mockMvc.perform(json(patch(FICHE, administrateur), "{pas du json").with(connecte(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(put(DROITS, administrateur), "{pas du json").with(connecte(admin)))
                .andExpect(status().isBadRequest());

        assertThat(fixture.etat(administrateur)).isEqualTo(avant);
    }

    /** Décision 4 : la collation SQL égalerait les deux codes, la comparaison Java non. */
    @Test
    void r10_varianteDeCasse_400_rienEcrit() throws Exception {
        UUID role = fixture.role("CAS-" + jeton);
        String noeud = "NOEUD-" + jeton;
        fixture.noeud(noeud);
        Etat avant = fixture.etat(role);

        appels.droits(role, List.of(noeud.toLowerCase(Locale.ROOT))).andExpect(status().isBadRequest());

        assertThat(fixture.etat(role)).isEqualTo(avant);
    }

    /** R9 : une ligne dont seule la casse diffère de l'arbre compte hors arbre et survit à un PUT vide. */
    @Test
    void r9_ligneVarianteDeCasse_resteHorsArbre() throws Exception {
        UUID role = fixture.role("VAR-" + jeton);
        String noeud = "NOEUD-" + jeton;
        fixture.noeud(noeud);
        UUID variante = fixture.droit(role, noeud.toLowerCase(Locale.ROOT));

        appels.droits(role, List.of()).andExpect(status().isOk());

        assertThat(fixture.lignesDroit(role, noeud)).containsExactly(variante);
        appels.fiche(role).andExpect(jsonPath("$.droitsHorsArbre").value(1));
    }

    /** Toute portée réunie, aucune route du lot n'est ouverte à une clé d'API. */
    @Test
    void cleApi_aucunePortee_neCouvreLesRoles() {
        Set<PorteeCleApi> toutes = EnumSet.allOf(PorteeCleApi.class);
        String racine = FixtureRoles.RACINE;
        UUID id = UUID.randomUUID();

        assertThat(List.of(racine, racine + "/export", racine + "/arbre-droits", racine + "/" + id))
                .isNotEmpty().noneMatch(chemin -> PorteeCleApi.autorise(toutes, "GET", chemin));
        assertThat(List.of("POST", "PATCH", "PUT", "DELETE")).isNotEmpty()
                .noneMatch(verbe -> PorteeCleApi.autorise(toutes, verbe, racine + "/" + id));
    }

    /** Une trace existe ; {@code /api/audit} ne la rend à aucun rôle hors écran : le rôle n'est pas un porteur. */
    @Test
    void journal_commercial_404_surTraceDeRole() throws Exception {
        UUID role = fixture.role("JNL-" + jeton);
        appels.modifier(role, corps().avec(DESCRIPTION, "Tracee")).andExpect(status().isOk());
        assertThat(fixture.traces(role, "Modified")).hasSize(1);

        mockMvc.perform(get(JOURNAL).param(OBJET, role.toString()).with(FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial")))
                .andExpect(status().isNotFound());
    }

    @Test
    void r14_droits_uneSeuleLigneDeTrace_malgreUserUpdate() throws Exception {
        UUID role = fixture.role("UNE-" + jeton);
        String code = "AJ-" + jeton;
        fixture.noeud(code);

        appels.droits(role, List.of(code)).andExpect(status().isOk());

        assertThat(fixture.traces(role, "Modified")).hasSize(1).first().asString().contains("1 droits ajoutés, 0 retirés");
    }

    @Test
    void r14_suppression_neCiteAucunCodeDeDroit() throws Exception {
        UUID role = fixture.role("SUP-" + jeton);
        String code = "DROIT-SECRET-" + UUID.randomUUID();
        UUID ligne = fixture.droit(role, code);

        appels.supprimer(role).andExpect(status().isNoContent());

        assertThat(fixture.traces(role, "Deleted")).hasSize(1).first().asString().doesNotContain(code);
        assertThat(fixture.tracesObjet(ligne)).isZero();
    }
}

package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import com.tsix.immocrm.fixtures.FixtureRoles.Restriction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.ADMINISTRATEUR;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureRoles.RESTRICTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Création et modification — FEATURE-admin-roles.md, règles 2 à 6. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesEcritureTest {

    private static final UUID INCONNU = UUID.fromString("1E2F3A4B-5C6D-4E7F-8091-A2B3C4D5E6F7");
    private static final String COLONNE_CODE = "Code";
    private static final String COLONNE_DESCRIPTION = "Description";
    private static final String DEPART = "Depart";
    private static final String NOUVELLE = "Nouvelle";

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

    private static CorpsJson toutes(boolean valeur) {
        CorpsJson restrictions = corps();
        Arrays.stream(Restriction.values()).forEach(r -> restrictions.avec(r.champ(), valeur));
        return restrictions;
    }

    private int statutCreation(String code) throws Exception {
        return appels.creer(corps().avec(CODE, code)).andReturn().getResponse().getStatus();
    }

    // ------------------------------------------------------------------ règle 2

    @Test
    void r2_creation_201_ligneEnBase() throws Exception {
        String code = "CRE-" + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, "Mon role")).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code))).containsEntry(COLONNE_CODE, code).containsEntry(COLONNE_DESCRIPTION, "Mon role");
    }

    @Test
    void r2_codeAbsent_400() throws Exception {
        appels.creer(corps().avec(DESCRIPTION, "Sans code " + jeton)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void r2_codeVide_400(String code) throws Exception {
        appels.creer(corps().avec(CODE, code)).andExpect(status().isBadRequest());
    }

    /** « Sans espaces de bord » : rejeté ou nettoyé, le spec ne tranche pas ; le code stocké n'en a jamais. */
    @Test
    void r2_espacesDeBord_jamaisStockes() throws Exception {
        assertThat(statutCreation(" BRD-" + jeton + " ")).isIn(201, 400);

        assertThat(fixture.codesAvecEspacesDeBord()).isZero();
    }

    @Test
    void r2_espacesDeBord_neContournentPasLeCodePris() throws Exception {
        String pris = "PRI-" + jeton;
        fixture.role(pris);

        assertThat(statutCreation(pris + " ")).isIn(400, 409);

        assertThat(fixture.nombreRoles(pris)).isEqualTo(1);
    }

    @Test
    void r2_code255_accepte() throws Exception {
        String code = jeton + "X".repeat(255 - jeton.length());

        appels.creer(corps().avec(CODE, code)).andExpect(status().isCreated());

        assertThat(fixture.nombreRoles(code)).isEqualTo(1);
    }

    @Test
    void r2_code256_400() throws Exception {
        String code = jeton + "X".repeat(256 - jeton.length());

        appels.creer(corps().avec(CODE, code)).andExpect(status().isBadRequest());

        assertThat(fixture.nombreRoles(code)).isZero();
    }

    @Test
    void r2_codeDejaPris_409() throws Exception {
        String code = "DBL-" + jeton;
        fixture.role(code);

        appels.creer(corps().avec(CODE, code)).andExpect(status().isConflict());

        assertThat(fixture.nombreRoles(code)).isEqualTo(1);
    }

    @Test
    void r2_codeDejaPris_autreCasse_409() throws Exception {
        String code = "Cas-" + jeton;
        fixture.role(code);

        appels.creer(corps().avec(CODE, code.toLowerCase(Locale.ROOT))).andExpect(status().isConflict());

        assertThat(fixture.nombreRoles(code)).isEqualTo(1);
    }

    /** Un second rôle « ADMINISTRATEUR » échapperait au verrou de la règle 7. */
    @Test
    void r2_administrateurEnMajuscules_409() throws Exception {
        fixture.administrateur();
        int avant = fixture.nombreRoles(ADMINISTRATEUR);

        appels.creer(corps().avec(CODE, ADMINISTRATEUR.toUpperCase(Locale.ROOT))).andExpect(status().isConflict());

        assertThat(fixture.nombreRoles(ADMINISTRATEUR)).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ règle 3

    private void codeIgnore(UUID role, String code, String envoye) throws Exception {
        appels.modifier(role, corps().avec(CODE, envoye).avec(DESCRIPTION, NOUVELLE)).andExpect(status().isOk());

        assertThat(fixture.ligne(role)).containsEntry(COLONNE_CODE, code).containsEntry(COLONNE_DESCRIPTION, NOUVELLE);
    }

    @Test
    void r3_codeDifferent_ignore() throws Exception {
        String code = "IMM-" + jeton;

        codeIgnore(fixture.role(code, DEPART), code, "AUTRE-" + jeton);
    }

    @Test
    void r3_codeIdentique_ignore() throws Exception {
        String code = "IDE-" + jeton;

        codeIgnore(fixture.role(code, DEPART), code, code);
    }

    @Test
    void r3_codeAutreCasse_ignore() throws Exception {
        String code = "Cas-" + jeton;

        codeIgnore(fixture.role(code, DEPART), code, code.toLowerCase(Locale.ROOT));
    }

    @Test
    void r3_codeDUnAutreRole_ignore_pasDeConflit() throws Exception {
        String code = "PRM-" + jeton;
        String autre = "SEC-" + jeton;
        fixture.role(autre);

        codeIgnore(fixture.role(code, DEPART), code, autre);
    }

    @Test
    void r3_codeSeul_rienNeChange() throws Exception {
        UUID role = fixture.role("SEU-" + jeton, DEPART);
        Etat avant = fixture.etat(role);

        appels.modifier(role, corps().avec(CODE, "AUTRE-" + jeton)).andExpect(status().isOk());

        assertThat(fixture.etat(role).role().getFirst()).containsEntry(COLONNE_CODE, avant.role().getFirst().get(COLONNE_CODE));
    }

    // ------------------------------------------------------------------ règle 4

    @Test
    void r4_descriptionFacultative_creation() throws Exception {
        String code = "SAN-" + jeton;

        appels.creer(corps().avec(CODE, code)).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code)).get(COLONNE_DESCRIPTION)).isNull();
    }

    @Test
    void r4_description255_accepteeEnCreation() throws Exception {
        String code = "D55-" + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, "d".repeat(255))).andExpect(status().isCreated());

        assertThat(fixture.nombreRoles(code)).isEqualTo(1);
    }

    @Test
    void r4_description256_400_enCreation() throws Exception {
        String code = "D56-" + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, "d".repeat(256))).andExpect(status().isBadRequest());

        assertThat(fixture.nombreRoles(code)).isZero();
    }

    @Test
    void r4_description255_accepteeEnModification() throws Exception {
        UUID role = fixture.role("M55-" + jeton, DEPART);

        appels.modifier(role, corps().avec(DESCRIPTION, "d".repeat(255))).andExpect(status().isOk());

        assertThat(fixture.ligne(role)).containsEntry(COLONNE_DESCRIPTION, "d".repeat(255));
    }

    @Test
    void r4_description256_400_enModification_rienChange() throws Exception {
        UUID role = fixture.role("M56-" + jeton, DEPART);
        Etat avant = fixture.etat(role);

        appels.modifier(role, corps().avec(DESCRIPTION, "d".repeat(256))).andExpect(status().isBadRequest());

        assertThat(fixture.etat(role)).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ règle 5

    private UUID roleAvecRestrictionsMelangees() {
        UUID role = fixture.role("PAR-" + jeton, DEPART);
        fixture.restriction(role, Restriction.AUDITS, false);
        fixture.restriction(role, Restriction.RAPPORTS, true);
        return role;
    }

    private void restrictionsIntactes(UUID role) {
        assertThat(fixture.ligne(role))
                .containsEntry(Restriction.AUDITS.colonne(), false)
                .containsEntry(Restriction.RAPPORTS.colonne(), true)
                .containsEntry(Restriction.SEQUENCES.colonne(), null);
    }

    @Test
    void r5_descriptionSeule_restrictionsInchangees() throws Exception {
        UUID role = roleAvecRestrictionsMelangees();

        appels.modifier(role, corps().avec(DESCRIPTION, NOUVELLE)).andExpect(status().isOk());

        assertThat(fixture.ligne(role)).containsEntry(COLONNE_DESCRIPTION, NOUVELLE);
        restrictionsIntactes(role);
    }

    @Test
    void r5_descriptionNull_inchangee() throws Exception {
        UUID role = fixture.role("NUL-" + jeton, DEPART);

        appels.modifier(role, corps().avec(DESCRIPTION, null)).andExpect(status().isOk());

        assertThat(fixture.ligne(role)).containsEntry(COLONNE_DESCRIPTION, DEPART);
    }

    @Test
    void r5_corpsVide_rienNeChange() throws Exception {
        UUID role = roleAvecRestrictionsMelangees();

        appels.modifier(role, corps()).andExpect(status().isOk());

        assertThat(fixture.ligne(role)).containsEntry(COLONNE_DESCRIPTION, DEPART);
        restrictionsIntactes(role);
    }

    @Test
    void r5_restrictionsNull_inchangees() throws Exception {
        UUID role = roleAvecRestrictionsMelangees();

        appels.modifier(role, corps().avec(RESTRICTIONS, null)).andExpect(status().isOk());

        restrictionsIntactes(role);
    }

    @Test
    void r5_uneRestrictionNull_gardeSaValeur() throws Exception {
        UUID role = roleAvecRestrictionsMelangees();

        appels.modifier(role, corps().avec(RESTRICTIONS, corps().avec(Restriction.AUDITS.champ(), null)
                .avec(Restriction.RAPPORTS.champ(), null))).andExpect(status().isOk());

        restrictionsIntactes(role);
    }

    /** Ne change que sa colonne, vers les deux valeurs : les NULL voisins restent NULL (un NULL ne doit pas devenir false). */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Restriction.class)
    void r5_uneRestriction_neChangeQueSaColonne(Restriction restriction) throws Exception {
        UUID role = fixture.role("UNE-" + jeton, DEPART);

        appels.modifier(role, corps().avec(RESTRICTIONS, corps().avec(restriction.champ(), false))).andExpect(status().isOk());

        assertThat(fixture.ligne(role)).containsEntry(restriction.colonne(), false).containsEntry(COLONNE_DESCRIPTION, DEPART);
        assertThat(Arrays.stream(Restriction.values()).filter(r -> r != restriction).toList()).isNotEmpty()
                .allSatisfy(r -> assertThat(fixture.ligne(role).get(r.colonne())).isNull());
    }

    @Test
    void r5_roleInconnu_404() throws Exception {
        UUID existant = fixture.role("EXI-" + jeton);
        appels.modifier(existant, corps().avec(DESCRIPTION, NOUVELLE)).andExpect(status().isOk());

        appels.modifier(INCONNU, corps().avec(DESCRIPTION, NOUVELLE)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ règle 6

    @Test
    void r6_restrictionsAbsentes_dixATrue_enBaseEtAuFil() throws Exception {
        String code = "DEF-" + jeton;

        appels.creer(corps().avec(CODE, code)).andExpect(status().isCreated());

        UUID role = fixture.idDe(code);
        assertThat(Arrays.asList(Restriction.values())).hasSize(10)
                .allSatisfy(r -> assertThat(fixture.ligne(role)).containsEntry(r.colonne(), true));
        appels.fiche(role).andExpect(status().isOk())
                .andExpect(jsonPath("$.restrictions.length()").value(10))
                .andExpect(jsonPath("$.restrictions.*", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Restriction.class)
    void r6_restrictionsEnvoyees_chacuneDansSaColonne(Restriction restriction) throws Exception {
        String code = "MAP-" + jeton;

        appels.creer(corps().avec(CODE, code).avec(RESTRICTIONS, toutes(false).avec(restriction.champ(), true)))
                .andExpect(status().isCreated());

        UUID role = fixture.idDe(code);
        assertThat(Arrays.asList(Restriction.values())).isNotEmpty()
                .allSatisfy(r -> assertThat(fixture.ligne(role)).containsEntry(r.colonne(), r == restriction));
    }
}

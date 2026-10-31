package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTES_DEPOT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTE_IDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLE_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R9 (rôles) et R12 (comptes de dépôt) : la différence seule est écrite, un id inconnu ne laisse rien. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursRolesTest {

    private static final UUID INCONNU = UUID.fromString("5B6C7D8E-9FA0-4B1C-82D3-E4F506172839");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AppelsUtilisateurs appels;
    private UUID cible;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
        cible = fixture.compte().getId();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private void poser(String route, CorpsJson corps) throws Exception {
        acceptee(mockMvc.perform(json(put(route, cible), corps.json()).with(connecte(fixture.compte()))));
    }

    // ------------------------------------------------------------------ R9

    /** La ligne gardée conserve sa PKey : un vidage suivi d'une réinsertion en changerait. */
    @Test
    void r9_seuleLaDifferenceEstEcrite() throws Exception {
        UUID garde = fixture.role("GAR-" + jeton);
        UUID retire = fixture.role("RET-" + jeton);
        UUID ajoute = fixture.role("AJO-" + jeton);
        UUID ligneGardee = fixture.lierRole(cible, garde);
        fixture.lierRole(cible, retire);

        acceptee(appels.roles(cible, garde, ajoute));

        assertThat(fixture.rolesDe(cible)).containsExactlyInAnyOrder(garde, ajoute);
        assertThat(fixture.lignesRole(cible, garde)).containsExactly(ligneGardee);
    }

    @Test
    void r9_doublonDansLeCorps_uneSeuleLigne() throws Exception {
        UUID role = fixture.role("DBL-" + jeton);

        acceptee(appels.roles(cible, role, role));

        assertThat(fixture.lignesRole(cible, role)).hasSize(1);
    }

    @Test
    void r9_idInconnu_400_sansEcriture() throws Exception {
        UUID retire = fixture.role("RET-" + jeton);
        UUID ajoute = fixture.role("AJO-" + jeton);
        fixture.lierRole(cible, retire);
        Etat avant = fixture.etat(cible);

        appels.roles(cible, ajoute, INCONNU).andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void r9_listeVide_acceptee() throws Exception {
        fixture.lierRole(cible, fixture.role("RET-" + jeton));

        acceptee(appels.roles(cible));

        assertThat(fixture.rolesDe(cible)).isEmpty();
    }

    @Test
    void r9_changement_incrementeTokenVersion() throws Exception {
        fixture.credential(cible, 7);

        acceptee(appels.roles(cible, fixture.role("AJO-" + jeton)));

        assertThat(fixture.tokenVersion(cible)).isEqualTo(8);
    }

    @Test
    void r9_retraitSeul_incrementeTokenVersion() throws Exception {
        UUID role = fixture.role("RET-" + jeton);
        fixture.lierRole(cible, role);
        fixture.credential(cible, 7);

        acceptee(appels.roles(cible));

        assertThat(fixture.tokenVersion(cible)).isEqualTo(8);
    }

    @Test
    void r9_changement_sansIdentifiants_nEnCreePas() throws Exception {
        acceptee(appels.roles(cible, fixture.role("AJO-" + jeton)));

        assertThat(fixture.tokenVersion(cible)).isNull();
    }

    /** Ordre et doublon différents, même ensemble : rien ne bouge, ni ligne, ni version, ni trace. */
    @Test
    void r9_rienNeChange_rienNestEcrit() throws Exception {
        UUID a = fixture.role("A-" + jeton);
        UUID b = fixture.role("B-" + jeton);
        fixture.lierRole(cible, a);
        fixture.lierRole(cible, b);
        fixture.credential(cible, 7);
        Etat avant = fixture.etat(cible);

        acceptee(appels.roles(cible, b, a, b));

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    /** Réponse 11 : seule une liste vide explicite retire tout ; un corps tronqué ne touche à rien. */
    private void corpsTronque400(String route, String corps) throws Exception {
        fixture.lierRole(cible, fixture.role("GAR-" + jeton));
        fixture.lierDepot(cible, fixture.depot("GAR-" + jeton));
        fixture.credential(cible, 7);
        Etat avant = fixture.etat(cible);

        mockMvc.perform(json(put(route, cible), corps).with(connecte(fixture.compte()))).andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"roleIds\":null}"})
    void reponse11_roleIdsAbsentsOuNull_400_etatInchange(String corps) throws Exception {
        corpsTronque400(ROLES, corps);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"compteIds\":null}"})
    void reponse11_compteIdsAbsentsOuNull_400_etatInchange(String corps) throws Exception {
        corpsTronque400(COMPTES_DEPOT, corps);
    }

    @Test
    void reponse13_putRoles_200_avecLaFiche() throws Exception {
        UUID role = fixture.role("AJO-" + jeton);

        appels.roles(cible, role).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cible.toString()))
                .andExpect(jsonPath("$.roleIds[0]", equalToIgnoringCase(role.toString())));
    }

    @Test
    void reponse13_putComptes_200_avecLaFiche() throws Exception {
        UUID depot = fixture.depot("AJO-" + jeton);

        appels.comptesDepot(cible, depot).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cible.toString()))
                .andExpect(jsonPath("$.compteDepotIds[0]", equalToIgnoringCase(depot.toString())));
    }

    // ------------------------------------------------------------------ R12

    @Test
    void r12_seuleLaDifferenceEstEcrite() throws Exception {
        UUID garde = fixture.depot("GAR-" + jeton);
        UUID retire = fixture.depot("RET-" + jeton);
        UUID ajoute = fixture.depot("AJO-" + jeton);
        UUID ligneGardee = fixture.lierDepot(cible, garde);
        fixture.lierDepot(cible, retire);

        acceptee(appels.comptesDepot(cible, garde, ajoute));

        assertThat(fixture.depotsDe(cible)).containsExactlyInAnyOrder(garde, ajoute);
        assertThat(fixture.lignesDepot(cible, garde)).containsExactly(ligneGardee);
    }

    @Test
    void r12_idInconnu_400_sansEcriture() throws Exception {
        UUID ajoute = fixture.depot("AJO-" + jeton);
        fixture.lierDepot(cible, fixture.depot("RET-" + jeton));
        Etat avant = fixture.etat(cible);

        appels.comptesDepot(cible, ajoute, INCONNU).andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void r12_listeVide_retireTout() throws Exception {
        fixture.lierDepot(cible, fixture.depot("RET-" + jeton));

        acceptee(appels.comptesDepot(cible));

        assertThat(fixture.depotsDe(cible)).isEmpty();
    }

    @Test
    void r12_rienNeChange_rienNestEcrit() throws Exception {
        UUID depot = fixture.depot("GAR-" + jeton);
        fixture.lierDepot(cible, depot);
        Etat avant = fixture.etat(cible);

        acceptee(appels.comptesDepot(cible, depot));

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void r12_neTouchePasAuxRoles() throws Exception {
        UUID role = fixture.role("GAR-" + jeton);
        fixture.lierRole(cible, role);
        fixture.credential(cible, 3);

        poser(COMPTES_DEPOT, corps().avec(COMPTE_IDS, List.of(fixture.depot("AJO-" + jeton))));

        assertThat(fixture.rolesDe(cible)).containsExactly(role);
        assertThat(fixture.tokenVersion(cible)).isEqualTo(3);
    }

    @Test
    void r9_corpsRolesNeTouchePasAuxDepots() throws Exception {
        UUID depot = fixture.depot("GAR-" + jeton);
        fixture.lierDepot(cible, depot);

        poser(ROLES, corps().avec(ROLE_IDS, List.of(fixture.role("AJO-" + jeton))));

        assertThat(fixture.depotsDe(cible)).containsExactly(depot);
    }
}

package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import com.tsix.immocrm.fixtures.FixtureRoles.Restriction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureRoles.RESTRICTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Règle 7 de FEATURE-admin-roles.md : le rôle Administrateur ne bouge par aucune écriture. Chaque 409 relit l'état. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesVerrouTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AppelsRoles appels;
    private UUID administrateur;
    private String noeud;
    private Etat avant;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsRoles(mockMvc, connecte(fixture.compte()));
        administrateur = fixture.administrateur();
        noeud = "ADM-" + jeton;
        fixture.noeud(noeud);
        fixture.droit(administrateur, noeud);
        fixture.droit(administrateur, "HORS-" + jeton);
        avant = fixture.etat(administrateur);
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private void inchange() {
        assertThat(fixture.etat(administrateur)).isEqualTo(avant);
    }

    @Test
    void r7_ficheAdministrateur_verrouillee() throws Exception {
        appels.fiche(administrateur).andExpect(status().isOk()).andExpect(jsonPath("$.verrouille").value(true));
    }

    @Test
    void r7_patchDescription_409() throws Exception {
        appels.modifier(administrateur, corps().avec(DESCRIPTION, "Pirate " + jeton)).andExpect(status().isConflict());

        inchange();
    }

    @Test
    void r7_patchRestriction_409() throws Exception {
        appels.modifier(administrateur, corps().avec(RESTRICTIONS, corps().avec(Restriction.RAPPORTS.champ(), true)))
                .andExpect(status().isConflict());

        inchange();
    }

    @Test
    void r7_patchVide_409() throws Exception {
        appels.modifier(administrateur, corps()).andExpect(status().isConflict());

        inchange();
    }

    @Test
    void r7_patchCode_409() throws Exception {
        appels.modifier(administrateur, corps().avec(CODE, "Commercial")).andExpect(status().isConflict());

        inchange();
    }

    @Test
    void r7_putDroitsVide_409() throws Exception {
        appels.droits(administrateur, List.of()).andExpect(status().isConflict());

        inchange();
    }

    @Test
    void r7_putDroitsInchanges_409() throws Exception {
        appels.droits(administrateur, List.of(noeud)).andExpect(status().isConflict());

        inchange();
    }

    @Test
    void r7_suppression_409() throws Exception {
        appels.supprimer(administrateur).andExpect(status().isConflict());

        inchange();
    }

    /** Témoin : le verrou vise ce rôle, pas tout rôle. */
    @Test
    void r7_autreRole_pasVerrouille() throws Exception {
        UUID autre = fixture.role("LIB-" + jeton);

        appels.fiche(autre).andExpect(status().isOk()).andExpect(jsonPath("$.verrouille").value(false));

        appels.modifier(autre, corps().avec(DESCRIPTION, "Libre")).andExpect(status().isOk());
    }
}

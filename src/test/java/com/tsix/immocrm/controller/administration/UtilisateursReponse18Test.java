package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.REFERENCES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Réponse 18 de FEATURE-admin-utilisateurs.md : coordonnées du personnel dans {@code /references}, vidage des deux liens au PATCH. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursReponse18Test {

    private static final String VIDER_PERSONNEL = "viderPersonnel";
    private static final String VIDER_ROLE_WEB = "viderRoleWeb";
    private static final String CFG_EMPLOYE = "CfgEmployeeId";
    private static final String ROLE_WEB = "WebRoleId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private AppelsUtilisateurs appels;
    private UUID cible;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        AdmUser compte = fixture.compte();
        appels = new AppelsUtilisateurs(mockMvc, connecte(compte));
        cible = fixture.compte().getId();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private String personnelJson(UUID id, String champ) {
        return "$.personnels[?(@.id =~ /(?i)" + id + "/)]." + champ;
    }

    @Test
    void references_personnel_porteSesCoordonnees() throws Exception {
        UUID personnel = fixture.personnel("Nom-" + jeton);
        jdbc.update("UPDATE CfgTier SET EMail = ?, Mobile = ?, Phone = ? WHERE PKey = ?", "p@example.com", "0600", "0522",
                personnel.toString());

        appels.lire(REFERENCES).andExpect(status().isOk())
                .andExpect(jsonPath(personnelJson(personnel, "email"), contains("p@example.com")))
                .andExpect(jsonPath(personnelJson(personnel, "mobile"), contains("0600")))
                .andExpect(jsonPath(personnelJson(personnel, "telephone"), contains("0522")));
    }

    @Test
    void references_coordonneesVides_luesNull() throws Exception {
        UUID personnel = fixture.personnel("Nom-" + jeton);
        jdbc.update("UPDATE CfgTier SET EMail = '', Mobile = '  ', Phone = NULL WHERE PKey = ?", personnel.toString());

        appels.lire(REFERENCES).andExpect(status().isOk())
                .andExpect(jsonPath(personnelJson(personnel, "email"), contains(nullValue())))
                .andExpect(jsonPath(personnelJson(personnel, "mobile"), contains(nullValue())))
                .andExpect(jsonPath(personnelJson(personnel, "telephone"), contains(nullValue())));
    }

    @Test
    void viderPersonnel_remetNull() throws Exception {
        fixture.poser(cible, CFG_EMPLOYE, fixture.personnel("Nom-" + jeton).toString());

        acceptee(appels.modifier(cible, corps().avec(VIDER_PERSONNEL, true)));

        assertThat(fixture.ligne(cible)).containsEntry(CFG_EMPLOYE, null);
    }

    @Test
    void viderRoleWeb_remetNull() throws Exception {
        fixture.poser(cible, ROLE_WEB, fixture.role("WEB-" + jeton).toString());

        acceptee(appels.modifier(cible, corps().avec(VIDER_ROLE_WEB, true)));

        assertThat(fixture.ligne(cible)).containsEntry(ROLE_WEB, null);
    }

    @Test
    void viderSurLienDejaNull_rienEcrit() throws Exception {
        Etat avant = fixture.etat(cible);

        acceptee(appels.modifier(cible, corps().avec(VIDER_PERSONNEL, true).avec(VIDER_ROLE_WEB, true)));

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void viderFaux_liensInchanges() throws Exception {
        String personnel = fixture.personnel("Nom-" + jeton).toString();
        String roleWeb = fixture.role("WEB-" + jeton).toString();
        fixture.poser(cible, CFG_EMPLOYE, personnel);
        fixture.poser(cible, ROLE_WEB, roleWeb);
        Etat avant = fixture.etat(cible);

        acceptee(appels.modifier(cible, corps().avec(VIDER_PERSONNEL, false).avec(VIDER_ROLE_WEB, false)));
        acceptee(appels.modifier(cible, corps().avec(DESCRIPTION, fixture.ligne(cible).get(DESCRIPTION).toString())));

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }
}

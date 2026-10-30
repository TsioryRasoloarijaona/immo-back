package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Secrets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.statut;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.HABILITATIONS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.INACTIF;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.RACINE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.REFERENCES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.SEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R16 sur le texte brut de chaque route : les 5 colonnes sont remplies de valeurs factices, une fuite se voit quel que soit le nom du
 * champ. Chaque réponse doit aussi porter le code du compte, sans quoi un 404 serait « sans secret ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class UtilisateursSecretsTest {

    private static final String AUTRE = "Autre libelle";
    private static final List<String> CHAMPS = List.of("\"pass\"", "webPass", "emailPassword", "openBeePass", "passwordHash");
    private static final String TENTATIVE = "TENTATIVE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AppelsUtilisateurs appels;
    private AdmUser cible;
    private Secrets secrets;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
        cible = fixture.compte();
        secrets = fixture.secrets(cible.getId());
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private void sansSecret(String texte) {
        assertThat(texte).doesNotContain(secrets.valeurs());
        CHAMPS.forEach(champ -> assertThat(texte).doesNotContainIgnoringCase(champ));
    }

    private static String corpsDe(ResultActions reponse) throws Exception {
        return reponse.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static CorpsJson avecSecrets(CorpsJson corps) {
        return corps.avec("pass", TENTATIVE).avec("webPass", TENTATIVE).avec("emailPassword", TENTATIVE)
                .avec("openBeePass", TENTATIVE).avec("passwordHash", TENTATIVE);
    }

    @Test
    void r16_liste() throws Exception {
        String corps = appels.corpsLu(RACINE, SEARCH, cible.getCode());

        assertThat(corps).contains(cible.getCode());
        sansSecret(corps);
    }

    @Test
    void r16_fiche() throws Exception {
        String corps = corpsDe(appels.fiche(cible.getId()));

        assertThat(corps).contains(cible.getCode());
        sansSecret(corps);
    }

    @Test
    void r16_references() throws Exception {
        sansSecret(appels.corpsLu(REFERENCES));
    }

    @Test
    void r16_habilitations() throws Exception {
        sansSecret(appels.corpsLu(HABILITATIONS.replace("{id}", cible.getId().toString())));
    }

    @Test
    void r16_export() throws Exception {
        MvcResult resultat = appels.exporter(EXPORT, SEARCH, cible.getCode());
        status().isOk().match(resultat);

        StringBuilder texte = new StringBuilder();
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(resultat.getResponse().getContentAsByteArray()))) {
            for (Row ligne : classeur.getSheetAt(0)) {
                ligne.forEach(cellule -> texte.append(cellule).append('|'));
            }
        }

        assertThat(texte.toString()).contains(cible.getCode());
        sansSecret(texte.toString());
    }

    @Test
    void r16_reponsesDesEcritures() throws Exception {
        sansSecret(corpsDe(appels.modifier(cible.getId(), corps().avec(DESCRIPTION, AUTRE))));
        sansSecret(corpsDe(appels.roles(cible.getId(), fixture.role("ROL-" + jeton))));
        sansSecret(corpsDe(appels.comptesDepot(cible.getId(), fixture.depot("DEP-" + jeton))));
        sansSecret(corpsDe(appels.habilitations(cible.getId())));
    }

    @Test
    void r16_reponseCreation() throws Exception {
        sansSecret(appels.creer(corps().avec(CODE, "Neuf." + jeton).avec(DESCRIPTION, AUTRE)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** {@code EcouteurAuditOmran} relit toutes les colonnes mappées : un secret mappé ne doit pas y passer. */
    @Test
    void r16_traces_sansSecret() throws Exception {
        UUID id = cible.getId();
        fixture.poser(id, "Email", "avant@example.com");

        corpsDe(appels.modifier(id, corps().avec(DESCRIPTION, AUTRE).avec("email", "apres@example.com")));
        corpsDe(appels.modifier(id, corps().avec(INACTIF, true)));
        corpsDe(appels.roles(id, fixture.role("ROL-" + jeton)));

        List<String> traces = fixture.textesTraces(id);
        assertThat(traces).isNotEmpty();
        sansSecret(String.join(" ", traces));
    }

    /** Décision 2 : champ inconnu ignoré ou corps refusé, jamais écrit. 405 / 404 = route absente, donc pas un refus. */
    @Test
    void r16_decision2_posterUnSecret_jamaisEcrit() throws Exception {
        String code = "Neuf." + jeton;

        int statut = statut(appels.creer(avecSecrets(corps().avec(CODE, code).avec(DESCRIPTION, AUTRE))));

        assertThat(statut).isIn(201, 400);
        if (statut == 201) {
            UUID id = fixture.idDe(code);
            assertThat(fixture.ligne(id)).containsEntry("Pass", null).containsEntry("WebPass", null)
                    .containsEntry("EmailPassword", null).containsEntry("OpenBeePass", null);
            assertThat(fixture.textesTraces(id)).isNotEmpty().noneMatch(t -> t.contains(TENTATIVE));
        }
    }

    @Test
    void r16_decision2_patcherUnSecret_jamaisEcrit() throws Exception {
        int statut = statut(appels.modifier(cible.getId(), avecSecrets(corps().avec(DESCRIPTION, AUTRE))));

        assertThat(statut).isIn(200, 204, 400);
        assertThat(fixture.ligne(cible.getId())).containsEntry("Pass", secrets.pass()).containsEntry("WebPass", secrets.webPass())
                .containsEntry("EmailPassword", secrets.emailPassword()).containsEntry("OpenBeePass", secrets.openBeePass());
    }

    @Test
    void r16_journal_sansSecret(CapturedOutput sortie) throws Exception {
        corpsDe(appels.fiche(cible.getId()));
        corpsDe(appels.modifier(cible.getId(), corps().avec(DESCRIPTION, AUTRE)));

        assertThat(sortie.getAll()).doesNotContain(secrets.valeurs());
    }
}

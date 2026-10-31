package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.INACTIF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Création : R2, R3 (séquentiel), R5, R6, R11. La concurrence de R3 est dans {@link UtilisateursConcurrenceTest}. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursCreationTest {

    private static final String LIBELLE = "Libelle compte";
    private static final String TELEPHONE = "0522000000";
    private static final String MOBILE = "0600000000";
    private static final String COURRIEL = "compte@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AppelsUtilisateurs appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private static CorpsJson corpsAvec(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, LIBELLE);
    }

    private int comptesCreesParApi() {
        return fixture.codesDuJeton().size() - 1;
    }

    // ------------------------------------------------------------------ R2

    @ParameterizedTest
    @ValueSource(strings = {" ", " ", " ", " "})
    void r2_blancsDeBord_retires_unicodeCompris(String blanc) throws Exception {
        String code = "Bord." + jeton;

        appels.creer(corpsAvec(blanc + code + blanc)).andExpect(status().isCreated());

        assertThat(fixture.codesDuJeton()).contains(code);
        assertThat(comptesCreesParApi()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "  "})
    void r2_codeVide_400(String code) throws Exception {
        appels.creer(corpsAvec(code)).andExpect(status().isBadRequest());

        assertThat(comptesCreesParApi()).isZero();
    }

    @Test
    void r2_codeAbsent_400() throws Exception {
        appels.creer(corps().avec(DESCRIPTION, LIBELLE)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_centCaracteres_acceptes() throws Exception {
        String code = jeton + "x".repeat(100 - jeton.length());

        appels.creer(corpsAvec(code)).andExpect(status().isCreated());

        assertThat(fixture.nombreComptes(code)).isEqualTo(1);
    }

    @Test
    void r2_centUnCaracteres_400() throws Exception {
        String code = jeton + "x".repeat(101 - jeton.length());

        appels.creer(corpsAvec(code)).andExpect(status().isBadRequest());

        assertThat(fixture.nombreComptes(code)).isZero();
    }

    // ------------------------------------------------------------------ R3

    @Test
    void r3_memeCodeAutreCasse_409() throws Exception {
        String code = "Sarah." + jeton;
        appels.creer(corpsAvec(code)).andExpect(status().isCreated());

        appels.creer(corpsAvec(code.toLowerCase(Locale.ROOT))).andExpect(status().isConflict());

        assertThat(fixture.nombreComptes(code)).isEqualTo(1);
    }

    @Test
    void r3_compteOmranExistant_autreCasse_409() throws Exception {
        String code = "omar." + jeton.toLowerCase(Locale.ROOT);
        fixture.compte(code);

        appels.creer(corpsAvec(code.toUpperCase(Locale.ROOT))).andExpect(status().isConflict());

        assertThat(fixture.nombreComptes(code)).isEqualTo(1);
    }

    /** Le sosie à blanc insécable ne passe pas la garde : le nettoyage précède la comparaison. */
    @Test
    void r3_sosieBlancUnicode_409() throws Exception {
        String code = "Sosie." + jeton;
        fixture.compte(code);

        appels.creer(corpsAvec(code + " ")).andExpect(status().isConflict());

        assertThat(fixture.nombreComptes(code)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ R5

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void r5_descriptionVide_400(String description) throws Exception {
        String code = "Vide." + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, description)).andExpect(status().isBadRequest());

        assertThat(fixture.nombreComptes(code)).isZero();
    }

    @Test
    void r5_descriptionAbsente_400() throws Exception {
        String code = "Absente." + jeton;

        appels.creer(corps().avec(CODE, code)).andExpect(status().isBadRequest());

        assertThat(fixture.nombreComptes(code)).isZero();
    }

    @Test
    void r5_description255_acceptee() throws Exception {
        appels.creer(corps().avec(CODE, "Long." + jeton).avec(DESCRIPTION, "d".repeat(255))).andExpect(status().isCreated());
    }

    @Test
    void r5_description256_400() throws Exception {
        String code = "Long." + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, "d".repeat(256))).andExpect(status().isBadRequest());

        assertThat(fixture.nombreComptes(code)).isZero();
    }

    // ------------------------------------------------------------------ R6

    @Test
    void r6_defautsOmran() throws Exception {
        String code = "Defauts." + jeton;

        appels.creer(corpsAvec(code)).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code)))
                .containsEntry("Disabled", true)
                .containsEntry("LinesPerPage", 50)
                .containsEntry("ConfirmDelete", true)
                .containsEntry("LockDelay", 3000)
                .containsEntry("CanDisapearNotification", true)
                .containsEntry("EmailPortNumber", 0)
                .containsEntry("EmailSSLActive", false)
                .containsEntry("IsVisibleForChat", false)
                .containsEntry("IsMobileUser", false)
                .containsEntry("Pass", null);
    }

    @Test
    void r6_auteurEtDateRenseignes() throws Exception {
        String code = "Auteur." + jeton;

        appels.creer(corpsAvec(code)).andExpect(status().isCreated());

        Map<String, Object> ligne = fixture.ligne(fixture.idDe(code));
        assertThat(ligne.get("UserCreate")).isNotNull();
        assertThat(ligne.get("UserCreateDateTime")).isNotNull();
    }

    @Test
    void r6_inactifFalseExplicite_respecte() throws Exception {
        String code = "Actif." + jeton;

        appels.creer(corpsAvec(code).avec(INACTIF, false)).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code))).containsEntry("Disabled", false);
    }

    @Test
    void r6_champsModifiablesAuPost_ecrits() throws Exception {
        String code = "Champs." + jeton;
        CorpsJson corps = corpsAvec(code).avec(INACTIF, false).avec("email", COURRIEL).avec("telephone", TELEPHONE)
                .avec("mobile", MOBILE).avec("avecNotification", true).avec("peutMasquerNotification", false)
                .avec("accesMobile", true).avec("smtpServeur", "smtp.example.com").avec("smtpPort", 587).avec("smtpSsl", true);

        appels.creer(corps).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code)))
                .containsEntry("Email", COURRIEL)
                .containsEntry("Phone", TELEPHONE)
                .containsEntry("Mobile", MOBILE)
                .containsEntry("IsVisibleForChat", true)
                .containsEntry("CanDisapearNotification", false)
                .containsEntry("IsMobileUser", true)
                .containsEntry("EmailSMTPServer", "smtp.example.com")
                .containsEntry("EmailPortNumber", 587)
                .containsEntry("EmailSSLActive", true);
    }

    /** Réponse 14 : R7 vaut aussi au POST, que l'inactivité soit demandée ou par défaut (R6). */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reponse14_creationInactive_chatEtMobileAZero(boolean explicite) throws Exception {
        String code = "Inactif." + jeton;
        CorpsJson corps = corpsAvec(code).avec("avecNotification", true).avec("accesMobile", true);

        appels.creer(explicite ? corps.avec(INACTIF, true) : corps).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code)))
                .containsEntry("Disabled", true).containsEntry("IsVisibleForChat", false).containsEntry("IsMobileUser", false);
    }

    /** Décision 2 : le mot de passe Arpent ne naît que par la route de réinitialisation. */
    @Test
    void r6_compteCree_sansCompteWeb() throws Exception {
        String code = "Web." + jeton;

        appels.creer(corpsAvec(code)).andExpect(status().isCreated());

        UUID id = fixture.idDe(code);
        assertThat(fixture.tokenVersion(id)).isNull();
        appels.fiche(id).andExpect(status().isOk()).andExpect(jsonPath("$.compteWeb").value(false));
    }

    // ------------------------------------------------------------------ R11

    @Test
    void r11_personnelEmploye_lie() throws Exception {
        String code = "Perso." + jeton;
        UUID personnel = fixture.personnel("Nom-" + jeton);

        appels.creer(corpsAvec(code).avec("personnelId", personnel)).andExpect(status().isCreated());

        assertThat(fixture.ligne(fixture.idDe(code)).get("CfgEmployeeId").toString()).isEqualToIgnoringCase(personnel.toString());
    }

    @Test
    void r11_personnelInconnu_400() throws Exception {
        String code = "Perso." + jeton;

        appels.creer(corpsAvec(code).avec("personnelId", UUID.randomUUID())).andExpect(status().isBadRequest());

        assertThat(fixture.nombreComptes(code)).isZero();
    }

    @Test
    void r11_personnelHorsTypeEmploye_400() throws Exception {
        String code = "Perso." + jeton;

        appels.creer(corpsAvec(code).avec("personnelId", fixture.client("Client-" + jeton))).andExpect(status().isBadRequest());

        assertThat(fixture.nombreComptes(code)).isZero();
    }
}

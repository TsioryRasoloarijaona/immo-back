package com.tsix.immocrm.security;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * FEATURE-erreur-500-401.md : une erreur serveur répond 500, jamais 401 (le front déconnecterait). Vrai serveur
 * (RANDOM_PORT) : seul le dispatch ERROR réel vers /error reproduit le défaut, MockMvc ne le rejoue pas.
 * Données validées pour de bon (le serveur lit dans sa propre transaction), supprimées en {@code @AfterEach}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErreurServeurTest {

    private static final String MESSAGE_INTERNE = "detail-interne-a-ne-pas-divulguer";
    private static final String ERREUR_IMPREVUE = "/test-erreur-serveur/imprevue";
    private static final String MOT_DE_PASSE = "Password123";
    private static final String CORPS_JSON = "/test-erreur-serveur/corps";

    @RestController
    static class ControleurEnErreur {

        @GetMapping(ERREUR_IMPREVUE)
        String imprevue() {
            throw new IllegalStateException(MESSAGE_INTERNE);
        }

        /** Laissée par le gestionnaire au résolveur par défaut, qui fait sendError(500) : vrai dispatch ERROR. */
        @GetMapping("/test-erreur-serveur/conteneur")
        String conteneur() {
            throw new ConversionNotSupportedException(MESSAGE_INTERNE, String.class, null);
        }

        @GetMapping("/test-erreur-serveur/admin")
        @PreAuthorize("hasRole('Administrateur')")
        String reserveeAdministrateur() {
            return "ok";
        }

        @PostMapping(CORPS_JSON)
        String corpsJson(@RequestBody CorpsTest corps) {
            return corps.nom();
        }

        @GetMapping("/test-erreur-serveur/acces-refuse")
        String accesRefuse() {
            throw new AccessDeniedException(MESSAGE_INTERNE);
        }

        @GetMapping("/test-erreur-serveur/authentification")
        String authentification() {
            throw new BadCredentialsException(MESSAGE_INTERNE);
        }
    }

    record CorpsTest(String nom) {
    }

    @TestConfiguration
    static class ControleurDeTest {
        @Bean
        ControleurEnErreur controleurEnErreur() {
            return new ControleurEnErreur();
        }
    }

    @Value("${local.server.port}") private int port;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private AppUserCredentialRepository credentialRepository;
    @Autowired private CfgTierRepository tierRepository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final UUID tierId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID credentialId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final UUID userRoleId = UUID.randomUUID();
    private String username;

    @BeforeEach
    void creerCompte() {
        username = "erreur500." + UUID.randomUUID();
        new TransactionTemplate(transactionManager).executeWithoutResult(t -> enregistrerCompte());
    }

    @AfterEach
    void supprimerCompte() {
        new TransactionTemplate(transactionManager).executeWithoutResult(t -> {
            userRoleRepository.deleteById(userRoleId);
            roleRepository.deleteById(roleId);
            credentialRepository.deleteById(credentialId);
            userRepository.deleteById(userId);
            tierRepository.deleteById(tierId);
        });
    }

    @Test
    void r1_utilisateurConnecte_exceptionImprevue_500ProblemJsonSansMessageInterne() throws Exception {
        HttpResponse<String> reponse = get(ERREUR_IMPREVUE, connecter());

        assertAll(
                () -> assertThat(reponse.statusCode()).isEqualTo(500),
                () -> assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(
                        type -> assertThat(type).startsWith("application/problem+json")),
                () -> assertThat(reponse.body())
                        .contains("Erreur interne", "Une erreur inattendue est survenue.")
                        .doesNotContain(MESSAGE_INTERNE, "IllegalStateException", "at com."));
    }

    @Test
    void r4_erreurRenduePar_sendError_dispatchErrorReel_500EtNon401() throws Exception {
        HttpResponse<String> reponse = get("/test-erreur-serveur/conteneur", connecter());

        assertAll(
                () -> assertThat(reponse.statusCode()).isEqualTo(500),
                () -> assertThat(reponse.body()).doesNotContain(MESSAGE_INTERNE));
    }

    @Test
    void r2_nonAuthentifie_routeProtegee_toujours401() throws Exception {
        assertAll(
                () -> assertThat(get(ERREUR_IMPREVUE, null).statusCode()).isEqualTo(401),
                () -> assertThat(get("/api/utilisateurs", null).statusCode()).isEqualTo(401));
    }

    @Test
    void r3_roleInsuffisant_toujours403_niAvaleEn500() throws Exception {
        String cookie = connecter();

        assertAll(
                () -> assertThat(get("/api/utilisateurs", cookie).statusCode()).isEqualTo(403),
                () -> assertThat(get("/test-erreur-serveur/admin", cookie).statusCode()).isEqualTo(403));
    }

    @Test
    void routeInconnue_utilisateurConnecte_404EtNon500() throws Exception {
        assertThat(get("/api/route-qui-n-existe-pas", connecter()).statusCode()).isEqualTo(404);
    }

    /** Revue : la règle ERROR/FORWARD n'ouvre pas /error à une requête directe. */
    @Test
    void revue_errorEnAccesDirect_nonAuthentifie_401() throws Exception {
        assertThat(get("/error", null).statusCode()).isEqualTo(401);
    }

    /** Revue : les 4xx du framework (rendues par sendError puis dispatch ERROR) gardent leur statut. */
    @Test
    void revue_erreursDuFramework_gardentLeur4xx() throws Exception {
        String cookie = connecter();

        assertAll(
                () -> assertThat(post(CORPS_JSON, cookie, "application/json", "{pas du json").statusCode())
                        .isEqualTo(400),
                () -> assertThat(post(CORPS_JSON, cookie, "text/plain", "x").statusCode()).isEqualTo(415),
                () -> assertThat(get(CORPS_JSON, cookie).statusCode()).isEqualTo(405));
    }

    /** Revue : une exception Spring Security levée dans un contrôleur n'est pas avalée en 500. */
    @Test
    void revue_exceptionsSecuriteLeveesDansLeControleur_gardent403Et401() throws Exception {
        String cookie = connecter();

        assertAll(
                () -> assertThat(get("/test-erreur-serveur/acces-refuse", cookie).statusCode()).isEqualTo(403),
                () -> assertThat(get("/test-erreur-serveur/authentification", cookie).statusCode()).isEqualTo(401));
    }

    // ------------------------------------------------------------------ outils

    private HttpResponse<String> post(String chemin, String cookie, String type, String corps)
            throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(url(chemin))
                .header("Cookie", cookie)
                .header("Content-Type", type)
                .POST(HttpRequest.BodyPublishers.ofString(corps))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String chemin, String cookie) throws IOException, InterruptedException {
        HttpRequest.Builder requete = HttpRequest.newBuilder(url(chemin)).GET();
        if (cookie != null) {
            requete.header("Cookie", cookie);
        }
        return client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String connecter() throws IOException, InterruptedException {
        String corps = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, MOT_DE_PASSE);
        HttpResponse<String> reponse = client.send(HttpRequest.newBuilder(url("/api/auth/connexion"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corps))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(reponse.statusCode()).isEqualTo(200);
        String setCookie = reponse.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith(CookieJwt.NOM + "="))
                .findFirst()
                .orElseThrow();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private URI url(String chemin) {
        return URI.create("http://localhost:" + port + chemin);
    }

    /** Compte connecté avec un rôle sans droit d'administration : ni mot de passe temporaire ni profil incomplet. */
    private void enregistrerCompte() {
        CfgTier employe = new CfgTier();
        employe.setId(tierId);
        employe.setCode("EMP-E500-" + tierId.toString().substring(0, 8));
        tierRepository.save(employe);

        AdmUser user = new AdmUser();
        user.setId(userId);
        user.setCode(username);
        user.setDescription("Test erreur 500");
        user.setDisabled(false);
        user.setCfgEmployee(employe);
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(credentialId);
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(MOT_DE_PASSE));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        AdmRole role = new AdmRole();
        role.setId(roleId);
        role.setCode("TST-E500-" + roleId.toString().substring(0, 8));
        roleRepository.save(role);

        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(userRoleId);
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);
    }
}

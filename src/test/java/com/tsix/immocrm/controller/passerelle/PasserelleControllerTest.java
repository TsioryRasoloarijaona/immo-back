package com.tsix.immocrm.controller.passerelle;

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
import com.tsix.immocrm.security.CookieJwt;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/passerelle/verifier} par le vrai cookie de connexion, comme
 * {@code AccesRefuseTest} : c'est exactement ce que Caddy relaie depuis le navigateur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasserelleControllerTest {

    private static final String URL = "/api/passerelle/verifier";
    private static final String MOT_DE_PASSE = "Password123";

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private AppUserCredentialRepository credentialRepository;
    @Autowired private CfgTierRepository tierRepository;

    @Test
    void cookieValide_204_etIdentiteDansLesEnTetes() throws Exception {
        Compte compte = creerCompte(true, false, "Commercial", "Direction Générale");
        Cookie cookie = connecter(compte.username());

        MockHttpServletResponse reponse = mockMvc.perform(get(URL).cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(reponse.getContentAsString()).isEmpty();
        assertThat(reponse.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(reponse.getHeader(PasserelleController.EN_TETE_USER_ID)).isEqualTo(compte.user().getId().toString());
        assertThat(reponse.getHeader(PasserelleController.EN_TETE_USERNAME)).isEqualTo(compte.username());
        assertThat(reponse.getHeader(PasserelleController.EN_TETE_EMPLOYEE_ID))
                .isEqualTo(compte.user().getCfgEmployee().getId().toString());

        String roles = reponse.getHeader(PasserelleController.EN_TETE_ROLES);
        assertThat(roles).isNotNull().matches("\\p{ASCII}*").doesNotContain(" ");
        List<String> decodes = Arrays.stream(roles.split(","))
                .map(r -> URLDecoder.decode(r, StandardCharsets.UTF_8))
                .toList();
        assertThat(decodes).containsExactlyInAnyOrder("Commercial", "Direction Générale");
    }

    @Test
    void sansFicheEmploye_pasDEnTeteEmployeeId() throws Exception {
        Compte compte = creerCompte(false, false, "Commercial");

        MockHttpServletResponse reponse = mockMvc.perform(get(URL).cookie(connecter(compte.username())))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(reponse.getHeader(PasserelleController.EN_TETE_USER_ID)).isNotNull();
        assertThat(reponse.containsHeader(PasserelleController.EN_TETE_EMPLOYEE_ID)).isFalse();
    }

    @Test
    void sansCookie_401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    /** Un en-tête d'identité envoyé par le client ne vaut pas authentification. */
    @Test
    void enTeteForgeSansCookie_401() throws Exception {
        mockMvc.perform(get(URL).header(PasserelleController.EN_TETE_USER_ID, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    /** Changement de mot de passe / réinitialisation : TokenVersion incrémenté, l'ancien cookie ne passe plus. */
    @Test
    void tokenRevoque_401() throws Exception {
        Compte compte = creerCompte(true, false, "Commercial");
        Cookie cookie = connecter(compte.username());

        compte.credential().setTokenVersion(compte.credential().getTokenVersion() + 1);
        credentialRepository.saveAndFlush(compte.credential());

        mockMvc.perform(get(URL).cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void motDePasseTemporaire_403_avecLeCodeDOnboarding() throws Exception {
        Compte compte = creerCompte(true, true, "Commercial");

        mockMvc.perform(get(URL).cookie(connecter(compte.username())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MOT_DE_PASSE_TEMPORAIRE"));
    }

    // ------------------------------------------------------------------ fixtures

    private record Compte(String username, AdmUser user, AppUserCredential credential) {
    }

    private Cookie connecter(String username) throws Exception {
        Cookie cookie = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, MOT_DE_PASSE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(CookieJwt.NOM);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private Compte creerCompte(boolean avecFicheEmploye, boolean motDePasseTemporaire, String... roleCodes) {
        String username = "passerelle." + UUID.randomUUID();

        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDescription("Test PASSERELLE");
        user.setDisabled(false);
        if (avecFicheEmploye) {
            CfgTier employe = new CfgTier();
            employe.setId(UUID.randomUUID());
            employe.setCode("EMP-PASS-" + UUID.randomUUID().toString().substring(0, 8));
            tierRepository.save(employe);
            user.setCfgEmployee(employe);
        }
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(MOT_DE_PASSE));
        credential.setMustChangePassword(motDePasseTemporaire);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        for (String roleCode : roleCodes) {
            AdmRole role = new AdmRole();
            role.setId(UUID.randomUUID());
            role.setCode(roleCode);
            roleRepository.save(role);

            AdmUserRole userRole = new AdmUserRole();
            userRole.setId(UUID.randomUUID());
            userRole.setAdmUser(user);
            userRole.setAdmRole(role);
            userRoleRepository.save(userRole);
        }
        return new Compte(username, user, credential);
    }
}

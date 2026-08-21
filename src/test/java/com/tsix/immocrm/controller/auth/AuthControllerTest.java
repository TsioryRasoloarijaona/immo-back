package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private AdmRoleRepository roleRepository;

    @Autowired
    private AdmUserRoleRepository userRoleRepository;

    @Autowired
    private AppUserCredentialRepository credentialRepository;

    private AdmUser newUserWithCredential(String username, String password, boolean disabled) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDisabled(disabled);
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode("Commercial-TEST-" + UUID.randomUUID());
        roleRepository.save(role);

        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);

        return user;
    }

    @Test
    void connexion_validCredentials_returns200WithCookieAndProfile() throws Exception {
        AdmUser user = newUserWithCredential("test.auth.valid." + UUID.randomUUID(), "MotDePasse123", false);
        String body = """
                {"username":"%s","password":"MotDePasse123"}""".formatted(user.getCode());

        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getCode()))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.profileCompleted").value(true))
                .andExpect(jsonPath("$.roles[0]").exists())
                .andExpect(cookie().exists(AuthController.COOKIE_NAME))
                .andExpect(cookie().httpOnly(AuthController.COOKIE_NAME, true));
    }

    @Test
    void connexion_wrongPassword_returns401() throws Exception {
        AdmUser user = newUserWithCredential("test.auth.wrongpwd." + UUID.randomUUID(), "MotDePasse123", false);
        String body = """
                {"username":"%s","password":"mauvais"}""".formatted(user.getCode());

        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void connexion_unknownUsername_returns401() throws Exception {
        String body = """
                {"username":"inconnu.%s","password":"x"}""".formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void connexion_disabledAccount_returns401() throws Exception {
        AdmUser user = newUserWithCredential("test.auth.disabled." + UUID.randomUUID(), "MotDePasse123", true);
        String body = """
                {"username":"%s","password":"MotDePasse123"}""".formatted(user.getCode());

        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deconnexion_clearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/deconnexion"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthController.COOKIE_NAME, 0));
    }
}

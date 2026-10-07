package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.security.CookieJwt;
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
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plus de portail de première connexion (FEATURE-acces-arpent-depuis-omran.md B3) : un compte
 * dont les drapeaux hérités disent encore mustChangePassword=true / profileCompleted=false
 * accède à toute l'API. Deliberately does NOT use @WithMockUser — every request here carries a
 * real JWT cookie obtained by actually logging in, exactly like a real client would, since the
 * removed gate read the flags from that cookie's principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MonCompteControllerTest {

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

    @Autowired
    private CfgTierRepository tierRepository;

    private AdmUser newOnboardingUser(String username, String tempPassword) {
        CfgTier employee = new CfgTier();
        employee.setId(UUID.randomUUID());
        tierRepository.save(employee);

        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDisabled(false);
        user.setCfgEmployee(employee);
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(tempPassword));
        credential.setMustChangePassword(true);
        credential.setProfileCompleted(false);
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

    private Cookie login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}""".formatted(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie(CookieJwt.NOM);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void drapeauxHerites_neBloquentPlusAucuneRoute() throws Exception {
        AdmUser user = newOnboardingUser("test.onboarding." + UUID.randomUUID(), "TempPass123");
        Cookie cookie = login(user.getCode(), "TempPass123");

        // Routes métier accessibles malgré mustChangePassword=true / profileCompleted=false.
        mockMvc.perform(get("/api/prospections").cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/referentiels/etapes-prospection").cookie(cookie))
                .andExpect(status().isOk());

        // Les drapeaux restent renvoyés (contrat), le front les ignore.
        mockMvc.perform(get("/api/mon-compte").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.profileCompleted").value(false));
    }

    /** Changement de mot de passe volontaire et PATCH profil : laissés en place (B3). */
    @Test
    void changementVolontaireEtProfil_fonctionnentToujours() throws Exception {
        AdmUser user = newOnboardingUser("test.onboarding." + UUID.randomUUID(), "TempPass123");
        Cookie cookie = login(user.getCode(), "TempPass123");

        // Change password → new cookie.
        String changePasswordBody = """
                {"ancienMotDePasse":"TempPass123","nouveauMotDePasse":"NouveauMotDePasse456"}""";
        MvcResult changeResult = mockMvc.perform(post("/api/mon-compte/changer-mot-de-passe").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content(changePasswordBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.profileCompleted").value(false))
                .andReturn();
        Cookie cookieAfterPasswordChange = changeResult.getResponse().getCookie(CookieJwt.NOM);
        assertThat(cookieAfterPasswordChange).isNotNull();

        mockMvc.perform(get("/api/referentiels/etapes-prospection").cookie(cookieAfterPasswordChange))
                .andExpect(status().isOk());

        // The old cookie (pre-password-change) is now invalid (TokenVersion bumped).
        mockMvc.perform(get("/api/mon-compte").cookie(cookie))
                .andExpect(status().isUnauthorized());

        // PATCH profil still accepted (no longer called by the frontend).
        String completeProfilBody = """
                {"prenom":"Jean","nom":"Dupont","telephone":"0612345678","email":"jean.dupont@example.com","dateNaissance":"1990-01-01T00:00:00"}""";
        MvcResult completeResult = mockMvc.perform(patch("/api/mon-compte/profil").cookie(cookieAfterPasswordChange)
                        .contentType(MediaType.APPLICATION_JSON).content(completeProfilBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(true))
                .andReturn();
        Cookie finalCookie = completeResult.getResponse().getCookie(CookieJwt.NOM);
        assertThat(finalCookie).isNotNull();

        mockMvc.perform(get("/api/referentiels/etapes-prospection").cookie(finalCookie))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRoute_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/referentiels/etapes-prospection"))
                .andExpect(status().isUnauthorized());
    }
}

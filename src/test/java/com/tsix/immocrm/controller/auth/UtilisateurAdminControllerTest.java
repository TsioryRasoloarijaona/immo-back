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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UtilisateurAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private AppUserCredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdmRoleRepository roleRepository;

    @Autowired
    private AdmUserRoleRepository userRoleRepository;

    private AdmRole newRole(String code) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        return roleRepository.save(role);
    }

    private AdmUser newUser(String username) {
        return newUser(username, null);
    }

    /** Compte AdmUser portant (ou non) une adresse sur AdmUser.Email. */
    private AdmUser newUser(String username, String email) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDisabled(false);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialiser_accountWithNoPriorCredential_createsOneAndReturnsTempPassword() throws Exception {
        AdmUser user = newUser("test.admin-reset.new." + UUID.randomUUID(), "reset.new@example.com");

        MvcResult result = mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getCode()))
                .andExpect(jsonPath("$.motDePasseTemporaire").exists())
                .andReturn();

        String tempPassword = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.motDePasseTemporaire");
        assertThat(tempPassword).hasSize(12);

        AppUserCredential credential = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        // Compte utilisable tout de suite : plus de portail de première connexion.
        assertThat(credential.getMustChangePassword()).isFalse();
        assertThat(credential.getProfileCompleted()).isTrue();
        assertThat(credential.getTokenVersion()).isEqualTo(1);
        assertThat(passwordEncoder.matches(tempPassword, credential.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialiser_existingCredential_bumpsTokenVersionAndClearsOnboardingFlags() throws Exception {
        AdmUser user = newUser("test.admin-reset.existing." + UUID.randomUUID(), "reset.existing@example.com");
        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode("AncienMotDePasse123"));
        // Drapeaux hérités de l'ancien portail : la réinitialisation doit les neutraliser.
        credential.setMustChangePassword(true);
        credential.setProfileCompleted(false);
        credential.setTokenVersion(5);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isOk());

        AppUserCredential updated = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(updated.getMustChangePassword()).isFalse();
        assertThat(updated.getTokenVersion()).isEqualTo(6);
        assertThat(updated.getProfileCompleted()).isTrue();
    }

    @Test
    @WithMockUser // default role USER — not Administrateur
    void reinitialiser_withoutAdminRole_returns403() throws Exception {
        AdmUser user = newUser("test.admin-reset.forbidden." + UUID.randomUUID());

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialiser_unknownUser_returns404() throws Exception {
        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void findAll_returnsUserWithDetailsAndRoles() throws Exception {
        String username = "test.admin-list." + UUID.randomUUID();
        AdmUser user = newUser(username);
        user.setDescription("Jean Rakoto");
        user.setEmail("jean.rakoto@example.com");
        user.setMobile("0600000001");
        userRepository.save(user);

        AdmRole role = newRole("Comptabilite-" + UUID.randomUUID());
        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(passwordEncoder.encode("Passw0rd123"));
        credential.setMustChangePassword(true);
        credential.setProfileCompleted(false);
        credential.setTokenVersion(0);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        mockMvc.perform(get("/api/utilisateurs").param("search", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value(username))
                .andExpect(jsonPath("$.content[0].email").value("jean.rakoto@example.com"))
                .andExpect(jsonPath("$.content[0].disabled").value(false))
                .andExpect(jsonPath("$.content[0].roles[0]").value(role.getCode()))
                .andExpect(jsonPath("$.content[0].compteWebInitialise").value(true))
                .andExpect(jsonPath("$.content[0].mustChangePassword").value(true))
                .andExpect(jsonPath("$.content[0].profileCompleted").value(false));
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void findAll_userWithNoCredentialYet_reportsCompteWebInitialiseFalse() throws Exception {
        String username = "test.admin-list.nocred." + UUID.randomUUID();
        newUser(username);

        mockMvc.perform(get("/api/utilisateurs").param("search", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].compteWebInitialise").value(false))
                .andExpect(jsonPath("$.content[0].mustChangePassword").value(false))
                .andExpect(jsonPath("$.content[0].roles").isEmpty());
    }

    @Test
    @WithMockUser // default role USER — not Administrateur
    void findAll_withoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isForbidden());
    }
}

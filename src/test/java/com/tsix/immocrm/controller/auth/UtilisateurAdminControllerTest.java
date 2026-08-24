package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private CfgTierKindRepository tierKindRepository;

    @Autowired
    private AdmRoleRepository roleRepository;

    private CfgTierKind newTierKind(String code) {
        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        return tierKindRepository.save(kind);
    }

    private AdmRole newRole(String code) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        return roleRepository.save(role);
    }

    private AdmUser newUser(String username) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDisabled(false);
        return userRepository.save(user);
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialiser_accountWithNoPriorCredential_createsOneAndReturnsTempPassword() throws Exception {
        AdmUser user = newUser("test.admin-reset.new." + UUID.randomUUID());

        MvcResult result = mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getCode()))
                .andExpect(jsonPath("$.motDePasseTemporaire").exists())
                .andReturn();

        String tempPassword = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.motDePasseTemporaire");
        assertThat(tempPassword).hasSize(12);

        AppUserCredential credential = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(credential.getMustChangePassword()).isTrue();
        assertThat(credential.getTokenVersion()).isEqualTo(1);
        assertThat(passwordEncoder.matches(tempPassword, credential.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialiser_existingCredential_bumpsTokenVersionAndResetsFlag() throws Exception {
        AdmUser user = newUser("test.admin-reset.existing." + UUID.randomUUID());
        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode("AncienMotDePasse123"));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setTokenVersion(5);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isOk());

        AppUserCredential updated = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(updated.getMustChangePassword()).isTrue();
        assertThat(updated.getTokenVersion()).isEqualTo(6);
        assertThat(updated.getProfileCompleted()).isTrue(); // untouched by a password reset
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
    void creerEmploye_happyPath_createsTierUserRoleAndCredential() throws Exception {
        CfgTierKind kind = newTierKind("COM-CREATE-" + UUID.randomUUID());
        AdmRole role = newRole("Commercial-" + UUID.randomUUID());
        String username = "test.employe.create." + UUID.randomUUID();

        String body = """
                {"prenom":"Luc","nom":"Andria","identifiant":"%s","email":"luc@example.com",
                "mobile":"0600000000","typeEmployeCode":"%s","roles":["%s"]}""".formatted(
                username, kind.getCode(), role.getCode());

        MvcResult result = mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifiant").value(username))
                .andExpect(jsonPath("$.cfgTierId").exists())
                .andExpect(jsonPath("$.admUserId").exists())
                .andExpect(jsonPath("$.roles[0]").value(role.getCode()))
                .andExpect(jsonPath("$.motDePasseTemporaire").exists())
                .andReturn();

        String tempPassword = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.motDePasseTemporaire");
        assertThat(tempPassword).hasSize(12);

        AdmUser createdUser = userRepository.findByCode(username).orElseThrow();
        assertThat(createdUser.getCfgEmployee()).isNotNull();
        assertThat(createdUser.getCfgEmployee().getFirstName()).isEqualTo("Luc");

        AppUserCredential credential = credentialRepository.findByAdmUserId(createdUser.getId()).orElseThrow();
        assertThat(credential.getMustChangePassword()).isTrue();
        assertThat(credential.getProfileCompleted()).isFalse();
        assertThat(passwordEncoder.matches(tempPassword, credential.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void creerEmploye_duplicateIdentifiant_returns409() throws Exception {
        CfgTierKind kind = newTierKind("COM-DUP-TEST-" + UUID.randomUUID());
        AdmRole role = newRole("Commercial-DUP-" + UUID.randomUUID());
        AdmUser existing = newUser("test.employe.dup." + UUID.randomUUID());

        String body = """
                {"prenom":"Luc","nom":"Andria","identifiant":"%s","email":"luc@example.com",
                "typeEmployeCode":"%s","roles":["%s"]}""".formatted(
                existing.getCode(), kind.getCode(), role.getCode());

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void creerEmploye_unknownTypeEmployeCode_returns400() throws Exception {
        AdmRole role = newRole("Commercial-UNK-" + UUID.randomUUID());
        String body = """
                {"prenom":"Luc","nom":"Andria","identifiant":"test.employe.unk.%s",
                "email":"luc@example.com","typeEmployeCode":"CODE_INEXISTANT","roles":["%s"]}""".formatted(
                UUID.randomUUID(), role.getCode());

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser // default role USER — not Administrateur
    void creerEmploye_withoutAdminRole_returns403() throws Exception {
        String body = """
                {"prenom":"Luc","nom":"Andria","identifiant":"test.employe.forbidden.%s",
                "email":"luc@example.com","typeEmployeCode":"COM","roles":["Commercial"]}""".formatted(
                UUID.randomUUID());

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}

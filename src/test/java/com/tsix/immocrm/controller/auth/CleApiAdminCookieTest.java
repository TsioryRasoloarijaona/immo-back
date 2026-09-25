package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.security.CookieJwt;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/cles-api} par le <b>vrai</b> chemin d'un administrateur : connexion par
 * {@code POST /api/auth/connexion}, cookie JWT, puis création — ce que fait Postman. Le test
 * jumeau {@code CleApiAdminTest} passe par {@code @WithMockUser}, qui saute le filtre JWT ; celui-ci
 * prouve que le principal issu du cookie porte bien le rôle et l'auteur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleApiAdminCookieTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private AppUserCredentialRepository credentialRepository;

    @Test
    void administrateurConnecteParCookie_creeUneCle_201() throws Exception {
        Cookie cookie = connecter("Administrateur");

        mockMvc.perform(get("/api/mon-compte").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("Administrateur"));

        String nom = "cle-cookie-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/cles-api").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"" + nom + "\",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\",\"REFERENTIELS_LIRE\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cle").isNotEmpty())
                .andExpect(jsonPath("$.cleApi.creePar").value("Test Administrateur"));
    }

    @Test
    void commercialConnecteParCookie_403_etNon401() throws Exception {
        Cookie cookie = connecter("Commercial");
        mockMvc.perform(post("/api/cles-api").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"x\",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\"]}"))
                .andExpect(status().isForbidden());
    }

    private Cookie connecter(String roleCode) throws Exception {
        String username = "cle." + UUID.randomUUID();
        String motDePasse = "Password123";

        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDescription("Test " + roleCode);
        user.setDisabled(false);
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(motDePasse));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        AdmRole role = roleRepository.findAll().stream()
                .filter(r -> roleCode.equals(r.getCode())).findFirst().orElseGet(() -> {
                    AdmRole r = new AdmRole();
                    r.setId(UUID.randomUUID());
                    r.setCode(roleCode);
                    return roleRepository.save(r);
                });
        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);

        MvcResult resultat = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, motDePasse)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = resultat.getResponse().getCookie(CookieJwt.NOM);
        assertThat(cookie).isNotNull();
        return cookie;
    }
}

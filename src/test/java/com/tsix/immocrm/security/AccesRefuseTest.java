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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>Refus de rôle ⇒ 403, jamais 401</b>, sur un appelant réellement authentifié.
 *
 * <p>Cette distinction n'est pas cosmétique. Un 401 signifie « je ne sais pas qui tu es » et
 * déclenche côté frontend le scénario « session expirée » : purge du cookie, redirection vers
 * l'écran de connexion. Or l'utilisateur est parfaitement connecté — il lui manque seulement un
 * rôle. Un commercial ouvrant par erreur un écran d'administration se retrouvait <b>déconnecté</b>
 * au lieu de lire « accès refusé », et se reconnecter n'y changeait rien.
 *
 * <p><b>Avertissement sur la portée de ce test.</b> Il passe par le vrai cookie plutôt que par
 * {@code SecurityMockMvcRequestPostProcessors.authentication(...)}, ce qui est plus fidèle — mais
 * ce n'était pas là que se jouait le défaut, et <b>MockMvc ne peut pas le reproduire</b>. Le 401
 * de production venait du gestionnaire de refus par défaut, qui appelle {@code sendError(403)} et
 * provoque un forward vers {@code /error} ; ce forward repasse dans la chaîne de sécurité avec un
 * contexte déjà vidé et récrit la réponse en 401. MockMvc ne rejoue pas cette dispatch ERROR :
 * cette classe rendait donc 403 <i>avant même</i> la correction. Elle documente et verrouille le
 * contrat, elle ne l'a pas révélé.
 *
 * <p>La preuve, elle, a été faite hors suite : deux instances réelles lancées côte à côte sur la
 * même base, l'une avec le gestionnaire explicite (403) et l'autre sans (401). C'est le seul
 * montage qui distingue les deux, et il faut y revenir avant de toucher à
 * {@code SecurityConfig.exceptionHandling}.
 *
 * <p>Le cas est vérifié sur <b>deux</b> routes réservées à {@code Administrateur} — une ancienne
 * ({@code /api/utilisateurs}) et une récente ({@code /api/kpi/commerciaux}) — pour que le test
 * porte sur la configuration de sécurité elle-même et non sur un contrôleur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccesRefuseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private AppUserCredentialRepository credentialRepository;
    @Autowired private CfgTierRepository tierRepository;

    @Test
    void appelantAuthentifieSansLeRole_recoit403_etNon401() throws Exception {
        Cookie cookie = connecter("Commercial");

        mockMvc.perform(get("/api/utilisateurs").cookie(cookie))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/kpi/commerciaux").cookie(cookie))
                .andExpect(status().isForbidden());
    }

    /** Le 401 doit rester le 401 : sans cookie, l'appelant n'est pas identifiable. */
    @Test
    void appelantNonAuthentifie_recoitToujours401() throws Exception {
        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/kpi/commerciaux"))
                .andExpect(status().isUnauthorized());
    }

    /** Contrôle de non-régression : avec le rôle, la route répond normalement. */
    @Test
    void administrateur_accedeAuxDeuxRoutes() throws Exception {
        Cookie cookie = connecter("Administrateur");

        mockMvc.perform(get("/api/utilisateurs").cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/kpi/commerciaux").cookie(cookie))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ fixtures

    private Cookie connecter(String role) throws Exception {
        String username = "acces." + UUID.randomUUID();
        String motDePasse = "Password123";
        creerCompte(username, motDePasse, role);

        MvcResult resultat = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, motDePasse)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = resultat.getResponse().getCookie(CookieJwt.NOM);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    /** Compte pleinement intégré : ni mot de passe temporaire ni profil incomplet à franchir. */
    private void creerCompte(String username, String motDePasse, String roleCode) {
        CfgTier employe = new CfgTier();
        employe.setId(UUID.randomUUID());
        employe.setCode("EMP-ACCES-" + UUID.randomUUID().toString().substring(0, 8));
        tierRepository.save(employe);

        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(username);
        user.setDescription("Test ACCES " + roleCode);
        user.setDisabled(false);
        user.setCfgEmployee(employe);
        userRepository.save(user);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(new BCryptPasswordEncoder().encode(motDePasse));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

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
}

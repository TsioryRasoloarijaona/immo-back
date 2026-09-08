package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.mail.MotDePasseTemporaireEmis;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Envoi du mot de passe temporaire par email — voir
 * {@code instructions/FEATURE-service-envoi-email.md}.
 *
 * <p><b>Pourquoi ces tests observent l'événement et non le message envoyé.</b> La suite est
 * {@code @Transactional} et chaque test est annulé à la fin ; or l'expédition est portée par un
 * écouteur {@code @TransactionalEventListener(AFTER_COMMIT)}, qui par construction ne se
 * déclenche <b>jamais</b> dans une transaction annulée. Attendre un message dans
 * {@code EnvoiEmailMemoire} produirait donc un test qui échoue toujours, ou pire, qui passe
 * pour une mauvaise raison.
 *
 * <p>{@code @RecordApplicationEvents} capture en revanche les événements <i>publiés</i>, y
 * compris dans une transaction annulée. On vérifie ici ce qui relève du service — qu'un
 * événement part, et vers quelle adresse. Ce que l'écouteur en fait est vérifié séparément par
 * {@code EmailMotDePasseListenerTest}, et l'envoi lui-même par {@code EnvoiEmailSmtpTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class UtilisateurAdminEmailTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents evenements;

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTierKindRepository tierKindRepository;

    @Autowired
    private AdmRoleRepository roleRepository;

    @Autowired
    private AppUserCredentialRepository credentialRepository;

    private List<MotDePasseTemporaireEmis> evenementsEmis() {
        return evenements.stream(MotDePasseTemporaireEmis.class).toList();
    }

    private AdmUser newUser(String email, CfgTier employe) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode("test.mail." + UUID.randomUUID());
        user.setDisabled(false);
        user.setEmail(email);
        user.setCfgEmployee(employe);
        return userRepository.save(user);
    }

    private CfgTier newTier(String email) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setFirstName("Fiche");
        tier.setLastName("Employe");
        tier.setEMail(email);
        return tierRepository.save(tier);
    }

    private String corpsCreation(String identifiant, String email) {
        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("COM-MAIL-" + UUID.randomUUID());
        tierKindRepository.save(kind);

        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode("Commercial-MAIL-" + UUID.randomUUID());
        roleRepository.save(role);

        return """
                {"prenom":"Luc","nom":"Andria","identifiant":"%s","email":%s,
                "typeEmployeCode":"%s","roles":["%s"]}""".formatted(
                identifiant, email == null ? "null" : "\"" + email + "\"",
                kind.getCode(), role.getCode());
    }

    // --- Création d'employé ---

    @Test
    @WithMockUser(roles = "Administrateur")
    void creation_publieUnEvenementVersLAdresseDeLaRequete() throws Exception {
        String identifiant = "test.mail.create." + UUID.randomUUID();

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation(identifiant, "luc.andria@exemple.ma")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                // Masquée : l'administrateur doit pouvoir vérifier la destination sans que
                // l'adresse complète soit réémise dans la réponse.
                .andExpect(jsonPath("$.destinataire").value("l***@exemple.ma"))
                .andExpect(jsonPath("$.motDePasseTemporaire").exists());

        assertThat(evenementsEmis()).singleElement().satisfies(e -> {
            assertThat(e.destinataire()).isEqualTo("luc.andria@exemple.ma");
            assertThat(e.identifiant()).isEqualTo(identifiant);
            assertThat(e.nomComplet()).isEqualTo("Luc Andria");
            assertThat(e.creation()).isTrue();
            assertThat(e.motDePasse()).isNotBlank();
        });
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void creation_sansEmail_retourne400EtNeCreeAucunCompte() throws Exception {
        String identifiant = "test.mail.noemail." + UUID.randomUUID();

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation(identifiant, null)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByCode(identifiant)).isEmpty();
        assertThat(evenementsEmis()).isEmpty();
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void creation_emailMalforme_retourne400EtNeCreeAucunCompte() throws Exception {
        String identifiant = "test.mail.malforme." + UUID.randomUUID();

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation(identifiant, "luc.andria.exemple.ma")))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByCode(identifiant)).isEmpty();
        assertThat(evenementsEmis()).isEmpty();
    }

    // --- Réinitialisation ---

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_adresseSurAdmUser_publieUnEvenement() throws Exception {
        AdmUser user = newUser("jean.rakoto@exemple.ma", null);

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                .andExpect(jsonPath("$.destinataire").value("j***@exemple.ma"));

        assertThat(evenementsEmis()).singleElement().satisfies(e -> {
            assertThat(e.destinataire()).isEqualTo("jean.rakoto@exemple.ma");
            assertThat(e.creation()).isFalse();
        });
    }

    /**
     * Le repli mesuré sur {@code yesRemsProd} : 24 comptes portent une adresse sur
     * {@code AdmUser}, 17 sur {@code CfgTier}, 25 sur l'une ou l'autre. Sans ce repli, ce
     * compte-là serait refusé alors qu'il est joignable.
     */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_adresseSeulementSurLaFicheEmploye_appliqueLeRepli() throws Exception {
        AdmUser user = newUser(null, newTier("fiche.employe@exemple.ma"));

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinataire").value("f***@exemple.ma"));

        assertThat(evenementsEmis()).singleElement()
                .satisfies(e -> assertThat(e.destinataire()).isEqualTo("fiche.employe@exemple.ma"));
    }

    /** Une chaîne vide, fréquente en base à la place de {@code NULL}, ne vaut pas une adresse. */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_adresseVide_estTraiteeCommeAbsente() throws Exception {
        AdmUser user = newUser("   ", null);

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isBadRequest());

        assertThat(evenementsEmis()).isEmpty();
    }

    /**
     * Le point le plus important de la règle R2 : le refus intervient <b>avant</b> toute
     * écriture. Un 400 ne doit pas laisser l'utilisateur déconnecté de tous ses appareils avec
     * un mot de passe que personne n'a reçu.
     */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_sansAdresse_retourne400EtLaisseLeCompteIntact() throws Exception {
        AdmUser user = newUser(null, null);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash("hash-inchange");
        credential.setMustChangePassword(false);
        credential.setTokenVersion(7);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", user.getId()))
                .andExpect(status().isBadRequest());

        AppUserCredential apres = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(apres.getTokenVersion()).isEqualTo(7);
        assertThat(apres.getPasswordHash()).isEqualTo("hash-inchange");
        assertThat(apres.getMustChangePassword()).isFalse();
        assertThat(evenementsEmis()).isEmpty();
    }

    /** Le 404 doit continuer de primer : un id inconnu n'a pas d'adresse à vérifier. */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_utilisateurInconnu_resteEn404() throws Exception {
        mockMvc.perform(post("/api/utilisateurs/{id}/reinitialiser-mot-de-passe", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

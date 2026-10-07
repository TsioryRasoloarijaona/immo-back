package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.mail.MotDePasseTemporaireEmis;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Réinitialisation du mot de passe Arpent d'un compte Omran et envoi par email — voir
 * {@code instructions/FEATURE-acces-arpent-depuis-omran.md} B1 et
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
 * événement part (ou ne part pas), et vers quelle adresse. Ce que l'écouteur en fait est vérifié
 * séparément par {@code EmailMotDePasseListenerTest}, et l'envoi lui-même par
 * {@code EnvoiEmailSmtpTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class UtilisateurAdminEmailTest {

    private static final String URL = "/api/utilisateurs/{id}/reinitialiser-mot-de-passe";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents evenements;

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private CfgTierRepository tierRepository;

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

    /** Identifiants web hérités de l'ancien portail : mot de passe à changer, profil incomplet. */
    private AppUserCredential credentialExistant(AdmUser user, String hash, int tokenVersion) {
        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(hash);
        credential.setMustChangePassword(true);
        credential.setProfileCompleted(false);
        credential.setTokenVersion(tokenVersion);
        credential.setUserCreateDateTime(LocalDateTime.now());
        return credentialRepository.save(credential);
    }

    /** Appelle la réinitialisation, avec un corps JSON si {@code corps} n'est pas null. */
    private String reinitialiser(AdmUser user, String corps, int statutAttendu) throws Exception {
        MockHttpServletRequestBuilder requete = post(URL, user.getId());
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return mockMvc.perform(requete)
                .andExpect(status().is(statutAttendu))
                .andReturn().getResponse().getContentAsString();
    }

    // --- Compte avec adresse connue ---

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_adresseSurAdmUser_publieUnEvenementEtRendLeCompteUtilisable() throws Exception {
        AdmUser user = newUser("jean.rakoto@exemple.ma", null);
        credentialExistant(user, "ancien-hash", 3);

        mockMvc.perform(post(URL, user.getId()))
                .andExpect(status().isOk())
                // Le service d'envoi de test est actif (application.properties de test).
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                // Masquée : l'administrateur vérifie la destination sans que l'adresse complète
                // soit réémise dans la réponse.
                .andExpect(jsonPath("$.destinataire").value("j***@exemple.ma"))
                .andExpect(jsonPath("$.motDePasseTemporaire").isNotEmpty());

        assertThat(evenementsEmis()).singleElement().satisfies(e -> {
            assertThat(e.destinataire()).isEqualTo("jean.rakoto@exemple.ma");
            assertThat(e.identifiant()).isEqualTo(user.getCode());
            assertThat(e.motDePasse()).isNotBlank();
        });

        AppUserCredential apres = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(apres.getMustChangePassword()).isFalse();
        assertThat(apres.getProfileCompleted()).isTrue();
        assertThat(apres.getTokenVersion()).isEqualTo(4);
    }

    /**
     * Le repli mesuré sur {@code yesRemsProd} : 24 comptes portent une adresse sur
     * {@code AdmUser}, 17 sur {@code CfgTier}, 25 sur l'une ou l'autre.
     */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_adresseSeulementSurLaFicheEmploye_appliqueLeRepli() throws Exception {
        AdmUser user = newUser(null, newTier("fiche.employe@exemple.ma"));

        mockMvc.perform(post(URL, user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinataire").value("f***@exemple.ma"));

        assertThat(evenementsEmis()).singleElement()
                .satisfies(e -> assertThat(e.destinataire()).isEqualTo("fiche.employe@exemple.ma"));
    }

    // --- Compte sans adresse : plus de 400 (5 des 15 comptes actifs de yesRemsProd) ---

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_sansAdresseNiCorps_reussitSansAucunMail() throws Exception {
        AdmUser user = newUser(null, null);

        mockMvc.perform(post(URL, user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getCode()))
                .andExpect(jsonPath("$.motDePasseTemporaire").isNotEmpty())
                .andExpect(jsonPath("$.emailEnvoye").value(false))
                .andExpect(jsonPath("$.destinataire").value(nullValue()));

        assertThat(evenementsEmis()).isEmpty();
        AppUserCredential apres = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(apres.getMustChangePassword()).isFalse();
        assertThat(apres.getProfileCompleted()).isTrue();
        assertThat(apres.getTokenVersion()).isEqualTo(1);
    }

    /** Une chaîne blanche, fréquente en base à la place de {@code NULL}, ne vaut pas une adresse. */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_adresseBlancheEtCorpsVide_estTraiteeCommeAbsente() throws Exception {
        AdmUser user = newUser("   ", null);

        reinitialiser(user, "{}", 200);
        // Un email blanc dans le corps est ignoré, pas refusé.
        reinitialiser(user, "{\"email\":\"  \"}", 200);

        assertThat(evenementsEmis()).isEmpty();
    }

    // --- Adresse fournie dans le corps ---

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_emailDuCorps_estLeDestinataireEtNestJamaisEnregistre() throws Exception {
        CfgTier fiche = newTier("fiche.connue@exemple.ma");
        AdmUser user = newUser("compte.connu@exemple.ma", fiche);

        mockMvc.perform(post(URL, user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" ponctuel@exemple.ma \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                .andExpect(jsonPath("$.destinataire").value("p***@exemple.ma"));

        assertThat(evenementsEmis()).singleElement()
                .satisfies(e -> assertThat(e.destinataire()).isEqualTo("ponctuel@exemple.ma"));

        userRepository.flush();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail())
                .isEqualTo("compte.connu@exemple.ma");
        assertThat(tierRepository.findById(fiche.getId()).orElseThrow().getEMail())
                .isEqualTo("fiche.connue@exemple.ma");
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_emailDuCorpsSurCompteSansAdresse_envoieSansEnregistrer() throws Exception {
        AdmUser user = newUser(null, null);

        reinitialiser(user, "{\"email\":\"ponctuel@exemple.ma\"}", 200);

        assertThat(evenementsEmis()).singleElement()
                .satisfies(e -> assertThat(e.destinataire()).isEqualTo("ponctuel@exemple.ma"));
        userRepository.flush();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isNull();
    }

    /**
     * Le refus intervient <b>avant</b> toute écriture : un 400 ne doit pas déconnecter
     * l'utilisateur de tous ses appareils ni changer son mot de passe.
     */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_emailDuCorpsInvalide_retourne400EtLaisseLeCompteIntact() throws Exception {
        AdmUser user = newUser("jean.rakoto@exemple.ma", null);
        credentialExistant(user, "hash-inchange", 7);

        String reponse = reinitialiser(user, "{\"email\":\"pas-une-adresse\"}", 400);
        assertThat(reponse).contains("adresse email fournie n'est pas valide");

        AppUserCredential apres = credentialRepository.findByAdmUserId(user.getId()).orElseThrow();
        assertThat(apres.getTokenVersion()).isEqualTo(7);
        assertThat(apres.getPasswordHash()).isEqualTo("hash-inchange");
        assertThat(apres.getMustChangePassword()).isTrue();
        assertThat(evenementsEmis()).isEmpty();
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_compteSansCredential_emailInvalide_neCreeRien() throws Exception {
        AdmUser user = newUser(null, null);

        reinitialiser(user, "{\"email\":\"a@b\"}", 400);

        assertThat(credentialRepository.findByAdmUserId(user.getId())).isEmpty();
        assertThat(evenementsEmis()).isEmpty();
    }

    // --- Bout en bout : le mot de passe renvoyé ouvre bien la session ---

    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_puisConnexionAvecLeMotDePasseRenvoye_200() throws Exception {
        AdmUser user = newUser(null, null);
        credentialExistant(user, "ancien-hash", 0);

        String motDePasse = com.jayway.jsonpath.JsonPath.read(reinitialiser(user, null, 200),
                "$.motDePasseTemporaire");

        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(user.getCode(), motDePasse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    // --- Refus ---

    /** Le 404 doit continuer de primer, même avec une adresse valide dans le corps. */
    @Test
    @WithMockUser(roles = "Administrateur")
    void reinitialisation_utilisateurInconnu_resteEn404() throws Exception {
        mockMvc.perform(post(URL, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x@exemple.ma\"}"))
                .andExpect(status().isNotFound());
        assertThat(evenementsEmis()).isEmpty();
    }

    @Test
    @WithMockUser // rôle USER par défaut — pas Administrateur
    void reinitialisation_nonAdmin_403EtAucuneEcriture() throws Exception {
        AdmUser user = newUser("jean.rakoto@exemple.ma", null);

        mockMvc.perform(post(URL, user.getId()))
                .andExpect(status().isForbidden());

        assertThat(evenementsEmis()).isEmpty();
        assertThat(credentialRepository.findByAdmUserId(user.getId())).isEmpty();
    }
}

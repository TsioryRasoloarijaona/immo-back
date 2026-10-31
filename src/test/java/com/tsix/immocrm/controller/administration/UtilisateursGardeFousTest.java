package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Etat;
import com.tsix.immocrm.security.CookieJwt;
import com.tsix.immocrm.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.INACTIF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R7 et R8 (R8 sous concurrence : {@link UtilisateursConcurrenceTest}). L'appelant de {@link #appels} n'a aucun rôle en base, son
 * jeton seul le fait Administrateur : la cible des tests est donc seule à compter. Chaque 409 relit tout l'état du compte.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursGardeFousTest {

    private static final CorpsJson DESACTIVATION = corps().avec(INACTIF, true);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;
    @Autowired private JwtService jwtService;

    private String jeton;
    private AppelsUtilisateurs appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        fixture.suspendreLesAdministrateurs();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private AppelsUtilisateurs parLui(AdmUser compte) {
        return new AppelsUtilisateurs(mockMvc, connecte(compte));
    }

    private void refuse409(AppelsUtilisateurs auteur, UUID cible, CorpsJson corps) throws Exception {
        Etat avant = fixture.etat(cible);

        auteur.modifier(cible, corps).andExpect(status().isConflict());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    private void retraitRefuse409(AppelsUtilisateurs auteur, UUID cible) throws Exception {
        Etat avant = fixture.etat(cible);

        auteur.roles(cible).andExpect(status().isConflict());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ R7

    @Test
    void r7_desactivation_forceChatEtMobileAZero() throws Exception {
        AdmUser cible = fixture.compte();
        fixture.poser(cible.getId(), "IsVisibleForChat", true);
        fixture.poser(cible.getId(), "IsMobileUser", true);

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));

        assertThat(fixture.ligne(cible.getId()))
                .containsEntry("Disabled", true).containsEntry("IsVisibleForChat", false).containsEntry("IsMobileUser", false);
    }

    /** Le corps ne rouvre pas ce que l'inactivité ferme. */
    @Test
    void r7_desactivation_lEmporteSurLesCasesDuMemeCorps() throws Exception {
        AdmUser cible = fixture.compte();

        acceptee(appels.modifier(cible.getId(), corps().avec(INACTIF, true).avec("avecNotification", true).avec("accesMobile", true)));

        assertThat(fixture.ligne(cible.getId())).containsEntry("IsVisibleForChat", false).containsEntry("IsMobileUser", false);
    }

    @Test
    void r7_desactivation_incrementeTokenVersion() throws Exception {
        AdmUser cible = fixture.compte();
        fixture.credential(cible.getId(), 4);

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));

        assertThat(fixture.tokenVersion(cible.getId())).isEqualTo(5);
    }

    @Test
    void r7_desactivation_sansIdentifiants_nEnCreePas() throws Exception {
        AdmUser cible = fixture.compte();

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));

        assertThat(fixture.tokenVersion(cible.getId())).isNull();
    }

    /** Par le vrai cookie : c'est la lecture de {@code TokenVersion} par le filtre JWT qui fait tomber la session. */
    @Test
    void r7_desactivation_sessionTombeAussitot() throws Exception {
        AdmUser cible = fixture.compte();
        fixture.credential(cible.getId(), 2);
        Cookie session = new Cookie(CookieJwt.NOM,
                jwtService.generate(cible.getId(), cible.getCode(), List.of("Commercial"), null, false, true, 2));
        mockMvc.perform(get("/api/mon-compte").cookie(session)).andExpect(status().isOk());

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));

        mockMvc.perform(get("/api/mon-compte").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void r7_modificationSansDesactivation_tokenVersionInchange() throws Exception {
        AdmUser cible = fixture.compte();
        fixture.credential(cible.getId(), 4);

        acceptee(appels.modifier(cible.getId(), corps().avec(DESCRIPTION, "Autre " + jeton)));

        assertThat(fixture.tokenVersion(cible.getId())).isEqualTo(4);
    }

    // ------------------------------------------------------------------ R8, désactivation

    @Test
    void r8_seDesactiverSoiMeme_409() throws Exception {
        AdmUser moi = fixture.administrateurActif();
        fixture.administrateurActif();

        refuse409(parLui(moi), moi.getId(), DESACTIVATION);
    }

    /** Vrai pour tout compte, pas seulement un Administrateur en base. */
    @Test
    void r8_seDesactiverSoiMeme_sansRoleEnBase_409() throws Exception {
        AdmUser moi = fixture.compte();
        fixture.administrateurActif();

        refuse409(parLui(moi), moi.getId(), DESACTIVATION);
    }

    @Test
    void r8_refus_rienDuCorpsNEstEcrit() throws Exception {
        AdmUser dernier = fixture.administrateurActif();

        refuse409(appels, dernier.getId(), corps().avec(INACTIF, true).avec(DESCRIPTION, "Jamais " + jeton));
    }

    @Test
    void r8_desactiverDernierAdministrateur_409() throws Exception {
        refuse409(appels, fixture.administrateurActif().getId(), DESACTIVATION);
    }

    @Test
    void r8_temoin_deuxAdministrateurs_desactivationAcceptee() throws Exception {
        AdmUser cible = fixture.administrateurActif();
        fixture.administrateurActif();

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));

        assertThat(fixture.ligne(cible.getId())).containsEntry("Disabled", true);
    }

    @Test
    void r8_disabledNull_compteCommeActif() throws Exception {
        AdmUser cible = fixture.administrateurActif();
        fixture.lierRole(fixture.compte((Boolean) null).getId(), fixture.administrateur());

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));

        assertThat(fixture.ligne(cible.getId())).containsEntry("Disabled", true);
    }

    @Test
    void r8_administrateurInactif_neComptePas() throws Exception {
        AdmUser cible = fixture.administrateurActif();
        fixture.lierRole(fixture.compte(true).getId(), fixture.administrateur());

        refuse409(appels, cible.getId(), DESACTIVATION);
    }

    @Test
    void r8_cibleNonAdministrateur_pasDeGarde() throws Exception {
        fixture.administrateurActif();
        AdmUser cible = fixture.compte();

        acceptee(appels.modifier(cible.getId(), DESACTIVATION));
    }

    // ------------------------------------------------------------------ R8, rôles

    @Test
    void r8_seRetirerSoiMemeLeRole_409() throws Exception {
        AdmUser moi = fixture.administrateurActif();
        fixture.administrateurActif();

        retraitRefuse409(parLui(moi), moi.getId());
    }

    @Test
    void r8_retirerLeRoleAuDernierAdministrateur_409() throws Exception {
        retraitRefuse409(appels, fixture.administrateurActif().getId());
    }

    /** Le remplacer par un autre rôle reste un retrait. */
    @Test
    void r8_remplacerLeRoleDuDernier_409() throws Exception {
        UUID cible = fixture.administrateurActif().getId();
        UUID autre = fixture.role("AUT-" + jeton);
        Etat avant = fixture.etat(cible);

        appels.roles(cible, autre).andExpect(status().isConflict());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void r8_temoin_deuxAdministrateurs_retraitAccepte() throws Exception {
        AdmUser cible = fixture.administrateurActif();
        fixture.administrateurActif();

        acceptee(appels.roles(cible.getId()));

        assertThat(fixture.rolesDe(cible.getId())).isEmpty();
    }

    /** Garder le rôle Administrateur en ajoutant un autre n'est pas un retrait. */
    @Test
    void r8_temoin_dernierGardeSonRole_ajoutAccepte() throws Exception {
        UUID cible = fixture.administrateurActif().getId();
        UUID autre = fixture.role("AUT-" + jeton);

        acceptee(appels.roles(cible, fixture.administrateur(), autre));

        assertThat(fixture.rolesDe(cible)).containsExactlyInAnyOrder(fixture.administrateur(), autre);
    }

    /** R9 : {@code WebRoleId} ne donne aucun droit, il ne compte donc pas au décompte. */
    @Test
    void r9_webRoleAdministrateur_neComptePasPourLeDecompte() throws Exception {
        UUID dernier = fixture.administrateurActif().getId();
        fixture.poser(fixture.compte().getId(), "WebRoleId", fixture.administrateur().toString());

        refuse409(appels, dernier, DESACTIVATION);
    }
}

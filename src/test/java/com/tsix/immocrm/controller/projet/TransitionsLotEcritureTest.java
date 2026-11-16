package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TRANSITION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TRANSITIONS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.creation;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.modification;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.suppression;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.transition;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.DROIT_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.DROIT_TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Transitions de lot — FEATURE-restes-projet-achat.md §6 « Transitions de lot » (réponse 4 : dans F1) : POST / PUT / DELETE
 * {@code /api/projet/config/transitions-lot} sur le modèle des transitions d'achat (unicité genre + départ + arrivée → 409,
 * départ ≠ arrivée → 400) ; droit {@code Yes.Prj.Front.Config.MarketWorkFlowList} (réponse 6), ordre 404 → 403 → 400 → 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsLotEcritureTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsLot fixture;
    @Autowired private FixtureStatutsAchat comptes;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private UUID a;
    private UUID b;
    private UUID c;
    private RequestPostProcessor avecDroit;
    private RequestPostProcessor sansDroit;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1T");
        fixture.genres();
        a = fixture.statut(ENTREPRENEURS, "F1TA-" + jeton, 1);
        b = fixture.statut(ENTREPRENEURS, "F1TB-" + jeton, 2);
        c = fixture.statut(ENTREPRENEURS, "F1TC-" + jeton, 3);
        avecDroit = comptes.compte(jeton + "O", DROIT_TRANSITIONS);
        sansDroit = comptes.compte(jeton + "N");
    }

    private Map<String, Object> aVersB(String description) {
        return transition(ENTREPRENEURS, a, b, description);
    }

    // ------------------------------------------------------------------ POST

    @Test
    void creation_201_ecritLaTransitionDuGenre() throws Exception {
        String description = "A vers B " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, aVersB(description)).with(avecDroit)).andExpect(status().isCreated());

        assertThat(fixture.transitionsEntre(description, ENTREPRENEURS, a, b)).isEqualTo(1);
    }

    /** Réponse 5 : les 5 genres sont éditables, Fournisseurs compris. */
    @Test
    void creation_genreFournisseurs_201() throws Exception {
        UUID x = fixture.statut(FOURNISSEURS, "F1TX-" + jeton, 1);
        UUID y = fixture.statut(FOURNISSEURS, "F1TY-" + jeton, 2);
        String description = "X vers Y " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, transition(FOURNISSEURS, x, y, description)).with(avecDroit))
                .andExpect(status().isCreated());

        assertThat(fixture.transitionsEntre(description, FOURNISSEURS, x, y)).isEqualTo(1);
    }

    @Test
    void creation_departEgalArrivee_400_rienEcrit() throws Exception {
        String description = "Boucle " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, transition(ENTREPRENEURS, a, a, description)).with(avecDroit))
                .andExpect(status().isBadRequest());

        assertThat(fixture.transitionsDecrites(description)).isZero();
    }

    /** Unicité genre + départ + arrivée : le couple existe déjà dans le genre → 409, rien d'écrit. */
    @Test
    void creation_coupleDejaPresentDansLeGenre_409_rienEcrit() throws Exception {
        fixture.transition(ENTREPRENEURS, "Existante " + jeton, a, b);
        String description = "Doublon " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, aVersB(description)).with(avecDroit)).andExpect(status().isConflict());

        assertThat(fixture.transitionsDecrites(description)).isZero();
    }

    /** Contrat 16/11 : statuts de deux genres différents → 400 ; témoin : même couple de genre → 201. */
    @Test
    void creation_statutsDeGenresDifferents_400_temoin201() throws Exception {
        UUID etranger = fixture.statut(FOURNISSEURS, "F1TF-" + jeton, 1);

        mockMvc.perform(creation(json, TRANSITIONS, aVersB("Témoin genre " + jeton)).with(avecDroit))
                .andExpect(status().isCreated());
        mockMvc.perform(creation(json, TRANSITIONS, transition(ENTREPRENEURS, a, etranger, "Mixte " + jeton)).with(avecDroit))
                .andExpect(status().isBadRequest());

        assertThat(fixture.transitionsDecrites("Mixte " + jeton)).isZero();
    }

    /** Contrat 16/11 : statut de départ ou d'arrivée inconnu → 400 ; témoin 201. */
    @Test
    void creation_statutInconnu_400_temoin201() throws Exception {
        mockMvc.perform(creation(json, TRANSITIONS, aVersB("Témoin inconnu " + jeton)).with(avecDroit))
                .andExpect(status().isCreated());
        mockMvc.perform(creation(json, TRANSITIONS, transition(ENTREPRENEURS, UUID.randomUUID(), b, "Départ inconnu " + jeton))
                .with(avecDroit)).andExpect(status().isBadRequest());
        mockMvc.perform(creation(json, TRANSITIONS, transition(ENTREPRENEURS, a, UUID.randomUUID(), "Arrivée inconnue " + jeton))
                .with(avecDroit)).andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(fixture.transitionsDecrites("Départ inconnu " + jeton)).isZero(),
                () -> assertThat(fixture.transitionsDecrites("Arrivée inconnue " + jeton)).isZero());
    }

    // ------------------------------------------------------------------ rôles (roleIds = ids AdmRole)

    /** Contrat 16/11 : {@code roleIds} au POST → un lien {@code StrWorkFlowIntervener} par rôle, via {@code StrIntervener}. */
    @Test
    void creation_roleIds_liensIntervenantsEcrits() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        UUID r2 = config.role("RB-" + jeton);
        String description = "Avec rôles " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, transition(ENTREPRENEURS, a, b, description, r1, r2)).with(avecDroit))
                .andExpect(status().isCreated());

        UUID t = fixture.transitionDecrite(description, ENTREPRENEURS);
        assertThat(t).isNotNull();
        assertThat(fixture.rolesDeTransition(t))
                .containsExactlyInAnyOrder(r1.toString().toLowerCase(), r2.toString().toLowerCase());
    }

    /** Contrat 16/11 : l'intervenant (rôle, genre) existant est réutilisé, pas dupliqué. */
    @Test
    void creation_intervenantExistant_reutilise() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        UUID ancienne = fixture.transition(ENTREPRENEURS, "Ancienne " + jeton, b, c);
        UUID intervenant = fixture.intervenant(ancienne, ENTREPRENEURS, r1, "IA-" + jeton);
        String description = "Réutilise " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, transition(ENTREPRENEURS, a, b, description, r1)).with(avecDroit))
                .andExpect(status().isCreated());

        UUID t = fixture.transitionDecrite(description, ENTREPRENEURS);
        assertThat(t).isNotNull();
        assertAll(
                () -> assertThat(fixture.intervenantsDuRole(r1, ENTREPRENEURS)).isEqualTo(1),
                () -> assertThat(fixture.liensVers(t, intervenant)).isEqualTo(1));
    }

    /** Contrat 16/11 : le PUT remplace la liste des rôles en bloc. */
    @Test
    void modification_roleIds_remplacesEnBloc() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        UUID r2 = config.role("RB-" + jeton);
        UUID t = fixture.transition(ENTREPRENEURS, "Rôles " + jeton, a, b);
        fixture.intervenant(t, ENTREPRENEURS, r1, "IA-" + jeton);

        mockMvc.perform(modification(json, TRANSITION, t, transition(ENTREPRENEURS, a, b, "Rôles " + jeton, r2))
                .with(avecDroit)).andExpect(status().isOk());

        assertThat(fixture.rolesDeTransition(t)).containsExactly(r2.toString().toLowerCase());
    }

    // ------------------------------------------------------------------ PUT

    @Test
    void modification_200_descriptionEtArriveeMisesAJour() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Avant " + jeton, a, b);
        String description = "Après " + jeton;

        mockMvc.perform(modification(json, TRANSITION, t, transition(ENTREPRENEURS, a, c, description)).with(avecDroit))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.transitionsEntre(description, ENTREPRENEURS, a, c)).isEqualTo(1),
                () -> assertThat(fixture.transitionsDecrites("Avant " + jeton)).isZero());
    }

    /** 404 sur id inconnu ; témoin 200 sur une transition existante (la route existe). */
    @Test
    void modification_idInconnu_404_temoin200() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Témoin " + jeton, a, b);

        mockMvc.perform(modification(json, TRANSITION, t, aVersB("Témoin 2 " + jeton)).with(avecDroit))
                .andExpect(status().isOk());
        mockMvc.perform(modification(json, TRANSITION, UUID.randomUUID(), aVersB("Fantôme " + jeton)).with(avecDroit))
                .andExpect(status().isNotFound());
    }

    /** Modifier une transition vers le couple d'une autre du genre → 409, la transition reste inchangée. */
    @Test
    void modification_coupleDUneAutre_409_inchangee() throws Exception {
        fixture.transition(ENTREPRENEURS, "Occupe " + jeton, a, b);
        UUID t = fixture.transition(ENTREPRENEURS, "Mobile " + jeton, a, c);

        mockMvc.perform(modification(json, TRANSITION, t, aVersB("Mobile " + jeton)).with(avecDroit))
                .andExpect(status().isConflict());

        assertThat(fixture.transitionsEntre("Mobile " + jeton, ENTREPRENEURS, a, c)).isEqualTo(1);
    }

    @Test
    void modification_departEgalArrivee_400_inchangee() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Stable " + jeton, a, b);

        mockMvc.perform(modification(json, TRANSITION, t, transition(ENTREPRENEURS, b, b, "Stable " + jeton))
                .with(avecDroit)).andExpect(status().isBadRequest());

        assertThat(fixture.transitionsEntre("Stable " + jeton, ENTREPRENEURS, a, b)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ DELETE

    /** Suppression → 204 ; les liens rôles ({@code StrWorkFlowIntervener}) partent avec la transition. */
    @Test
    void suppression_204_transitionEtLiensIntervenantsSupprimes() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Partante " + jeton, a, b);
        fixture.intervenant(t, ENTREPRENEURS, config.role("R1-" + jeton), "I1-" + jeton);
        fixture.intervenant(t, ENTREPRENEURS, config.role("R2-" + jeton), "I2-" + jeton);
        int liensAvant = fixture.liensIntervenants(t);

        mockMvc.perform(suppression(TRANSITION, t).with(avecDroit)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(liensAvant).isEqualTo(2),
                () -> assertThat(fixture.transitionsParId(t)).isZero(),
                () -> assertThat(fixture.liensIntervenants(t)).isZero());
    }

    @Test
    void suppression_idInconnu_404_temoin204() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Témoin " + jeton, a, b);

        mockMvc.perform(suppression(TRANSITION, t).with(avecDroit)).andExpect(status().isNoContent());
        mockMvc.perform(suppression(TRANSITION, UUID.randomUUID()).with(avecDroit)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ droits

    @Test
    void creation_sansLeDroit_403_rienEcrit() throws Exception {
        String description = "Interdite " + jeton;

        mockMvc.perform(creation(json, TRANSITIONS, aVersB(description)).with(sansDroit)).andExpect(status().isForbidden());

        assertThat(fixture.transitionsDecrites(description)).isZero();
    }

    /** Le droit des statuts de lot n'ouvre pas l'écriture des transitions. */
    @Test
    void creation_droitDesStatutsSeulement_403() throws Exception {
        RequestPostProcessor statuts = comptes.compte(jeton + "S", DROIT_STATUTS);

        mockMvc.perform(creation(json, TRANSITIONS, aVersB("Statuts " + jeton)).with(statuts))
                .andExpect(status().isForbidden());
    }

    @Test
    void creation_administrateur_201() throws Exception {
        mockMvc.perform(creation(json, TRANSITIONS, aVersB("Admin " + jeton)).with(comptes.administrateur(jeton)))
                .andExpect(status().isCreated());
    }

    /** 403 avant 400 : départ = arrivée ; témoin 400 pour le compte porteur du droit. */
    @Test
    void creation_sansLeDroit_corpsInvalide_403_temoin400() throws Exception {
        Map<String, Object> boucle = transition(ENTREPRENEURS, a, a, "Boucle " + jeton);

        mockMvc.perform(creation(json, TRANSITIONS, boucle).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(creation(json, TRANSITIONS, boucle).with(avecDroit)).andExpect(status().isBadRequest());
    }

    @Test
    void modification_sansLeDroit_403_inchangee() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Gardée " + jeton, a, b);

        mockMvc.perform(modification(json, TRANSITION, t, aVersB("Volée " + jeton)).with(sansDroit))
                .andExpect(status().isForbidden());

        assertThat(fixture.transitionsEntre("Gardée " + jeton, ENTREPRENEURS, a, b)).isEqualTo(1);
    }

    @Test
    void suppression_sansLeDroit_403_conservee() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Conservée " + jeton, a, b);

        mockMvc.perform(suppression(TRANSITION, t).with(sansDroit)).andExpect(status().isForbidden());

        assertThat(fixture.transitionsParId(t)).isEqualTo(1);
    }

    /** 404 avant 403 : sans droit, id inconnu → 404 (PUT et DELETE) ; témoin : 403 sur une transition existante. */
    @Test
    void sansLeDroit_idInconnu_404_temoin403() throws Exception {
        UUID t = fixture.transition(ENTREPRENEURS, "Existante " + jeton, a, b);
        mockMvc.perform(modification(json, TRANSITION, t, aVersB("Existante " + jeton)).with(sansDroit))
                .andExpect(status().isForbidden());
        mockMvc.perform(suppression(TRANSITION, t).with(sansDroit)).andExpect(status().isForbidden());

        mockMvc.perform(modification(json, TRANSITION, UUID.randomUUID(), aVersB("Fantôme " + jeton)).with(sansDroit))
                .andExpect(status().isNotFound());
        mockMvc.perform(suppression(TRANSITION, UUID.randomUUID()).with(sansDroit)).andExpect(status().isNotFound());
    }
}

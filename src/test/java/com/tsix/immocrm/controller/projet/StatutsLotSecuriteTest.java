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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CATALOGUE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CODE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUT;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUTS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.corps;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.creation;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.modification;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.suppression;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.DROIT_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.DROIT_TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.TYPE_INTERMEDIAIRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des statuts de lot — FEATURE-restes-projet-achat.md §6 « Droits » (réponse 6) : lectures ouvertes à tout compte
 * connecté ; POST / PUT / DELETE exigent {@code Yes.Prj.Front.Config.MarketStatusList} ({@code AdmRight} des rôles du compte),
 * l'Administrateur passe ; ordre 404 → 403 → 400 → 409. Chaque refus a son témoin (même verbe, compte porteur du droit).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsLotSecuriteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsLot fixture;
    @Autowired private FixtureStatutsAchat comptes;

    private String jeton;
    private UUID existant;
    private RequestPostProcessor sansDroit;
    private RequestPostProcessor avecDroit;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1S");
        fixture.genres();
        existant = fixture.statut(ENTREPRENEURS, "F1SE-" + jeton, 1);
        sansDroit = comptes.compte(jeton + "N");
        avecDroit = comptes.compte(jeton + "O", DROIT_STATUTS);
    }

    private Map<String, Object> corpsCreation() {
        return corps(ENTREPRENEURS, "F1SC-" + jeton, TYPE_INTERMEDIAIRE);
    }

    private Map<String, Object> corpsModification() {
        Map<String, Object> c = corps(ENTREPRENEURS, "F1SE-" + jeton, TYPE_INTERMEDIAIRE);
        c.put(DESCRIPTION, "Modifié " + jeton);
        return c;
    }

    // ------------------------------------------------------------------ lectures

    @Test
    void lectures_compteSansDroit_200() throws Exception {
        mockMvc.perform(get(STATUTS).with(sansDroit)).andExpect(status().isOk());
        mockMvc.perform(get(CATALOGUE).param("genreId", ENTREPRENEURS.toString()).with(sansDroit)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ POST

    @Test
    void creation_sansLeDroit_403_rienEcrit_temoin201() throws Exception {
        mockMvc.perform(creation(json, STATUTS, corpsCreation()).with(sansDroit)).andExpect(status().isForbidden());
        int apresRefus = fixture.statutsDuCode(ENTREPRENEURS, "F1SC-" + jeton);
        mockMvc.perform(creation(json, STATUTS, corpsCreation()).with(avecDroit)).andExpect(status().isCreated());

        assertAll(
                () -> assertThat(apresRefus).isZero(),
                () -> assertThat(fixture.statutsDuCode(ENTREPRENEURS, "F1SC-" + jeton)).isEqualTo(1));
    }

    /** Le droit des transitions de lot n'ouvre pas l'écriture des statuts. */
    @Test
    void creation_droitDesTransitionsSeulement_403() throws Exception {
        RequestPostProcessor transitions = comptes.compte(jeton + "T", DROIT_TRANSITIONS);

        mockMvc.perform(creation(json, STATUTS, corpsCreation()).with(transitions)).andExpect(status().isForbidden());
    }

    /** Le rôle Administrateur (en base) passe sans ligne {@code AdmRight}. */
    @Test
    void creation_administrateurSansAdmRight_201() throws Exception {
        mockMvc.perform(creation(json, STATUTS, corpsCreation()).with(comptes.administrateur(jeton)))
                .andExpect(status().isCreated());
    }

    /** 403 avant 400 : corps invalide (code absent) ; témoin : 400 pour le compte porteur du droit. */
    @Test
    void creation_sansLeDroit_corpsInvalide_403_temoin400() throws Exception {
        Map<String, Object> invalide = corpsCreation();
        invalide.remove(CODE);

        mockMvc.perform(creation(json, STATUTS, invalide).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(creation(json, STATUTS, invalide).with(avecDroit)).andExpect(status().isBadRequest());
    }

    /** 403 avant 409 : code déjà pris dans le genre ; témoin : 409 pour le compte porteur du droit. */
    @Test
    void creation_sansLeDroit_codePris_403_temoin409() throws Exception {
        Map<String, Object> doublon = corps(ENTREPRENEURS, "F1SE-" + jeton, TYPE_INTERMEDIAIRE);

        mockMvc.perform(creation(json, STATUTS, doublon).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(creation(json, STATUTS, doublon).with(avecDroit)).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ PUT

    @Test
    void modification_sansLeDroit_403_inchange_temoin200() throws Exception {
        mockMvc.perform(modification(json, STATUT, existant, corpsModification()).with(sansDroit))
                .andExpect(status().isForbidden());
        Object apresRefus = fixture.ligne(existant).get("Description");
        mockMvc.perform(modification(json, STATUT, existant, corpsModification()).with(avecDroit)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(apresRefus).isEqualTo("F1SE-" + jeton),
                () -> assertThat(fixture.ligne(existant)).isNotNull().containsEntry("Description", "Modifié " + jeton));
    }

    @Test
    void modification_sansLeDroit_corpsInvalide_403_temoin400() throws Exception {
        Map<String, Object> invalide = corpsModification();
        invalide.remove(CODE);

        mockMvc.perform(modification(json, STATUT, existant, invalide).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(modification(json, STATUT, existant, invalide).with(avecDroit)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ DELETE

    @Test
    void suppression_sansLeDroit_403_conserve_temoin204() throws Exception {
        mockMvc.perform(suppression(STATUT, existant).with(sansDroit)).andExpect(status().isForbidden());
        int apresRefus = fixture.statutsParId(existant);
        mockMvc.perform(suppression(STATUT, existant).with(avecDroit)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(apresRefus).isEqualTo(1),
                () -> assertThat(fixture.statutsParId(existant)).isZero());
    }

    // ------------------------------------------------------------------ ordre 404 → 403

    /** Sans droit, un id inconnu répond 404 ; témoin : 403 sur le statut existant (PUT et DELETE). */
    @Test
    void sansLeDroit_idInconnu_404_temoin403() throws Exception {
        List<MockHttpServletRequestBuilder> existantes = List.of(
                modification(json, STATUT, existant, corpsModification()), suppression(STATUT, existant));
        List<MockHttpServletRequestBuilder> inconnues = List.of(
                modification(json, STATUT, UUID.randomUUID(), corpsModification()), suppression(STATUT, UUID.randomUUID()));

        for (MockHttpServletRequestBuilder r : existantes) {
            mockMvc.perform(r.with(sansDroit)).andExpect(status().isForbidden());
        }
        for (MockHttpServletRequestBuilder r : inconnues) {
            mockMvc.perform(r.with(sansDroit)).andExpect(status().isNotFound());
        }
    }
}

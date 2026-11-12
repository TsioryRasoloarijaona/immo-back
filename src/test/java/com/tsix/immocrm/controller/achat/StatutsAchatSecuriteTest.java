package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CHAMPS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CODE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DESCRIPTION;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.IMAGES;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPES;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPES_DOCUMENTS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.creation;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.modification;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.suppression;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits — FEATURE-statuts-achat-parite.md règle 1 et R1 : lectures ouvertes à tout compte connecté ; POST / PUT / DELETE
 * exigent {@code Yes.Buy.Front.Config.BuyDocumentStatus} ({@code AdmRight} des rôles du compte, lu en base) ; le rôle
 * Administrateur passe toujours. Chaque refus a son témoin (même verbe, même corps, compte porteur du droit).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatSecuriteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private UUID type;
    private UUID existant;
    private RequestPostProcessor sansDroit;
    private RequestPostProcessor avecDroit;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2S");
        statuts.tables();
        type = config.typeEtat("Intermédiaire " + jeton);
        existant = config.statut(Statut.simple("K2SE-" + jeton, 1));
        sansDroit = statuts.compte(jeton + "N");
        avecDroit = statuts.compte(jeton + "O", FixtureStatutsAchat.DROIT_STATUTS);
    }

    private Map<String, Object> corpsCreation() {
        return corps("K2SC-" + jeton, type);
    }

    private Map<String, Object> corpsModification() {
        Map<String, Object> c = corps("K2SE-" + jeton, type);
        c.put(DESCRIPTION, "Modifié " + jeton);
        return c;
    }

    private Map<String, Object> existantRelu() throws Exception {
        return ligne(lire(mockMvc, STATUTS, avecDroit), existant);
    }

    // ------------------------------------------------------------------ lectures

    /** Règle 1 : les lectures sont ouvertes à un compte sans aucun droit OMRAN. */
    @ParameterizedTest
    @ValueSource(strings = {STATUTS, TYPES, CHAMPS, TYPES_DOCUMENTS, IMAGES})
    void r1_lectures_compteSansDroit_200(String url) throws Exception {
        mockMvc.perform(get(url).with(sansDroit)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {TYPES, CHAMPS, TYPES_DOCUMENTS, IMAGES})
    void r1_lectures_sansJeton_401(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ POST

    @Test
    void r1_creation_sansLeDroit_403_rienEcrit() throws Exception {
        mockMvc.perform(creation(json, corpsCreation()).with(sansDroit)).andExpect(status().isForbidden());

        assertThat(statuts.statutsNommes("K2SC-" + jeton, "Statut K2SC-" + jeton)).isZero();
    }

    @Test
    void r1_creation_avecLeDroit_temoin201() throws Exception {
        mockMvc.perform(creation(json, corpsCreation()).with(avecDroit)).andExpect(status().isCreated());
    }

    /** Le droit d'un autre écran (Images) n'ouvre pas l'écriture des statuts. */
    @Test
    void r1_creation_droitDUnAutreEcran_403() throws Exception {
        RequestPostProcessor images = statuts.compte(jeton + "I", "Yes.Cfg.Front.Image.ImageList");

        mockMvc.perform(creation(json, corpsCreation()).with(images)).andExpect(status().isForbidden());
    }

    /** R1 : le rôle Administrateur (en base) passe sans ligne {@code AdmRight}. */
    @Test
    void r1_creation_administrateurSansAdmRight_201() throws Exception {
        mockMvc.perform(creation(json, corpsCreation()).with(statuts.administrateur(jeton))).andExpect(status().isCreated());
    }

    /** Réponses 2ᵉ passe n° 5 : 403 avant 400 — corps invalide (code absent) ; témoin : 400 pour le compte porteur du droit. */
    @Test
    void p5_creation_sansLeDroit_corpsInvalide_403_temoin400() throws Exception {
        Map<String, Object> invalide = corpsCreation();
        invalide.remove(CODE);

        mockMvc.perform(creation(json, invalide).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(creation(json, invalide).with(avecDroit)).andExpect(status().isBadRequest());
    }

    /** N° 5 sur PUT : corps invalide sans le droit → 403 ; témoin 400 avec le droit. */
    @Test
    void p5_modification_sansLeDroit_corpsInvalide_403_temoin400() throws Exception {
        Map<String, Object> invalide = corpsModification();
        invalide.remove(CODE);

        mockMvc.perform(modification(json, existant, invalide).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(modification(json, existant, invalide).with(avecDroit)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ PUT

    @Test
    void r1_modification_sansLeDroit_403_ficheInchangee() throws Exception {
        mockMvc.perform(modification(json, existant, corpsModification()).with(sansDroit)).andExpect(status().isForbidden());

        assertThat(existantRelu()).isNotNull().containsEntry(DESCRIPTION, "Statut K2SE-" + jeton);
    }

    @Test
    void r1_modification_avecLeDroit_temoin200() throws Exception {
        mockMvc.perform(modification(json, existant, corpsModification()).with(avecDroit)).andExpect(status().isOk());

        assertThat(existantRelu()).isNotNull().containsEntry(DESCRIPTION, "Modifié " + jeton)
                .containsEntry(CODE, "K2SE-" + jeton);
    }

    // ------------------------------------------------------------------ DELETE

    @Test
    void r1_suppression_sansLeDroit_403_statutConserve() throws Exception {
        mockMvc.perform(suppression(existant).with(sansDroit)).andExpect(status().isForbidden());

        assertThat(statuts.statutsParId(existant)).isEqualTo(1);
    }

    @Test
    void r1_suppression_avecLeDroit_temoin204() throws Exception {
        mockMvc.perform(suppression(existant).with(avecDroit)).andExpect(status().isNoContent());

        assertThat(statuts.statutsParId(existant)).isZero();
    }
}

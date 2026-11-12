package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CHAMPS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CODE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DESCRIPTION;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.IMAGES;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPES;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPES_DOCUMENTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Listes de la fiche statut — FEATURE-statuts-achat-parite.md « Endpoints » : types d'état ({@code AdmWorkFlowStateType}),
 * catalogues des champs obligatoires (R6, Q6), types de documents ({@code AdmAttachedFileType}), bibliothèque d'images
 * ({@code CfgImage}, réponse du manager n° 3 : sélecteur seul).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatCataloguesTest {

    private static final String NOM = "nom";
    private static final String LIBELLE = "libelle";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2L");
        qui = statuts.compte(jeton);
    }

    /** GET /statuts/types : chaque type d'état avec id, code, description. */
    @Test
    void types_listeLesTypesEtat() throws Exception {
        UUID type = config.typeEtat("Terminer avec succès " + jeton);

        Map<String, Object> l = ligne(lire(mockMvc, TYPES, qui), type);

        assertThat(l).isNotNull()
                .containsEntry(CODE, "Terminer avec succès " + jeton)
                .containsEntry(DESCRIPTION, "Terminer avec succès " + jeton);
    }

    /** GET /statuts/types-documents : chaque {@code AdmAttachedFileType} avec id, code, description. */
    @Test
    void typesDocuments_listeLesTypes() throws Exception {
        UUID type = statuts.typeDocument("Attestation " + jeton);

        Map<String, Object> l = ligne(lire(mockMvc, TYPES_DOCUMENTS, qui), type);

        assertThat(l).isNotNull()
                .containsEntry(CODE, "Attestation " + jeton)
                .containsEntry(DESCRIPTION, "Attestation " + jeton);
    }

    private Map<String, List<Map<String, Object>>> catalogue() throws Exception {
        String corps = mockMvc.perform(get(CHAMPS).with(qui)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corps, "$");
    }

    /** R6 : catalogue pièce = les 7 propriétés [IsWorkFlow] de DocumentBase (au moins), chacune avec un libellé. */
    @Test
    void r6_cataloguePiece_contientLesProprietesDeDocumentBase() throws Exception {
        List<Map<String, Object>> piece = catalogue().get("piece");

        assertAll(
                () -> assertThat(piece).extracting(c -> c.get(NOM)).contains("MaturityDate", "PersonToContact", "DocRef",
                        "OfficeOrderRef", "Notes", "CfgTierTriggerId", "Notes2"),
                () -> assertThat(piece).isNotEmpty()
                        .allSatisfy(c -> assertThat(String.valueOf(c.get(LIBELLE))).isNotBlank().isNotEqualTo("null")));
    }

    /** R6 : catalogue fournisseur = propriétés [IsWorkFlow] de CfgTier (ex. Address, Phone, Fax, WebSite), libellées. */
    @Test
    void r6_catalogueFournisseur_contientLesProprietesDeCfgTier() throws Exception {
        List<Map<String, Object>> fournisseur = catalogue().get("fournisseur");

        assertAll(
                () -> assertThat(fournisseur).extracting(c -> c.get(NOM))
                        .contains("Address", "ZipCode", "Phone", "Fax", "WebSite", "BankAgency"),
                () -> assertThat(fournisseur).isNotEmpty()
                        .allSatisfy(c -> assertThat(String.valueOf(c.get(LIBELLE))).isNotBlank().isNotEqualTo("null")));
    }

    /** GET /images : chaque image de la bibliothèque avec id, code et taille en octets. */
    @Test
    void images_listeLaBibliotheque_idCodeTaille() throws Exception {
        UUID image = statuts.image("IMG-" + jeton, FixtureStatutsAchat.PNG);

        Map<String, Object> l = ligne(lire(mockMvc, IMAGES, qui), image);

        assertThat(l).isNotNull()
                .containsEntry(CODE, "IMG-" + jeton)
                .containsEntry("taille", FixtureStatutsAchat.PNG.length);
    }
}

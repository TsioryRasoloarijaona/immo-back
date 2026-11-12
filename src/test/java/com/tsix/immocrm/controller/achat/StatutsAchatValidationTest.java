package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CHAMPS_FOURNISSEUR;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CHAMPS_PIECE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CODE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.COULEUR;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DESCRIPTION;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DOCUMENTS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DUREE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.IMAGE_ID;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.NOTES;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.ORDRE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPE_ID;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.creation;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.detail;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.document;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.id;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.modification;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Refus 400 de POST / PUT /api/achat/config/statuts — FEATURE-statuts-achat-parite.md règles 2, 4, 5, 6, 7, 8 et
 * « Vérification » (un test par refus). Chaque refus n'écrit rien ; témoin : le corps de base répond 201.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String code;
    private RequestPostProcessor admin;
    private UUID type;
    private UUID typeDocument;

    @BeforeEach
    void setUp() {
        String jeton = FixtureConfigurationAchat.jeton("K2V");
        code = "K2V-" + jeton;
        statuts.tables();
        admin = statuts.administrateur(jeton);
        type = config.typeEtat("Nouveau " + jeton);
        typeDocument = statuts.typeDocument("Facture " + jeton);
    }

    private Map<String, Object> base() {
        return corps(code, type);
    }

    /** Témoin : le corps de base, sans mutation, est accepté. */
    @Test
    void temoin_corpsDeBase_201() throws Exception {
        mockMvc.perform(creation(json, base()).with(admin)).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ R2 obligatoires (message nommant le champ)

    static Stream<Arguments> obligatoires() {
        return Stream.of(
                arguments("ordre absent", ORDRE, (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.remove(ORDRE)),
                arguments("code absent", CODE, (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.remove(CODE)),
                arguments("code blanc", CODE, (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(CODE, "   ")),
                arguments("description absente", DESCRIPTION,
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.remove(DESCRIPTION)),
                arguments("description blanche", DESCRIPTION,
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(DESCRIPTION, "  ")),
                arguments("type absent", "type", (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.remove(TYPE_ID)));
    }

    /** R2 : ordre, code, description, type obligatoires → 400, le {@code detail} nomme le champ, rien n'est écrit. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("obligatoires")
    void r2_obligatoireManquant_400_messageNommeLeChamp(String cas, String champ,
                                                       BiConsumer<Map<String, Object>, UUID> mutation) throws Exception {
        Map<String, Object> c = base();
        String description = (String) c.get(DESCRIPTION);
        mutation.accept(c, typeDocument);

        MvcResult r = mockMvc.perform(creation(json, c).with(admin)).andExpect(status().isBadRequest()).andReturn();

        assertAll(
                () -> assertThat(detail(r)).as(cas).containsIgnoringCase(champ),
                () -> assertThat(statuts.statutsNommes(code, description)).isZero());
    }

    // ------------------------------------------------------------------ autres refus 400

    static Stream<Arguments> invalides() {
        return Stream.of(
                arguments("R2 code > 255", (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(CODE, "C".repeat(256))),
                arguments("R2 description > 255",
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(DESCRIPTION, "D".repeat(256))),
                arguments("R2 type inconnu",
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(TYPE_ID, UUID.randomUUID().toString())),
                arguments("R2 notes > 1000", (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(NOTES, "n".repeat(1001))),
                arguments("R4 durée négative", (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(DUREE, -1)),
                arguments("R5 couleur inconnue",
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(COULEUR, "RougeVifInconnu")),
                arguments("R6 image inexistante",
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(IMAGE_ID, UUID.randomUUID().toString())),
                arguments("R7 champ pièce hors catalogue",
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(CHAMPS_PIECE, List.of("DocRef", "ChampInconnuK2"))),
                arguments("R7 champ fournisseur hors catalogue",
                        (BiConsumer<Map<String, Object>, UUID>) (c, d) -> c.put(CHAMPS_FOURNISSEUR, List.of("ChampInconnuK2"))),
                arguments("R8 type de document inconnu", (BiConsumer<Map<String, Object>, UUID>) (c, d) ->
                        c.put(DOCUMENTS, List.of(document(UUID.randomUUID(), true, false)))),
                arguments("R8 même type de document deux fois", (BiConsumer<Map<String, Object>, UUID>) (c, d) ->
                        c.put(DOCUMENTS, List.of(document(d, true, false), document(d, false, true)))));
    }

    /** Un test par refus de la « Vérification » : 400 et aucun statut écrit. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalides")
    void creation_invalide_400_sansEcriture(String cas, BiConsumer<Map<String, Object>, UUID> mutation) throws Exception {
        Map<String, Object> c = base();
        String description = (String) c.get(DESCRIPTION);
        mutation.accept(c, typeDocument);

        mockMvc.perform(creation(json, c).with(admin)).andExpect(status().isBadRequest());

        assertThat(statuts.statutsNommes(code, description)).as(cas).isZero();
    }

    /** Les mêmes refus en modification : 400 et la fiche reste telle quelle. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalides")
    void modification_invalide_400_ficheInchangee(String cas, BiConsumer<Map<String, Object>, UUID> mutation) throws Exception {
        UUID id = id(mockMvc.perform(creation(json, base()).with(admin)).andExpect(status().isCreated()).andReturn());
        Map<String, Object> c = base();
        mutation.accept(c, typeDocument);

        mockMvc.perform(modification(json, id, c).with(admin)).andExpect(status().isBadRequest());

        assertThat(ligne(lire(mockMvc, STATUTS, admin), id)).as(cas).isNotNull()
                .containsEntry(CODE, code)
                .containsEntry(DESCRIPTION, "Statut " + code)
                .containsEntry(DUREE, 0);
    }
}

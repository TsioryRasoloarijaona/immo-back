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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.AVEC_TITRE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CHAMPS_FOURNISSEUR;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CHAMPS_PIECE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CODE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.COULEUR;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DESCRIPTION;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DOCUMENTS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DUREE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.IMAGE_ID;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.LECTURE_SEULE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.NOTES;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.OBLIGATOIRE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.OK_PAIEMENT;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.ORDRE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.STATUT;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPE_DOCUMENT_ID;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.TYPE_ID;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.VALIDE_COMPTA;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.creation;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.document;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.id;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.liste;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.modification;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.noms;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Création, relecture, modification de {@code BuyDocumentStatus} — FEATURE-statuts-achat-parite.md « Endpoints », « DTO »,
 * règles 3, 7, 8, 12 et « Vérification » (création complète, relecture, modification).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatEcritureTest {

    private static final String NOMBRE_PIECES = "nombrePieces";
    private static final String NOMBRE_TRANSITIONS = "nombreTransitions";
    private static final String NOMBRE_HISTORIQUES = "nombreHistoriques";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID type;
    private UUID image;
    private UUID contrat;
    private UUID plan;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2E");
        statuts.tables();
        admin = statuts.administrateur(jeton);
        type = config.typeEtat("Intermédiaire " + jeton);
        image = statuts.image("IMG-" + jeton, FixtureStatutsAchat.PNG);
        contrat = statuts.typeDocument("Contrat " + jeton);
        plan = statuts.typeDocument("Plan " + jeton);
    }

    private Map<String, Object> complet(String code) {
        Map<String, Object> c = corps(code, type);
        c.put(ORDRE, 12);
        c.put(DUREE, 5);
        c.put(COULEUR, "Pink");
        c.put(OK_PAIEMENT, true);
        c.put(VALIDE_COMPTA, true);
        c.put(LECTURE_SEULE, true);
        c.put(IMAGE_ID, image.toString());
        c.put(NOTES, "Note " + code);
        c.put(CHAMPS_PIECE, List.of("DocRef", "Notes"));
        c.put(CHAMPS_FOURNISSEUR, List.of("Address", "Phone"));
        c.put(DOCUMENTS, List.of(document(contrat, true, false)));
        return c;
    }

    private UUID creer(Map<String, Object> corps) throws Exception {
        MvcResult r = mockMvc.perform(creation(json, corps).with(admin)).andExpect(status().isCreated()).andReturn();
        return id(r);
    }

    private Map<String, Object> relire(UUID id) throws Exception {
        return ligne(lire(mockMvc, STATUTS, admin), id);
    }

    // ------------------------------------------------------------------ création, relecture

    /** Vérification « création complète » : 201, puis chaque champ relu par GET /statuts. */
    @Test
    void creationComplete_201_relueParGetStatuts() throws Exception {
        String code = "K2C-" + jeton;

        UUID id = creer(complet(code));

        Map<String, Object> s = relire(id);
        assertThat(s).isNotNull()
                .containsEntry(CODE, code)
                .containsEntry(DESCRIPTION, "Statut " + code)
                .containsEntry(ORDRE, 12)
                .containsEntry(TYPE_ID, type.toString())
                .containsEntry("categorie", "Intermédiaire " + jeton)
                .containsEntry(DUREE, 5)
                .containsEntry(COULEUR, "Pink")
                .containsEntry(OK_PAIEMENT, true)
                .containsEntry(VALIDE_COMPTA, true)
                .containsEntry(LECTURE_SEULE, true)
                .containsEntry(IMAGE_ID, image.toString())
                .containsEntry(NOTES, "Note " + code)
                .containsEntry(NOMBRE_PIECES, 0)
                .containsEntry(NOMBRE_TRANSITIONS, 0)
                .containsEntry(NOMBRE_HISTORIQUES, 0);
        assertAll(
                () -> assertThat(noms(s, CHAMPS_PIECE)).containsExactlyInAnyOrder("DocRef", "Notes"),
                () -> assertThat(noms(s, CHAMPS_FOURNISSEUR)).containsExactlyInAnyOrder("Address", "Phone"),
                () -> assertThat(liste(s, DOCUMENTS)).singleElement().satisfies(d -> assertThat(d)
                        .containsEntry(TYPE_DOCUMENT_ID, contrat.toString())
                        .containsEntry("typeDocumentLibelle", "Contrat " + jeton)
                        .containsEntry(OBLIGATOIRE, true)
                        .containsEntry(AVEC_TITRE, false)),
                () -> assertThat(statuts.documentsDuStatut(id)).isEqualTo(1));
    }

    /** POST répond le {@code StatutAchatConfigDto} créé. */
    @Test
    void creation_reponseEstLeStatutCree() throws Exception {
        String code = "K2R-" + jeton;

        MvcResult r = mockMvc.perform(creation(json, complet(code)).with(admin)).andExpect(status().isCreated()).andReturn();

        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":\"" + code + "\"");
    }

    /** DTO : défauts — durée 0, booléens faux, sans image, notes null, listes vides, comptes à 0. */
    @Test
    void creationMinimale_valeursParDefaut() throws Exception {
        UUID id = creer(corps("K2M-" + jeton, type));

        Map<String, Object> s = relire(id);
        assertThat(s).isNotNull()
                .containsEntry(DUREE, 0)
                .containsEntry(OK_PAIEMENT, false)
                .containsEntry(VALIDE_COMPTA, false)
                .containsEntry(LECTURE_SEULE, false)
                .containsEntry(IMAGE_ID, null)
                .containsEntry(NOTES, null)
                .containsEntry(NOMBRE_TRANSITIONS, 0)
                .containsEntry(NOMBRE_HISTORIQUES, 0);
        assertAll(
                () -> assertThat(noms(s, CHAMPS_PIECE)).isNotNull().isEmpty(),
                () -> assertThat(noms(s, CHAMPS_FOURNISSEUR)).isNotNull().isEmpty(),
                () -> assertThat(liste(s, DOCUMENTS)).isNotNull().isEmpty());
    }

    /** R2 : code et description stockés sans leurs espaces de bord. */
    @Test
    void r2_codeEtDescription_trim() throws Exception {
        String code = "K2T-" + jeton;
        Map<String, Object> c = corps("  " + code + "  ", type);
        c.put(DESCRIPTION, "  Libellé " + code + " ");

        UUID id = creer(c);

        assertThat(relire(id)).isNotNull().containsEntry(CODE, code).containsEntry(DESCRIPTION, "Libellé " + code);
    }

    /** Couleur : chacune des 8 valeurs réelles et le vide sont acceptés (R5). */
    @ParameterizedTest
    @ValueSource(strings = {"Black", "DarkGreen", "Lime", "Orange", "Pink", "Purple", "Red", "Yellow", ""})
    void r5_couleurNommeeOuVide_201(String couleur) throws Exception {
        Map<String, Object> c = corps("K2K-" + jeton, type);
        c.put(COULEUR, couleur);

        mockMvc.perform(creation(json, c).with(admin)).andExpect(status().isCreated());
    }

    /** R4 : durée 0 (infinie) acceptée. */
    @Test
    void r4_dureeZero_201() throws Exception {
        Map<String, Object> c = corps("K2Z-" + jeton, type);
        c.put(DUREE, 0);

        UUID id = creer(c);

        assertThat(relire(id)).isNotNull().containsEntry(DUREE, 0);
    }

    // ------------------------------------------------------------------ modification

    /** PUT remplace la fiche entière ; documents à joindre resynchronisés (ajout et retrait). */
    @Test
    void modification_remplaceLaFiche_documentsResynchronises() throws Exception {
        String code = "K2P-" + jeton;
        UUID id = creer(complet(code));
        Map<String, Object> c = corps(code, type);
        c.put(DESCRIPTION, "Modifié " + code);
        c.put(COULEUR, "DarkGreen");
        c.put(CHAMPS_PIECE, List.of("MaturityDate"));
        c.put(CHAMPS_FOURNISSEUR, List.of());
        c.put(DOCUMENTS, List.of(document(plan, false, true)));

        mockMvc.perform(modification(json, id, c).with(admin)).andExpect(status().isOk());

        Map<String, Object> s = relire(id);
        assertThat(s).isNotNull()
                .containsEntry(DESCRIPTION, "Modifié " + code)
                .containsEntry(COULEUR, "DarkGreen")
                .containsEntry(OK_PAIEMENT, false)
                .containsEntry(DUREE, 0)
                .containsEntry(IMAGE_ID, null)
                .containsEntry(NOTES, null);
        assertAll(
                () -> assertThat(noms(s, CHAMPS_PIECE)).containsExactly("MaturityDate"),
                () -> assertThat(noms(s, CHAMPS_FOURNISSEUR)).isNotNull().isEmpty(),
                () -> assertThat(liste(s, DOCUMENTS)).singleElement().satisfies(d -> assertThat(d)
                        .containsEntry(TYPE_DOCUMENT_ID, plan.toString())
                        .containsEntry(OBLIGATOIRE, false)
                        .containsEntry(AVEC_TITRE, true)),
                () -> assertThat(statuts.documentsDuStatut(id)).isEqualTo(1));
    }

    /** PUT répond 200 et le statut modifié. */
    @Test
    void modification_200_reponseEstLeStatut() throws Exception {
        String code = "K2Q-" + jeton;
        UUID id = creer(corps(code, type));
        Map<String, Object> c = corps(code, type);
        c.put(DESCRIPTION, "Neuf " + code);

        MvcResult r = mockMvc.perform(modification(json, id, c).with(admin)).andExpect(status().isOk()).andReturn();

        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"description\":\"Neuf " + code + "\"");
    }

    /** 404 sur id inconnu ; témoin : le même corps sur un statut existant répond 200 (la route existe). */
    @Test
    void modification_idInconnu_404_temoin200() throws Exception {
        String code = "K2N-" + jeton;
        UUID id = creer(corps(code, type));

        mockMvc.perform(modification(json, id, corps(code, type)).with(admin)).andExpect(status().isOk());
        mockMvc.perform(modification(json, UUID.randomUUID(), corps(code, type)).with(admin))
                .andExpect(status().isNotFound());
    }

    /** {@code {id}} mal formé → 400 (avant le droit). */
    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void idMalForme_400(String methode) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(methode), STATUT, "pas-un-uuid").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps("K2X-" + jeton, type))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R3 unicité du code

    /** R3 : code déjà pris par un autre statut, comparé sans casse ni espaces de bord → 409, rien d'écrit. */
    @Test
    void r3_creation_codeEnDouble_sansCasseNiEspaces_409() throws Exception {
        String code = "K2U-" + jeton;
        config.statut(Statut.simple(code, 1));
        Map<String, Object> c = corps("  " + code.toLowerCase(Locale.ROOT) + " ", type);
        c.put(DESCRIPTION, "Doublon " + code);

        mockMvc.perform(creation(json, c).with(admin)).andExpect(status().isConflict());

        assertThat(statuts.statutsNommes(code, "Doublon " + code)).isEqualTo(1);
    }

    /** R3 : la fiche modifiée ne se compare pas à elle-même (même code, autre casse). */
    @Test
    void r3_modification_memeCode_seComparePasASoi_200() throws Exception {
        String code = "K2S-" + jeton;
        UUID id = creer(corps(code, type));

        mockMvc.perform(modification(json, id, corps(code.toLowerCase(Locale.ROOT), type)).with(admin))
                .andExpect(status().isOk());
    }

    /** R3 : prendre le code d'un autre statut en modification → 409. */
    @Test
    void r3_modification_codeDUnAutre_409() throws Exception {
        String codeA = "K2A-" + jeton;
        config.statut(Statut.simple(codeA, 1));
        UUID b = creer(corps("K2B-" + jeton, type));

        mockMvc.perform(modification(json, b, corps(" " + codeA.toLowerCase(Locale.ROOT), type)).with(admin))
                .andExpect(status().isConflict());

        assertThat(relire(b)).isNotNull().containsEntry(CODE, "K2B-" + jeton);
    }

    /** R3 : l'ordre n'est pas unique. */
    @Test
    void r3_ordreEnDouble_accepte() throws Exception {
        config.statut(Statut.simple("K2O-" + jeton, 4));
        Map<String, Object> c = corps("K2O2-" + jeton, type);
        c.put(ORDRE, 4);

        mockMvc.perform(creation(json, c).with(admin)).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ comptes de la liste

    /** DTO : nombreTransitions = transitions citant le statut en départ ou en arrivée ; nombreHistoriques = passages. */
    @Test
    void liste_nombreTransitionsEtNombreHistoriques() throws Exception {
        UUID s = config.statut(Statut.simple("K2L-" + jeton, 1));
        UUID autre = config.statut(Statut.simple("K2L2-" + jeton, 2));
        config.transition(null, 1, "Aller " + jeton, s, autre);
        config.transition(null, 2, "Retour " + jeton, autre, s);
        UUID piece = config.piece(autre, null);
        config.passage(piece, null, s, LocalDateTime.of(2022, 4, 1, 9, 0));

        Map<String, Object> l = relire(s);

        assertThat(l).isNotNull()
                .containsEntry(NOMBRE_TRANSITIONS, 2)
                .containsEntry(NOMBRE_HISTORIQUES, 1)
                .containsEntry(NOMBRE_PIECES, 0);
    }
}

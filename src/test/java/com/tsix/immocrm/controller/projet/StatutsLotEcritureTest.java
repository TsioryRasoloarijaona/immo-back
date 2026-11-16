package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CASES;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CATALOGUE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CHAMPS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.COULEUR;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DUREE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.IMAGE_ID;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.NOTES;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.ORDRE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUT;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUTS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.corps;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.creation;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.id;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.ligne;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.modification;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CHAMP_CAUSE_RESILIATION;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CHAMP_LANCEMENT_TRAVAUX;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CHAMP_SIGNATURE_MARCHE;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CONCESSIONNAIRES;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.TYPE_INTERMEDIAIRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statuts de lot éditables — FEATURE-restes-projet-achat.md §6 « Endpoints », « Champs d'un statut », règles d'unicité et
 * « Vérification » (création complète, relecture, modification, catalogue). Les champs ajoutés à {@code StatutLotDto} n'ont pas
 * de nom fixé par le spec : la relecture GET ne porte que sur les champs déjà exposés, le reste est relu en base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsLotEcritureTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsLot fixture;
    @Autowired private FixtureStatutsAchat comptes;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID image;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1E");
        fixture.genres();
        fixture.libellesEntrepreneurs();
        admin = comptes.administrateur(jeton);
        image = comptes.image("IMG-" + jeton, FixtureStatutsAchat.PNG);
    }

    private Map<String, Object> base(String code) {
        return corps(ENTREPRENEURS, code, TYPE_INTERMEDIAIRE);
    }

    private Map<String, Object> complet(String code) {
        Map<String, Object> c = base(code);
        c.put(ORDRE, 12);
        c.put(DUREE, 5);
        c.put(COULEUR, "Pink");
        c.put(IMAGE_ID, image.toString());
        c.put(NOTES, "Note " + code);
        c.put(CASES, List.of(1, 3, 10));
        c.put(CHAMPS, List.of(CHAMP_CAUSE_RESILIATION.toString(), CHAMP_SIGNATURE_MARCHE.toString()));
        return c;
    }

    private UUID creer(Map<String, Object> corps) throws Exception {
        return id(mockMvc.perform(creation(json, STATUTS, corps).with(admin)).andExpect(status().isCreated()).andReturn());
    }

    private static String minuscule(UUID id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ création, relecture

    /** Vérification « création complète » : chaque champ du corps est écrit dans StrStatus et ses champs obligatoires. */
    @Test
    void creationComplete_201_ecritEnBase() throws Exception {
        String code = "F1C-" + jeton;

        UUID id = creer(complet(code));

        Map<String, Object> s = fixture.ligne(id);
        assertThat(s).isNotNull()
                .containsEntry("Code", code)
                .containsEntry("Description", "Statut " + code)
                .containsEntry("Sorting", 12)
                .containsEntry("Delay", 5)
                .containsEntry("ColorName", "Pink")
                .containsEntry("Notes", "Note " + code)
                .containsEntry("CfgImageId", minuscule(image))
                .containsEntry("StrStatusTypeId", minuscule(TYPE_INTERMEDIAIRE))
                .containsEntry("StrConfigEntityId", minuscule(ENTREPRENEURS))
                .containsEntry("B1", 1).containsEntry("B3", 1).containsEntry("B10", 1)
                .containsEntry("B2", 0).containsEntry("B4", 0);
        assertThat(fixture.champsObligatoires(id))
                .containsExactlyInAnyOrder(minuscule(CHAMP_CAUSE_RESILIATION), minuscule(CHAMP_SIGNATURE_MARCHE));
    }

    /** Vérification « relecture » : le statut créé apparaît dans GET /statuts-lot avec les champs déjà exposés. */
    @Test
    void creation_relueParGetStatutsLot() throws Exception {
        String code = "F1L-" + jeton;

        UUID id = creer(complet(code));

        assertThat(ligne(mockMvc, admin, id)).isNotNull()
                .containsEntry("code", code)
                .containsEntry("description", "Statut " + code)
                .containsEntry("ordre", 12)
                .containsEntry("couleur", "Pink")
                .containsEntry("genre", "Lot Entrepreneurs");
    }

    /** POST répond le {@code StatutLotDto} créé. */
    @Test
    void creation_reponseEstLeStatutCree() throws Exception {
        String code = "F1R-" + jeton;

        MvcResult r = mockMvc.perform(creation(json, STATUTS, base(code)).with(admin)).andExpect(status().isCreated()).andReturn();

        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":\"" + code + "\"");
    }

    /** Durée de vie 0 (= infinie) acceptée ; corps minimal : aucune case, aucun champ obligatoire. */
    @Test
    void creationMinimale_dureeZero_sansCaseNiChamp() throws Exception {
        Map<String, Object> c = base("F1M-" + jeton);
        c.put(DUREE, 0);

        UUID id = creer(c);

        assertAll(
                () -> assertThat(fixture.ligne(id)).isNotNull().containsEntry("Delay", 0).containsEntry("B1", 0),
                () -> assertThat(fixture.champsObligatoires(id)).isEmpty());
    }

    /** Couleur : un nom .NET connu ou le vide (§6 règles). */
    @ParameterizedTest
    @ValueSource(strings = {"LimeGreen", "Red", ""})
    void couleurNommeeOuVide_201(String couleur) throws Exception {
        Map<String, Object> c = base("F1K-" + jeton);
        c.put(COULEUR, couleur);

        mockMvc.perform(creation(json, STATUTS, c).with(admin)).andExpect(status().isCreated());
    }

    /** Q5 : l'éditeur couvre les 5 genres — un statut se crée aussi dans « Lot Concessionnaires ». */
    @Test
    void q5_creationDansLeGenreConcessionnaires_201() throws Exception {
        String code = "F1D-" + jeton;

        UUID id = creer(corps(CONCESSIONNAIRES, code, TYPE_INTERMEDIAIRE));

        assertThat(fixture.ligne(id)).isNotNull().containsEntry("StrConfigEntityId", minuscule(CONCESSIONNAIRES));
    }

    // ------------------------------------------------------------------ modification

    /** PUT = remplacement entier : champs non renvoyés vidés, cases et champs obligatoires resynchronisés. */
    @Test
    void modification_remplaceLaFiche_casesEtChampsResynchronises() throws Exception {
        String code = "F1P-" + jeton;
        UUID id = creer(complet(code));
        Map<String, Object> c = base(code);
        c.put(DESCRIPTION, "Modifié " + code);
        c.put(COULEUR, "Red");
        c.put(CASES, List.of(2));
        c.put(CHAMPS, List.of(CHAMP_LANCEMENT_TRAVAUX.toString()));

        mockMvc.perform(modification(json, STATUT, id, c).with(admin)).andExpect(status().isOk());

        Map<String, Object> s = fixture.ligne(id);
        assertAll(
                () -> assertThat(s).isNotNull()
                        .containsEntry("Description", "Modifié " + code)
                        .containsEntry("ColorName", "Red")
                        .containsEntry("Sorting", 3)
                        .containsEntry("CfgImageId", null)
                        .containsEntry("B1", 0).containsEntry("B2", 1).containsEntry("B10", 0),
                () -> assertThat(fixture.champsObligatoires(id)).containsExactly(minuscule(CHAMP_LANCEMENT_TRAVAUX)));
    }

    /** PUT répond 200 et le statut modifié. */
    @Test
    void modification_200_reponseEstLeStatut() throws Exception {
        String code = "F1Q-" + jeton;
        UUID id = creer(base(code));
        Map<String, Object> c = base(code);
        c.put(DESCRIPTION, "Neuf " + code);

        MvcResult r = mockMvc.perform(modification(json, STATUT, id, c).with(admin)).andExpect(status().isOk()).andReturn();

        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"description\":\"Neuf " + code + "\"");
    }

    /** 404 sur id inconnu ; témoin : le même corps sur un statut existant répond 200. */
    @Test
    void modification_idInconnu_404_temoin200() throws Exception {
        String code = "F1N-" + jeton;
        UUID id = creer(base(code));

        mockMvc.perform(modification(json, STATUT, id, base(code)).with(admin)).andExpect(status().isOk());
        mockMvc.perform(modification(json, STATUT, UUID.randomUUID(), base(code)).with(admin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ unicité du code dans le genre, sans casse (409)

    @Test
    void unicite_creation_codeDuGenre_autreCasse_409_rienEcrit() throws Exception {
        String code = "F1U-" + jeton;
        fixture.statut(ENTREPRENEURS, code, 1);
        Map<String, Object> c = base(code.toLowerCase(Locale.ROOT));
        c.put(DESCRIPTION, "Doublon " + code);

        mockMvc.perform(creation(json, STATUTS, c).with(admin)).andExpect(status().isConflict());

        assertThat(fixture.statutsDecrits("Doublon " + code)).isZero();
    }

    /** L'unicité est « dans le genre » : le même code dans un autre genre est accepté. */
    @Test
    void unicite_memeCode_autreGenre_201() throws Exception {
        String code = "F1G-" + jeton;
        fixture.statut(CONCESSIONNAIRES, code, 1);

        mockMvc.perform(creation(json, STATUTS, base(code)).with(admin)).andExpect(status().isCreated());

        assertThat(fixture.statutsDuCode(ENTREPRENEURS, code)).isEqualTo(1);
    }

    /** La fiche modifiée ne se compare pas à elle-même (même code, autre casse). */
    @Test
    void unicite_modification_sonPropreCode_200() throws Exception {
        String code = "F1S-" + jeton;
        UUID id = creer(base(code));

        mockMvc.perform(modification(json, STATUT, id, base(code.toLowerCase(Locale.ROOT))).with(admin))
                .andExpect(status().isOk());
    }

    @Test
    void unicite_modification_codeDUnAutreDuGenre_409_inchange() throws Exception {
        String codeA = "F1A-" + jeton;
        fixture.statut(ENTREPRENEURS, codeA, 1);
        UUID b = creer(base("F1B-" + jeton));

        mockMvc.perform(modification(json, STATUT, b, base(codeA.toLowerCase(Locale.ROOT))).with(admin))
                .andExpect(status().isConflict());

        assertThat(fixture.ligne(b)).isNotNull().containsEntry("Code", "F1B-" + jeton);
    }

    // ------------------------------------------------------------------ catalogue du genre

    /** GET /statuts-lot/catalogue?genreId= : 200 pour un genre de lot ; 404 pour un genre inconnu. */
    @Test
    void catalogue_genreConnu_200_genreInconnu_404() throws Exception {
        mockMvc.perform(get(CATALOGUE).param("genreId", ENTREPRENEURS.toString()).with(admin)).andExpect(status().isOk());
        mockMvc.perform(get(CATALOGUE).param("genreId", UUID.randomUUID().toString()).with(admin))
                .andExpect(status().isNotFound());
    }
}

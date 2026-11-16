package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CASES;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CHAMPS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.CODE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.COULEUR;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DUREE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.GENRE_ID;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.ORDRE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUT;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUTS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TYPE_ID;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.corps;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.creation;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.detail;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.modification;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CHAMP_CAUSE_RESILIATION;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.TYPE_INTERMEDIAIRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Refus 400 de POST / PUT /api/projet/config/statuts-lot — FEATURE-restes-projet-achat.md §6 « Règles » et « Vérification » :
 * chaque obligatoire (le {@code detail} nomme le champ), type / couleur / case / champ inconnus, durée &lt; 0 ; ordre 400 → 409.
 * Chaque refus n'écrit rien ; témoin : le corps de base répond 201.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsLotValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureStatutsLot fixture;
    @Autowired private FixtureStatutsAchat comptes;

    private String jeton;
    private String code;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1V");
        code = "F1V-" + jeton;
        fixture.genres();
        fixture.libellesEntrepreneurs();
        admin = comptes.administrateur(jeton);
    }

    private Map<String, Object> base() {
        return corps(ENTREPRENEURS, code, TYPE_INTERMEDIAIRE);
    }

    private void refuse400SansEcriture(Map<String, Object> corps) throws Exception {
        mockMvc.perform(creation(json, STATUTS, corps).with(admin)).andExpect(status().isBadRequest());

        assertThat(fixture.statutsDecrits("Statut " + code)).isZero();
    }

    @Test
    void temoin_corpsDeBase_201() throws Exception {
        mockMvc.perform(creation(json, STATUTS, base()).with(admin)).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ obligatoires (message nommant le champ)

    static Stream<Arguments> obligatoires() {
        return Stream.of(
                arguments("ordre absent", ORDRE, (Consumer<Map<String, Object>>) c -> c.remove(ORDRE)),
                arguments("code absent", CODE, (Consumer<Map<String, Object>>) c -> c.remove(CODE)),
                arguments("code blanc", CODE, (Consumer<Map<String, Object>>) c -> c.put(CODE, "   ")),
                arguments("description absente", DESCRIPTION, (Consumer<Map<String, Object>>) c -> c.remove(DESCRIPTION)),
                arguments("description blanche", DESCRIPTION, (Consumer<Map<String, Object>>) c -> c.put(DESCRIPTION, "  ")),
                arguments("type absent", "type", (Consumer<Map<String, Object>>) c -> c.remove(TYPE_ID)));
    }

    /** O.D.T, Code, Description, Type obligatoires → 400 dont le {@code detail} nomme le champ, rien d'écrit. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("obligatoires")
    void obligatoireManquant_400_messageNommeLeChamp(String cas, String champ, Consumer<Map<String, Object>> mutation)
            throws Exception {
        Map<String, Object> c = base();
        String description = (String) c.get(DESCRIPTION);
        mutation.accept(c);

        MvcResult r = mockMvc.perform(creation(json, STATUTS, c).with(admin)).andExpect(status().isBadRequest()).andReturn();

        assertAll(
                () -> assertThat(detail(r)).as(cas).containsIgnoringCase(champ),
                () -> assertThat(fixture.statutsDecrits(description)).isZero());
    }

    /** Le genre ({@code genreId}) est requis à la création : un statut sans genre n'appartient à aucun écran. */
    @Test
    void genreAbsent_400() throws Exception {
        Map<String, Object> c = base();
        c.remove(GENRE_ID);

        refuse400SansEcriture(c);
    }

    // ------------------------------------------------------------------ valeurs refusées

    @Test
    void typeInconnu_400() throws Exception {
        Map<String, Object> c = base();
        c.put(TYPE_ID, UUID.randomUUID().toString());

        refuse400SansEcriture(c);
    }

    @Test
    void couleurInconnue_400() throws Exception {
        Map<String, Object> c = base();
        c.put(COULEUR, "PasUneCouleur");

        refuse400SansEcriture(c);
    }

    @Test
    void dureeNegative_400() throws Exception {
        Map<String, Object> c = base();
        c.put(DUREE, -1);

        refuse400SansEcriture(c);
    }

    /** Case hors catalogue du genre : 006 n'a pas de libellé chez Entrepreneurs ; 21 est hors 1-20. */
    @ParameterizedTest
    @ValueSource(ints = {6, 21})
    void caseHorsCatalogue_400(int numero) throws Exception {
        Map<String, Object> c = base();
        c.put(CASES, List.of(1, numero));

        refuse400SansEcriture(c);
    }

    @Test
    void champInconnu_400() throws Exception {
        Map<String, Object> c = base();
        c.put(CHAMPS, List.of(CHAMP_CAUSE_RESILIATION.toString(), UUID.randomUUID().toString()));

        refuse400SansEcriture(c);
    }

    /** Un champ d'un autre genre (Fournisseurs) n'est pas au catalogue du genre Entrepreneurs. */
    @Test
    void champDUnAutreGenre_400() throws Exception {
        UUID autre = fixture.champDuGenre("F1X-" + jeton, FOURNISSEURS);
        Map<String, Object> c = base();
        c.put(CHAMPS, List.of(autre.toString()));

        refuse400SansEcriture(c);
    }

    /** PUT refusé (durée &lt; 0) : la fiche reste inchangée. */
    @Test
    void modification_dureeNegative_400_inchange() throws Exception {
        UUID id = fixture.statut(ENTREPRENEURS, code, 1);
        Map<String, Object> c = base();
        c.put(DESCRIPTION, "Changé " + code);
        c.put(DUREE, -5);

        mockMvc.perform(modification(json, STATUT, id, c).with(admin)).andExpect(status().isBadRequest());

        assertThat(fixture.ligne(id)).isNotNull().containsEntry("Description", code).containsEntry("Delay", 0);
    }

    /** Contrat 16/11 : {@code genreId} est fixe ; un PUT qui l'envoie différent → 400, fiche inchangée ; témoin : même genre → 200. */
    @Test
    void modification_autreGenre_400_inchange_temoin200() throws Exception {
        UUID id = fixture.statut(ENTREPRENEURS, code, 1);
        Map<String, Object> autreGenre = corps(FOURNISSEURS, code, TYPE_INTERMEDIAIRE);
        autreGenre.put(DESCRIPTION, "Déplacé " + code);

        mockMvc.perform(modification(json, STATUT, id, autreGenre).with(admin)).andExpect(status().isBadRequest());
        Map<String, Object> apresRefus = fixture.ligne(id);
        mockMvc.perform(modification(json, STATUT, id, base()).with(admin)).andExpect(status().isOk());

        assertThat(apresRefus).isNotNull().containsEntry("Description", code)
                .containsEntry("StrConfigEntityId", ENTREPRENEURS.toString().toLowerCase());
    }

    /** Ordre 400 → 409 : un corps invalide dont le code est déjà pris répond 400. */
    @Test
    void ordre_corpsInvalideEtCodePris_400() throws Exception {
        fixture.statut(ENTREPRENEURS, code, 1);
        Map<String, Object> c = base();
        c.put(DUREE, -1);

        mockMvc.perform(creation(json, STATUTS, c).with(admin)).andExpect(status().isBadRequest());
    }

    /** {@code {id}} mal formé → 400. */
    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void idMalForme_400(String methode) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(methode), STATUT, "pas-un-uuid").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(base())))
                .andExpect(status().isBadRequest());
    }
}

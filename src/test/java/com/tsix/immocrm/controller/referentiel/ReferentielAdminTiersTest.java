package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers.ListeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RACINE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.CHOIX;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.FORMES_SOCIALES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_FORMES_SOCIALES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_NATURES_TIERS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_PAYS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_SALAIRES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_TITRES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_VILLES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.NATURES_TIERS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.PAYS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.SALAIRES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.SEXES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.TIERS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.TITRES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.TYPES_TIERS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.TYPE_TIERS_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.VILLES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.ASSUJETTISSEMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pays, villes et référentiels des tiers sur {@code /api/referentiels-admin} — FEATURE-admin-referentiels-tiers.md, règles
 * 1 à 4 et réponses R1, R3, R4, R5, R7. Les noms des champs que le spec ne fixe pas (pays d'une ville, sexe d'un titre,
 * cases à cocher) sont lus dans le descripteur : seul leur type et leur rôle sont contractuels.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminTiersTest {

    private static final String ID = "$.id";
    private static final String DETAIL = "$.detail";
    private static final String REFERENCE = "REFERENCE";
    private static final String BOOLEEN = "BOOLEEN";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsTiers fixture;

    private AdmUser admin;
    private String jeton;
    private String descripteurs;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    static Stream<String> slugs() {
        return FixtureReferentielsTiers.slugs();
    }

    static Stream<ListeTiers> modifiables() {
        return FixtureReferentielsTiers.modifiables();
    }

    /** Listes à clé « code » seule : toutes sauf Villes (code, pays) et Natures (code, type). */
    static Stream<ListeTiers> cleCodeSeul() {
        return modifiables().filter(l -> !l.slug().equals(VILLES) && !l.slug().equals(NATURES_TIERS));
    }

    static Stream<Arguments> referencesAChoix() {
        return Stream.of(Arguments.of(VILLES, PAYS), Arguments.of(NATURES_TIERS, TYPES_TIERS), Arguments.of(TITRES, SEXES));
    }

    // ------------------------------------------------------------------ outils

    private static String utf8(ResultActions reponse) throws UnsupportedEncodingException {
        return reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String descripteurs() throws Exception {
        if (descripteurs == null) {
            descripteurs = utf8(mockMvc.perform(get(RACINE).with(connecte(admin))).andExpect(status().isOk()));
        }
        return descripteurs;
    }

    private <T> List<T> lire(String slug, String suite) throws Exception {
        return JsonPath.read(descripteurs(), "$[?(@.slug=='" + slug + "')]" + suite);
    }

    private <T> List<T> champ(String slug, String nom, String propriete) throws Exception {
        return lire(slug, ".champs[?(@.nom=='" + nom + "')]." + propriete);
    }

    /** Nom sur le fil du seul champ de ce type ({@code REFERENCE}, {@code BOOLEEN}…) de la liste. */
    private String champDeType(String slug, String type) throws Exception {
        List<String> noms = lire(slug, ".champs[?(@.type=='" + type + "')].nom");
        assertThat(noms).as("champ %s du descripteur %s", type, slug).hasSize(1);
        return noms.getFirst();
    }

    private ResultActions poster(String slug, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, slug).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(String slug, UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(LIGNE, slug, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creer(String slug, CorpsJson corps) throws Exception {
        String json = utf8(poster(slug, corps).andExpect(status().isCreated()));
        return UUID.fromString(JsonPath.read(json, ID));
    }

    private List<String> codesListes(String slug) throws Exception {
        String json = utf8(mockMvc.perform(get(TABLE, slug).with(connecte(admin))).andExpect(status().isOk()));
        List<String> codes = JsonPath.read(json, "$[*].code");
        return codes.stream().filter(c -> c != null && c.endsWith(jeton)).toList();
    }

    private CorpsJson ville(String code, UUID pays) throws Exception {
        return L_VILLES.saisie(code).avec(champDeType(VILLES, REFERENCE), pays);
    }

    private CorpsJson nature(String code, UUID type) {
        return L_NATURES_TIERS.saisie(code).avec(TYPE_TIERS_ID, type);
    }

    // ------------------------------------------------------------------ règle 1 : descripteurs

    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r1_chaqueSlug_listeDansLaFamilleTiers(String slug) throws Exception {
        List<String> familles = lire(slug, ".famille");

        assertThat(familles).containsExactly(TIERS);
    }

    /** Décision 2 : Types de tiers seul en lecture seule. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r6_lectureSeule_vraieSeulementPourLesTypesDeTiers(String slug) throws Exception {
        List<Boolean> lectureSeule = lire(slug, ".lectureSeule");

        assertThat(lectureSeule).containsExactly(TYPES_TIERS.equals(slug));
    }

    /** Décision 2 et R1 : le code est requis et non modifiable après création. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r1_code_requisEtNonModifiable(String slug) throws Exception {
        List<Boolean> obligatoire = champ(slug, CODE, "obligatoire");
        List<Boolean> modifiable = champ(slug, CODE, "modifiable");

        assertAll(
                () -> assertThat(obligatoire).containsExactly(true),
                () -> assertThat(modifiable).containsExactly(false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modifiables")
    void r3_description_requiseEtModifiable(ListeTiers l) throws Exception {
        List<Boolean> obligatoire = champ(l.slug(), DESCRIPTION, "obligatoire");
        List<Boolean> modifiable = champ(l.slug(), DESCRIPTION, "modifiable");

        assertAll(
                () -> assertThat(obligatoire).containsExactly(true),
                () -> assertThat(modifiable).containsExactly(true));
    }

    /** R3 : aucune des 16 tables n'a de colonne Notes. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r3_aucunChampNotes(String slug) throws Exception {
        List<String> noms = lire(slug, ".champs[*].nom");

        assertThat(noms).isNotEmpty().contains(CODE, DESCRIPTION).doesNotContain("notes");
    }

    /** R7 : après création, seuls le libellé, l'ordre et le pays d'une ville se modifient. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("modifiables")
    void r7_seulsLibelleTriEtPaysDeVilleModifiables(ListeTiers l) throws Exception {
        List<String> permis = new ArrayList<>(List.of(DESCRIPTION, SORTING));
        if (l.slug().equals(VILLES)) {
            permis.add(champDeType(VILLES, REFERENCE));
        }

        List<String> modifiables = lire(l.slug(), ".champs[?(@.modifiable==true)].nom");

        assertThat(modifiables).isNotEmpty().contains(DESCRIPTION).isSubsetOf(permis);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cleCodeSeul")
    void r3_cleUnique_codeSeul(ListeTiers l) throws Exception {
        List<String> uniques = lire(l.slug(), ".champs[?(@.unique==true)].nom");

        assertThat(uniques).containsExactly(CODE);
    }

    @Test
    void r4_villes_cleUnique_codeEtPays_paysObligatoireEtModifiable() throws Exception {
        String pays = champDeType(VILLES, REFERENCE);

        List<String> uniques = lire(VILLES, ".champs[?(@.unique==true)].nom");
        List<Boolean> obligatoire = champ(VILLES, pays, "obligatoire");
        List<Boolean> modifiable = champ(VILLES, pays, "modifiable");

        assertAll(
                () -> assertThat(uniques).containsExactlyInAnyOrder(CODE, pays),
                () -> assertThat(obligatoire).containsExactly(true),
                () -> assertThat(modifiable).containsExactly(true));
    }

    @Test
    void r4_natures_cleUnique_codeEtType_typeObligatoireNonModifiable() throws Exception {
        List<String> uniques = lire(NATURES_TIERS, ".champs[?(@.unique==true)].nom");
        List<String> types = champ(NATURES_TIERS, TYPE_TIERS_ID, "type");
        List<Boolean> obligatoire = champ(NATURES_TIERS, TYPE_TIERS_ID, "obligatoire");
        List<Boolean> modifiable = champ(NATURES_TIERS, TYPE_TIERS_ID, "modifiable");

        assertAll(
                () -> assertThat(uniques).containsExactlyInAnyOrder(CODE, TYPE_TIERS_ID),
                () -> assertThat(types).containsExactly(REFERENCE),
                () -> assertThat(obligatoire).containsExactly(true),
                () -> assertThat(modifiable).containsExactly(false));
    }

    /** OMRAN : « Sexe (lien non obligatoire) » ; R7 : non modifiable. */
    @Test
    void r7_titres_sexeOptionnelEtNonModifiable() throws Exception {
        String sexe = champDeType(TITRES, REFERENCE);

        List<Boolean> obligatoire = champ(TITRES, sexe, "obligatoire");
        List<Boolean> modifiable = champ(TITRES, sexe, "modifiable");

        assertAll(
                () -> assertThat(obligatoire).containsExactly(false),
                () -> assertThat(modifiable).containsExactly(false));
    }

    /** R7 : IsPerson (formes sociales), IsExonarated et IsMajorated (assujettissements) mappés, non modifiables. */
    @Test
    void r7_casesACocher_mappeesEtNonModifiables() throws Exception {
        List<Boolean> formes = lire(FORMES_SOCIALES, ".champs[?(@.type=='" + BOOLEEN + "')].modifiable");
        List<Boolean> assujettissements = lire(ASSUJETTISSEMENTS, ".champs[?(@.type=='" + BOOLEEN + "')].modifiable");

        assertAll(
                () -> assertThat(formes).containsExactly(false),
                () -> assertThat(assujettissements).containsExactly(false, false));
    }

    /** Spec §« Suppression… » point 5 : pays, types de tiers et sexes servis par /choix/{slug}. */
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("referencesAChoix")
    void r1_reference_choixParListeDuLot(String slug, String liste) throws Exception {
        List<String> choix = lire(slug, ".champs[?(@.type=='" + REFERENCE + "')].choix");

        assertThat(choix).containsExactly("/api/referentiels-admin/choix/" + liste);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("referencesAChoix")
    void r1_choix_200_contientLaLigneCreee(String slug, String liste) throws Exception {
        UUID id = switch (liste) {
            case PAYS -> fixture.pays("CH-" + jeton);
            case TYPES_TIERS -> fixture.typeTiers("CH-" + jeton);
            default -> fixture.sexe("CH-" + jeton);
        };

        mockMvc.perform(get(CHOIX, liste).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].code").value("CH-" + jeton));
    }

    // ------------------------------------------------------------------ règle 2 : liste et tri

    @Test
    void r2_salaires_triesParOrdrePuisCode() throws Exception {
        fixture.ligne(L_SALAIRES, "A-" + jeton, 30);
        fixture.ligne(L_SALAIRES, "C-" + jeton, 10);
        fixture.ligne(L_SALAIRES, "B-" + jeton, 10);

        assertThat(codesListes(SALAIRES)).containsExactly("B-" + jeton, "C-" + jeton, "A-" + jeton);
    }

    @Test
    void r2_pays_sansOrdre_triesParCode() throws Exception {
        fixture.pays("B-" + jeton);
        fixture.pays("A-" + jeton);

        assertThat(codesListes(PAYS)).containsExactly("A-" + jeton, "B-" + jeton);
    }

    /** R5 : les villes sans pays restent lisibles ; la liste rend toute la table. */
    @Test
    void r2_villes_toutesLesLignes_dontSansPays() throws Exception {
        UUID sansPays = fixture.ville("SP-" + jeton, null);

        String json = utf8(mockMvc.perform(get(TABLE, VILLES).with(connecte(admin))).andExpect(status().isOk()));

        List<String> ids = JsonPath.read(json, "$[*].id");
        List<Object> pays = JsonPath.read(json, "$[?(@.id=='" + sansPays + "')]." + champDeType(VILLES, REFERENCE));
        assertAll(
                () -> assertThat(ids).hasSize(fixture.compter("SELECT COUNT(*) FROM AdmCity")),
                () -> assertThat(pays).hasSize(1).containsOnlyNulls());
    }

    // ------------------------------------------------------------------ règle 3 : création

    @Test
    void r3_creation_201_ligneEcriteEtRelue() throws Exception {
        String code = "CRE-" + jeton;

        UUID id = creer(SALAIRES, L_SALAIRES.saisie(code));

        mockMvc.perform(get(LIGNE, SALAIRES, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.description").value("Libelle " + code));
        assertThat(fixture.compter("SELECT COUNT(*) FROM AdmSalary WHERE PKey = :id AND Code = N'" + code
                + "' AND Description = N'Libelle " + code + "' AND Sorting = " + FixtureReferentielsTiers.TRI, id))
                .isEqualTo(1);
    }

    @Test
    void r3_creation_400_codeAbsent() throws Exception {
        poster(SALAIRES, L_SALAIRES.saisie("SC-" + jeton).sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_creation_400_libelleAbsent() throws Exception {
        poster(SALAIRES, L_SALAIRES.saisie("SD-" + jeton).sans(DESCRIPTION)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_creation_409_codeDejaPris_casseEtEspacesIgnores() throws Exception {
        creer(SALAIRES, L_SALAIRES.saisie("Casse-" + jeton));

        poster(SALAIRES, L_SALAIRES.saisie("  cASSE-" + jeton.toLowerCase(Locale.ROOT) + "  "))
                .andExpect(status().isConflict());
    }

    @Test
    void r3_creation_codeEnregistreSansEspaces() throws Exception {
        UUID id = creer(PAYS, L_PAYS.saisie("  TRIM-" + jeton + "  "));

        assertThat(fixture.texte(L_PAYS, id, "Code")).isEqualTo("TRIM-" + jeton);
    }

    @Test
    void r4_villes_creation_201_memeCodeDansUnAutrePays() throws Exception {
        UUID premier = fixture.pays("P1-" + jeton);
        UUID second = fixture.pays("P2-" + jeton);
        creer(VILLES, ville("V-" + jeton, premier));

        UUID id = creer(VILLES, ville("V-" + jeton, second));

        assertThat(fixture.guid(L_VILLES, id, "AdmCountryId")).isEqualTo(second);
    }

    @Test
    void r4_villes_creation_409_memeCodeMemePays_casseIgnoree() throws Exception {
        UUID pays = fixture.pays("P-" + jeton);
        creer(VILLES, ville("Ville-" + jeton, pays));

        poster(VILLES, ville("vILLE-" + jeton.toLowerCase(Locale.ROOT), pays)).andExpect(status().isConflict());
    }

    @Test
    void r5_villes_creation_400_sansPays() throws Exception {
        poster(VILLES, L_VILLES.saisie("SANS-" + jeton)).andExpect(status().isBadRequest());
    }

    @Test
    void r4_natures_creation_201_memeCodeSousUnAutreType() throws Exception {
        UUID premier = fixture.typeTiers("T1-" + jeton);
        UUID second = fixture.typeTiers("T2-" + jeton);
        creer(NATURES_TIERS, nature("N-" + jeton, premier));

        UUID id = creer(NATURES_TIERS, nature("N-" + jeton, second));

        assertThat(fixture.guid(L_NATURES_TIERS, id, "CfgTierTypeId")).isEqualTo(second);
    }

    @Test
    void r4_natures_creation_409_memeCodeMemeType() throws Exception {
        UUID type = fixture.typeTiers("T-" + jeton);
        creer(NATURES_TIERS, nature("Nat-" + jeton, type));

        poster(NATURES_TIERS, nature("nAT-" + jeton.toLowerCase(Locale.ROOT), type)).andExpect(status().isConflict());
    }

    @Test
    void decision2_natures_creation_400_sansType() throws Exception {
        poster(NATURES_TIERS, L_NATURES_TIERS.saisie("SANS-" + jeton)).andExpect(status().isBadRequest());
    }

    /** R7 : les champs en plus d'OMRAN se saisissent à la création. */
    @Test
    void r7_formesSociales_caseACocherSaisieALaCreation() throws Exception {
        UUID id = creer(FORMES_SOCIALES, L_FORMES_SOCIALES.saisie("FS-" + jeton)
                .avec(champDeType(FORMES_SOCIALES, BOOLEEN), true));

        assertThat(fixture.compter("SELECT COUNT(*) FROM AdmSocialForm WHERE PKey = :id AND IsPerson = 1", id))
                .isEqualTo(1);
    }

    @Test
    void r7_titres_sexeSaisiALaCreation() throws Exception {
        UUID sexe = fixture.sexe("S-" + jeton);

        UUID id = creer(TITRES, L_TITRES.saisie("TI-" + jeton).avec(champDeType(TITRES, REFERENCE), sexe));

        assertThat(fixture.guid(L_TITRES, id, "AdmSexTypeId")).isEqualTo(sexe);
    }

    // ------------------------------------------------------------------ règle 4 : modification

    @Test
    void r1_modification_400_codeDifferent_nommeLeChamp_ligneInchangee() throws Exception {
        String code = "MC-" + jeton;
        UUID id = fixture.ligne(L_SALAIRES, code);

        modifier(SALAIRES, id, corps().avec(CODE, "AUTRE-" + jeton).avec(DESCRIPTION, "Nouveau " + jeton))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(CODE)));

        assertAll(
                () -> assertThat(fixture.texte(L_SALAIRES, id, "Code")).isEqualTo(code),
                () -> assertThat(fixture.texte(L_SALAIRES, id, "Description")).isEqualTo("Libelle " + code));
    }

    @Test
    void r1_modification_200_codeIdentiqueAccepte() throws Exception {
        String code = "MI-" + jeton;
        UUID id = fixture.ligne(L_SALAIRES, code);

        modifier(SALAIRES, id, corps().avec(CODE, code).avec(DESCRIPTION, "Nouveau " + jeton))
                .andExpect(status().isOk());

        assertThat(fixture.texte(L_SALAIRES, id, "Description")).isEqualTo("Nouveau " + jeton);
    }

    @Test
    void r4_modification_200_libelleEtOrdreEcrits_codeConserve() throws Exception {
        String code = "ML-" + jeton;
        UUID id = fixture.ligne(L_SALAIRES, code);

        modifier(SALAIRES, id, corps().avec(DESCRIPTION, "Nouveau " + jeton).avec(SORTING, 99)).andExpect(status().isOk());

        assertThat(fixture.compter("SELECT COUNT(*) FROM AdmSalary WHERE PKey = :id AND Code = N'" + code
                + "' AND Description = N'Nouveau " + jeton + "' AND Sorting = 99", id)).isEqualTo(1);
    }

    @Test
    void r4_natures_modification_400_typeDifferent_ligneInchangee() throws Exception {
        UUID type = fixture.typeTiers("T1-" + jeton);
        UUID id = fixture.nature("NT-" + jeton, type);

        modifier(NATURES_TIERS, id, corps().avec(TYPE_TIERS_ID, fixture.typeTiers("T2-" + jeton)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(TYPE_TIERS_ID)));

        assertThat(fixture.guid(L_NATURES_TIERS, id, "CfgTierTypeId")).isEqualTo(type);
    }

    @Test
    void r4_villes_modification_200_paysModifiable() throws Exception {
        UUID ancien = fixture.pays("P1-" + jeton);
        UUID nouveau = fixture.pays("P2-" + jeton);
        UUID id = fixture.ville("VM-" + jeton, ancien);

        modifier(VILLES, id, corps().avec(champDeType(VILLES, REFERENCE), nouveau)).andExpect(status().isOk());

        assertThat(fixture.guid(L_VILLES, id, "AdmCountryId")).isEqualTo(nouveau);
    }

    /** R4 : l'unicité (code, pays) est revérifiée quand le pays change. */
    @Test
    void r4_villes_modification_409_nouveauPaysDejaPorteurDuCode_ligneInchangee() throws Exception {
        UUID premier = fixture.pays("P1-" + jeton);
        UUID second = fixture.pays("P2-" + jeton);
        fixture.ville("VD-" + jeton, premier);
        UUID id = fixture.ville("VD-" + jeton, second);

        modifier(VILLES, id, corps().avec(champDeType(VILLES, REFERENCE), premier)).andExpect(status().isConflict());

        assertThat(fixture.guid(L_VILLES, id, "AdmCountryId")).isEqualTo(second);
    }

    /** R5 : enregistrer une ville sans pays exige le pays. */
    @Test
    void r5_villeSansPays_modification_400_sansLePays() throws Exception {
        UUID id = fixture.ville("VSP-" + jeton, null);

        modifier(VILLES, id, corps().avec(DESCRIPTION, "Nouveau " + jeton)).andExpect(status().isBadRequest());

        assertThat(fixture.texte(L_VILLES, id, "Description")).isEqualTo("Libelle VSP-" + jeton);
    }

    @Test
    void r5_villeSansPays_modification_200_avecLePays() throws Exception {
        UUID pays = fixture.pays("P-" + jeton);
        UUID id = fixture.ville("VAP-" + jeton, null);

        modifier(VILLES, id, corps().avec(DESCRIPTION, "Nouveau " + jeton).avec(champDeType(VILLES, REFERENCE), pays))
                .andExpect(status().isOk());

        assertThat(fixture.guid(L_VILLES, id, "AdmCountryId")).isEqualTo(pays);
    }

    @Test
    void r7_formesSociales_modification_400_caseACocherDifferente_ligneInchangee() throws Exception {
        UUID id = fixture.ligne(L_FORMES_SOCIALES, "FM-" + jeton);
        fixture.definir(L_FORMES_SOCIALES, id, "IsPerson", false);

        modifier(FORMES_SOCIALES, id, corps().avec(champDeType(FORMES_SOCIALES, BOOLEEN), true))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM AdmSocialForm WHERE PKey = :id AND IsPerson = 0", id))
                .isEqualTo(1);
    }

    @Test
    void r7_titres_modification_400_sexeDifferent_ligneInchangee() throws Exception {
        UUID sexe = fixture.sexe("S1-" + jeton);
        UUID id = fixture.titre("TM-" + jeton, sexe);

        modifier(TITRES, id, corps().avec(champDeType(TITRES, REFERENCE), fixture.sexe("S2-" + jeton)))
                .andExpect(status().isBadRequest());

        assertThat(fixture.guid(L_TITRES, id, "AdmSexTypeId")).isEqualTo(sexe);
    }
}

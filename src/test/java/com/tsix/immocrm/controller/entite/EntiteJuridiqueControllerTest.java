package com.tsix.immocrm.controller.entite;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixtureEntites.TreizeColonnes;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écran « ENTITES JURIDIQUE » — FEATURE-entites-juridiques.md §2 amendé par §7.2-7.5 (EntiteJuridiqueControllerTest).
 *
 * <p>Le spec ne nomme sur le fil que {@code code}, {@code description}, {@code deviseId}, {@code isActive},
 * {@code champsAVider}, {@code actif}, {@code search} : les 13 colonnes du lot sont repérées dans la fiche par leur
 * valeur (posée en SQL), puis écrites en PATCH sous le même nom.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntiteJuridiqueControllerTest {

    private static final String ENTITES = "/api/entites-juridiques";
    private static final String ENTITE = "/api/entites-juridiques/{id}";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String DEVISE_ID = "deviseId";
    private static final String ACTIF = "actif";
    private static final String SEARCH = "search";
    private static final String TOUS = "tous";
    private static final String COMPTE_SEQUENCES = "SELECT COUNT(*) FROM AdmSequence WHERE CfgCompanyId = :id";
    private static final String COMPTE_ENTITE = "SELECT COUNT(*) FROM CfgCompany WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private Clock clock;

    private AdmUser admin;
    private String jeton;
    private UUID devise;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        devise = entites.devise("DEV-EJ-" + jeton);
    }

    // ------------------------------------------------------------------ outils

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ENTITES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(ENTITE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private CorpsJson corpsMinimal(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Entite " + code).avec(DEVISE_ID, devise);
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions lister(RequestPostProcessor qui, String... parametres) throws Exception {
        var requete = get(ENTITES).with(qui);
        for (int i = 0; i + 1 < parametres.length; i += 2) {
            requete = requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete).andExpect(status().isOk());
    }

    private static String present(UUID id) {
        return "$..[?(@.id=='" + id + "')]";
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private Map<String, Object> fiche(UUID id) throws Exception {
        String json = mockMvc.perform(get(ENTITE, id).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$");
    }

    /** Clés de premier niveau de la fiche dont la valeur vérifie {@code critere}. */
    private static List<String> cles(Map<String, Object> fiche, Predicate<Object> critere) {
        return fiche.entrySet().stream().filter(e -> e.getValue() != null && critere.test(e.getValue()))
                .map(Map.Entry::getKey).toList();
    }

    private static Predicate<Object> egal(String texte) {
        return v -> texte.equals(v.toString());
    }

    private static Predicate<Object> guidEgal(UUID id) {
        return v -> id.toString().equalsIgnoreCase(v.toString());
    }

    private static Predicate<Object> dateDu(String jour) {
        return v -> v.toString().startsWith(jour);
    }

    private static Predicate<Object> nombre(String valeur) {
        return v -> {
            try {
                return new BigDecimal(v.toString()).compareTo(new BigDecimal(valeur)) == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }

    /** Colonne CfgCompany → champs de la fiche qui portent sa valeur ; {@code fiche} = la fiche lue. */
    private record Reperage(Map<String, List<String>> cles, Map<String, Object> fiche) {

        String seule(String colonne) {
            assertThat(cles.get(colonne)).as("champ de la fiche portant %s", colonne).hasSize(1);
            return cles.get(colonne).get(0);
        }

        /** La fiche sert-elle les dates en date-heure ({@code 2001-02-03T00:00:00}) ? Le PATCH reprend son format. */
        boolean dateHeure() {
            return String.valueOf(fiche.get(seule("CreationDate"))).contains("T");
        }
    }

    /** Lit la fiche d'une entité aux valeurs repérables posées en SQL. */
    private Reperage clesDesTreizeColonnes() throws Exception {
        CfgCompany repere = fixture.entite("EJ-REPERE-" + jeton, true);
        UUID groupe = entites.groupe("GRP-EJ-" + jeton);
        entites.treizeColonnes(repere.getId(), new TreizeColonnes("IF-" + jeton, "RC-" + jeton,
                "2001-02-03T00:00:00", new BigDecimal("1234567.5"), "CNSS-" + jeton, "PAT-" + jeton, "ICE-" + jeton,
                17, 7017, "2031-04-05T00:00:00", new BigDecimal("0.375"), groupe, devise));
        Map<String, Object> f = fiche(repere.getId());

        Map<String, List<String>> cles = new LinkedHashMap<>();
        cles.put("FiscalNumber", cles(f, egal("IF-" + jeton)));
        cles.put("CommercialRegister", cles(f, egal("RC-" + jeton)));
        cles.put("CreationDate", cles(f, dateDu("2001-02-03")));
        cles.put("Capital", cles(f, nombre("1234567.5")));
        cles.put("Cnss", cles(f, egal("CNSS-" + jeton)));
        cles.put("Patent", cles(f, egal("PAT-" + jeton)));
        cles.put("ICE", cles(f, egal("ICE-" + jeton)));
        cles.put("TotalSharing", cles(f, nombre("17")));
        cles.put("SharingNumber", cles(f, nombre("7017")));
        cles.put("LiquidationDate", cles(f, dateDu("2031-04-05")));
        cles.put("HomePart", cles(f, nombre("0.375")));
        cles.put("CfgGroupShareholderId", cles(f, guidEgal(groupe)));
        cles.put("StlCurrencyId", cles(f, guidEgal(devise)));
        return new Reperage(cles, f);
    }

    // ------------------------------------------------------------------ §2 liste : non filtrée par IsActive

    @Test
    void r2_liste_actifTous_renvoieActivesEtInactives() throws Exception {
        CfgCompany active = fixture.entite("EJ-LT-A-" + jeton, true);
        CfgCompany inactive = fixture.entite("EJ-LT-I-" + jeton, false);

        lister(connecte(admin), ACTIF, TOUS, SEARCH, jeton)
                .andExpect(jsonPath(present(active.getId())).exists())
                .andExpect(jsonPath(present(inactive.getId())).exists());
    }

    @Test
    void r2_r7_2_liste_actifFalse_inactivesEtDrapeauNul_sansLesActives() throws Exception {
        CfgCompany active = fixture.entite("EJ-LF-A-" + jeton, true);
        CfgCompany inactive = fixture.entite("EJ-LF-I-" + jeton, false);
        CfgCompany jamaisRenseignee = fixture.entite("EJ-LF-N-" + jeton, true);
        entites.actifNull(jamaisRenseignee.getId());

        lister(connecte(admin), ACTIF, "false", SEARCH, jeton)
                .andExpect(jsonPath(present(inactive.getId())).exists())
                .andExpect(jsonPath(present(jamaisRenseignee.getId())).exists())
                .andExpect(jsonPath(present(active.getId())).doesNotExist());
    }

    @Test
    void r2_r7_2_liste_actifTrue_activesSeulement() throws Exception {
        CfgCompany active = fixture.entite("EJ-LV-A-" + jeton, true);
        CfgCompany inactive = fixture.entite("EJ-LV-I-" + jeton, false);
        CfgCompany jamaisRenseignee = fixture.entite("EJ-LV-N-" + jeton, true);
        entites.actifNull(jamaisRenseignee.getId());

        lister(connecte(admin), ACTIF, "true", SEARCH, jeton)
                .andExpect(jsonPath(present(active.getId())).exists())
                .andExpect(jsonPath(present(inactive.getId())).doesNotExist())
                .andExpect(jsonPath(present(jamaisRenseignee.getId())).doesNotExist());
    }

    @Test
    void r2_liste_recherche_codeEtDescription_espacesRetiresDesDeuxCotes() throws Exception {
        CfgCompany parCode = fixture.entite("EJ R " + jeton, true);
        CfgCompany autre = fixture.entite("EJ-AUTRE-" + jeton, true);

        lister(connecte(admin), ACTIF, TOUS, SEARCH, "E JR" + jeton)
                .andExpect(jsonPath(present(parCode.getId())).exists())
                .andExpect(jsonPath(present(autre.getId())).doesNotExist());
    }

    @Test
    void r2_liste_recherche_identifiantFiscalEtRegistreDuCommerce_espacesRetires() throws Exception {
        CfgCompany parFiscal = fixture.entite("EJ-F-" + jeton, true);
        entites.identifiants(parFiscal.getId(), "IF 77 " + jeton, "");
        CfgCompany parRegistre = fixture.entite("EJ-RC-" + jeton, true);
        entites.identifiants(parRegistre.getId(), "", "RC 88 " + jeton);

        lister(connecte(admin), ACTIF, TOUS, SEARCH, "IF77" + jeton)
                .andExpect(jsonPath(present(parFiscal.getId())).exists())
                .andExpect(jsonPath(present(parRegistre.getId())).doesNotExist());
        lister(connecte(admin), ACTIF, TOUS, SEARCH, "RC  88" + jeton)
                .andExpect(jsonPath(present(parRegistre.getId())).exists());
    }

    /** §2 rôle exempté ; §7.4 : aucune habilitation n'est exigée de l'Administrateur. */
    @Test
    void r2_liste_administrateurSansHabilitation_voitLEntite() throws Exception {
        CfgCompany entite = fixture.entite("EJ-ADM-" + jeton, true);

        assertThat(fixture.habilitations(admin.getId())).isEmpty();
        lister(connecte(admin), ACTIF, TOUS, SEARCH, jeton)
                .andExpect(jsonPath(present(entite.getId())).exists());
    }

    /** §7.4 : liste d'habilitations vide = tout (convention du CRM). */
    @Test
    void r7_4_liste_compteSansAucuneHabilitation_voitLEntite() throws Exception {
        AdmUser compte = fixture.administrateur();
        CfgCompany entite = fixture.entite("EJ-COM-" + jeton, true);

        lister(commercial(compte), ACTIF, TOUS, SEARCH, jeton)
                .andExpect(jsonPath(present(entite.getId())).exists());
    }

    // ------------------------------------------------------------------ §2 fiche

    @Test
    void r2_fiche_200_codeDescriptionEtLibellesDesCles() throws Exception {
        CfgCompany entite = fixture.entite("EJ-FI-" + jeton, true);
        UUID groupe = entites.groupe("GRP-FI-" + jeton);
        entites.groupeDeLEntite(entite.getId(), groupe);
        entites.deviseDeLEntite(entite.getId(), devise);

        String json = mockMvc.perform(get(ENTITE, entite.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entite.getId().toString()))
                .andExpect(jsonPath("$.code").value("EJ-FI-" + jeton))
                .andExpect(jsonPath("$.description").value("Entite EJ-FI-" + jeton))
                .andReturn().getResponse().getContentAsString();

        assertThat(json).contains(FixtureEntites.libelle("DEV-EJ-" + jeton), FixtureEntites.libelle("GRP-FI-" + jeton));
    }

    /** §0 : l'écran gère le drapeau, sa fiche n'est pas masquée par R4. */
    @Test
    void r2_fiche_entiteInactive_200() throws Exception {
        CfgCompany inactive = fixture.entite("EJ-FINA-" + jeton, false);

        mockMvc.perform(get(ENTITE, inactive.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inactive.getId().toString()));
    }

    @Test
    void r2_fiche_404_idInconnu() throws Exception {
        mockMvc.perform(get(ENTITE, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r2_fiche_repartitionIncluse() throws Exception {
        CfgCompany entite = fixture.entite("EJ-FREP-" + jeton, true);
        UUID actionnaire = entites.actionnaire("ACT-FREP-" + jeton);
        entites.parts(entite.getId(), 40, 1);
        entites.repartition(entite.getId(), actionnaire, 40);

        mockMvc.perform(get(ENTITE, entite.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.actionnaireId=='" + actionnaire + "')].nombreParts").value(40));
    }

    // ------------------------------------------------------------------ §7.2 les 13 colonnes du lot

    @Test
    void r7_2_fiche_lesTreizeColonnesSontLues() throws Exception {
        Reperage reperage = clesDesTreizeColonnes();

        assertThat(reperage.cles()).hasSize(13).allSatisfy((colonne, champs) ->
                assertThat(champs).as("champ de la fiche portant %s", colonne).hasSize(1));
    }

    @Test
    void r7_2_patch_lesTreizeColonnesSontEcrites() throws Exception {
        Reperage r = clesDesTreizeColonnes();
        boolean dateHeure = r.dateHeure();
        CfgCompany cible = fixture.entite("EJ-ECR-" + jeton, true);
        UUID autreGroupe = entites.groupe("GRP-ECR-" + jeton);
        UUID autreDevise = entites.devise("DEV-ECR-" + jeton);

        modifier(cible.getId(), corps()
                .avec(r.seule("FiscalNumber"), "IF2-" + jeton)
                .avec(r.seule("CommercialRegister"), "RC2-" + jeton)
                .avec(r.seule("CreationDate"), dateHeure ? "2002-03-04T00:00:00" : "2002-03-04")
                .avec(r.seule("Capital"), new BigDecimal("7654321.25"))
                .avec(r.seule("Cnss"), "CNSS2-" + jeton)
                .avec(r.seule("Patent"), "PAT2-" + jeton)
                .avec(r.seule("ICE"), "ICE2-" + jeton)
                .avec(r.seule("TotalSharing"), 19)
                .avec(r.seule("SharingNumber"), 9009)
                .avec(r.seule("LiquidationDate"), dateHeure ? "2032-05-06T00:00:00" : "2032-05-06")
                .avec(r.seule("HomePart"), new BigDecimal("0.125"))
                .avec(r.seule("CfgGroupShareholderId"), autreGroupe)
                .avec(r.seule("StlCurrencyId"), autreDevise))
                .andExpect(status().isOk());

        Tuple t = entites.ligneEntite(cible.getId());
        assertAll(
                () -> assertThat(t.get("FiscalNumber")).isEqualTo("IF2-" + jeton),
                () -> assertThat(t.get("CommercialRegister")).isEqualTo("RC2-" + jeton),
                () -> assertThat(String.valueOf(t.get("CreationDate"))).startsWith("2002-03-04"),
                () -> assertThat(decimal(t.get("Capital"))).isEqualByComparingTo("7654321.25"),
                () -> assertThat(t.get("Cnss")).isEqualTo("CNSS2-" + jeton),
                () -> assertThat(t.get("Patent")).isEqualTo("PAT2-" + jeton),
                () -> assertThat(t.get("ICE")).isEqualTo("ICE2-" + jeton),
                () -> assertThat(t.get("TotalSharing")).isEqualTo(19),
                () -> assertThat(t.get("SharingNumber")).isEqualTo(9009),
                () -> assertThat(String.valueOf(t.get("LiquidationDate"))).startsWith("2032-05-06"),
                () -> assertThat(decimal(t.get("HomePart"))).isEqualByComparingTo("0.125"),
                () -> assertThat(guid(t.get("CfgGroupShareholderId"))).isEqualTo(guid(autreGroupe)),
                () -> assertThat(guid(t.get("StlCurrencyId"))).isEqualTo(guid(autreDevise)));
    }

    // ------------------------------------------------------------------ §2 création

    @Test
    void r2_creation_201_ligneEcrite_actifParDefaut_deviseSaisie() throws Exception {
        String code = "EJ-CRE-" + jeton;

        UUID id = creerOk(corpsMinimal(code));

        Tuple t = entites.ligneEntite(id);
        assertAll(
                () -> assertThat(t.get("Code")).isEqualTo(code),
                () -> assertThat(t.get("Description")).isEqualTo("Entite " + code),
                () -> assertThat(t.get("IsActive")).isEqualTo(true),
                () -> assertThat(guid(t.get("StlCurrencyId"))).isEqualTo(guid(devise)));
    }

    @Test
    void r2_r7_4_creation_habilitationDuCreateurSurLEntite() throws Exception {
        UUID id = creerOk(corpsMinimal("EJ-HAB-" + jeton));

        assertThat(fixture.habilitations(admin.getId()))
                .extracting(h -> guid(h.get("CfgCompanyId")))
                .containsExactly(guid(id));
    }

    @Test
    void r2_r7_5_creation_quatreSequencesDeLEntite() throws Exception {
        int annee = LocalDate.now(clock).getYear();

        UUID id = creerOk(corpsMinimal("EJ-SEQ-" + jeton));

        List<Tuple> sequences = entites.sequences(id);
        assertThat(sequences).extracting(s -> s.get("TypeCode") + "|" + s.get("Code"))
                .containsExactlyInAnyOrder("INVOICE|Facture", "REFUND|Avoir",
                        "RSSheetCode|Certificat de retenue à la source", "Receipt|Reçu");
        assertThat(sequences).isNotEmpty().allSatisfy(s -> assertAll(
                () -> assertThat(s.get("SeqYear")).isEqualTo(annee),
                () -> assertThat(s.get("BasedOnSystemYear")).isEqualTo(true),
                () -> assertThat(s.get("SeqYearNumber")).isEqualTo(2),
                () -> assertThat(s.get("SeqValueNumber")).isEqualTo(3),
                () -> assertThat(s.get("SeqValue")).isEqualTo(0),
                () -> assertThat(s.get("Separator")).isEqualTo(""),
                () -> assertThat(s.get("SeqPrefix")).isEqualTo(""),
                () -> assertThat(s.get("SeqSuffix")).isEqualTo("")));
    }

    /** §7.5 : la deuxième création ne lit pas les séquences de la première (aucune lecture par Code seul). */
    @Test
    void r7_5_deuxCreations_chacuneSesQuatreSequences() throws Exception {
        UUID premiere = creerOk(corpsMinimal("EJ-SEQ1-" + jeton));
        UUID seconde = creerOk(corpsMinimal("EJ-SEQ2-" + jeton));

        assertAll(
                () -> assertThat(entites.compter(COMPTE_SEQUENCES, premiere)).isEqualTo(4),
                () -> assertThat(entites.compter(COMPTE_SEQUENCES, seconde)).isEqualTo(4));
    }

    /** « unique table entière » : une entité inactive garde son code. */
    @Test
    void r2_creation_409_codeDejaPrisMemeParUneEntiteInactive() throws Exception {
        CfgCompany existante = fixture.entite("EJ-DBL-" + jeton, false);

        creer(corpsMinimal(existante.getCode())).andExpect(status().isConflict());

        assertThat(entites.entitesDeCode(existante.getCode())).isEqualTo(1);
    }

    @Test
    void r2_creation_400_sansCode() throws Exception {
        creer(corpsMinimal("x").sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_creation_400_sansDescription() throws Exception {
        creer(corpsMinimal("EJ-SDESC-" + jeton).sans(DESCRIPTION)).andExpect(status().isBadRequest());
    }

    @Test
    void r7_3_creation_sansDevise_deviseMADParDefaut() throws Exception {
        UUID mad = entites.deviseMad();

        UUID id = creerOk(corpsMinimal("EJ-MAD-" + jeton).sans(DEVISE_ID));

        assertThat(guid(entites.ligneEntite(id).get("StlCurrencyId"))).isEqualTo(guid(mad));
    }

    @Test
    void r7_3_creation_sansDevise_MADAbsente_400_rienEcrit() throws Exception {
        entites.deviseMad();
        entites.masquerDeviseMad();
        String code = "EJ-NOMAD-" + jeton;

        creer(corpsMinimal(code).sans(DEVISE_ID)).andExpect(status().isBadRequest());

        assertThat(entites.entitesDeCode(code)).isZero();
    }

    // ------------------------------------------------------------------ §2 PATCH

    @Test
    void r2_patch_isActive_desactivePuisReactive() throws Exception {
        CfgCompany entite = fixture.entite("EJ-BASC-" + jeton, true);

        modifier(entite.getId(), corps().avec("isActive", false)).andExpect(status().isOk());
        assertThat(entites.ligneEntite(entite.getId()).get("IsActive")).isEqualTo(false);

        modifier(entite.getId(), corps().avec("isActive", true)).andExpect(status().isOk());
        assertThat(entites.ligneEntite(entite.getId()).get("IsActive")).isEqualTo(true);
    }

    @Test
    void r2_patch_champsAVider_videLaDateDeCreation() throws Exception {
        String champDate = clesDesTreizeColonnes().seule("CreationDate");
        CfgCompany entite = fixture.entite("EJ-VID-" + jeton, true);
        entites.dateCreation(entite.getId(), "2010-01-01T00:00:00");

        mockMvc.perform(patch(ENTITE, entite.getId()).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"champsAVider\":[\"" + champDate + "\"]}"))
                .andExpect(status().isOk());

        assertThat(entites.ligneEntite(entite.getId()).get("CreationDate")).isNull();
    }

    @Test
    void r2_patch_404_idInconnu() throws Exception {
        modifier(UUID.randomUUID(), corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §2 DELETE

    @Test
    void r2_suppression_204_entiteEtSequencesSupprimees() throws Exception {
        UUID id = creerOk(corpsMinimal("EJ-DEL-" + jeton));
        assertThat(entites.compter(COMPTE_SEQUENCES, id)).isEqualTo(4);

        mockMvc.perform(delete(ENTITE, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(entites.compter(COMPTE_ENTITE, id)).isZero(),
                () -> assertThat(entites.compter(COMPTE_SEQUENCES, id)).isZero());
    }

    @Test
    void r2_suppression_409_programmeRattache_entiteConservee() throws Exception {
        CfgCompany entite = fixture.entite("EJ-DEL409-" + jeton, true);
        fixture.projet("PRJ-DEL409-" + jeton, entite);

        mockMvc.perform(delete(ENTITE, entite.getId()).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(entites.compter(COMPTE_ENTITE, entite.getId())).isEqualTo(1);
    }

    @Test
    void r2_suppression_404_idInconnu() throws Exception {
        mockMvc.perform(delete(ENTITE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}

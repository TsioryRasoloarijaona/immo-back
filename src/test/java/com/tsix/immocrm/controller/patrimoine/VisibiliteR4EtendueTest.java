package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R4 étendue — FEATURE-entites-juridiques.md §7.6 et FEATURE-patrimoine-reliquat-v2.md §1 : une entité juridique
 * inactive rend introuvables (404, Administrateur compris) la tranche et l'article par id, leurs sous-ressources,
 * leurs écritures, leur audit et leurs notes ; règle unique {@code soc.isActive = true OR soc.id IS NULL}.
 *
 * <p>Le drapeau est basculé en SQL (l'écran des entités a ses propres tests) : chaque route est vue active,
 * désactivée puis réactivée. Jetons {@code {T}} (tranche), {@code {A}} (article), {@code {S}} (sous-article libre de
 * la tranche) et {@code {CAT}}, {@code {TYP}}, {@code {ETG}} (référentiels du sous-article) remplacés au setUp.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisibiliteR4EtendueTest {

    private static final String TRANCHE = "/api/tranches/{T}";
    private static final String ARTICLE = "/api/articles/{A}";
    private static final String DESCRIPTION_X = "{\"description\":\"R4\"}";
    private static final String NOTE_TRANCHE = "{\"porteur\":\"tranche\",\"id\":\"{T}\",\"texte\":\"Note R4\"}";
    private static final String NOTE_ARTICLE = "{\"porteur\":\"article\",\"id\":\"{A}\",\"texte\":\"Note R4\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private StkItemRepository itemRepository;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItem article;
    private StkItem sousArticle;
    private StkItem sousArticle2;
    private StkItem rattache;
    private StkItem rattache2;
    private StkItemCategory categorieSous;
    private StkItemType typeSous;
    private StkFloor etage;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "R4E" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        fixture.referentielArticle();
        StkKind nature = fixture.natureBat();
        entite = fixture.entite("ENT-" + jeton, true);
        projet = fixture.projet("PRJ-" + jeton, entite);
        tranche = fixture.tranche("TR-" + jeton, projet);
        article = article("ART-" + jeton, fixture.categorie("CAT-" + jeton, nature, false),
                fixture.type("TYP-" + jeton, nature, false), null);
        article.setStkBuilding(fixture.immeuble("IMM-" + jeton));
        article = itemRepository.save(article);
        categorieSous = fixture.categorie("CATS-" + jeton, nature, true);
        typeSous = fixture.type("TYPS-" + jeton, nature, true);
        sousArticle = article("SA1-" + jeton, categorieSous, typeSous, null);
        sousArticle2 = article("SA2-" + jeton, categorieSous, typeSous, null);
        rattache = article("SR1-" + jeton, categorieSous, typeSous, article);
        rattache2 = article("SR2-" + jeton, categorieSous, typeSous, article);
        etage = fixture.etage("ETG-" + jeton);
    }

    /** {@code parent} non nul : sous-article rattaché ; {@code categorie} pour sous-article : sous-article libre. */
    private StkItem article(String code, StkItemCategory categorie, StkItemType type, StkItem parent) {
        boolean sous = Boolean.TRUE.equals(categorie.getForUnderItem());
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemCategory(categorie);
        item.setStkItemType(type);
        item.setStkKind(fixture.natureBat());
        item.setStkVocation(fixture.vocationVente());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(sous);
        item.setAlwaysAffected(false);
        item.setParent(parent);
        return itemRepository.save(item);
    }

    private String url(String route) {
        return route.replace("{T}", tranche.getId().toString())
                .replace("{A}", article.getId().toString())
                .replace("{S2}", sousArticle2.getId().toString())
                .replace("{S}", sousArticle.getId().toString())
                .replace("{R2}", rattache2.getId().toString())
                .replace("{R}", rattache.getId().toString())
                .replace("{CAT}", categorieSous.getId().toString())
                .replace("{TYP}", typeSous.getId().toString())
                .replace("{ETG}", etage.getId().toString());
    }

    // ------------------------------------------------------------------ lectures : active → 2xx, inactive → 404, réactivée → 2xx

    static Stream<String> lectures() {
        return Stream.of(
                TRANCHE,
                TRANCHE + "/articles",
                TRANCHE + "/sous-articles-libres",
                ARTICLE,
                ARTICLE + "/sous-articles",
                ARTICLE + "/pieces-jointes",
                ARTICLE + "/actions",
                "/api/audit?objetId={T}",
                "/api/audit?objetId={A}",
                "/api/notes?porteur=tranche&id={T}",
                "/api/notes?porteur=article&id={A}");
    }

    @ParameterizedTest(name = "GET {0}")
    @MethodSource("lectures")
    void r4_lecture_404EntiteDesactivee_puisVisibleALaReactivation(String route) throws Exception {
        mockMvc.perform(get(url(route)).with(connecte(admin))).andExpect(status().is2xxSuccessful());

        entites.actif(entite.getId(), false);
        mockMvc.perform(get(url(route)).with(connecte(admin))).andExpect(status().isNotFound());

        entites.actif(entite.getId(), true);
        mockMvc.perform(get(url(route)).with(connecte(admin))).andExpect(status().is2xxSuccessful());
    }

    /** R3 : drapeau jamais renseigné = inactif. */
    @ParameterizedTest(name = "GET {0}")
    @MethodSource("lectures")
    void r4_r3_lecture_404DrapeauNul(String route) throws Exception {
        entites.actifNull(entite.getId());

        mockMvc.perform(get(url(route)).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ écritures : acceptées active, 404 désactivée

    private static String sousArticle(String code) {
        return """
                {"code":"%s","description":"Parking R4","categorieId":"{CAT}","typeId":"{TYP}","etageId":"{ETG}",
                 "surfaceProvisoire":10,"surfaceDefinitive":10,"prixM2ProvisoireHT":5000,"prixM2DefinitifHT":5000,
                 "tauxTva":0.20}""".formatted(code);
    }

    /**
     * Méthode, puis route et corps joués l'entité active (2xx attendu : la requête est valide), puis route et corps
     * joués une fois l'entité désactivée (404). {@code {S}}/{@code {S2}} : sous-articles libres ; {@code {R}}/{@code {R2}} :
     * sous-articles rattachés à l'article.
     */
    static Stream<Arguments> ecritures() {
        String affecter = ARTICLE + "/sous-articles/%s/affecter";
        String desaffecter = ARTICLE + "/sous-articles/%s/desaffecter";
        return Stream.of(
                Arguments.of(HttpMethod.PATCH, TRANCHE, DESCRIPTION_X, TRANCHE, DESCRIPTION_X),
                Arguments.of(HttpMethod.PATCH, ARTICLE, DESCRIPTION_X, ARTICLE, DESCRIPTION_X),
                Arguments.of(HttpMethod.POST, ARTICLE + "/sous-articles", sousArticle("SA-R4-N1"),
                        ARTICLE + "/sous-articles", sousArticle("SA-R4-N2")),
                Arguments.of(HttpMethod.POST, affecter.formatted("{S}"), "{}", affecter.formatted("{S2}"), "{}"),
                Arguments.of(HttpMethod.POST, desaffecter.formatted("{R}"), "{}", desaffecter.formatted("{R2}"), "{}"),
                Arguments.of(HttpMethod.POST, "/api/notes", NOTE_TRANCHE, "/api/notes", NOTE_TRANCHE),
                Arguments.of(HttpMethod.POST, "/api/notes", NOTE_ARTICLE, "/api/notes", NOTE_ARTICLE));
    }

    @ParameterizedTest(name = "{0} {3}")
    @MethodSource("ecritures")
    void r4_ecriture_404EntiteDesactivee(HttpMethod methode, String routeActive, String corpsActif,
                                         String routeInactive, String corpsInactif) throws Exception {
        mockMvc.perform(request(methode, url(routeActive)).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(url(corpsActif)))
                .andExpect(status().is2xxSuccessful());

        entites.actif(entite.getId(), false);

        mockMvc.perform(request(methode, url(routeInactive)).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(url(corpsInactif)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ création de tranche

    @Test
    void r4_creationTranche_programmeDUneEntiteInactive_400() throws Exception {
        entites.actif(entite.getId(), false);

        mockMvc.perform(post("/api/tranches").with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("projetId", projet.getId()).avec("code", "TR-NEW-" + jeton)
                                .avec("description", "Tranche R4").json()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §7.6 règle unique : sans entité = visible

    @Test
    void r7_6_programmeSansEntite_visibleParId() throws Exception {
        CfgProject orphelin = fixture.projet("PRJ-ORPH-" + jeton, null);

        mockMvc.perform(get("/api/projets/{id}", orphelin.getId()).with(connecte(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void r7_6_trancheDUnProgrammeSansEntite_visibleParId() throws Exception {
        CfgTranche orpheline = fixture.tranche("TR-ORPH-" + jeton, fixture.projet("PRJ-ORPH2-" + jeton, null));

        mockMvc.perform(get("/api/tranches/{id}", orpheline.getId()).with(connecte(admin)))
                .andExpect(status().isOk());
    }
}

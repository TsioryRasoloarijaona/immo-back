package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pièces jointes en écriture sur article, tranche, programme — FEATURE-patrimoine-article-onglets.md §4, §7, §8.2, §8.3,
 * §8.5, §11 : {@code POST /api/{articles|tranches|projets}/{id}/pieces-jointes} (multipart {@code fichier}, {@code typeId},
 * {@code description}, {@code groupe}) → 201, {@code DELETE …/{pjId}} → 204 (suppression logique), lectures de tranche et
 * programme, Administrateur seul, type inconnu → 404, R4 du porteur, groupes {@code GET /api/articles/groupes-piece-jointe}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PiecesJointesEcritureTest {

    private static final String PIECES = "/api/%s/%s/pieces-jointes";
    private static final String ARTICLES = "articles";
    private static final String TRANCHES = "tranches";
    private static final String PROJETS = "projets";
    private static final String COMMERCIAL = "Commercial";
    private static final String ID = "$.id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente ventes;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private FixtureEntites entites;
    @Autowired private AdmAttachedFileTypeRepository typeRepository;
    @Autowired private EntityManager entityManager;

    private String jeton;
    private AdmUser admin;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItem article;
    private AdmAttachedFileType type;

    @BeforeEach
    void setUp() {
        jeton = "P2BPJ" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        patrimoine.referentielArticle();
        entite = patrimoine.entite("ENT-" + jeton, true);
        projet = patrimoine.projet("PRJ-" + jeton, entite);
        tranche = patrimoine.tranche("TR-" + jeton, projet);
        article = onglets.article("ART-" + jeton, tranche);
        type = new AdmAttachedFileType();
        type.setId(UUID.randomUUID());
        type.setCode("PLAN-" + jeton);
        type.setDescription("Plan " + jeton);
        typeRepository.save(type);
    }

    // ------------------------------------------------------------------ outils

    private UUID porteur(String segment) {
        return switch (segment) {
            case ARTICLES -> article.getId();
            case TRANCHES -> tranche.getId();
            default -> projet.getId();
        };
    }

    /** Un second porteur du même genre, dans le même programme. */
    private UUID autrePorteur(String segment) {
        return switch (segment) {
            case ARTICLES -> onglets.article("ART2-" + jeton, tranche).getId();
            case TRANCHES -> patrimoine.tranche("TR2-" + jeton, projet).getId();
            default -> patrimoine.projet("PRJ2-" + jeton, entite).getId();
        };
    }

    private static String pieces(String segment, UUID id) {
        return PIECES.formatted(segment, id);
    }

    private static MockMultipartFile pdf(String nom) {
        return new MockMultipartFile("fichier", nom, MediaType.APPLICATION_PDF_VALUE,
                "PDF-CONTENU".getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions deposer(String segment, UUID id, MockMultipartFile fichier, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(multipart(pieces(segment, id)).file(fichier)
                .param("typeId", type.getId().toString())
                .param("description", "Plan " + jeton)
                .param("groupe", "GRP-" + jeton)
                .with(qui));
    }

    private UUID deposerPdf(String segment) throws Exception {
        String json = deposer(segment, porteur(segment), pdf("plan.pdf"), connecte(admin))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(JsonPath.read(json, ID));
    }

    private Tuple ligne(UUID pieceId) {
        entityManager.flush();
        entityManager.clear();
        return (Tuple) entityManager.createNativeQuery("""
                        SELECT f.Category, f.CategoryDescription, f.FoderGroup,
                               CONVERT(varchar(36), f.CategoryId) AS CategoryId,
                               o.StorageKey, o.DeletedAt
                        FROM AdmAttachedFile f LEFT JOIN AppFileObject o ON o.AdmAttachedFileId = f.PKey
                        WHERE f.PKey = :id""", Tuple.class)
                .setParameter("id", pieceId)
                .getSingleResult();
    }

    private static RequestPostProcessor commercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, COMMERCIAL);
    }

    // ------------------------------------------------------------------ §4 dépôt sur les trois porteurs

    @Test
    void r4_article_depot201_categorieArticle_typeEtGroupe() throws Exception {
        deposer(ARTICLES, article.getId(), pdf("plan.pdf"), connecte(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(ID).isNotEmpty())
                .andExpect(jsonPath("$.category").value("Yes.Stk.Building.Item"))
                .andExpect(jsonPath("$.type").value(type.getDescription()))
                .andExpect(jsonPath("$.description").value("Plan " + jeton))
                .andExpect(jsonPath("$.groupe").value("GRP-" + jeton))
                .andExpect(jsonPath("$.extension").value(".pdf"));
    }

    /** §8.3 : tranche → {@code Category = Yes.Cfg.CfgTranche}, {@code CategoryDescription = Description}, groupe écrit. */
    @Test
    void r4_tranche_depot201_categorieTranche_descriptionDeTranche_groupe() throws Exception {
        UUID piece = deposerPdf(TRANCHES);

        Tuple l = ligne(piece);
        assertAll(
                () -> assertThat(l.get("Category")).isEqualTo("Yes.Cfg.CfgTranche"),
                () -> assertThat(l.get("CategoryDescription")).isEqualTo(tranche.getDescription()),
                () -> assertThat(l.get("FoderGroup")).isEqualTo("GRP-" + jeton),
                () -> assertThat(FixturePatrimoine.guid(l.get("CategoryId"))).isEqualTo(FixturePatrimoine.guid(tranche.getId())));
    }

    /** §8.3 : programme → {@code Category = Yes.Cfg.CfgProject}, {@code CategoryDescription = Code}. */
    @Test
    void r4_projet_depot201_categorieProgramme_codeDuProgramme() throws Exception {
        UUID piece = deposerPdf(PROJETS);

        Tuple l = ligne(piece);
        assertAll(
                () -> assertThat(l.get("Category")).isEqualTo("Yes.Cfg.CfgProject"),
                () -> assertThat(l.get("CategoryDescription")).isEqualTo(projet.getCode()),
                () -> assertThat(FixturePatrimoine.guid(l.get("CategoryId"))).isEqualTo(FixturePatrimoine.guid(projet.getId())));
    }

    /** §8.2 : lectures de tranche et programme (liste et redirection vers l'objet stocké). */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {TRANCHES, PROJETS})
    void r4_lecture_listeEtFichier302(String segment) throws Exception {
        UUID piece = deposerPdf(segment);
        String cle = (String) ligne(piece).get("StorageKey");

        mockMvc.perform(get(pieces(segment, porteur(segment))).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + piece + "')].groupe").value(List.of("GRP-" + jeton)));
        mockMvc.perform(get(pieces(segment, porteur(segment)) + "/" + piece + "/fichier").with(connecte(admin)))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString(cle)));
    }

    // ------------------------------------------------------------------ §8.1-13 suppression logique

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r4_suppression204_logique_retireeDeLaListe_ligneConservee(String segment) throws Exception {
        UUID piece = deposerPdf(segment);

        mockMvc.perform(delete(pieces(segment, porteur(segment)) + "/" + piece).with(connecte(admin)))
                .andExpect(status().isNoContent());

        String liste = mockMvc.perform(get(pieces(segment, porteur(segment))).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Tuple l = ligne(piece);
        assertAll(
                () -> assertThat(liste).doesNotContain(piece.toString()),
                () -> assertThat(l.get("DeletedAt")).isNotNull());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r4_suppression_pieceDUnAutrePorteur404(String segment) throws Exception {
        UUID piece = deposerPdf(segment);

        mockMvc.perform(delete(pieces(segment, autrePorteur(segment)) + "/" + piece).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ refus : 404 type, 400 vide, 404 porteur

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r11_typeInconnu404(String segment) throws Exception {
        mockMvc.perform(multipart(pieces(segment, porteur(segment))).file(pdf("plan.pdf"))
                        .param("typeId", UUID.randomUUID().toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r4_fichierVide400(String segment) throws Exception {
        MockMultipartFile vide = new MockMultipartFile("fichier", "vide.pdf", MediaType.APPLICATION_PDF_VALUE,
                new byte[0]);

        deposer(segment, porteur(segment), vide, connecte(admin))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r4_porteurInconnu404(String segment) throws Exception {
        deposer(segment, UUID.randomUUID(), pdf("plan.pdf"), connecte(admin))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §0 écriture Administrateur

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r0_commercial_depot403(String segment) throws Exception {
        AdmUser compte = ventes.compte("COM-" + jeton, COMMERCIAL);

        deposer(segment, porteur(segment), pdf("plan.pdf"), commercial(compte))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, TRANCHES, PROJETS})
    void r0_commercial_suppression403(String segment) throws Exception {
        UUID piece = deposerPdf(segment);
        AdmUser compte = ventes.compte("COM-" + jeton, COMMERCIAL);

        mockMvc.perform(delete(pieces(segment, porteur(segment)) + "/" + piece).with(commercial(compte)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ §8.2 R4 du porteur

    static Stream<Arguments> routesR4() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, TRANCHES),
                Arguments.of(HttpMethod.GET, PROJETS),
                Arguments.of(HttpMethod.POST, ARTICLES),
                Arguments.of(HttpMethod.POST, TRANCHES),
                Arguments.of(HttpMethod.POST, PROJETS));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routesR4")
    void r4_porteurDUneEntiteInactive404(HttpMethod methode, String segment) throws Exception {
        UUID id = porteur(segment);
        jouer(methode, segment, id).andExpect(status().is2xxSuccessful());

        entites.actif(entite.getId(), false);

        jouer(methode, segment, id).andExpect(status().isNotFound());
    }

    private ResultActions jouer(HttpMethod methode, String segment, UUID id) throws Exception {
        return methode == HttpMethod.GET
                ? mockMvc.perform(get(pieces(segment, id)).with(connecte(admin)))
                : deposer(segment, id, pdf("plan.pdf"), connecte(admin));
    }

    // ------------------------------------------------------------------ §8.2 groupes, §8.5 nom assaini

    @Test
    void r8_2_groupesPieceJointe_contientLeGroupeDepose() throws Exception {
        deposerPdf(TRANCHES);

        String corps = mockMvc.perform(get("/api/articles/groupes-piece-jointe").with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(corps).contains("GRP-" + jeton);
    }

    /** §8.5 : légende par défaut = nom d'origine sans séparateur de chemin ni caractère de contrôle. */
    @Test
    void r8_5_nomAssaini_legendeParDefautSansSeparateurNiControle() throws Exception {
        String json = mockMvc.perform(multipart(pieces(TRANCHES, tranche.getId()))
                        .file(pdf("..\\secret/plan\u0007rdc.pdf")).with(connecte(admin)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String legende = JsonPath.read(json, "$.description");
        assertThat(legende).contains("plan").doesNotContain("/", "\\", "\u0007");
    }
}

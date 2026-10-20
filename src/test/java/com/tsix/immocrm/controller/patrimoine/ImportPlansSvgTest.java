package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import SVG d'un étage — FEATURE-patrimoine-plans.md §2 (règles {@code ImportSvgPath}, messages exacts), « Recalage
 * sur main » (piège 5, {@code apercu}, {@code href}), Question 6 et « Réponses du manager » (sécurité testée : DTD /
 * entité externe → 400, image non png/jpeg → 400, {@code d} hors grammaire → 400, &gt; 50 Mo → 413), réponse 5
 * (mise à jour : Code et Description gardés).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportPlansSvgTest {

    private static final String IMPORT = "/api/tranches/{id}/plans/import";
    private static final String D_A = "m 10.5,20 h 30 v 40 z";
    private static final String D_B = "M 100,100 L 150,100 L 150,150 Z";
    private static final String INTROUVABLE = "Le code article saisi est introuvable dans notre référenciel";
    private static final String PLANS_DE_LA_TRANCHE = "SELECT COUNT(*) FROM StkHierarchyPlan WHERE StkHierarchyId = :id";
    private static final String TRACES_DE_LA_TRANCHE = """
            SELECT COUNT(*) FROM StkHierarchyPath p JOIN StkItem i ON i.PKey = p.StkItemId
            WHERE i.StkHierarchyId = :id""";
    private static final String TRACES_DE_L_ARTICLE = """
            SELECT CONVERT(varchar(36), PKey) AS PKey, CAST(SvgPath AS varchar(max)) AS SvgPath,
                   CONVERT(varchar(36), StkFloorId) AS StkFloorId
            FROM StkHierarchyPath WHERE StkItemId = :id""";
    private static final String PLAN_DE_LA_TRANCHE = """
            SELECT CONVERT(varchar(36), PKey) AS PKey, Code, Description, PlanImage, width, height, X, Y,
                   CONVERT(varchar(36), StkFloorId) AS StkFloorId, UserCreateDateTime
            FROM StkHierarchyPlan WHERE StkHierarchyId = :id""";
    private static final String SVG_PATH = "SvgPath";
    private static final String LARGEUR_SVG = "1312.5979";
    private static final String HAUTEUR_SVG = "928.15833";
    private static final String XLINK = "xlink:href";
    private static final String PLAN_IMAGE = "PlanImage";
    private static final String CODE = "Code";
    private static final String PKEY = "PKey";
    private static final String DESCRIPTION = "Description";
    private static final String ANCIEN_CODE = "ANCIEN CODE";
    private static final long CINQUANTE_MO = 50L * 1024 * 1024;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureArticleFiche lecture;

    @TempDir Path dossierTemporaire;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;
    private StkFloor etage;
    private StkItem articleA;
    private StkItem articleB;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = plans.tranche("IMP-" + jeton, true);
        etage = plans.etage("ETG-" + jeton);
        articleA = plans.article(tranche, "A-" + jeton, false, etage);
        articleB = plans.article(tranche, "B-" + jeton, false, etage);
    }

    // ------------------------------------------------------------------ outils SVG

    private static String image(String largeur, String hauteur, String attributHref, String href) {
        return "<image width=\"%s\" height=\"%s\" x=\"73.34\" y=\"22.7\" %s=\"%s\"/>"
                .formatted(largeur, hauteur, attributHref, href);
    }

    private static String imagePng() {
        return image(LARGEUR_SVG, HAUTEUR_SVG, XLINK, FixturePlans.dataUriPng());
    }

    private static String trace(String id, String d) {
        return "<path id=\"%s\" d=\"%s\"/>".formatted(id, d);
    }

    private static String svg(String... elements) {
        return svgAvecPrologue("", elements);
    }

    private static String svgAvecPrologue(String prologue, String... elements) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                %s<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1312" height="928">
                <g>%s</g>
                </svg>""".formatted(prologue, String.join("\n", elements));
    }

    private String nominal() {
        return svg(imagePng(), trace(articleA.getCode(), D_A), trace(articleB.getCode(), D_B));
    }

    private MockMultipartHttpServletRequestBuilder requete(byte[] fichier) {
        MockMultipartHttpServletRequestBuilder requete = multipart(IMPORT, tranche.getId());
        requete.file(new MockMultipartFile("fichier", "plan.svg", "image/svg+xml", fichier))
                .param("etageId", etage.getId().toString());
        requete.with(connecte(admin));
        return requete;
    }

    private ResultActions importer(String svg) throws Exception {
        return mockMvc.perform(requete(svg.getBytes(StandardCharsets.UTF_8)));
    }

    private String corps(ResultActions resultat) throws Exception {
        return resultat.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<Tuple> tracesDe(StkItem article) {
        return lecture.lignes(TRACES_DE_L_ARTICLE, article.getId());
    }

    private Tuple seulPlan() {
        List<Tuple> lignes = lecture.lignes(PLAN_DE_LA_TRANCHE, tranche.getId());
        assertThat(lignes).as("plans de la tranche").hasSize(1);
        return lignes.get(0);
    }

    private void aucuneEcriture() {
        assertAll(
                () -> assertThat(lecture.compter(PLANS_DE_LA_TRANCHE, tranche.getId())).as("plans").isZero(),
                () -> assertThat(lecture.compter(TRACES_DE_LA_TRANCHE, tranche.getId())).as("tracés").isZero());
    }

    // ------------------------------------------------------------------ §2 nominal

    @Test
    void r2_nominal_creeLePlanDeLEtage_codeEtDescriptionEgauxAuCodeDeLEtage() throws Exception {
        importer(nominal()).andExpect(status().is2xxSuccessful());

        Tuple plan = seulPlan();
        assertAll(
                () -> assertThat(plan.get(CODE)).isEqualTo(etage.getCode()),
                () -> assertThat(plan.get(DESCRIPTION)).isEqualTo(etage.getCode()),
                () -> assertThat(guid(plan.get("StkFloorId"))).isEqualTo(guid(etage.getId())),
                () -> assertThat(decimal(plan.get("width"))).isEqualByComparingTo("1312.5979"),
                () -> assertThat(decimal(plan.get("height"))).isEqualByComparingTo("928.15833"),
                () -> assertThat(decimal(plan.get("X"))).isEqualByComparingTo("73.34"),
                () -> assertThat(decimal(plan.get("Y"))).isEqualByComparingTo("22.7"),
                () -> assertThat(plan.get(PLAN_IMAGE)).isEqualTo(FixturePlans.dataUriPng()),
                () -> assertThat(plan.get("UserCreateDateTime")).isNotNull());
    }

    @Test
    void r2_nominal_creeUnTraceParPath_surLEtageChoisi() throws Exception {
        importer(nominal()).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(tracesDe(articleA)).singleElement().satisfies(t -> {
                    assertThat(t.get(SVG_PATH)).isEqualTo(D_A);
                    assertThat(guid(t.get("StkFloorId"))).isEqualTo(guid(etage.getId()));
                }),
                () -> assertThat(tracesDe(articleB)).singleElement()
                        .satisfies(t -> assertThat(t.get(SVG_PATH)).isEqualTo(D_B)));
    }

    @Test
    void r2_idEnMinusculesAvecSouligne_retrouveLeCodeEnMajusculesAvecEspace() throws Exception {
        StkItem espace = plans.article(tranche, "LOT " + jeton + " C", false, etage);
        String id = ("lot_" + jeton + "_c").toLowerCase(Locale.ROOT);

        importer(svg(imagePng(), trace(id, D_A))).andExpect(status().is2xxSuccessful());

        assertThat(tracesDe(espace)).singleElement().satisfies(t -> assertThat(t.get(SVG_PATH)).isEqualTo(D_A));
    }

    @Test
    void piege5_codeDUnSousArticleDeLaTranche_retrouve() throws Exception {
        StkItem sous = plans.article(tranche, "SA-" + jeton, true, etage);

        importer(svg(imagePng(), trace(sous.getCode(), D_A))).andExpect(status().is2xxSuccessful());

        assertThat(tracesDe(sous)).hasSize(1);
    }

    @Test
    void r2_codeIntrouvable_ligneDuBilan_lesAutresTracesSontImportes() throws Exception {
        String corps = corps(importer(svg(imagePng(), trace("INCONNU-" + jeton, D_B), trace(articleA.getCode(), D_A)))
                .andExpect(status().is2xxSuccessful()));

        assertAll(
                () -> assertThat(corps).contains(INTROUVABLE),
                () -> assertThat(tracesDe(articleA)).hasSize(1),
                () -> assertThat(lecture.compter(TRACES_DE_LA_TRANCHE, tranche.getId())).isEqualTo(1));
    }

    @Test
    void r2_codeDUnArticleDUneAutreTranche_introuvable() throws Exception {
        CfgTranche autre = plans.tranche("IMP-X-" + jeton, true);
        StkItem etranger = plans.article(autre, "X-" + jeton, false, etage);

        String corps = corps(importer(svg(imagePng(), trace(etranger.getCode(), D_B), trace(articleA.getCode(), D_A)))
                .andExpect(status().is2xxSuccessful()));

        assertAll(
                () -> assertThat(corps).contains(INTROUVABLE),
                () -> assertThat(tracesDe(etranger)).isEmpty());
    }

    // ------------------------------------------------------------------ piège 5 : apercu, mise à jour

    @Test
    void piege5_apercu_rendLeBilanSansRienEcrire() throws Exception {
        MockMultipartHttpServletRequestBuilder requete = requete(nominal().getBytes(StandardCharsets.UTF_8));
        requete.param("apercu", "true");

        mockMvc.perform(requete).andExpect(status().is2xxSuccessful());

        aucuneEcriture();
    }

    @Test
    void reponse5_planExistant_misAJour_codeEtDescriptionGardes() throws Exception {
        String ancienneImage = FixturePlans.PREFIXE_JPEG + Base64.getEncoder().encodeToString(FixturePlans.jpeg());
        UUID existant = plans.plan(tranche.getId(), etage.getId(), ANCIEN_CODE, ancienneImage);

        importer(svg(image("500.5", "400.25", "xlink:href", FixturePlans.dataUriPng()), trace(articleA.getCode(), D_A)))
                .andExpect(status().is2xxSuccessful());

        Tuple plan = seulPlan();
        assertAll(
                () -> assertThat(guid(plan.get(PKEY))).isEqualTo(guid(existant)),
                () -> assertThat(plan.get(CODE)).isEqualTo(ANCIEN_CODE),
                () -> assertThat(plan.get(DESCRIPTION)).isEqualTo(ANCIEN_CODE),
                () -> assertThat(decimal(plan.get("width"))).isEqualByComparingTo("500.5"),
                () -> assertThat(decimal(plan.get("height"))).isEqualByComparingTo("400.25"),
                () -> assertThat(plan.get(PLAN_IMAGE)).isEqualTo(FixturePlans.dataUriPng()));
    }

    @Test
    void r2_traceExistantDifferent_misAJourSurPlace() throws Exception {
        UUID existant = plans.trace(articleA.getId(), etage.getId(), "M 0,0 h 1");

        importer(nominal()).andExpect(status().is2xxSuccessful());

        assertThat(tracesDe(articleA)).singleElement().satisfies(t -> {
            assertThat(guid(t.get(PKEY))).isEqualTo(guid(existant));
            assertThat(t.get(SVG_PATH)).isEqualTo(D_A);
        });
    }

    @Test
    void r2_traceIdentique_inchange_bilanNAPasEteModifie() throws Exception {
        UUID existant = plans.trace(articleA.getId(), etage.getId(), D_A);
        plans.plan(tranche.getId(), etage.getId(), etage.getCode(), FixturePlans.dataUriPng());

        String corps = corps(importer(svg(imagePng(), trace(articleA.getCode(), D_A)))
                .andExpect(status().is2xxSuccessful()));

        assertAll(
                () -> assertThat(corps).contains("n'a pas été modifié"),
                () -> assertThat(tracesDe(articleA)).singleElement().satisfies(t -> {
                    assertThat(guid(t.get(PKEY))).isEqualTo(guid(existant));
                    assertThat(t.get(SVG_PATH)).isEqualTo(D_A);
                }));
    }

    // ------------------------------------------------------------------ §2 messages exacts (refus du fichier)

    private void refuseAvecMessage(String svg, String message) throws Exception {
        String corps = corps(importer(svg).andExpect(status().is4xxClientError()));

        assertThat(corps).contains(message);
        aucuneEcriture();
    }

    @Test
    void r2_aucunPath_aucuneSegmentation() throws Exception {
        refuseAvecMessage(svg(imagePng()), "Votre fichier ne contient aucune segmentation !");
    }

    @Test
    void r2_aucuneImage_pasUneImageDePlan() throws Exception {
        refuseAvecMessage(svg(trace(articleA.getCode(), D_A)), "Votre fichier ne contient pas une image de plan !");
    }

    @Test
    void r2_largeurNulle_largeurOuHauteurIncorrecte() throws Exception {
        refuseAvecMessage(svg(image("0", "928.15833", "xlink:href", FixturePlans.dataUriPng()),
                trace(articleA.getCode(), D_A)), "La valeur de la largeur et/ou de la hauteur incorrecte !");
    }

    @Test
    void r2_memeIdSurDeuxPaths_segmentAPlusieursArticles() throws Exception {
        refuseAvecMessage(svg(imagePng(), trace(articleA.getCode(), D_A), trace(articleA.getCode(), D_B)),
                "Il existe dans ce fichier une affectation d'un segment à plusieur articles !");
    }

    // ------------------------------------------------------------------ Q6 / réponses : sécurité de l'import

    private void refuse400(String svg) throws Exception {
        importer(svg).andExpect(status().isBadRequest());
        aucuneEcriture();
    }

    @Test
    void q6_entiteExterne_400_sansLireLeFichierCible() throws Exception {
        String marqueur = "SECRET-XXE-" + jeton;
        Path cible = Files.writeString(dossierTemporaire.resolve("cible.txt"), marqueur);
        String svg = svgAvecPrologue("<!DOCTYPE svg [<!ENTITY xxe SYSTEM \"" + cible.toUri() + "\">]>\n",
                imagePng(), trace(articleA.getCode(), "&xxe;"));

        String corps = corps(importer(svg).andExpect(status().isBadRequest()));

        assertThat(corps).doesNotContain(marqueur);
        aucuneEcriture();
    }

    @Test
    void q6_doctypeSeul_400() throws Exception {
        refuse400(svgAvecPrologue("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" "
                + "\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n", nominalElements()));
    }

    @Test
    void q6_entitesInternesImbriquees_billionLaughs_400() throws Exception {
        StringBuilder dtd = new StringBuilder("<!DOCTYPE svg [<!ENTITY lol0 \"lol\">");
        for (int i = 1; i <= 9; i++) {
            String precedent = "&lol" + (i - 1) + ";";
            dtd.append("<!ENTITY lol").append(i).append(" \"").append(precedent.repeat(10)).append("\">");
        }
        dtd.append("]>\n");

        refuse400(svgAvecPrologue(dtd.toString(), imagePng(), trace(articleA.getCode(), "&lol9;")));
    }

    @Test
    void q6_xinclude_400() throws Exception {
        Path cible = Files.writeString(dossierTemporaire.resolve("inclus.xml"), "<path id=\"X\" d=\"M0 0\"/>");
        String inclusion = "<xi:include xmlns:xi=\"http://www.w3.org/2001/XInclude\" href=\"" + cible.toUri() + "\"/>";

        refuse400(svg(imagePng(), trace(articleA.getCode(), D_A), inclusion));
    }

    private String[] nominalElements() {
        return new String[] {imagePng(), trace(articleA.getCode(), D_A)};
    }

    @ParameterizedTest(name = "href = {0}")
    @ValueSource(strings = {"http://exemple.test/plan.png", "file:///C:/Windows/win.ini", "plan.png",
            "data:image/gif;base64,R0lGODlhAQABAAAAACw=", "data:image/svg+xml;base64,PHN2Zy8+"})
    void q6_imageNonDataUriPngOuJpeg_400(String href) throws Exception {
        refuse400(svg(image(LARGEUR_SVG, HAUTEUR_SVG, XLINK, href), trace(articleA.getCode(), D_A)));
    }

    @Test
    void q6_dataUriPngDontLesOctetsNeSontPasUnPng_400() throws Exception {
        String faux = FixturePlans.PREFIXE_PNG
                + Base64.getEncoder().encodeToString("GIF89a pas un png".getBytes(StandardCharsets.US_ASCII));

        refuse400(svg(image(LARGEUR_SVG, HAUTEUR_SVG, XLINK, faux), trace(articleA.getCode(), D_A)));
    }

    @Test
    void recalage_hrefSansEspaceDeNoms_svg2_accepte() throws Exception {
        importer(svg(image("1312.5979", "928.15833", "href", FixturePlans.dataUriPng()), trace(articleA.getCode(), D_A)))
                .andExpect(status().is2xxSuccessful());

        assertThat(seulPlan().get(PLAN_IMAGE)).isEqualTo(FixturePlans.dataUriPng());
    }

    @Test
    void q6_imageJpeg_acceptee() throws Exception {
        String jpeg = FixturePlans.PREFIXE_JPEG + Base64.getEncoder().encodeToString(FixturePlans.jpeg());

        importer(svg(image(LARGEUR_SVG, HAUTEUR_SVG, XLINK, jpeg), trace(articleA.getCode(), D_A)))
                .andExpect(status().is2xxSuccessful());

        assertThat(seulPlan().get(PLAN_IMAGE)).isEqualTo(jpeg);
    }

    @ParameterizedTest(name = "d = {0}")
    @ValueSource(strings = {"M0 0 L10 10 url(javascript:alert(1))", "M0 0 L10 10 Z; onload=alert(1)", "M0 0 #x"})
    void q6_dHorsGrammaire_400(String d) throws Exception {
        refuse400(svg(imagePng(), trace(articleA.getCode(), D_A), trace(articleB.getCode(), d)));
    }

    @Test
    void q6_fichierDePlusDe50Mo_413() throws Exception {
        byte[] gros = new byte[(int) CINQUANTE_MO + 1];
        Arrays.fill(gros, (byte) ' ');
        byte[] debut = nominal().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(debut, 0, gros, 0, debut.length);

        mockMvc.perform(requete(gros)).andExpect(status().isPayloadTooLarge());

        aucuneEcriture();
    }

    @Test
    void q6_dRespectantLaGrammaire_accepte() throws Exception {
        importer(svg(imagePng(), trace(articleA.getCode(), "M 1.5e2,-3 c 1,2 3,4 5,6 s 1 2 3 4 q 1 1 2 2 t 3 3 "
                + "a 5 5 0 0 1 10 10 H 1 V 2 l 1,1 Z")))
                .andExpect(status().is2xxSuccessful());

        assertThat(tracesDe(articleA)).hasSize(1);
    }
}

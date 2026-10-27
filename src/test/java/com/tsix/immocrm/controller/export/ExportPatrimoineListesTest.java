package com.tsix.immocrm.controller.export;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.web.export.ExportTableur;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.LectureExport.codes;
import static com.tsix.immocrm.fixtures.LectureExport.codesDesLignes;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.lignesXlsx;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9c §3.1 (GEN0012) — FEATURE-patrimoine-import-export.md, règles E1/E2 : {@code GET <liste>/export?format=xlsx|pdf}
 * rend les lignes de la liste sous les mêmes filtres et le même périmètre R4 (la ligne d'une entité inactive est absente
 * de la liste, donc de l'export), autre format → 400, même droit que la liste ; {@code ExportTableur} inchangé ailleurs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExportPatrimoineListesTest {

    private static final String PROJETS = "/api/projets";
    private static final String TRANCHES = "/api/tranches";
    private static final String ARTICLES = "/api/articles";
    private static final String SOUS_ARTICLES = "/api/sous-articles";
    private static final String TERRAINS = "/api/terrains";
    private static final String LOTISSEMENTS = "/api/lotissements";
    private static final String PROSPECTIONS_TERRAIN = "/api/prospections-terrain";
    private static final String ETUDES = "/api/etudes-faisabilite";
    private static final String ENTITES = "/api/entites-juridiques";
    private static final String EXPORT = "/export";
    private static final String FORMAT = "format";
    private static final String XLSX = "xlsx";
    private static final String PDF = "pdf";
    private static final String SEARCH = "search";
    private static final String COLONNES = "colonnes";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureExportPatrimoine articles;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureProspectionsTerrain prospections;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private CfgCompany active;
    private CfgCompany inactive;
    private CfgTranche trancheActive;
    private CfgTranche trancheInactive;

    /** Une liste, ses filtres, le code d'une ligne qu'elle rend et celui d'une ligne qu'elle écarte (filtre ou R4). */
    private record Cas(String liste, MultiValueMap<String, String> filtres, String present, String absent) {
    }

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = FixtureExportPatrimoine.jeton();
        active = fixture.entite("ENT-A-" + jeton, true);
        inactive = fixture.entite("ENT-I-" + jeton, false);
        trancheActive = fixture.tranche("TR-A-" + jeton, fixture.projet("PRJ-A-" + jeton, active));
        trancheInactive = fixture.tranche("TR-I-" + jeton, fixture.projet("PRJ-I-" + jeton, inactive));
    }

    // ------------------------------------------------------------------ E1 : xlsx = les lignes de la liste

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, SOUS_ARTICLES, TERRAINS, LOTISSEMENTS, PROSPECTIONS_TERRAIN,
            ETUDES, ENTITES})
    void e1_xlsx_memesLignesQueLaListeFiltree_ligneR4Absente(String liste) throws Exception {
        Cas cas = cas(liste);
        List<String> attendus = codesDeLaListe(cas, connecte(admin));

        MvcResult resultat = exporter(cas, XLSX, connecte(admin));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getContentType()).isEqualTo(ExportTableur.XLSX.toString());
        List<String> candidats = new ArrayList<>(attendus);
        candidats.add(cas.absent());
        assertThat(codesDesLignes(lignesXlsx(resultat.getResponse().getContentAsByteArray()), candidats))
                .containsExactlyInAnyOrderElementsOf(attendus);
    }

    // ------------------------------------------------------------------ E1 : pdf lisible, mêmes lignes

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, SOUS_ARTICLES, TERRAINS, LOTISSEMENTS, PROSPECTIONS_TERRAIN,
            ETUDES, ENTITES})
    void e1_pdf_lisible_memesLignesQueLaListeFiltree_ligneR4Absente(String liste) throws Exception {
        Cas cas = cas(liste);
        List<String> attendus = codesDeLaListe(cas, connecte(admin));

        MvcResult resultat = exporter(cas, PDF, connecte(admin));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE);
        byte[] contenu = resultat.getResponse().getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        String texte = compact(textePdf(contenu));
        assertThat(attendus).isNotEmpty().allSatisfy(code -> assertThat(texte).contains(code));
        assertThat(texte).doesNotContain(cas.absent());
    }

    // ------------------------------------------------------------------ E1 : colonnes dans l'ordre reçu

    @Test
    void e1_articles_xlsx_colonnesDansLOrdreRecu() throws Exception {
        FixtureExportPatrimoine.Referentiel r = articles.referentiel(jeton);
        StkItem article = articles.article(r, "ART-A-" + jeton, trancheActive, null);
        articles.article(r, "ART-I-" + jeton, trancheInactive, null);
        relire();

        MvcResult resultat = executer(mockMvc, get(ARTICLES + EXPORT).with(connecte(admin))
                .param(FORMAT, XLSX).param(SEARCH, jeton).param(COLONNES, "description,code"));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(lignesXlsx(resultat.getResponse().getContentAsByteArray())).containsExactly(
                List.of(ColonneArticle.DESCRIPTION.libelle(), ColonneArticle.CODE.libelle()),
                List.of(article.getDescription(), article.getCode()));
    }

    @Test
    void e1_tranches_xlsx_colonnesDansLOrdreRecu() throws Exception {
        relire();

        MvcResult resultat = executer(mockMvc, get(TRANCHES + EXPORT).with(connecte(admin))
                .param(FORMAT, XLSX).param(SEARCH, jeton).param(COLONNES, "description,code"));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(lignesXlsx(resultat.getResponse().getContentAsByteArray())).containsExactly(
                List.of(ColonneTranche.DESCRIPTION.libelle(), ColonneTranche.CODE.libelle()),
                List.of(trancheActive.getDescription(), trancheActive.getCode()));
    }

    // ------------------------------------------------------------------ E2 : format inconnu

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJETS, TRANCHES, ARTICLES, SOUS_ARTICLES, TERRAINS, LOTISSEMENTS, PROSPECTIONS_TERRAIN,
            ETUDES, ENTITES})
    void e2_formatInconnu_400NommantLeFormat(String liste) throws Exception {
        MvcResult resultat = executer(mockMvc, get(liste + EXPORT).with(connecte(admin)).param(FORMAT, "csv"));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultat.getResponse().getContentAsString()).contains(FORMAT);
    }

    // ------------------------------------------------------------------ droit = celui de la liste

    @Test
    void droit_etudes_exportReserveCommeLaListe_commercial403() throws Exception {
        RequestPostProcessor commercial = FixtureExportPatrimoine.commercial(fixture.administrateur());
        mockMvc.perform(get(ETUDES).with(commercial)).andExpect(status().isForbidden());

        MvcResult resultat = executer(mockMvc, get(ETUDES + EXPORT).with(commercial).param(FORMAT, XLSX));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(403);
    }

    /** Décision manager 24/10 : la liste des entités n'a pas de @PreAuthorize, l'export non plus. */
    @Test
    void droit_entites_exportOuvertCommeLaListe_commercial200() throws Exception {
        RequestPostProcessor commercial = FixtureExportPatrimoine.commercial(fixture.administrateur());
        relire();
        mockMvc.perform(get(ENTITES).with(commercial).param(SEARCH, jeton)).andExpect(status().isOk());

        MvcResult resultat = executer(mockMvc, get(ENTITES + EXPORT).with(commercial)
                .param(FORMAT, XLSX).param(SEARCH, jeton));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ ExportTableur inchangé pour la Vente

    @Test
    void nonRegression_exportVente_xlsxInchange() throws Exception {
        MvcResult resultat = executer(mockMvc, get("/api/prospects/export").with(connecte(admin))
                .param(FORMAT, XLSX).param(COLONNES, "code").param(SEARCH, jeton));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getContentType()).isEqualTo(ExportTableur.XLSX.toString());
        assertThat(lignesXlsx(resultat.getResponse().getContentAsByteArray()))
                .containsExactly(List.of(ColonneProspect.CODE.libelle()));
    }

    /** §3.1 : {@code exigerXlsx} reste tel quel pour les autres écrans (le PDF n'est ouvert qu'aux listes du lot). */
    @Test
    void nonRegression_exportVente_pdfToujoursRefuse400() throws Exception {
        MvcResult resultat = executer(mockMvc, get("/api/prospects/export").with(connecte(admin)).param(FORMAT, PDF));

        assertThat(resultat.getResponse().getStatus()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ jeux de données par liste

    private Cas cas(String liste) throws Exception {
        Cas cas = switch (liste) {
            case PROJETS -> new Cas(PROJETS, filtres(SEARCH, jeton), "PRJ-A-" + jeton, "PRJ-I-" + jeton);
            case TRANCHES -> new Cas(TRANCHES, filtres(SEARCH, jeton), trancheActive.getCode(), trancheInactive.getCode());
            case ARTICLES -> articles();
            case SOUS_ARTICLES -> sousArticles();
            case TERRAINS -> terrains();
            case LOTISSEMENTS -> lotissements();
            case PROSPECTIONS_TERRAIN -> prospectionsTerrain();
            case ETUDES -> etudes();
            case ENTITES -> entitesJuridiques();
            default -> throw new IllegalArgumentException(liste);
        };
        relire();
        return cas;
    }

    private Cas articles() {
        FixtureExportPatrimoine.Referentiel r = articles.referentiel(jeton);
        String present = articles.article(r, "ART-A-" + jeton, trancheActive, null).getCode();
        String absent = articles.article(r, "ART-I-" + jeton, trancheInactive, null).getCode();
        MultiValueMap<String, String> filtres = filtres(SEARCH, jeton);
        filtres.add(COLONNES, "code,description");
        return new Cas(ARTICLES, filtres, present, absent);
    }

    private Cas sousArticles() {
        FixtureExportPatrimoine.Referentiel r = articles.referentiel(jeton);
        String present = articles.sousArticle(r, "SA-A-" + jeton, trancheActive, null).getCode();
        String absent = articles.sousArticle(r, "SA-I-" + jeton, trancheInactive, null).getCode();
        MultiValueMap<String, String> filtres = filtres(SEARCH, jeton);
        filtres.add(COLONNES, "code,description");
        return new Cas(SOUS_ARTICLES, filtres, present, absent);
    }

    private Cas terrains() {
        FixtureTerrains.Contexte c = terrains.contexte("P9C");
        terrains.terrain(c, "TER-A-" + jeton);
        terrains.terrain(inactif(c), "TER-I-" + jeton);
        return new Cas(TERRAINS, filtres(SEARCH, jeton), "TER-A-" + jeton, "TER-I-" + jeton);
    }

    private Cas lotissements() {
        FixtureTerrains.Contexte c = terrains.contexte("P9C");
        UUID vente = fixture.vocationVente().getId();
        UUID present = terrains.terrain(c, "LOT-A-" + jeton, trancheActive.getId(), vente);
        // FEATURE-patrimoine-lotissements §1.1 : un lot porte l'entité « article », pas LandProject.
        fixture.entiteStkItem();
        terrains.definir(present, "StrEntityId", FixturePatrimoine.ENTITE_STK_ITEM);
        terrains.terrain(inactif(c), "LOT-I-" + jeton, trancheInactive.getId(), vente);
        return new Cas(LOTISSEMENTS, filtres(SEARCH, jeton), "LOT-A-" + jeton, "LOT-I-" + jeton);
    }

    /** Même contexte de référentiels, société inactive. */
    private FixtureTerrains.Contexte inactif(FixtureTerrains.Contexte c) {
        return new FixtureTerrains.Contexte(c.jeton(), inactive, c.pays(), c.ville(), c.natureTer(), c.categorie(),
                c.type());
    }

    /** Prospections terrain : pas d'entité, la ligne écartée l'est par le filtre {@code archivee=false}. */
    private Cas prospectionsTerrain() {
        FixtureProspectionsTerrain.Contexte c = prospections.contexte("P9C");
        prospections.prospection(c, "PT-A-" + jeton);
        UUID archivee = prospections.prospection(c, "PT-X-" + jeton);
        prospections.definir(archivee, "IsInArchive", true);
        MultiValueMap<String, String> filtres = filtres(SEARCH, jeton);
        filtres.add("archivee", "false");
        return new Cas(PROSPECTIONS_TERRAIN, filtres, "PT-A-" + jeton, "PT-X-" + jeton);
    }

    private Cas etudes() throws Exception {
        faisabilite.parametrage();
        ApiFaisabilite api = new ApiFaisabilite(mockMvc, admin);
        api.etude(FixtureFaisabilite.fiche("ETU-A-" + jeton, active.getId()));
        CfgCompany aEteindre = fixture.entite("ENT-E-" + jeton, true);
        api.etude(FixtureFaisabilite.fiche("ETU-I-" + jeton, aEteindre.getId()));
        entites.actif(aEteindre.getId(), false);
        return new Cas(ETUDES, filtres(SEARCH, jeton), "ETU-A-" + jeton, "ETU-I-" + jeton);
    }

    /** Entités : la liste rend actives et inactives ; la ligne écartée l'est par le filtre {@code actif=true}. */
    private Cas entitesJuridiques() {
        MultiValueMap<String, String> filtres = filtres(SEARCH, jeton);
        filtres.add("actif", "true");
        return new Cas(ENTITES, filtres, active.getCode(), inactive.getCode());
    }

    // ------------------------------------------------------------------ outils

    private static MultiValueMap<String, String> filtres(String nom, String valeur) {
        MultiValueMap<String, String> filtres = new LinkedMultiValueMap<>();
        filtres.add(nom, valeur);
        return filtres;
    }

    /** Codes rendus par la liste elle-même (comportement existant) : la ligne retenue y est, l'écartée non. */
    private List<String> codesDeLaListe(Cas cas, RequestPostProcessor qui) throws Exception {
        String json = mockMvc.perform(get(cas.liste()).with(qui).params(cas.filtres()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> codes = codes(json);
        assertThat(codes).contains(cas.present()).doesNotContain(cas.absent());
        return codes;
    }

    private MvcResult exporter(Cas cas, String format, RequestPostProcessor qui) throws Exception {
        MockHttpServletRequestBuilder requete = get(cas.liste() + EXPORT).with(qui).params(cas.filtres())
                .param(FORMAT, format);
        return executer(mockMvc, requete);
    }

    private void relire() {
        em.flush();
        em.clear();
    }
}

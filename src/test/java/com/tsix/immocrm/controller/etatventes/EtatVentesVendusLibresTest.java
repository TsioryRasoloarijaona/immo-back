package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/etat-des-ventes/synthese-vendus-libres} (+ {@code /export}) et filtres {@code famille}/{@code etat}
 * de la liste — FEATURE-tdb-vente-vendus-libres.md §3 (R1–R6), §4, §6. Chaque test filtre sur ses propres tranches.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesVendusLibresTest {

    private static final String URL = "/api/etat-des-ventes/synthese-vendus-libres";
    private static final String URL_EXPORT = URL + "/export";
    private static final String URL_LISTE = "/api/etat-des-ventes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String FAMILLE = "famille";
    private static final String ETAT = "etat";
    private static final String FORMAT = "format";
    private static final String COMMERCIAL = "Commercial";
    private static final String TOTAL_ELEMENTS = "$.totalElements";

    private static final String APPARTEMENT = "APPARTEMENT";
    private static final String PARKING = "PARKING";
    private static final String MAGASIN = "MAGASIN";
    private static final String AUTRE = "AUTRE";

    private static final String LIBRES = ".libres";
    private static final String RESERVES = ".reserves";
    private static final String VENDUS = ".vendus";
    private static final String SANS_STATUT = ".sansStatut";
    private static final String TOTAL = ".total";
    private static final String PCT_LIBRES = ".pctLibres";
    private static final String PCT_RESERVES = ".pctReserves";
    private static final String PCT_VENDUS = ".pctVendus";
    private static final String GENERAL = "$.general";
    private static final String LIGNES = "$.lignes";
    private static final String FAMILLE_CODE = ".familleCode";
    private static final String FAMILLES_CODES = "$.familles[*].code";

    private static final List<String> COMPTES = List.of("libres", "reserves", "vendus", "sansStatut", "total");
    private static final String FEUILLE = "Libres Réservés Vendus";
    private static final List<String> ENTETES = List.of("Tranche", "Famille", "Libres", "Réservés", "Vendus", "Total",
            "% Libres", "% Réservés", "% Vendus");
    private static final int PREMIERE_COLONNE_NOMBRE = 2;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory app;
    private StkItemCategory par;
    private StkItemCategory com;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVF");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        app = fixture.categorie("APP");
        par = fixture.categorie("PAR");
        com = fixture.categorie("COM");
        type = fixture.type("T3-" + prefixe);
    }

    // ------------------------------------------------------------------ outils

    private StkItem lot(String suffixe, StkItemCategory categorie) {
        return lot(tranche, suffixe, categorie);
    }

    private StkItem lot(CfgTranche t, String suffixe, StkItemCategory categorie) {
        return fixture.article(prefixe + suffixe, t, categorie, type);
    }

    private void vendre(StkItem article, StrStatus statut) {
        fixture.vendre(article, "DOS-" + article.getCode(), statut, null, LocalDateTime.of(2025, 3, 15, 10, 0));
    }

    private void vendre(StkItem article, StatutDossier statut) {
        vendre(article, fixture.statut(statut));
    }

    private void vendreSansStatut(StkItem article) {
        vendre(article, (StrStatus) null);
    }

    private static String ligne(CfgTranche t, String famille) {
        return ligne(t.getId().toString(), famille);
    }

    private static String ligne(String trancheId, String famille) {
        return "$.lignes[?(@.trancheId=='" + trancheId + "' && @.familleCode=='" + famille + "')]";
    }

    private static String lignesDe(CfgTranche t) {
        return lignesDe(t.getId().toString());
    }

    private static String lignesDe(String trancheId) {
        return "$.lignes[?(@.trancheId=='" + trancheId + "')]";
    }

    private static String totalTranche(CfgTranche t) {
        return totalTranche(t.getId().toString());
    }

    private static String totalTranche(String trancheId) {
        return "$.totalTranche[?(@.trancheId=='" + trancheId + "')]";
    }

    private static String totalFamille(String famille) {
        return "$.totauxFamille[?(@.familleCode=='" + famille + "')]";
    }

    private static String[] ids(CfgTranche... tranches) {
        return Arrays.stream(tranches).map(t -> t.getId().toString()).toArray(String[]::new);
    }

    private MockHttpServletRequestBuilder synthese(CfgTranche... tranches) {
        return get(URL).param(TRANCHE_ID, ids(tranches));
    }

    private MockHttpServletRequestBuilder export(CfgTranche... tranches) {
        return get(URL_EXPORT).param(TRANCHE_ID, ids(tranches)).param(FORMAT, "xlsx");
    }

    private MockHttpServletRequestBuilder liste(String... familles) {
        return get(URL_LISTE).param(TRANCHE_ID, tranche.getId().toString()).param(FAMILLE, familles);
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long totalElements(MockHttpServletRequestBuilder requete) throws Exception {
        return ((Number) JsonPath.read(corps(requete), TOTAL_ELEMENTS)).longValue();
    }

    /** Valeur numérique d'un chemin JSON ; un filtre {@code [?()]} doit désigner exactement un élément. */
    private static double nombre(String corps, String chemin) {
        Object valeur = JsonPath.read(corps, chemin);
        if (valeur instanceof List<?> liste) {
            assertThat(liste).as(chemin).hasSize(1);
            valeur = liste.getFirst();
        }
        return ((Number) valeur).doubleValue();
    }

    private static long somme(String corps, String chemin) {
        List<Number> valeurs = JsonPath.read(corps, chemin);
        return valeurs.stream().mapToLong(Number::longValue).sum();
    }

    private MockHttpServletResponse reponseExport(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete).andExpect(request().asyncStarted()).andReturn();
        return mockMvc.perform(asyncDispatch(asynchrone)).andExpect(status().isOk()).andReturn().getResponse();
    }

    /** Lignes de la première feuille (ou de {@code feuille}) : texte, nombre ({@code Double}) ou {@code null}. */
    private List<List<Object>> lignesClasseur(MockHttpServletRequestBuilder requete, String feuille) throws Exception {
        byte[] contenu = reponseExport(requete).getContentAsByteArray();
        List<List<Object>> lignes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet sheet = feuille == null ? classeur.getSheetAt(0) : classeur.getSheet(feuille);
            assertThat(sheet).as("feuille %s", feuille).isNotNull();
            for (Row row : sheet) {
                List<Object> cellules = new ArrayList<>();
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    cellules.add(valeur(row.getCell(i)));
                }
                lignes.add(cellules);
            }
        }
        return lignes;
    }

    private static Object valeur(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        return cell.toString();
    }

    /** Jeu mixte de la tranche du test : APP 2 libres, 2 réservés (dont 1 sans statut), 1 vendu ; PAR 1 libre, 1 vendu. */
    private void jeuMixte() {
        lot("-A1", app);
        vendre(lot("-A2", app), StatutDossier.RESERVATION);
        vendreSansStatut(lot("-A3", app));
        vendre(lot("-A4", app), StatutDossier.CONCRETISATION);
        vendre(lot("-A5", app), StatutDossier.ANNULATION);
        fixture.parkingAutonome(prefixe + "-P1", tranche, par, type);
        vendre(lot("-P2", par), StatutDossier.FORMALISE);
    }

    // ------------------------------------------------------------------ R6 — refus de visibilité

    private CfgTranche trancheDUnAutreProjet() {
        return fixture.tranche("TRB-" + prefixe, fixture.projet("PRJB-" + prefixe, true), true);
    }

    /** Commercial à périmètre restreint au projet du test (tranche A). */
    private AdmUser commercialDuProjet() {
        AdmUser commercial = fixtureDossier.compte("edvf.com." + prefixe);
        fixture.affecter(commercial, projet);
        return commercial;
    }

    @Test
    void r6_commercialRestreintSurA_neVoitNiLaTrancheBNiSesLots() throws Exception {
        CfgTranche trancheB = trancheDUnAutreProjet();
        lot("-A1", app);
        vendre(lot("-A2", app), StatutDossier.RESERVATION);
        vendre(lot(trancheB, "-B1", app), StatutDossier.CONCRETISATION);
        lot(trancheB, "-B2", par);
        AdmUser commercial = commercialDuProjet();
        fixture.relire();

        mockMvc.perform(get(URL).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(lignesDe(trancheB)).doesNotExist())
                .andExpect(jsonPath(totalTranche(trancheB)).doesNotExist())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + TOTAL).value(2))
                .andExpect(jsonPath(totalFamille(APPARTEMENT) + VENDUS).value(0))
                .andExpect(jsonPath(GENERAL + TOTAL).value(2))
                .andExpect(jsonPath(GENERAL + VENDUS).value(0));
    }

    @Test
    void r6_trancheHorsPerimetreDemandeeExplicitement_aucuneLigne_pas403() throws Exception {
        CfgTranche trancheB = trancheDUnAutreProjet();
        lot(trancheB, "-B1", app);
        AdmUser commercial = commercialDuProjet();
        fixture.relire();

        mockMvc.perform(synthese(trancheB).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES).isEmpty())
                .andExpect(jsonPath(GENERAL + TOTAL).value(0));
    }

    @Test
    void r6_trancheDEntiteInactive_exclue() throws Exception {
        CfgTranche inactive = fixture.tranche("TRI-" + prefixe, fixture.projet("PRJI-" + prefixe, false), true);
        lot(inactive, "-I1", app);
        fixture.relire();

        mockMvc.perform(synthese(inactive))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES).isEmpty())
                .andExpect(jsonPath(GENERAL + TOTAL).value(0));
    }

    @Test
    void r6_compteNonRestreint_voitToutesLesTranches() throws Exception {
        CfgTranche trancheB = trancheDUnAutreProjet();
        lot("-A1", app);
        lot(trancheB, "-B1", par);
        fixture.relire();

        mockMvc.perform(synthese(tranche, trancheB))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + TOTAL).value(1))
                .andExpect(jsonPath(ligne(trancheB, PARKING) + TOTAL).value(1))
                .andExpect(jsonPath(GENERAL + TOTAL).value(2));
    }

    @Test
    void r6_export_commercialRestreint_sansTrancheHorsPerimetre() throws Exception {
        CfgTranche trancheB = trancheDUnAutreProjet();
        lot("-A1", app);
        lot(trancheB, "-B1", app);
        lot(trancheB, "-B2", app);
        AdmUser commercial = commercialDuProjet();
        fixture.relire();

        List<List<Object>> lignes = lignesClasseur(export(tranche, trancheB).with(connecte(commercial, COMMERCIAL)),
                null);

        String texte = lignes.toString();
        assertThat(texte).contains(tranche.getCode()).doesNotContain(trancheB.getCode());
        assertThat(lignes.getLast().get(PREMIERE_COLONNE_NOMBRE + 3)).as("Total général").isEqualTo(1.0);
    }

    // ------------------------------------------------------------------ §4 / §6.7 — 400

    @Test
    void familleInconnue_synthese_rend400() throws Exception {
        mockMvc.perform(get(URL).param(FAMILLE, "VILLA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void familleInconnue_liste_rend400() throws Exception {
        mockMvc.perform(get(URL_LISTE).param(FAMILLE, "VILLA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void etatInconnu_liste_rend400() throws Exception {
        mockMvc.perform(get(URL_LISTE).param(ETAT, "ARCHIVE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void export_formatCsv_rend400() throws Exception {
        mockMvc.perform(get(URL_EXPORT).param(FORMAT, "csv"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R3 — familles

    @Test
    void r3_famillesParCodeDeCategorie_lotJdnEtCodeNull_versAutre() throws Exception {
        lot("-A1", app);
        lot("-P1", par);
        lot("-C1", com);
        lot("-L1", fixture.categorie("LOT"));
        lot("-J1", fixture.categorie("JDN"));
        lot("-N1", fixture.categorie(null));
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + TOTAL).value(1))
                .andExpect(jsonPath(ligne(tranche, PARKING) + TOTAL).value(1))
                .andExpect(jsonPath(ligne(tranche, MAGASIN) + TOTAL).value(1))
                .andExpect(jsonPath(ligne(tranche, AUTRE) + TOTAL).value(3))
                .andExpect(jsonPath(lignesDe(tranche) + FAMILLE_CODE).value(hasSize(4)));
    }

    @ParameterizedTest
    @CsvSource({"APPARTEMENT,1", "PARKING,1", "MAGASIN,1", "AUTRE,2"})
    void r3_familleIsolee_neRendQueSaLigne(String famille, int total) throws Exception {
        lot("-A1", app);
        lot("-P1", par);
        lot("-C1", com);
        lot("-L1", fixture.categorie("LOT"));
        lot("-J1", fixture.categorie("JDN"));
        fixture.relire();

        String corps = corps(synthese(tranche).param(FAMILLE, famille));

        List<String> familles = JsonPath.read(corps, lignesDe(tranche) + FAMILLE_CODE);
        List<String> entetes = JsonPath.read(corps, FAMILLES_CODES);
        assertThat(familles).containsExactly(famille);
        assertThat(entetes).containsExactly(famille);
        assertThat(nombre(corps, ligne(tranche, famille) + TOTAL)).isEqualTo(total);
    }

    @Test
    void r3_familleVide_pasDeLigne() throws Exception {
        lot("-A1", app);
        fixture.relire();

        String corps = corps(synthese(tranche));

        List<String> familles = JsonPath.read(corps, lignesDe(tranche) + FAMILLE_CODE);
        assertThat(familles).containsExactly(APPARTEMENT);
    }

    @Test
    void familles_ordreFixe_libellesFrancais() throws Exception {
        String corps = corps(synthese(tranche));

        List<String> codes = JsonPath.read(corps, FAMILLES_CODES);
        List<String> libelles = JsonPath.read(corps, "$.familles[*].libelle");
        List<Integer> ordres = JsonPath.read(corps, "$.familles[*].ordre");
        assertThat(codes).containsExactly(APPARTEMENT, PARKING, MAGASIN, AUTRE);
        assertThat(libelles).containsExactly("Appartements", "Parkings", "Magasins", "Autres");
        assertThat(ordres).hasSize(4).isSorted().doesNotHaveDuplicates();
    }

    @Test
    void familleMultiple_synthese_neRendQueCesFamilles_dansLOrdreFixe() throws Exception {
        lot("-A1", app);
        lot("-P1", par);
        lot("-C1", com);
        fixture.relire();

        String corps = corps(synthese(tranche).param(FAMILLE, PARKING, APPARTEMENT));

        List<String> entetes = JsonPath.read(corps, FAMILLES_CODES);
        List<String> familles = JsonPath.read(corps, lignesDe(tranche) + FAMILLE_CODE);
        assertThat(entetes).containsExactly(APPARTEMENT, PARKING);
        assertThat(familles).containsExactly(APPARTEMENT, PARKING);
        assertThat(nombre(corps, GENERAL + TOTAL)).isEqualTo(2);
    }

    @Test
    void familleMultiple_liste_additionneLesFamilles() throws Exception {
        lot("-A1", app);
        lot("-A2", app);
        lot("-P1", par);
        lot("-C1", com);
        fixture.relire();

        assertThat(new long[] {
                totalElements(liste(APPARTEMENT)),
                totalElements(liste(APPARTEMENT, MAGASIN)),
                totalElements(liste(PARKING))})
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    void etatMultiple_liste_additionneLesEtats() throws Exception {
        jeuMixte();
        fixture.relire();

        assertThat(totalElements(liste(APPARTEMENT).param(ETAT, "LIBRE", "VENDU"))).isEqualTo(3L);
    }

    // ------------------------------------------------------------------ R2 — états

    @Test
    void r2_sansDossier_libre() throws Exception {
        lot("-A1", app);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + LIBRES).value(1))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + RESERVES).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + TOTAL).value(1));
    }

    @Test
    void r2_dossierReservation_reserve() throws Exception {
        vendre(lot("-A1", app), StatutDossier.RESERVATION);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + LIBRES).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + RESERVES).value(1))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + SANS_STATUT).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(0));
    }

    @Test
    void r2_dossierSansStatut_reserve_etSansStatutUn() throws Exception {
        vendreSansStatut(lot("-A1", app));
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + RESERVES).value(1))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + SANS_STATUT).value(1))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + LIBRES).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(0))
                .andExpect(jsonPath(totalTranche(tranche) + SANS_STATUT).value(1))
                .andExpect(jsonPath(totalFamille(APPARTEMENT) + SANS_STATUT).value(1))
                .andExpect(jsonPath(GENERAL + SANS_STATUT).value(1));
    }

    @Test
    void r2_concretisationFormaliseCompromis_vendu() throws Exception {
        vendre(lot("-A1", app), StatutDossier.CONCRETISATION);
        vendre(lot("-A2", app), StatutDossier.FORMALISE);
        vendre(lot("-A3", app), StatutDossier.COMPROMIS);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(3))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + RESERVES).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + LIBRES).value(0));
    }

    /** Décision du manager : tout statut non-sortie autre que RESERVATION et « sans statut » est Vendu. */
    @Test
    void r2_statutNonSortieHorsEnum_vendu() throws Exception {
        vendre(lot("-A1", app), fixture.statutEnCours("ZNEW-" + prefixe));
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(1))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + RESERVES).value(0));
    }

    @Test
    void r2_dossierAnnuleOuDesiste_libereLeLot() throws Exception {
        vendre(lot("-A1", app), StatutDossier.ANNULATION);
        vendre(lot("-A2", app), StatutDossier.DESISTEMENT);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + LIBRES).value(2))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + RESERVES).value(0))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(0));
    }

    // ------------------------------------------------------------------ R4 — sous-articles

    @Test
    void r4_parkingRattache_compteNullePart() throws Exception {
        StkItem parent = lot("-A1", app);
        fixture.sousArticle(prefixe + "-A1-PK", parent, par, type);
        vendre(parent, StatutDossier.CONCRETISATION);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + VENDUS).value(1))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + TOTAL).value(1))
                .andExpect(jsonPath(ligne(tranche, PARKING)).doesNotExist())
                .andExpect(jsonPath(GENERAL + TOTAL).value(1));
        assertThat(totalElements(liste(PARKING))).isZero();
    }

    @Test
    void r4_parkingAutonome_compteEnParkings() throws Exception {
        fixture.parkingAutonome(prefixe + "-PK", tranche, par, type);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, PARKING) + LIBRES).value(1))
                .andExpect(jsonPath(ligne(tranche, PARKING) + TOTAL).value(1));
    }

    // ------------------------------------------------------------------ R1 — égalité avec la liste

    @Test
    void r1_chaqueCellule_egaleTotalElementsDeLaListeFiltree() throws Exception {
        jeuMixte();
        fixture.relire();

        String corps = corps(synthese(tranche));
        SoftAssertions soft = new SoftAssertions();
        for (String famille : List.of(APPARTEMENT, PARKING)) {
            String ligne = ligne(tranche, famille);
            long libres = (long) nombre(corps, ligne + LIBRES);
            long reserves = (long) nombre(corps, ligne + RESERVES);
            long vendus = (long) nombre(corps, ligne + VENDUS);
            long total = (long) nombre(corps, ligne + TOTAL);
            soft.assertThat(totalElements(liste(famille).param(ETAT, "LIBRE"))).as(famille + " libres")
                    .isEqualTo(libres).isPositive();
            soft.assertThat(totalElements(liste(famille).param(ETAT, "RESERVE"))).as(famille + " réservés")
                    .isEqualTo(reserves);
            soft.assertThat(totalElements(liste(famille).param(ETAT, "VENDU"))).as(famille + " vendus")
                    .isEqualTo(vendus).isPositive();
            soft.assertThat(totalElements(liste(famille))).as(famille + " total").isEqualTo(total);
            soft.assertThat(libres + reserves + vendus).as(famille + " somme").isEqualTo(total);
        }
        soft.assertThat(nombre(corps, ligne(tranche, APPARTEMENT) + RESERVES)).as("APP réservés").isEqualTo(2);
        soft.assertAll();
    }

    @Test
    void r1_sommeDesLignes_egaleTotauxFamille_totalTranche_etGeneral() throws Exception {
        CfgTranche tranche2 = fixture.tranche("TR2-" + prefixe, projet, true);
        jeuMixte();
        vendre(lot(tranche2, "-X1", app), StatutDossier.COMPROMIS);
        lot(tranche2, "-X2", com);
        fixture.relire();

        String corps = corps(synthese(tranche, tranche2));
        SoftAssertions soft = new SoftAssertions();
        for (String champ : COMPTES) {
            for (String famille : List.of(APPARTEMENT, PARKING, MAGASIN)) {
                soft.assertThat(somme(corps, "$.lignes[?(@.familleCode=='" + famille + "')]." + champ))
                        .as(famille + " " + champ).isEqualTo((long) nombre(corps, totalFamille(famille) + "." + champ));
            }
            for (CfgTranche t : List.of(tranche, tranche2)) {
                soft.assertThat(somme(corps, lignesDe(t) + "." + champ))
                        .as(t.getCode() + " " + champ).isEqualTo((long) nombre(corps, totalTranche(t) + "." + champ));
            }
            soft.assertThat(somme(corps, "$.lignes[*]." + champ)).as("général " + champ)
                    .isEqualTo((long) nombre(corps, GENERAL + "." + champ));
        }
        soft.assertThat(nombre(corps, GENERAL + TOTAL)).as("général total").isEqualTo(9);
        soft.assertAll();
    }

    @Test
    void lignes_trieesParCodeDeTranche_puisOrdreDeFamille() throws Exception {
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, projet, true);
        CfgTranche trancheA = fixture.tranche("TRA-" + prefixe, projet, true);
        lot(trancheB, "-B1", com);
        lot(trancheA, "-A1", fixture.categorie("JDN"));
        lot(trancheA, "-A2", par);
        lot(trancheA, "-A3", app);
        fixture.relire();

        String corps = corps(synthese(trancheB, trancheA));

        List<String> tranches = JsonPath.read(corps, "$.lignes[*].trancheId");
        List<String> familles = JsonPath.read(corps, "$.lignes[*].familleCode");
        assertThat(tranches).containsExactly(trancheA.getId().toString(), trancheA.getId().toString(),
                trancheA.getId().toString(), trancheB.getId().toString());
        assertThat(familles).containsExactly(APPARTEMENT, PARKING, AUTRE, MAGASIN);
    }

    @Test
    void ligne_porteCodeEtLibelleDeTranche() throws Exception {
        lot("-A1", app);
        fixture.relire();

        mockMvc.perform(synthese(tranche))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + ".trancheCode").value(tranche.getCode()))
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + ".trancheLibelle").value(tranche.getDescription()));
    }

    @Test
    void filtreProjet_neRendQueLesTranchesDuProjet() throws Exception {
        CfgTranche autre = fixture.tranche("TRD-" + prefixe, fixture.projet("PRJD-" + prefixe, true), true);
        lot("-A1", app);
        lot(autre, "-D1", app);
        fixture.relire();

        mockMvc.perform(get(URL).param("projetId", projet.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, APPARTEMENT) + TOTAL).value(1))
                .andExpect(jsonPath(lignesDe(autre)).doesNotExist())
                .andExpect(jsonPath(GENERAL + TOTAL).value(1));
    }

    // ------------------------------------------------------------------ R5 — pourcentages

    @Test
    void r5_pourcentages_uneDecimale_arrondis() throws Exception {
        lot("-A1", app);
        vendre(lot("-A2", app), StatutDossier.CONCRETISATION);
        vendre(lot("-A3", app), StatutDossier.FORMALISE);
        fixture.relire();

        String corps = corps(synthese(tranche));

        String ligne = ligne(tranche, APPARTEMENT);
        assertThat(new double[] {
                nombre(corps, ligne + PCT_LIBRES), nombre(corps, ligne + PCT_RESERVES), nombre(corps, ligne + PCT_VENDUS),
                nombre(corps, GENERAL + PCT_LIBRES), nombre(corps, GENERAL + PCT_RESERVES),
                nombre(corps, GENERAL + PCT_VENDUS)})
                .containsExactly(33.3, 0.0, 66.7, 33.3, 0.0, 66.7);
    }

    @Test
    void r5_sommeDesTroisPourcentages_procheDeCent() throws Exception {
        lot("-A1", app);
        vendre(lot("-A2", app), StatutDossier.RESERVATION);
        vendre(lot("-A3", app), StatutDossier.CONCRETISATION);
        fixture.relire();

        String corps = corps(synthese(tranche));

        String ligne = ligne(tranche, APPARTEMENT);
        double libres = nombre(corps, ligne + PCT_LIBRES);
        double reserves = nombre(corps, ligne + PCT_RESERVES);
        double vendus = nombre(corps, ligne + PCT_VENDUS);
        assertThat(new double[] {libres, reserves, vendus}).containsExactly(33.3, 33.3, 33.3);
        assertThat(libres + reserves + vendus).isCloseTo(100.0, within(0.2));
    }

    @Test
    void r5_totalZero_pourcentagesZero() throws Exception {
        String corps = corps(synthese(tranche));

        assertThat(new double[] {
                nombre(corps, GENERAL + TOTAL), nombre(corps, GENERAL + PCT_LIBRES),
                nombre(corps, GENERAL + PCT_RESERVES), nombre(corps, GENERAL + PCT_VENDUS)})
                .containsExactly(0.0, 0.0, 0.0, 0.0);
    }

    // ------------------------------------------------------------------ §4.3 — export

    @Test
    void export_rendUnClasseurXlsx() throws Exception {
        lot("-A1", app);
        fixture.relire();

        MockHttpServletResponse reponse = reponseExport(export(tranche));

        assertThat(reponse.getContentType()).startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml");
        assertThat(reponse.getHeader("Content-Disposition")).contains(".xlsx");
    }

    @Test
    void export_feuilleLibresReservesVendus_colonnesDansLOrdre() throws Exception {
        lot("-A1", app);
        fixture.relire();

        List<List<Object>> lignes = lignesClasseur(export(tranche), FEUILLE);

        assertThat(lignes).isNotEmpty();
        assertThat(lignes.getFirst()).containsExactlyElementsOf(ENTETES);
    }

    /** Mêmes lignes, même tri, mêmes valeurs que le JSON : tranche × famille, « Total tranche », « Total général ». */
    @Test
    void export_memesLignesEtTotauxQueLeJson() throws Exception {
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, projet, true);
        jeuMixte();
        vendre(lot(trancheB, "-B1", com), StatutDossier.RESERVATION);
        lot(trancheB, "-B2", app);
        fixture.relire();

        String corps = corps(get(URL).param(TRANCHE_ID, ids(tranche, trancheB)));
        List<List<Object>> lignes = lignesClasseur(export(tranche, trancheB), null);

        List<List<Double>> attendu = new ArrayList<>();
        List<String> tranches = JsonPath.read(corps, "$.lignes[*].trancheId");
        List<String> ordreTranches = tranches.stream().distinct().toList();
        assertThat(ordreTranches).hasSize(2);
        for (String id : ordreTranches) {
            List<String> familles = JsonPath.read(corps, lignesDe(id) + FAMILLE_CODE);
            for (String famille : familles) {
                attendu.add(valeurs(corps, ligne(id, famille)));
            }
            attendu.add(valeurs(corps, totalTranche(id)));
        }
        attendu.add(valeurs(corps, GENERAL));

        List<List<Object>> donnees = lignes.subList(1, lignes.size());
        assertThat(donnees.stream().map(EtatVentesVendusLibresTest::nombresDeLaLigne).toList())
                .containsExactlyElementsOf(attendu);
        assertThat(donnees.get(familles(corps, ordreTranches.getFirst())).toString()).contains("Total tranche");
        assertThat(donnees.getLast().toString()).contains("Total général");
    }

    private static int familles(String corps, String trancheId) {
        List<String> familles = JsonPath.read(corps, lignesDe(trancheId) + FAMILLE_CODE);
        return familles.size();
    }

    private static List<Double> valeurs(String corps, String chemin) {
        return Stream.of(LIBRES, RESERVES, VENDUS, TOTAL, PCT_LIBRES, PCT_RESERVES, PCT_VENDUS)
                .map(champ -> arrondi(nombre(corps, chemin + champ)))
                .toList();
    }

    private static List<Double> nombresDeLaLigne(List<Object> ligne) {
        List<Double> nombres = new ArrayList<>();
        for (int i = PREMIERE_COLONNE_NOMBRE; i < PREMIERE_COLONNE_NOMBRE + 7; i++) {
            Object v = i < ligne.size() ? ligne.get(i) : null;
            nombres.add(v instanceof Double d ? arrondi(d) : null);
        }
        return nombres;
    }

    private static Double arrondi(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}

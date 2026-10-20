package com.tsix.immocrm.controller.suiviaffectations;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSuiviAffectations;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.IN_WORK;
import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.POINTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/suivi-affectations} (+ {@code /groupes}, {@code /totaux-colonnes}, {@code /export}, {@code /statuts},
 * catalogue) — FEATURE-suivi-affectations.md §2 R1, R3-R5, R8, R9, §3, §11. Jeu d'une tranche propre au test :
 * <pre>
 *   L1 : D1 RESERVATION, échéance AVANCE,  pièce CHQ IN_WORK  crédit, échéance 10/03/2025, banque      1000
 *   L2 : D2 CONCRETISATION, échéance FRAIS, pièce VIR POINTED crédit, échéance 01/01, opération 31/03/2025  2000
 *   L3 : D3 sans statut, échéance sans catégorie, pièce sans type ni état, débit, échéance 01/04/2025      300
 *   L4 : D1, échéance AVANCE, pièce CANCELED crédit, sans date                                            500
 *   + ligne hors vente sur la pièce de L1 (exclue) ; L0 sur une tranche IsForSale = 0 (exclue)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SuiviAffectationsListeTest {

    private static final String URL = "/api/suivi-affectations";
    private static final String URL_GROUPES = URL + "/groupes";
    private static final String URL_TOTAUX = URL + "/totaux-colonnes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";
    private static final String STATUT = "statut";
    private static final String ETAT_PIECE = "etatPiece";
    private static final String SEARCH = "search";
    private static final String TOTAL_ELEMENTS = "$.totalElements";
    private static final String SIZE = "size";
    private static final int NOMBRE_LIGNES = 4;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviAffectations fixture;
    @Autowired
    private FixturePatrimoine patrimoine;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private CfgTranche trancheHorsVente;
    private CfgCompany autreEntite;
    private ComSettlementCategory avance;
    private StlSettlementType virement;
    private ComFolder d1;
    private CfgTier client;
    private StlSettlement pieceL2;
    private StlSettlementLine l1;
    private StlSettlementLine l2;
    private StlSettlementLine l3;
    private StlSettlementLine l4;
    private StlSettlementLine l0;
    private StlSettlementLine ligneHorsVente;

    @BeforeEach
    void setUp() {
        FixtureEtatVentes ev = fixture.etatVentes();
        prefixe = FixtureEtatVentes.prefixe("SAL");
        projet = ev.projet("PRJ-" + prefixe, true);
        tranche = ev.tranche("TR-" + prefixe, projet, true);
        trancheHorsVente = ev.tranche("TR0-" + prefixe, projet, false);
        autreEntite = patrimoine.entite("AUT-" + prefixe, true);
        StkItemCategory categorieArticle = ev.categorie("APP-" + prefixe);
        StkItemType typeArticle = ev.type("T3-" + prefixe);
        client = ev.client("ZCLI" + prefixe);

        avance = fixture.categorie("AV-" + prefixe);
        ComSettlementCategory frais = fixture.categorie("FR-" + prefixe);
        StlSettlementType cheque = fixture.typePiece("CHQ-" + prefixe);
        virement = fixture.typePiece("VIR-" + prefixe);

        d1 = ev.vendre(ev.article(prefixe + "-A1", tranche, categorieArticle, typeArticle), "DOS-" + prefixe + "-1",
                ev.statut(StatutDossier.RESERVATION), client, LocalDateTime.of(2025, 1, 10, 10, 0));
        ComFolder d2 = ev.vendre(ev.article(prefixe + "-A2", tranche, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-2", ev.statut(StatutDossier.CONCRETISATION), null, null);
        ComFolder d3 = ev.vendre(ev.article(prefixe + "-A3", tranche, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-3", null, null, null);
        ComFolder d0 = ev.vendre(ev.article(prefixe + "-A0", trancheHorsVente, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-0", ev.statut(StatutDossier.RESERVATION), null, null);

        StlSettlement p1 = fixture.piece("CHQ " + prefixe + " 1", true, IN_WORK,
                LocalDateTime.of(2025, 3, 10, 0, 0), null, new BigDecimal("1050"));
        p1.setStlSettlementType(cheque);
        p1.setTier(client);
        p1.setCfgBank(fixture.banque("BANQUE" + prefixe));
        p1.setCfgCompany(autreEntite);
        fixture.enregistrer(p1);
        l1 = fixture.ligne(p1, fixture.echeance(d1, avance, new BigDecimal("1000")), new BigDecimal("1000"));
        ligneHorsVente = fixture.ligneHorsVente(p1, new BigDecimal("50"));

        pieceL2 = fixture.piece("VIR " + prefixe + " 77", true, POINTED,
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 3, 31, 0, 0), new BigDecimal("2000"));
        pieceL2.setStlSettlementType(virement);
        fixture.enregistrer(pieceL2);
        l2 = fixture.ligne(pieceL2, fixture.echeance(d2, frais, new BigDecimal("2000")), new BigDecimal("2000"));

        StlSettlement p3 = fixture.piece("DEB " + prefixe, false, null,
                LocalDateTime.of(2025, 4, 1, 0, 0), null, new BigDecimal("300"));
        l3 = fixture.ligne(p3, fixture.echeance(d3, null, new BigDecimal("300")), new BigDecimal("300"));

        StlSettlement p4 = fixture.piece("ANN " + prefixe, true, CANCELED, null, null, new BigDecimal("500"));
        l4 = fixture.ligne(p4, fixture.echeance(d1, avance, new BigDecimal("500")), new BigDecimal("500"));

        StlSettlement p0 = fixture.piece("HV " + prefixe, true, IN_WORK,
                LocalDateTime.of(2025, 3, 10, 0, 0), null, new BigDecimal("700"));
        l0 = fixture.ligne(p0, fixture.echeance(d0, avance, new BigDecimal("700")), new BigDecimal("700"));
        fixture.relire();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(URL).param(TRANCHE_ID, tranche.getId().toString()).param(SIZE, "50");
    }

    private static String ligne(StlSettlementLine l) {
        return "$.content[?(@.id=='" + l.getId() + "')]";
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long totalElements(MockHttpServletRequestBuilder requete) throws Exception {
        return ((Number) JsonPath.read(corps(requete), TOTAL_ELEMENTS)).longValue();
    }

    private List<String> ids(MockHttpServletRequestBuilder requete) throws Exception {
        return JsonPath.read(corps(requete.param(COLONNES, "id")), "$.content[*].id");
    }

    // ------------------------------------------------------------------ R1 — grain ligne d'affectation de vente

    @Test
    void r1_uneLigneParAffectationDeVente_ligneHorsVenteExclue() throws Exception {
        assertThat(ids(liste())).hasSize(NOMBRE_LIGNES)
                .contains(l1.getId().toString(), l2.getId().toString(), l3.getId().toString(), l4.getId().toString())
                .doesNotContain(ligneHorsVente.getId().toString());
    }

    @Test
    void r1_echeanceSansCategorieEtDossierSansStatut_presents_sansStatut() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "id,categorieId,statutDossierCode,statutDossierLibelle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(l3)).exists())
                .andExpect(jsonPath(ligne(l3) + ".statutDossierCode").value("SANS_STATUT"))
                .andExpect(jsonPath(ligne(l3) + ".statutDossierLibelle").value("Sans statut"));
    }

    @Test
    void r1_trancheNonEnVente_exclue() throws Exception {
        assertThat(ids(get(URL).param("projetId", projet.getId().toString()).param(SIZE, "50")))
                .hasSize(NOMBRE_LIGNES)
                .doesNotContain(l0.getId().toString());
        assertThat(totalElements(get(URL).param(TRANCHE_ID, trancheHorsVente.getId().toString()))).isZero();
    }

    // ------------------------------------------------------------------ §4 — colonnes de la ligne

    @Test
    void ligne_porteDossierArticleTrancheEtPiece() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "id,dossierId,dossierCode,articleCode,trancheId,trancheCode,"
                        + "projetId,pieceId,pieceLibelle,typePieceCode,etatPiece,tiersId,montantPiece"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(l2) + ".dossierCode").value("DOS-" + prefixe + "-2"))
                .andExpect(jsonPath(ligne(l2) + ".articleCode").value(prefixe + "-A2"))
                .andExpect(jsonPath(ligne(l2) + ".trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath(ligne(l2) + ".projetId").value(projet.getId().toString()))
                .andExpect(jsonPath(ligne(l2) + ".pieceId").value(pieceL2.getId().toString()))
                .andExpect(jsonPath(ligne(l2) + ".pieceLibelle").value(pieceL2.getDescription()))
                .andExpect(jsonPath(ligne(l2) + ".typePieceCode").value(virement.getCode()))
                .andExpect(jsonPath(ligne(l2) + ".etatPiece").value(POINTED));
    }

    @Test
    void ligne_etatPieceNull_rendSansEtat() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "id,etatPiece"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(l3) + ".etatPiece").value("SANS_ETAT"));
    }

    /** D5 : l'entité est celle du projet, même quand la pièce porte une autre société. */
    @Test
    void d5_entiteDuProjet_pasLaSocieteDeLaPiece() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "id,entiteId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(l1) + ".entiteId").value(projet.getCfgCompany().getId().toString()));

        assertThat(totalElements(liste().param("entiteId", autreEntite.getId().toString()))).isZero();
        assertThat(totalElements(liste().param("entiteId", projet.getCfgCompany().getId().toString())))
                .isEqualTo(NOMBRE_LIGNES);
    }

    // ------------------------------------------------------------------ R3 — sens

    @Test
    void r3_sens_debitSiIsCreditZero_sinonCredit() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "id,sens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(l3) + ".sens").value("DEBIT"))
                .andExpect(jsonPath(ligne(l1) + ".sens").value("CREDIT"));
    }

    @Test
    void r3_filtreSens() throws Exception {
        assertThat(new long[] {
                totalElements(liste()),
                totalElements(liste().param("sens", "TOUS")),
                totalElements(liste().param("sens", "CREDIT")),
                totalElements(liste().param("sens", "DEBIT"))})
                .containsExactly(NOMBRE_LIGNES, NOMBRE_LIGNES, NOMBRE_LIGNES - 1L, 1L);
    }

    // ------------------------------------------------------------------ R4 — date du mouvement

    @Test
    void r4_dateMouvement_settlementDatePrioritaire() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "id,dateMouvement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(l2) + ".dateMouvement").value(Matchers.contains(
                        Matchers.startsWith("2025-03-31"))))
                .andExpect(jsonPath(ligne(l1) + ".dateMouvement").value(Matchers.contains(
                        Matchers.startsWith("2025-03-10"))));
    }

    @Test
    void r4_bornesInclusesJoursEntiers() throws Exception {
        assertThat(ids(liste().param("du", "2025-03-10").param("au", "2025-03-31")))
                .containsExactlyInAnyOrder(l1.getId().toString(), l2.getId().toString());
    }

    @Test
    void r4_ligneSansDate_exclueDesQuUneBorneEstDonnee() throws Exception {
        assertThat(ids(liste().param("du", "2000-01-01")))
                .hasSize(NOMBRE_LIGNES - 1)
                .doesNotContain(l4.getId().toString());
        assertThat(ids(liste().param("au", "2079-01-01")))
                .hasSize(NOMBRE_LIGNES - 1)
                .doesNotContain(l4.getId().toString());
    }

    @Test
    void r5_duApresAu_rend400() throws Exception {
        mockMvc.perform(liste().param("du", "2025-04-01").param("au", "2025-03-01"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R5 — filtres

    @Test
    void r5_filtreCategorie() throws Exception {
        assertThat(ids(liste().param("categorieId", avance.getId().toString())))
                .containsExactlyInAnyOrder(l1.getId().toString(), l4.getId().toString());
    }

    /** §11 Q2 : code {@code SANS_CATEGORIE} dans le filtre catégorie, symétrique de {@code SANS_STATUT}. */
    @Test
    void r5_filtreSansCategorie() throws Exception {
        assertThat(ids(liste().param("categorieId", "SANS_CATEGORIE"))).containsExactly(l3.getId().toString());
    }

    @Test
    void r5_filtreCategorie_ouDansLeFiltre() throws Exception {
        assertThat(ids(liste().param("categorieId", avance.getId().toString(), "SANS_CATEGORIE")))
                .containsExactlyInAnyOrder(l1.getId().toString(), l3.getId().toString(), l4.getId().toString());
    }

    @Test
    void r5_filtreTypePiece() throws Exception {
        assertThat(ids(liste().param("typeId", virement.getId().toString()))).containsExactly(l2.getId().toString());
    }

    @Test
    void r5_filtreStatutDossier() throws Exception {
        assertThat(ids(liste().param(STATUT, "RESERVATION")))
                .containsExactlyInAnyOrder(l1.getId().toString(), l4.getId().toString());
    }

    @Test
    void r5_filtreStatut_insensibleALaCasse() throws Exception {
        assertThat(totalElements(liste().param(STATUT, "reservation"))).isEqualTo(2);
    }

    @Test
    void r5_filtreStatut_multiple() throws Exception {
        assertThat(totalElements(liste().param(STATUT, "RESERVATION", "CONCRETISATION"))).isEqualTo(3);
    }

    @Test
    void r5_filtreSansStatut() throws Exception {
        assertThat(ids(liste().param(STATUT, "SANS_STATUT"))).containsExactly(l3.getId().toString());
    }

    @Test
    void r5_statutInconnu_rend400() throws Exception {
        mockMvc.perform(liste().param(STATUT, "STATUT_QUI_N_EXISTE_PAS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r5_filtreEtatPiece() throws Exception {
        assertThat(new List<?>[] {
                ids(liste().param(ETAT_PIECE, IN_WORK)),
                ids(liste().param(ETAT_PIECE, POINTED)),
                ids(liste().param(ETAT_PIECE, CANCELED)),
                ids(liste().param(ETAT_PIECE, "SANS_ETAT"))})
                .containsExactly(List.of(l1.getId().toString()), List.of(l2.getId().toString()),
                        List.of(l4.getId().toString()), List.of(l3.getId().toString()));
    }

    @Test
    void r5_etatPieceInconnu_rend400() throws Exception {
        mockMvc.perform(liste().param(ETAT_PIECE, "ETAT_INCONNU"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r5_filtres_combinesEnEt() throws Exception {
        assertThat(ids(liste().param("categorieId", avance.getId().toString()).param(ETAT_PIECE, CANCELED)))
                .containsExactly(l4.getId().toString());
    }

    @Test
    void r5_filtreProjet() throws Exception {
        assertThat(totalElements(get(URL).param("projetId", projet.getId().toString()))).isEqualTo(NOMBRE_LIGNES);
    }

    // ------------------------------------------------------------------ R8 — recherche

    @Test
    void r8_recherche_espacesOtesEtCasseIgnoree() throws Exception {
        String recherche = ("vir" + prefixe + "77").toLowerCase(Locale.ROOT);
        assertThat(ids(liste().param(SEARCH, recherche))).containsExactly(l2.getId().toString());
    }

    @Test
    void r8_recherche_dossier() throws Exception {
        assertThat(ids(liste().param(SEARCH, d1.getCode())))
                .containsExactlyInAnyOrder(l1.getId().toString(), l4.getId().toString());
    }

    @Test
    void r8_recherche_tiers() throws Exception {
        assertThat(ids(liste().param(SEARCH, client.getLastName()))).containsExactly(l1.getId().toString());
    }

    @Test
    void r8_recherche_banque() throws Exception {
        assertThat(ids(liste().param(SEARCH, "BANQUE" + prefixe))).containsExactly(l1.getId().toString());
    }

    @Test
    void r8_recherche_categorie() throws Exception {
        assertThat(ids(liste().param(SEARCH, avance.getCode())))
                .containsExactlyInAnyOrder(l1.getId().toString(), l4.getId().toString());
    }

    // ------------------------------------------------------------------ R9 — tri, pagination

    @Test
    void r9_triParDefaut_dateMouvementDecroissante() throws Exception {
        List<String> ordre = ids(liste());

        assertThat(ordre).hasSize(NOMBRE_LIGNES)
                .startsWith(l3.getId().toString(), l2.getId().toString(), l1.getId().toString());
    }

    @Test
    void r9_triParId_accepte() throws Exception {
        assertThat(totalElements(liste().param("sort", "id,asc"))).isEqualTo(NOMBRE_LIGNES);
    }

    @Test
    void triInconnu_rend400() throws Exception {
        mockMvc.perform(liste().param("sort", "colonneQuiNExistePas,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tri_dossierCode() throws Exception {
        mockMvc.perform(liste().param("sort", "dossierCode,desc").param(COLONNES, "id,dossierCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dossierCode").value("DOS-" + prefixe + "-3"));
    }

    @Test
    void pagination() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()).param(SIZE, "3").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(NOMBRE_LIGNES))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void colonneInconnue_rend400() throws Exception {
        mockMvc.perform(liste().param(COLONNES, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ groupes, totaux, export, catalogue

    @Test
    void groupes_parCategorie_sommeDesEffectifsEgaleLeTotalDeLaListe() throws Exception {
        String groupes = corps(get(URL_GROUPES).param(TRANCHE_ID, tranche.getId().toString())
                .param("groupBy", "categorieLibelle").param("inclureSansValeur", "true"));

        List<Integer> effectifs = JsonPath.read(groupes, "$.content[*].nombre");
        assertThat(effectifs).isNotEmpty();
        assertThat(effectifs.stream().mapToLong(Integer::longValue).sum()).isEqualTo(totalElements(liste()));
    }

    @Test
    void groupes_colonneInconnue_rend400() throws Exception {
        mockMvc.perform(get(URL_GROUPES).param("groupBy", "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupes_surNotes_rend400() throws Exception {
        mockMvc.perform(get(URL_GROUPES).param("groupBy", "notesPiece"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totauxColonnes_nombreTotalSurToutLeFiltre() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(TRANCHE_ID, tranche.getId().toString()).param(SIZE, "1")
                        .param("agregats", "montantAffecte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(NOMBRE_LIGNES));
    }

    @Test
    void export_xlsx_porteLesLignesDuFiltre() throws Exception {
        List<String> valeurs = cellules(get(URL + "/export").param("format", "xlsx")
                .param(TRANCHE_ID, tranche.getId().toString()).param(COLONNES, "dossierCode,articleCode"));

        assertThat(valeurs).contains("DOS-" + prefixe + "-1", "DOS-" + prefixe + "-2", "DOS-" + prefixe + "-3")
                .doesNotContain("DOS-" + prefixe + "-0");
    }

    @Test
    void catalogueDesColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/suivi-affectations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='montantAffecte')].agregeable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='montantPiece')].agregeable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='categorieLibelle')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='dateMouvement')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='notesPiece')].groupable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='dossierCode')]").exists());
    }

    // ------------------------------------------------------------------ §11 Q1 — statuts du filtre

    /** Liste complète des statuts de vente, sorties comprises, même pour un non-administrateur (D9). */
    @Test
    void statuts_listeCompleteDesStatutsDeVente() throws Exception {
        fixture.etatVentes().statut(StatutDossier.ANNULATION);
        fixture.relire();
        AdmUser commercial = fixtureDossier.compte("sal.com." + prefixe);

        mockMvc.perform(get(URL + "/statuts").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..code", Matchers.hasItems("RESERVATION", "CONCRETISATION", "ANNULATION")));
    }

    private List<String> cellules(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete).andExpect(request().asyncStarted()).andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<String> valeurs = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            for (Row row : feuille) {
                for (Cell cell : row) {
                    valeurs.add(cell.toString());
                }
            }
        }
        return valeurs;
    }
}

package com.tsix.immocrm.controller.suiviaffectations;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureSuiviAffectations;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité ligne par ligne (règle 6, chemins de refus) — FEATURE-suivi-affectations.md R6, D5, D6.
 * Projets A et B (entités actives), I (entité inactive) ; tout porte le préfixe du test (recherche) :
 * <pre>
 *   LA  : dossier A                         100
 *   LB  : dossier B                        1000
 *   LI  : dossier I (entité inactive)    100000
 *   LMA / LMB : une pièce, deux lignes, dossiers A (10) et B (10000)
 *   LX  : dossier sans article (tranche non résolue)   1
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviAffectationsVisibiliteTest {

    private static final String URL = "/api/suivi-affectations";
    private static final String SEARCH = "search";
    private static final String COMMERCIAL = "Commercial";
    private static final String MONTANT_AFFECTE = "montantAffecte";
    private static final double SOMME_VISIBLE_A = 111.0;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviAffectations fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private CfgTranche trancheI;
    private ComFolder dossierB;
    private ComFolder dossierI;
    private StlSettlementLine la;
    private StlSettlementLine lb;
    private StlSettlementLine li;
    private StlSettlementLine lma;
    private StlSettlementLine lmb;
    private StlSettlementLine lx;
    private AdmUser commercialA;

    @BeforeEach
    void setUp() {
        FixtureEtatVentes ev = fixture.etatVentes();
        prefixe = FixtureEtatVentes.prefixe("SAV");
        CfgProject projetA = ev.projet("PRJA-" + prefixe, true);
        trancheA = ev.tranche("TRA-" + prefixe, projetA, true);
        trancheB = ev.tranche("TRB-" + prefixe, ev.projet("PRJB-" + prefixe, true), true);
        trancheI = ev.tranche("TRI-" + prefixe, ev.projet("PRJI-" + prefixe, false), true);
        StkItemCategory categorieArticle = ev.categorie("APP-" + prefixe);
        StkItemType typeArticle = ev.type("T3-" + prefixe);
        ComSettlementCategory avance = fixture.categorie("AV-" + prefixe);

        ComFolder dossierA = ev.vendre(ev.article(prefixe + "-A1", trancheA, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-A", ev.statut(StatutDossier.RESERVATION), null, null);
        dossierB = ev.vendre(ev.article(prefixe + "-B1", trancheB, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-B", ev.statut(StatutDossier.RESERVATION), null, null);
        dossierI = ev.vendre(ev.article(prefixe + "-I1", trancheI, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-I", ev.statut(StatutDossier.RESERVATION), null, null);
        ComFolder dossierX = fixture.dossierSansArticle("DOS-" + prefixe + "-X");

        la = ligneSeule("PA", dossierA, avance, "100");
        lb = ligneSeule("PB", dossierB, avance, "1000");
        li = ligneSeule("PI", dossierI, avance, "100000");
        lx = ligneSeule("PX", dossierX, avance, "1");
        StlSettlement multi = piece("PM", "10010");
        lma = fixture.ligne(multi, fixture.echeance(dossierA, avance, new BigDecimal("10")), new BigDecimal("10"));
        lmb = fixture.ligne(multi, fixture.echeance(dossierB, avance, new BigDecimal("10000")),
                new BigDecimal("10000"));

        commercialA = fixtureDossier.compte("sav.com." + prefixe);
        ev.affecter(commercialA, projetA);
        fixture.relire();
    }

    private StlSettlement piece(String code, String montant) {
        return fixture.piece(code + " " + prefixe, true, IN_WORK, LocalDateTime.of(2025, 6, 1, 0, 0), null,
                new BigDecimal(montant));
    }

    private StlSettlementLine ligneSeule(String code, ComFolder dossier, ComSettlementCategory categorie,
                                         String montant) {
        return fixture.ligne(piece(code, montant), fixture.echeance(dossier, categorie, new BigDecimal(montant)),
                new BigDecimal(montant));
    }

    private RequestPostProcessor commercial() {
        return connecte(commercialA, COMMERCIAL);
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<String> ids(RequestPostProcessor qui) throws Exception {
        return JsonPath.read(corps(get(URL).param(SEARCH, prefixe).param("size", "50").param("colonnes", "id")
                .with(qui)), "$.content[*].id");
    }

    private static String id(StlSettlementLine l) {
        return l.getId().toString();
    }

    // ------------------------------------------------------------------ commercial habilité au seul projet A

    @Test
    void r6_commercialA_neVoitAucuneLigneDuProjetB_niDeLEntiteInactive() throws Exception {
        assertThat(ids(commercial())).containsExactlyInAnyOrder(id(la), id(lma), id(lx));
    }

    @Test
    void r6_pieceMultiDossiers_seuleLaLigneDuPerimetre() throws Exception {
        assertThat(ids(commercial())).contains(id(lma)).doesNotContain(id(lmb));
    }

    @Test
    void r6_ligneSansTrancheResolue_visible() throws Exception {
        assertThat(ids(commercial())).contains(id(lx));
    }

    @Test
    void r6_trancheHorsPerimetre_listeVide() throws Exception {
        mockMvc.perform(get(URL).param("trancheId", trancheB.getId().toString()).with(commercial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void r6_groupes_sansGroupeHorsPerimetre() throws Exception {
        String groupes = corps(get(URL + "/groupes").param(SEARCH, prefixe).param("groupBy", "trancheCode")
                .param("inclureSansValeur", "true").param("agregats", MONTANT_AFFECTE).with(commercial()));

        List<String> cles = JsonPath.read(groupes, "$.content[*].cle");
        List<Integer> effectifs = JsonPath.read(groupes, "$.content[*].nombre");
        assertThat(cles).contains(trancheA.getCode()).doesNotContain(trancheB.getCode(), trancheI.getCode());
        assertThat(effectifs.stream().mapToLong(Integer::longValue).sum()).isEqualTo(3);
    }

    @Test
    void r6_totaux_surLesSeulesLignesVisibles() throws Exception {
        mockMvc.perform(get(URL + "/totaux-colonnes").param(SEARCH, prefixe).param("agregats", MONTANT_AFFECTE)
                        .with(commercial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(3))
                .andExpect(jsonPath("$.totaux.montantAffecte").value(SOMME_VISIBLE_A));
    }

    @Test
    void r6_export_sansLigneHorsPerimetre() throws Exception {
        List<String> valeurs = cellules(get(URL + "/export").param("format", "xlsx").param(SEARCH, prefixe)
                .param("colonnes", "dossierCode,articleCode").with(commercial()));

        assertThat(valeurs).contains("DOS-" + prefixe + "-A", "DOS-" + prefixe + "-X")
                .doesNotContain(dossierB.getCode(), dossierI.getCode(), prefixe + "-B1", prefixe + "-I1");
    }

    // ------------------------------------------------------------------ exemptés, entité inactive

    @Test
    void r6_administrateur_voitAEtB_maisPasLEntiteInactive() throws Exception {
        AdmUser admin = fixtureDossier.compte("sav.adm." + prefixe, "Administrateur");

        assertThat(ids(connecte(admin, "Administrateur")))
                .containsExactlyInAnyOrder(id(la), id(lb), id(lma), id(lmb), id(lx))
                .doesNotContain(id(li));
    }

    @Test
    void r6_directionGenerale_voitAEtB() throws Exception {
        AdmUser direction = fixtureDossier.compte("sav.dg." + prefixe, "Direction Générale");

        assertThat(ids(connecte(direction, "Direction Générale")))
                .contains(id(la), id(lb), id(lma), id(lmb))
                .doesNotContain(id(li));
    }

    @Test
    void r6_entiteInactive_trancheFiltree_listeVideMemePourLAdministrateur() throws Exception {
        AdmUser admin = fixtureDossier.compte("sav.adm2." + prefixe, "Administrateur");

        mockMvc.perform(get(URL).param("trancheId", trancheI.getId().toString())
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
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

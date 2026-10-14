package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.web.export.ExportTableur;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Filtres, lots libres, totaux et export de la liste des dossiers (FEATURE-vente-liste.md §2 à §8). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeFiltresOmranTest {

    private static final String LISTE = "/api/dossiers";
    private static final String SEARCH = "search";
    private static final String IDS = "$.content[*].id";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureStatutsDossier statuts;
    @Autowired
    private FixtureReferentielStk referentielStk;
    @Autowired
    private EntityManager em;

    private CfgTranche tranche;
    private String prefixe;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TR-LSF-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "LSTF" + UUID.randomUUID().toString().substring(0, 6);
    }

    private ComFolder dossier(String suffixe, StatutDossier statut) {
        ComFolder f = fixture.dossier(prefixe + suffixe, tranche, null);
        f.setStrStatus(statuts.statut(statut));
        return f;
    }

    private void relire() {
        em.flush();
        em.clear();
    }

    @Test
    void sortis_exclusParDefaut_reintegresSurDemande() throws Exception {
        ComFolder actif = dossier("A", StatutDossier.CONCRETISATION);
        ComFolder annule = dossier("B", StatutDossier.ANNULATION);
        ComFolder desiste = dossier("C", StatutDossier.DESISTEMENT);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath(IDS).value(hasItem(actif.getId().toString())));
        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("inclureSortis", "true"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("statut", "ANNULATION"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath(IDS).value(hasItem(annule.getId().toString())))
                .andExpect(jsonPath(IDS).value(not(hasItem(desiste.getId().toString()))));
    }

    @Test
    void filtreStatut_serveur_parCode() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION);
        ComFolder reservation = dossier("B", StatutDossier.RESERVATION);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("statut", "RESERVATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(reservation.getId().toString()));
    }

    @Test
    void lotLibre_listeAvecLOption_ouvreLArticle() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION);
        StkItem libre = article(prefixe + "-LIBRE", f.getStkItem());
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("lignes", "DOSSIERS_ET_LIBRES")
                        .param("colonnes", "code,libre,articleCode,trancheCode,statut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[1].id").value(libre.getId().toString()))
                .andExpect(jsonPath("$.content[1].libre").value(true))
                .andExpect(jsonPath("$.content[1].articleCode").value(prefixe + "-LIBRE"))
                .andExpect(jsonPath("$.content[1].trancheCode").value(tranche.getCode()))
                .andExpect(jsonPath("$.content[0].libre").value(false));
        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("lignes", "LIBRES"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /** Un lot libre n'a aucune date de dossier : la période par défaut (mois courant) ne doit pas le masquer. */
    @Test
    void lotLibre_resteListeQuelleQueSoitLaPeriode() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION);
        StkItem libre = article(prefixe + "-LIBRE", f.getStkItem());
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("lignes", "LIBRES")
                        .param("champDate", "RESERVATION")
                        .param("dateDebut", "2001-01-01T00:00:00").param("dateFin", "2001-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(libre.getId().toString()));
    }

    /** Colonnes par défaut d'un dossier : un lot libre y montre son code, le statut LIBRE et son prix TTC. */
    @Test
    void lotLibre_colonnesParDefautLisibles() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION);
        StkItem libre = article(prefixe + "-LIBRE", f.getStkItem());
        libre.setComSalePrice(new BigDecimal("950000"));
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("lignes", "LIBRES")
                        .param("colonnes", "code,statut,prix,libre,trancheCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(prefixe + "-LIBRE"))
                .andExpect(jsonPath("$.content[0].statut").value("LIBRE"))
                .andExpect(jsonPath("$.content[0].prix").value(950000.0))
                .andExpect(jsonPath("$.content[0].trancheCode").value(tranche.getCode()));
    }

    /** Mobile saisi au format national ou international, stocké sans 0 ni indicatif. */
    @Test
    void recherche_mobileAuFormatNationalOuInternational() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION);
        String mobile = "6" + UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 8);
        f.getCfgCustomer().setMobile(mobile);
        f.getCfgCustomer().setMobileIndicatif("212");
        relire();

        for (String saisie : new String[] {"0" + mobile, "+212 " + mobile, "00212" + mobile}) {
            mockMvc.perform(get(LISTE).param(SEARCH, saisie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(f.getId().toString()));
        }
    }

    /** Lot d'un dossier sorti : redevenu libre. */
    @Test
    void articlePrincipalDUnDossierSorti_estUnLotLibre() throws Exception {
        ComFolder sorti = dossier("A", StatutDossier.ANNULATION);
        sorti.getStkItem().setComFolderId(null);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("lignes", "LIBRES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sorti.getStkItem().getId().toString()));
    }

    /** Pagination sur la concaténation : la page 1 de taille 1 est le lot libre. */
    @Test
    void lotLibre_paginationSurLaConcatenation() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION);
        StkItem libre = article(prefixe + "-LIBRE", f.getStkItem());
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("lignes", "DOSSIERS_ET_LIBRES")
                        .param("page", "1").param("size", "1"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(libre.getId().toString()));
    }

    @Test
    void periode_surUneNouvelleDate_etDateVide() throws Exception {
        ComFolder dedans = dossier("A", StatutDossier.CONCRETISATION);
        dedans.setBankRecordDate(LocalDateTime.of(2025, 3, 15, 0, 0));
        dossier("B", StatutDossier.CONCRETISATION).setBankRecordDate(LocalDateTime.of(2025, 5, 15, 0, 0));
        ComFolder vide = dossier("C", StatutDossier.CONCRETISATION);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("champDate", "FICHE_BANCAIRE")
                        .param("dateDebut", "2025-03-01T00:00:00").param("dateFin", "2025-03-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(dedans.getId().toString()));
        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("champDate", "FICHE_BANCAIRE")
                        .param("dateVide", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(vide.getId().toString()));
    }

    @Test
    void periode_surUneDateDeLaProspection_gardeLesDossiersSansProspectionPourDateVide() throws Exception {
        dossier("A", StatutDossier.CONCRETISATION);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("champDate", "PROSPECTION")
                        .param("dateVide", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void recherche_parCinParMobileAvecIndicatifEtParCoAcquereur() throws Exception {
        ComFolder parCin = dossier("A", StatutDossier.CONCRETISATION);
        CfgTier client = parCin.getCfgCustomer();
        String cin = "ZX" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        client.setIDNumber(cin);
        client.setMobile("6" + UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 8));
        client.setMobileIndicatif("212");

        ComFolder parCoAcquereur = dossier("B", StatutDossier.CONCRETISATION);
        CfgTier coAcquereur = fixture.tier("COACQ");
        String nomCo = "COACQ" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        coAcquereur.setDescription(nomCo + " Ilyas");
        ComFolderCustomer lien = new ComFolderCustomer();
        lien.setId(UUID.randomUUID());
        lien.setComFolder(parCoAcquereur);
        lien.setCfgTier(coAcquereur);
        em.persist(lien);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, cin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(parCin.getId().toString()));
        mockMvc.perform(get(LISTE).param(SEARCH, "+212 " + client.getMobile()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(parCin.getId().toString()));
        mockMvc.perform(get(LISTE).param(SEARCH, nomCo))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(parCoAcquereur.getId().toString()));
    }

    /** Σ en tête : `totaux` de /groupes porte sur toutes les lignes filtrées, sortis exclus comme la liste. */
    @Test
    void totaux_surToutLeResultatFiltre() throws Exception {
        ComFolder a = dossier("A", StatutDossier.CONCRETISATION);
        a.setPriceHT(new BigDecimal("100000"));
        a.setPrice(new BigDecimal("120000"));
        ComFolder b = dossier("B", StatutDossier.RESERVATION);
        b.setPriceHT(new BigDecimal("50000"));
        b.setPrice(new BigDecimal("60000"));
        ComFolder sorti = dossier("C", StatutDossier.ANNULATION);
        sorti.setPriceHT(new BigDecimal("999999"));
        relire();

        mockMvc.perform(get(LISTE + "/groupes").param(SEARCH, prefixe).param("groupBy", "statut")
                        .param("inclureSansValeur", "true").param("agregats", "prixHt,prix").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux.prixHt").value(150000.0))
                .andExpect(jsonPath("$.totaux.prix").value(180000.0));
    }

    @Test
    void ordreOmran_trieParCodeArticle() throws Exception {
        ComFolder b = dossier("B", StatutDossier.CONCRETISATION);
        b.getStkItem().setCode(prefixe + "-Z");
        ComFolder a = dossier("A", StatutDossier.CONCRETISATION);
        a.getStkItem().setCode(prefixe + "-A");
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param("ordreOmran", "true"))
                .andExpect(jsonPath("$.content[0].id").value(a.getId().toString()))
                .andExpect(jsonPath("$.content[1].id").value(b.getId().toString()));
    }

    @Test
    void export_enTetesEtUneLigne() throws Exception {
        ComFolder f = dossier("A", StatutDossier.CONCRETISATION);
        f.setPrice(new BigDecimal("700000"));
        f.setBankRecordDate(LocalDateTime.of(2025, 2, 6, 0, 0));
        relire();

        MvcResult asynchrone = mockMvc.perform(get(LISTE + "/export").param("format", "xlsx")
                        .param(SEARCH, prefixe).param("colonnes", "code,prix,dateFicheBancaire"))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", ExportTableur.XLSX.toString()))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern(".*dossiers_\\d{8}_\\d{6}\\.xlsx.*")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            Row entete = feuille.getRow(0);
            Row ligne = feuille.getRow(1);
            assertThat(feuille.getLastRowNum()).isEqualTo(1);
            assertThat(entete.getCell(0).getStringCellValue()).isEqualTo("Code");
            assertThat(entete.getCell(1).getStringCellValue()).isEqualTo("Prix");
            assertThat(entete.getCell(2).getStringCellValue()).isEqualTo("Fiche bancaire");
            assertThat(ligne.getCell(0).getStringCellValue()).isEqualTo(f.getCode());
            assertThat(ligne.getCell(1).getNumericCellValue()).isEqualTo(700000.0);
            assertThat(ligne.getCell(2).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2025, 2, 6, 0, 0));
        }
    }

    @Test
    void export_formatAutreQueXlsx_rend400() throws Exception {
        mockMvc.perform(get(LISTE + "/export").param("format", "csv"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void champsDate_les47DatesDansLOrdreOmran() throws Exception {
        mockMvc.perform(get(LISTE + "/champs-date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ChampDateDossier.values().length))
                .andExpect(jsonPath("$[0].code").value("RESERVATION"))
                .andExpect(jsonPath("$[1].code").value("PROSPECTION"));
        assertThat(ChampDateDossier.values()).hasSize(47);
    }

    @Test
    void statuts_signalentLesSorties() throws Exception {
        statuts.statut(StatutDossier.ANNULATION);
        statuts.statut(StatutDossier.CONCRETISATION);
        relire();

        mockMvc.perform(get(LISTE + "/statuts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='ANNULATION')].sortie").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.code=='CONCRETISATION')].sortie").value(hasItem(false)));
    }

    /** Article sans dossier, référentiels repris de l'article d'un dossier de la fixture. */
    private StkItem article(String code, StkItem modele) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setStkItemType(modele.getStkItemType());
        article.setStkItemCategory(modele.getStkItemCategory());
        article.setStkVocation(modele.getStkVocation());
        article.setStkHierarchy(tranche);
        article.setStkKind(referentielStk.natureParDefaut());
        article.setArea(new BigDecimal("80"));
        em.persist(article);
        return article;
    }
}

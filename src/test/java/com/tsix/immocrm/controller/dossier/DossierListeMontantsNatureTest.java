package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.web.export.ExportTableur;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.CellType;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Montants par nature de la liste des dossiers, formules de ComSaleFolderView (FEATURE-dossiers-liste-complete.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeMontantsNatureTest {

    private static final String LISTE = "/api/dossiers";
    private static final String PREMIER = "$.content[0].";
    private static final String SEARCH = "search";
    private static final String COLONNES = "colonnes";
    private static final String AVANCE = "AVANCE";
    private static final String POINTE = "POINTED";
    private static final String UN_MILLION = "1000000";
    private static final String CENT_MILLE = "100000";
    private static final String FORMAT = "format";
    private static final String ROLE_COMMERCIAL = "Commercial";
    private static final String TOTAUX = "/totaux";
    private static final String EXPORT = "/export";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private ComSettlementCategoryRepository categories;
    @Autowired
    private StlSettlementRepository reglements;
    @Autowired
    private StlSettlementLineRepository lignesReglement;
    @Autowired
    private AdmUserHabilitationRepository habilitations;
    @Autowired
    private EntityManager em;

    private CfgTranche tranche;
    private String prefixe;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TR-MNT-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "MNT" + UUID.randomUUID().toString().substring(0, 6);
    }

    private ComFolder dossier(CfgTranche t, String prix) {
        ComFolder f = fixture.dossier(prefixe, t, StatutDossier.CONCRETISATION);
        f.setPrice(new BigDecimal(prix));
        f.setPriceAfterDiscount(new BigDecimal(prix));
        return f;
    }

    /** Catégorie d'échéance : nature par drapeau, {@code auCredit} = ComSettlementCategory.IsCredit. */
    private ComSettlementCategory categorie(String nature, boolean auCredit) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(nature + "-" + prefixe + "-" + auCredit);
        c.setIsCredit(auCredit);
        c.setIsSaleAdvance("AVANCE".equals(nature));
        c.setIsSaleAutoFinancment("AUTOFIN".equals(nature));
        c.setIsSaleCredit("CREDIT".equals(nature));
        c.setIsSaleExpense("FRAIS".equals(nature));
        return categories.save(c);
    }

    private ComDocument echeance(ComFolder f, ComSettlementCategory c, String montant, boolean exclueDuPrix) {
        ComDocument d = fixture.echeance(f, c, new BigDecimal(montant), LocalDateTime.of(2025, 1, 1, 0, 0));
        d.setExcludeFromPrice(exclueDuPrix);
        return d;
    }

    private void regler(ComDocument d, String montant, boolean encaissement, String statut) {
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("RG-" + prefixe);
        s.setDocDate(LocalDateTime.of(2025, 2, 1, 0, 0));
        s.setAmount(new BigDecimal(montant));
        s.setIsCredit(encaissement);
        s.setStatus(statut);
        reglements.save(s);
        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(d);
        l.setAmount(new BigDecimal(montant));
        lignesReglement.save(l);
    }

    private void relire() {
        em.flush();
        em.clear();
    }

    /** Cas de la spec : AVANCE au crédit 100 000 et au débit 10 000, 60 000 réglés → 100 000 / 60 000 / 40 000. */
    @Test
    void avance_seulementLesEcheancesAuCredit_regleDeToutes() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        regler(echeance(f, categorie(AVANCE,true), CENT_MILLE, false), "60000", true, POINTE);
        echeance(f, categorie(AVANCE,false), "10000", false);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe)
                        .param(COLONNES, "avance,avanceReglee,avanceReste,tauxAvance,tauxAvanceReel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "avance").value(100000.0))
                .andExpect(jsonPath(PREMIER + "avanceReglee").value(60000.0))
                .andExpect(jsonPath(PREMIER + "avanceReste").value(40000.0))
                .andExpect(jsonPath(PREMIER + "tauxAvance").value(10.0))
                .andExpect(jsonPath(PREMIER + "tauxAvanceReel").value(60.0));
    }

    /** Un règlement annulé (CANCELED) ne compte pas, comme dans /montants. */
    @Test
    void autofinancement_reglementAnnuleExclu() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        ComDocument d = echeance(f, categorie("AUTOFIN", true), "200000", false);
        regler(d, "50000", true, "IN_WORK");
        regler(d, "30000", true, "CANCELED");
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe)
                        .param(COLONNES, "autofinancement,autofinancementRegle,autofinancementReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "autofinancement").value(200000.0))
                .andExpect(jsonPath(PREMIER + "autofinancementRegle").value(50000.0))
                .andExpect(jsonPath(PREMIER + "autofinancementReste").value(150000.0));
    }

    /** Un décaissement (StlSettlement.IsCredit = 0) se retranche du réglé. */
    @Test
    void credit_decaissementSigneNegatif() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        ComDocument d = echeance(f, categorie("CREDIT", true), "500000", false);
        regler(d, "300000", true, null);
        regler(d, "20000", false, null);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "credit,creditRegle,creditReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "credit").value(500000.0))
                .andExpect(jsonPath(PREMIER + "creditRegle").value(280000.0))
                .andExpect(jsonPath(PREMIER + "creditReste").value(220000.0));
    }

    @Test
    void frais_horsPrixDeVenteEtHorsPrixContractuel() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        f.setAgreementPrice(new BigDecimal(UN_MILLION));
        regler(echeance(f, categorie("FRAIS", true), "15000", true), "15000", true, POINTE);
        regler(echeance(f, categorie(AVANCE,true), CENT_MILLE, false), CENT_MILLE, true, POINTE);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES,
                        "frais,fraisRegles,fraisReste,prixRegle,prixReste,prixContractuel,prixContractuelRegle,"
                                + "prixContractuelReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "frais").value(15000.0))
                .andExpect(jsonPath(PREMIER + "fraisRegles").value(15000.0))
                .andExpect(jsonPath(PREMIER + "fraisReste").value(0.0))
                .andExpect(jsonPath(PREMIER + "prixRegle").value(100000.0))
                .andExpect(jsonPath(PREMIER + "prixReste").value(900000.0))
                .andExpect(jsonPath(PREMIER + "prixContractuel").value(1000000.0))
                .andExpect(jsonPath(PREMIER + "prixContractuelRegle").value(100000.0))
                .andExpect(jsonPath(PREMIER + "prixContractuelReste").value(900000.0));
    }

    /** Net à payer = prix + frais au crédit ; réglé = avance + autofinancement + crédit + frais. */
    @Test
    void netAPayer_totalEtTotalRegleDeLaVue() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        regler(echeance(f, categorie("FRAIS", true), "15000", true), "15000", true, POINTE);
        regler(echeance(f, categorie(AVANCE,true), CENT_MILLE, false), "60000", true, POINTE);
        regler(echeance(f, categorie("AUTOFIN", true), "400000", false), "50000", true, POINTE);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "netAPayerRegle,netAPayerReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "netAPayerRegle").value(125000.0))
                .andExpect(jsonPath(PREMIER + "netAPayerReste").value(890000.0));
    }

    @Test
    void tropPercu_dossierDebiteur_zero() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        regler(echeance(f, categorie(AVANCE,true), CENT_MILLE, false), "60000", true, POINTE);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe)
                        .param(COLONNES, "tropPercu,tropPercuRegle,tropPercuReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "tropPercu").value(0.0))
                .andExpect(jsonPath(PREMIER + "tropPercuRegle").value(0.0))
                .andExpect(jsonPath(PREMIER + "tropPercuReste").value(0.0));
    }

    @Test
    void tropPercu_dossierSurpaye_leSurplus() throws Exception {
        ComFolder f = dossier(tranche, CENT_MILLE);
        regler(echeance(f, categorie(AVANCE,true), CENT_MILLE, false), "120000", true, POINTE);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe)
                        .param(COLONNES, "tropPercu,tropPercuRegle,tropPercuReste,netAPayerReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "tropPercu").value(20000.0))
                .andExpect(jsonPath(PREMIER + "tropPercuRegle").value(0.0))
                .andExpect(jsonPath(PREMIER + "tropPercuReste").value(20000.0))
                .andExpect(jsonPath(PREMIER + "netAPayerReste").value(-20000.0));
    }

    @Test
    void tauxAvance_prixApresRemiseNul_zeroSansDivision() throws Exception {
        ComFolder f = dossier(tranche, "0");
        echeance(f, categorie(AVANCE,true), CENT_MILLE, false);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "tauxAvance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "tauxAvance").value(0.0));
    }

    @Test
    void colonnesNonDemandees_absentesEtSansRequete() throws Exception {
        dossier(tranche, UN_MILLION);
        relire();

        assertAll(
                () -> assertThat(MontantsParNatureDossier.requises(Set.of(ColonneDossier.CODE,
                        ColonneDossier.PRIX_CONTRACTUEL, ColonneDossier.MONTANT_AVANCE))).isFalse(),
                () -> assertThat(MontantsParNatureDossier.requises(Set.of(ColonneDossier.TROP_PERCU))).isTrue());
        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "code").exists())
                .andExpect(jsonPath(PREMIER + "avance").doesNotExist())
                .andExpect(jsonPath(PREMIER + "tropPercu").doesNotExist());
    }

    @Test
    void montantAvance_libelleMontantDAccord() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='montantAvance')].libelle").value("Montant d'accord"))
                .andExpect(jsonPath("$[?(@.nom=='avance')].libelle").value("Avance"));
    }

    @Test
    void export_avanceEtTropPercu_enTetesDansLOrdreEtCellulesMontant() throws Exception {
        ComFolder f = dossier(tranche, CENT_MILLE);
        regler(echeance(f, categorie(AVANCE,true), CENT_MILLE, false), "120000", true, POINTE);
        relire();

        MvcResult asynchrone = mockMvc.perform(get(LISTE + EXPORT).param(FORMAT, ExportTableur.FORMAT_XLSX)
                        .param(SEARCH, prefixe).param(COLONNES, "tropPercu,avance"))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            Row entete = feuille.getRow(0);
            Row ligne = feuille.getRow(1);
            assertAll(
                    () -> assertThat(entete.getCell(0).getStringCellValue()).isEqualTo("Trop perçu"),
                    () -> assertThat(entete.getCell(1).getStringCellValue()).isEqualTo("Avance"),
                    () -> assertThat(ligne.getCell(0).getCellType()).isEqualTo(CellType.NUMERIC),
                    () -> assertThat(ligne.getCell(0).getNumericCellValue()).isEqualTo(20000.0),
                    () -> assertThat(ligne.getCell(1).getNumericCellValue()).isEqualTo(100000.0));
        }
    }

    @Test
    void totaux_sommeDesLignesDuFiltre() throws Exception {
        regler(echeance(dossier(tranche, UN_MILLION), categorie(AVANCE,true), CENT_MILLE, false),
                "60000", true, POINTE);
        echeance(dossier(tranche, "500000"), categorie(AVANCE,true), "50000", false);
        relire();

        mockMvc.perform(get(LISTE + TOTAUX).param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(2))
                .andExpect(jsonPath("$.sommes.avance").value(150000.0))
                .andExpect(jsonPath("$.sommes.avanceReglee").value(60000.0))
                .andExpect(jsonPath("$.sommes.avanceReste").value(90000.0))
                .andExpect(jsonPath("$.sommes.prixReste").value(1440000.0))
                .andExpect(jsonPath("$.sommes.tauxAvance").doesNotExist());
    }

    @Test
    void totaux_lotsLibresSeuls_sommesNulles() throws Exception {
        echeance(dossier(tranche, UN_MILLION), categorie(AVANCE,true), CENT_MILLE, false);
        relire();

        mockMvc.perform(get(LISTE + TOTAUX).param(SEARCH, prefixe).param("lignes", "LIBRES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(0))
                .andExpect(jsonPath("$.sommes.avance").value(0));
    }

    /** Invariant de visibilité : un commercial n'additionne jamais un dossier hors de ses projets. */
    @Test
    void totaux_dUnCommercial_seulementSonPerimetre() throws Exception {
        CfgTranche autre = fixture.tranche("TR-MNT-B-" + UUID.randomUUID().toString().substring(0, 6), true);
        echeance(dossier(tranche, UN_MILLION), categorie(AVANCE,true), CENT_MILLE, false);
        echeance(dossier(autre, UN_MILLION), categorie(AVANCE,true), "70000", false);
        AdmUser commercial = commercialDe(tranche);
        relire();

        mockMvc.perform(get(LISTE + TOTAUX).param(SEARCH, prefixe)
                        .with(FixtureDossierVente.connecte(commercial, ROLE_COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(1))
                .andExpect(jsonPath("$.sommes.avance").value(100000.0));
    }

    @Test
    void export_dUnCommercial_sansLigneHorsPerimetre() throws Exception {
        CfgTranche autre = fixture.tranche("TR-MNT-C-" + UUID.randomUUID().toString().substring(0, 6), true);
        ComFolder visible = dossier(tranche, UN_MILLION);
        echeance(visible, categorie(AVANCE,true), CENT_MILLE, false);
        echeance(dossier(autre, UN_MILLION), categorie(AVANCE,true), "70000", false);
        AdmUser commercial = commercialDe(tranche);
        relire();

        MvcResult asynchrone = mockMvc.perform(get(LISTE + EXPORT).param(FORMAT, ExportTableur.FORMAT_XLSX)
                        .param(SEARCH, prefixe).param(COLONNES, "code,avance")
                        .with(FixtureDossierVente.connecte(commercial, ROLE_COMMERCIAL)))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            assertAll(
                    () -> assertThat(feuille.getLastRowNum()).isEqualTo(1),
                    () -> assertThat(feuille.getRow(1).getCell(0).getStringCellValue()).isEqualTo(visible.getCode()),
                    () -> assertThat(feuille.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(100000.0));
        }
    }

    // ------------------------------------------------------------------ FEATURE-etat-des-ventes-parite.md §3.3

    private static final String MODIFICATIONS = "modifications";
    private static final String PENALITES = "penalites";
    private static final String NET_A_PAYER = "netAPayer";

    /** Catégorie IsSaleModification ou IsSalePenality ; {@code auCredit} = ComSettlementCategory.IsCredit. */
    private ComSettlementCategory categorieModificationOuPenalite(boolean modification, boolean auCredit) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode((modification ? "MOD-" : "PEN-") + prefixe + "-" + UUID.randomUUID().toString().substring(0, 6));
        c.setIsCredit(auCredit);
        c.setIsSaleModification(modification);
        c.setIsSalePenality(!modification);
        return categories.save(c);
    }

    /** netAPayer = Bilan.total() de TropPercu : prix + frais au crédit (même cas que netAPayer_totalEtTotalRegleDeLaVue). */
    @Test
    void parite_netAPayer_prixPlusFrais() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        regler(echeance(f, categorie("FRAIS", true), "15000", true), "15000", true, POINTE);
        regler(echeance(f, categorie(AVANCE, true), CENT_MILLE, false), "60000", true, POINTE);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "netAPayer,netAPayerRegle,netAPayerReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + NET_A_PAYER).value(1015000.0))
                .andExpect(jsonPath(PREMIER + "netAPayerRegle").value(75000.0))
                .andExpect(jsonPath(PREMIER + "netAPayerReste").value(940000.0));
    }

    /** Montant = ComFolder.ModificationsAmount ; réglé = réglé des échéances IsSaleModification ; reste = différence. */
    @Test
    void parite_modifications_montantDuDossier_regleDesEcheances() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        f.setModificationsAmount(new BigDecimal("50000"));
        regler(echeance(f, categorieModificationOuPenalite(true, true), "50000", false), "20000", true, POINTE);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe)
                        .param(COLONNES, "modifications,modificationsReglees,modificationsReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + MODIFICATIONS).value(50000.0))
                .andExpect(jsonPath(PREMIER + "modificationsReglees").value(20000.0))
                .andExpect(jsonPath(PREMIER + "modificationsReste").value(30000.0));
    }

    /** Montant = Σ signé des échéances IsSalePenality (crédit +, débit −) ; réglé ; reste = montant − réglé. */
    @Test
    void parite_penalites_sommeSigneeDesEcheances() throws Exception {
        ComFolder f = dossier(tranche, UN_MILLION);
        regler(echeance(f, categorieModificationOuPenalite(false, true), "30000", false), "10000", true, POINTE);
        echeance(f, categorieModificationOuPenalite(false, false), "5000", false);
        relire();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "penalites,penalitesRegles,penalitesReste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + PENALITES).value(25000.0))
                .andExpect(jsonPath(PREMIER + "penalitesRegles").value(10000.0))
                .andExpect(jsonPath(PREMIER + "penalitesReste").value(15000.0));
    }

    /** Les 7 colonnes rejoignent les sommes de /totaux (effet de bord assumé, §3.3). */
    @Test
    void parite_totaux_septNouvellesColonnesSommees() throws Exception {
        ComFolder f1 = dossier(tranche, UN_MILLION);
        f1.setModificationsAmount(new BigDecimal("50000"));
        regler(echeance(f1, categorieModificationOuPenalite(true, true), "50000", false), "20000", true, POINTE);
        regler(echeance(f1, categorieModificationOuPenalite(false, true), "30000", false), "10000", true, POINTE);
        echeance(dossier(tranche, "500000"), categorieModificationOuPenalite(false, true), "4000", false);
        relire();

        mockMvc.perform(get(LISTE + TOTAUX).param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(2))
                .andExpect(jsonPath("$.sommes.netAPayer").isNumber())
                .andExpect(jsonPath("$.sommes.modifications").value(50000.0))
                .andExpect(jsonPath("$.sommes.modificationsReglees").value(20000.0))
                .andExpect(jsonPath("$.sommes.modificationsReste").value(30000.0))
                .andExpect(jsonPath("$.sommes.penalites").value(34000.0))
                .andExpect(jsonPath("$.sommes.penalitesRegles").value(10000.0))
                .andExpect(jsonPath("$.sommes.penalitesReste").value(24000.0));
    }

    @Test
    void parite_catalogue_septNouvellesColonnes_horsDefaut() throws Exception {
        String corps = mockMvc.perform(get("/api/referentiels/colonnes/dossiers"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> noms = List.of(NET_A_PAYER, MODIFICATIONS, "modificationsReglees", "modificationsReste",
                PENALITES, "penalitesRegles", "penalitesReste");

        List<Boolean> parDefaut = noms.stream()
                .<Boolean>flatMap(n -> JsonPath.<List<Boolean>>read(corps, "$[?(@.nom=='" + n + "')].parDefaut")
                        .stream())
                .toList();

        assertThat(parDefaut).hasSize(noms.size()).containsOnly(false);
    }

    private AdmUser commercialDe(CfgTranche t) {
        AdmUser commercial = fixture.compte("MNT-COM");
        AdmUserHabilitation h = new AdmUserHabilitation();
        h.setId(UUID.randomUUID());
        h.setAdmUser(commercial);
        h.setCfgProject(t.getCfgProject());
        habilitations.save(h);
        return commercial;
    }
}

package com.tsix.immocrm.controller.export;

import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.web.export.ExportTableur;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Cell;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Export Excel des listes (FEATURE-export-listes.md) : en-têtes dans l'ordre demandé, un filtre de la
 * liste qui écarte une ligne, et le périmètre commercial qui écarte la ligne d'un projet non affecté.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ExportListesTest {

    private static final UUID ID_CATEGORIE_PROSPECTION = UUID.fromString("69F8E7D6-1111-1111-1111-1AA8B1F02008");
    private static final String COLONNES = "colonnes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COMMERCIAL = "Commercial";
    private static final String EXPORT_PROSPECTIONS = "/api/prospections/export";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private EntityManager em;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private ComActionMessageCategoryRepository categorieRepository;
    @Autowired private ComActionMessageTypeRepository typeRepository;

    private String s;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private AdmUser commercialA;

    @BeforeEach
    void setUp() {
        s = UUID.randomUUID().toString().substring(0, 8);
        trancheA = fixture.tranche("TR-EXPA-" + s, true);
        trancheB = fixture.tranche("TR-EXPB-" + s, true);
        commercialA = fixture.compte("export.commercial", COMMERCIAL);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercialA);
        habilitation.setCfgProject(trancheA.getCfgProject());
        habilitationRepository.save(habilitation);
    }

    // ------------------------------------------------------------------ prospections

    @Test
    void prospections_enTetesDansLOrdre_filtreEtPerimetreAppliques() throws Exception {
        ComProspection ouverte = prospection("PSN-A-" + s, trancheA, false);
        prospection("PSN-AF-" + s, trancheA, true);
        prospection("PSN-B-" + s, trancheB, false);
        relire();

        List<List<Object>> lignes = exporter(get(EXPORT_PROSPECTIONS)
                .param(COLONNES, "fermee,code")
                .param(TRANCHE_ID, trancheA.getId().toString(), trancheB.getId().toString())
                .param("ouvertes", "true")
                .with(connecte(commercialA, COMMERCIAL)), "prospections");

        assertThat(lignes).containsExactly(
                List.of(ColonneProspection.FERMEE.libelle(), ColonneProspection.CODE.libelle()),
                List.of("Non", ouverte.getCode()));
    }

    @Test
    void preProspections_filtreEtPerimetreAppliques() throws Exception {
        ComProspection ouverte = preProspection("PRE-A-" + s, trancheA, false);
        preProspection("PRE-AF-" + s, trancheA, true);
        preProspection("PRE-B-" + s, trancheB, false);
        relire();

        List<List<Object>> lignes = exporter(get("/api/pre-prospections/export")
                .param(COLONNES, "code")
                .param(TRANCHE_ID, trancheA.getId().toString(), trancheB.getId().toString())
                .param("ouvertes", "true")
                .with(connecte(commercialA, COMMERCIAL)), "pre-prospections");

        assertThat(lignes).containsExactly(
                List.of(ColonneProspection.CODE.libelle()),
                List.of(ouverte.getCode()));
    }

    // ------------------------------------------------------------------ prospects (aucun périmètre)

    @Test
    void prospects_enTetesDansLOrdre_filtreApplique() throws Exception {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-EXP-" + s);
        tierTypeRepository.save(type);
        CfgTier retenu = prospect("PROS-EXP-1-" + s, "Retenu" + s, type);
        prospect("PROS-EXP-2-" + s, "Autre" + s, type);
        relire();

        List<List<Object>> lignes = exporter(get("/api/prospects/export")
                .param(COLONNES, "nom,code")
                .param("type", type.getCode())
                .param("search", "Retenu" + s), "prospects");

        assertThat(lignes).containsExactly(
                List.of(ColonneProspect.NOM.libelle(), ColonneProspect.CODE.libelle()),
                List.of("Retenu" + s, retenu.getCode()));
    }

    // ------------------------------------------------------------------ encaissements

    @Test
    void encaissements_montantTypeFiltreEtPerimetreAppliques() throws Exception {
        StlSettlement retenu = reglement(trancheA, "IN_WORK");
        reglement(trancheA, "CANCELED");
        reglement(trancheB, "IN_WORK");
        relire();

        List<List<Object>> lignes = exporter(get("/api/encaissements/export")
                .param(COLONNES, "id,montantBrut")
                .param(TRANCHE_ID, trancheA.getId().toString(), trancheB.getId().toString())
                .param("etat", "IN_WORK")
                .with(connecte(commercialA, COMMERCIAL)), "encaissements");

        assertThat(lignes).containsExactly(
                List.of(ColonneEncaissement.ID.libelle(), ColonneEncaissement.MONTANT_BRUT.libelle()),
                List.of(retenu.getId().toString(), 1000.0));
    }

    // ------------------------------------------------------------------ suivi des actions

    @Test
    void suiviActions_filtreEtPerimetreAppliques() throws Exception {
        ComActionMessageCategory categorie = categorieRepository.findById(ID_CATEGORIE_PROSPECTION).orElseGet(() -> {
            ComActionMessageCategory c = new ComActionMessageCategory();
            c.setId(ID_CATEGORIE_PROSPECTION);
            c.setCode("Prospection");
            c.setDescription("Prospection");
            return categorieRepository.save(c);
        });
        ComActionMessageType appel = typeAction("Appel " + s);
        ComActionMessageType visite = typeAction("Visite " + s);
        ComActionMessage retenue = action(categorie, appel, prospection("PSN-SA-" + s, trancheA, false));
        action(categorie, visite, prospection("PSN-SV-" + s, trancheA, false));
        action(categorie, appel, prospection("PSN-SB-" + s, trancheB, false));
        relire();

        List<List<Object>> lignes = exporter(get("/api/suivi-actions/export")
                .param(COLONNES, "trancheCode,id")
                .param("typeId", appel.getId().toString())
                .with(connecte(commercialA, COMMERCIAL)), "suivi-actions");

        assertThat(lignes).containsExactly(
                List.of(ColonneSuiviAction.TRANCHE_CODE.libelle(), ColonneSuiviAction.ID.libelle()),
                List.of(trancheA.getCode(), retenue.getId().toString()));
    }

    @Test
    void suiviActions_nonAdministrateur_nExportePasLesActionsDUnAutre() throws Exception {
        ComActionMessageCategory categorie = categorieRepository.findById(ID_CATEGORIE_PROSPECTION).orElseGet(() -> {
            ComActionMessageCategory c = new ComActionMessageCategory();
            c.setId(ID_CATEGORIE_PROSPECTION);
            c.setCode("Prospection");
            c.setDescription("Prospection");
            return categorieRepository.save(c);
        });
        ComActionMessageType appel = typeAction("Appel " + s);
        ComActionMessage mienne = action(categorie, appel, prospection("PSN-SM-" + s, trancheA, false));
        ComActionMessage autre = new ComActionMessage();
        autre.setId(UUID.randomUUID());
        autre.setDocDate(LocalDateTime.of(2026, 9, 21, 11, 0));
        autre.setUserName("Autre " + s);
        autre.setAdmUser(fixture.compte("export.autre", COMMERCIAL));
        autre.setComActionMessageCategory(categorie);
        autre.setComActionMessageType(appel);
        autre.setComProspection(prospection("PSN-SX-" + s, trancheA, false));
        autre.setIsSystem(false);
        autre.setIsUnpaid(false);
        messageRepository.save(autre);
        relire();

        List<List<Object>> lignes = exporter(get("/api/suivi-actions/export")
                .param(COLONNES, "id")
                .param("typeId", appel.getId().toString())
                .with(connecte(commercialA, COMMERCIAL)), "suivi-actions");

        assertThat(lignes).containsExactly(
                List.of(ColonneSuiviAction.ID.libelle()),
                List.of(mienne.getId().toString()));
    }

    @Test
    void prospections_filtreAvanceProjetApplique() throws Exception {
        AdmUserHabilitation habilitationB = new AdmUserHabilitation();
        habilitationB.setId(UUID.randomUUID());
        habilitationB.setAdmUser(commercialA);
        habilitationB.setCfgProject(trancheB.getCfgProject());
        habilitationRepository.save(habilitationB);
        ComProspection dansA = prospection("PSN-PA-" + s, trancheA, false);
        prospection("PSN-PB-" + s, trancheB, false);
        relire();

        List<List<Object>> lignes = exporter(get(EXPORT_PROSPECTIONS)
                .param(COLONNES, "code")
                .param(TRANCHE_ID, trancheA.getId().toString(), trancheB.getId().toString())
                .param("projetId", trancheA.getCfgProject().getId().toString())
                .with(connecte(commercialA, COMMERCIAL)), "prospections");

        assertThat(lignes).containsExactly(
                List.of(ColonneProspection.CODE.libelle()),
                List.of(dansA.getCode()));
    }

    // ------------------------------------------------------------------ format

    @Test
    void formatAutreQueXlsx_rend400SurChaqueListe() throws Exception {
        for (String route : List.of(EXPORT_PROSPECTIONS, "/api/pre-prospections/export",
                "/api/prospects/export", "/api/encaissements/export", "/api/suivi-actions/export")) {
            mockMvc.perform(get(route).param("format", "csv")).andExpect(status().isBadRequest());
        }
    }

    @Test
    void colonneInconnue_rend400() throws Exception {
        mockMvc.perform(get(EXPORT_PROSPECTIONS).param(COLONNES, "inexistante"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ outils

    private void relire() {
        em.flush();
        em.clear();
    }

    /** Lignes du classeur (en-tête compris) : texte, nombre ({@code Double}) ou {@code null} si vide. */
    private List<List<Object>> exporter(MockHttpServletRequestBuilder requete, String liste) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete.param("format", "xlsx"))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", ExportTableur.XLSX.toString()))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern(".*\"" + liste + "_\\d{8}_\\d{6}\\.xlsx\".*")))
                .andReturn().getResponse().getContentAsByteArray();
        List<List<Object>> lignes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            for (Row row : feuille) {
                List<Object> valeurs = new ArrayList<>();
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    valeurs.add(valeur(row.getCell(i)));
                }
                lignes.add(valeurs);
            }
        }
        return lignes;
    }

    private static Object valeur(Cell cellule) {
        if (cellule == null || cellule.getCellType() == CellType.BLANK) {
            return null;
        }
        return cellule.getCellType() == CellType.NUMERIC ? cellule.getNumericCellValue() : cellule.getStringCellValue();
    }

    private ComProspection prospection(String code, CfgTranche tranche, boolean fermee) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(fixture.tier("CLI-" + code));
        p.setStkHierarchy(tranche);
        p.setIsClosed(fermee);
        p.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        return prospectionRepository.save(p);
    }

    private ComProspection preProspection(String code, CfgTranche tranche, boolean fermee) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setStkHierarchy(tranche);
        p.setIsClosed(fermee);
        p.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        return prospectionRepository.save(p);
    }

    private CfgTier prospect(String code, String nom, CfgTierType type) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName(nom);
        tier.setIsProspect(true);
        tier.setCfgTierType(type);
        return tierRepository.save(tier);
    }

    /** Règlement d'une échéance d'un dossier de {@code tranche} : la chaîne du périmètre encaissement. */
    private StlSettlement reglement(CfgTranche tranche, String etat) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(fixture.dossier("DOS-EXP-" + s, tranche, null));
        echeance.setAmount(new BigDecimal("1000"));
        documentRepository.save(echeance);

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode("ENC-EXP-" + s);
        reglement.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        reglement.setAmount(new BigDecimal("1000"));
        reglement.setStatus(etat);
        reglement.setIsCredit(true);
        settlementRepository.save(reglement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("1000"));
        settlementLineRepository.save(ligne);
        return reglement;
    }

    private ComActionMessageType typeAction(String libelle) {
        ComActionMessageType t = new ComActionMessageType();
        t.setId(UUID.randomUUID());
        t.setCode(libelle);
        t.setDescription(libelle);
        return typeRepository.save(t);
    }

    /** Action du commercial (son compte, hors « Système ») : la portée non administrateur la retient. */
    private ComActionMessage action(ComActionMessageCategory categorie, ComActionMessageType type,
                                    ComProspection prospection) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(LocalDateTime.of(2026, 9, 21, 10, 0));
        m.setUserName("Export " + s);
        m.setAdmUser(commercialA);
        m.setComActionMessageCategory(categorie);
        m.setComActionMessageType(type);
        m.setComProspection(prospection);
        m.setIsSystem(false);
        m.setIsUnpaid(false);
        return messageRepository.save(m);
    }
}

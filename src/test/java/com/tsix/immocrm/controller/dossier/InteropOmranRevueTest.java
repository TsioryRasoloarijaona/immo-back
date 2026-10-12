package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureStatutDossier;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue de {@code FEATURE-interop-omran-ecritures.md} : écarts constatés entre ce que le CRM écrit et ce
 * qu'OMRAN écrit (mesuré sur {@code yesRemsProd}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class InteropOmranRevueTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private FixturePatrimoine fixturePatrimoine;
    @Autowired
    private FixtureStatutDossier fixtureStatutDossier;
    @Autowired
    private StkItemRepository stkItemRepository;
    @Autowired
    private StkBuildingRepository buildingRepository;
    @Autowired
    private StkFloorRepository floorRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private ComSettlementCategoryRepository categoryRepository;
    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    @BeforeEach
    void setUp() {
        fixtureStatutDossier.reservation();
    }

    private ComProspection prospection(String suffixe) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-" + suffixe);
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-" + suffixe);
        prospection.setCfgProspect(prospect);
        return prospectionRepository.save(prospection);
    }

    private UUID creerSansPrix(String suffixe, StkItem... articles) throws Exception {
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-" + suffixe);
        paymentMethodRepository.save(modePaie);

        String ids = java.util.Arrays.stream(articles)
                .map(article -> "\"" + article.getId() + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        String body = """
                {"prospectionId":"%s","articleIds":[%s],"modePaieId":"%s","cin":"CIN-%s"}
                """.formatted(prospection(suffixe).getId(), ids, modePaie.getId(), suffixe);
        String json = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    /**
     * {@code UnderItem(Item, Sale)} d'OMRAN copie aussi {@code AreaNet}, {@code StkBuildingId} et
     * {@code StkFloorId} du sous-article ; mesuré sur les 90 {@code ComFolderItem} d'OMRAN : 90/90 non
     * NULL, {@code AreaNet = StkItem.Area} 90/90, bâtiment et étage = ceux du sous-article 90/90.
     */
    @Test
    void create_sousArticle_ligneComFolderItemCopieSurfaceNetteBatimentEtEtageCommeOmran() throws Exception {
        var chaine = fixturePatrimoine.chaine("REVUE-CFI");
        StkBuilding batiment = new StkBuilding();
        batiment.setId(UUID.randomUUID());
        batiment.setCode("IMM-REVUE-CFI");
        batiment = buildingRepository.save(batiment);
        StkFloor etage = new StkFloor();
        etage.setId(UUID.randomUUID());
        etage.setDescription("Rez de chaussée");
        etage = floorRepository.save(etage);

        StkItem lot = fixturePatrimoine.article(chaine, "LOT-REVUE-CFI");
        StkItem terrasse = fixturePatrimoine.article(chaine, "LOT-REVUE-CFI TER");
        terrasse.setParent(lot);
        terrasse.setIsUnderItem(true);
        terrasse.setArea(new BigDecimal("15"));
        terrasse.setStkBuilding(batiment);
        terrasse.setStkFloor(etage);
        stkItemRepository.save(terrasse);

        UUID dossierId = creerSansPrix("REVUE-CFI", lot);

        Object[] ligne = (Object[]) entityManager.createNativeQuery("""
                        SELECT CASE WHEN AreaNet = 15 THEN 1 ELSE 0 END,
                               CASE WHEN StkBuildingId = :batiment THEN 1 ELSE 0 END,
                               CASE WHEN StkFloorId = :etage THEN 1 ELSE 0 END
                        FROM ComFolderItem WHERE ComFolderId = :dossier AND StkItemId = :terrasse""")
                .setParameter("batiment", batiment.getId())
                .setParameter("etage", etage.getId())
                .setParameter("dossier", dossierId)
                .setParameter("terrasse", terrasse.getId())
                .getSingleResult();
        assertAll(
                () -> assertThat(((Number) ligne[0]).intValue()).as("ComFolderItem.AreaNet = StkItem.Area").isEqualTo(1),
                () -> assertThat(((Number) ligne[1]).intValue()).as("ComFolderItem.StkBuildingId du sous-article").isEqualTo(1),
                () -> assertThat(((Number) ligne[2]).intValue()).as("ComFolderItem.StkFloorId du sous-article").isEqualTo(1));
    }

    /**
     * §4 : {@code ExcludeFromPrice} = celui de la catégorie, y compris en {@code updateEcheance} ; OMRAN le
     * remet à la valeur de la catégorie à chaque changement de catégorie ({@code ComDocument.cs:1146}).
     */
    @Test
    void updateEcheance_changementDeCategorie_reprendExcludeFromPriceDeLaNouvelleCategorie() throws Exception {
        var chaine = fixturePatrimoine.chaine("REVUE-EXCL");
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-REVUE-EXCL");
        folder.setStkVocation(chaine.vocation());
        folder.setStkItem(fixturePatrimoine.article(chaine, "LOT-REVUE-EXCL"));
        folderRepository.save(folder);
        ComSettlementCategory avance = categorie(chaine, "AVANCE", false);
        ComSettlementCategory frais = categorie(chaine, "FRAIS DE DOSSIER", true);
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setComSettlementCategory(avance);
        echeance.setExcludeFromPrice(false);
        echeance.setAmount(new BigDecimal("5000"));
        documentRepository.save(echeance);

        mockMvc.perform(patch("/api/dossiers/{id}/echeancier/{echeanceId}", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorieId\":\"" + frais.getId() + "\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(documentRepository.findById(echeance.getId()).orElseThrow().getExcludeFromPrice())
                .as("FRAIS DE DOSSIER est exclue du prix : la ligne ne doit plus compter dans le prix contractuel")
                .isTrue();
    }

    /**
     * Sans {@code prix}, la doc promet « prix de référence cumulé des articles demandés (remise nulle) ».
     * Lot principal sans prix de référence + annexe chiffrée : aucune remise ne doit apparaître.
     */
    @Test
    void create_sansPrix_principalSansPrixEtAnnexeChiffree_remiseNulle() throws Exception {
        var chaine = fixturePatrimoine.chaine("REVUE-REMISE");
        StkItem lot = fixturePatrimoine.article(chaine, "LOT-REVUE-REMISE");
        lot.setVat(new BigDecimal("0.20"));
        stkItemRepository.save(lot);
        StkItem parking = fixturePatrimoine.article(chaine, "PRK-REVUE-REMISE");
        parking.setVat(new BigDecimal("0.20"));
        parking.setComSalePrice(new BigDecimal("100000"));
        stkItemRepository.save(parking);

        UUID dossierId = creerSansPrix("REVUE-REMISE", lot, parking);

        ComFolder f = folderRepository.findById(dossierId).orElseThrow();
        assertAll(
                () -> assertThat(f.getPrice()).isEqualByComparingTo("100000"),
                () -> assertThat(f.getPriceBeforeDiscount()).as("prix avant remise = prix").isEqualByComparingTo("100000"),
                () -> assertThat(f.getDiscountAmount()).as("remise").isEqualByComparingTo("0"));
    }

    /**
     * {@code addReglement} écrit désormais {@code ReceiptCode = ""} et {@code Code = ""} (sans référence) ;
     * le contrôleur ne retombe sur l'identifiant du règlement que si ces colonnes sont NULL : tous les
     * reçus (et autorisations d'un règlement sans référence) se téléchargent sous le même nom.
     */
    @Test
    void documentsDuReglementSansReference_nomsDeFichierPortentLIdentifiantDuReglement() throws Exception {
        var chaine = fixturePatrimoine.chaine("REVUE-RECU");
        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-REVUE-RECU");
        client.setDescription("Client revue");
        tierRepository.save(client);
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-REVUE-RECU");
        folder.setStkVocation(chaine.vocation());
        folder.setStkItem(fixturePatrimoine.article(chaine, "LOT-REVUE-RECU"));
        folder.setCfgCustomer(client);
        folderRepository.save(folder);
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setComSettlementCategory(categorie(chaine, "AVANCE", false));
        echeance.setAmount(new BigDecimal("100000"));
        documentRepository.save(echeance);
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("ESPECES-REVUE");
        type.setDescription("ESPECES-REVUE");
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        settlementTypeRepository.save(type);
        String corps = "{\"montant\":1000,\"dateEcheance\":\"2026-09-01T10:00:00\",\"typeId\":\"" + type.getId()
                + "\",\"payeurId\":\"" + client.getId() + "\"}";

        String json = mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                                folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reglementId = com.jayway.jsonpath.JsonPath.read(json, "$.id");

        String recu = mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        folder.getId(), reglementId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        String autorisation = mockMvc.perform(get(
                        "/api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement",
                        folder.getId(), reglementId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertAll(
                () -> assertThat(recu).as("nom du fichier du reçu").contains(reglementId),
                () -> assertThat(autorisation).as("nom du fichier de l'autorisation").contains(reglementId));
    }

    private ComSettlementCategory categorie(FixturePatrimoine.Chaine chaine, String code, boolean exclue) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription(code);
        categorie.setStkVocation(chaine.vocation());
        categorie.setIsCredit(true);
        categorie.setIsSaleAdvance(false);
        categorie.setExcludeFromPrice(exclue);
        categorie.setVatRatio(new BigDecimal("0.2"));
        return categoryRepository.save(categorie);
    }
}

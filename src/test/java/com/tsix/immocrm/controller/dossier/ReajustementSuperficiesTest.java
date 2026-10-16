package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Réajuster les superficies d'une tranche — FEATURE-vente-fiche.md §B6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class ReajustementSuperficiesTest {

    private static final String REAJUSTEMENT = "/api/tranches/{id}/reajustement-superficies";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private ComFolderItemRepository folderItemRepository;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;
    @Autowired private EntityManager em;

    private CfgTranche tranche;
    private ComSettlementCategory avance;
    private ComSettlementCategory autofinancement;
    private ComSettlementCategory credit;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("REAJ-TR", true);
        avance = fixture.categorieEcheance("AVANCE-REAJ", true, false, false, null);
        autofinancement = fixture.categorieEcheance("AUTOFIN-REAJ", false, true, false, null);
        credit = fixture.categorieEcheance("CREDIT-REAJ", false, false, true, null);
    }

    /** 100 m² × 10 000 HT, TVA 20 %, sans remise : 1 200 000 TTC ; le stock passe à {@code stock} m². */
    private ComFolder dossier(String code, String stock) {
        ComFolder dossier = fixture.dossier(code, tranche, StatutDossier.CONCRETISATION);
        dossier.setPrice(new BigDecimal("1200000"));
        dossier.setPriceAfterDiscount(new BigDecimal("1200000"));
        dossier.setDiscountAmount(BigDecimal.ZERO);
        folderRepository.save(dossier);
        StkItem article = dossier.getStkItem();
        article.setArea(new BigDecimal(stock));
        stkItemRepository.save(article);
        return dossier;
    }

    private ResultActions valider(UUID... ids) throws Exception {
        StringBuilder corps = new StringBuilder("{\"dossierIds\":[");
        for (int i = 0; i < ids.length; i++) {
            corps.append(i > 0 ? "," : "").append('"').append(ids[i]).append('"');
        }
        corps.append("]}");
        return mockMvc.perform(post(REAJUSTEMENT, tranche.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corps.toString()));
    }

    @Test
    void verifierPuisValider_ecartReporteSurLaDerniereLigneDAutofinancement() throws Exception {
        ComFolder dossier = dossier("DOS-REAJ1", "105");
        fixture.echeance(dossier, avance, new BigDecimal("200000"), LocalDateTime.of(2026, 1, 1, 0, 0));
        fixture.echeance(dossier, autofinancement, new BigDecimal("400000"), LocalDateTime.of(2026, 2, 1, 0, 0));
        ComDocument derniere = fixture.echeance(dossier, autofinancement, new BigDecimal("600000"),
                LocalDateTime.of(2026, 3, 1, 0, 0));
        fixture.echeance(dossier, credit, BigDecimal.ZERO, LocalDateTime.of(2026, 4, 1, 0, 0));

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dossierId").value(dossier.getId().toString()))
                .andExpect(jsonPath("$[0].differenceSuperficie").value(5))
                .andExpect(jsonPath("$[0].differenceSchema").value(60000))
                .andExpect(jsonPath("$[0].ligneCible").value("AUTOFINANCEMENT"))
                .andExpect(jsonPath("$[0].ligneCibleId").value(derniere.getId().toString()));

        valider(dossier.getId()).andExpect(status().isOk());

        em.flush();
        em.clear();
        ComDocument relue = documentRepository.findById(derniere.getId()).orElseThrow();
        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        assertAll(
                () -> assertThat(relue.getAmount()).isEqualByComparingTo("660000"),
                () -> assertThat(relue.getNotes()).startsWith("Document modifié lors du réajustement des superficies par"),
                () -> assertThat(relu.getArea()).isEqualByComparingTo("105"),
                () -> assertThat(relu.getPriceAfterDiscount()).isEqualByComparingTo("1260000"),
                () -> assertThat(relu.getUserUpdateDateTime()).isNotNull());
    }

    @Test
    void valider_sansAutofinancement_ecartSurLaDerniereLigneDeCredit() throws Exception {
        ComFolder dossier = dossier("DOS-REAJ2", "95");
        fixture.echeance(dossier, avance, new BigDecimal("200000"), LocalDateTime.of(2026, 1, 1, 0, 0));
        ComDocument ligneCredit = fixture.echeance(dossier, credit, new BigDecimal("1000000"),
                LocalDateTime.of(2026, 5, 1, 0, 0));

        valider(dossier.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ligneCible").value("CREDIT"))
                .andExpect(jsonPath("$[0].differenceSchema").value(-60000));

        em.flush();
        em.clear();
        assertThat(documentRepository.findById(ligneCredit.getId()).orElseThrow().getAmount())
                .isEqualByComparingTo("940000");
    }

    @Test
    void valider_niAutofinancementNiCredit_creeUneLigneDAutofinancement() throws Exception {
        ComFolder dossier = dossier("DOS-REAJ3", "110");
        fixture.categorieEcheance("AUTOFIN-VOC", false, true, false, dossier.getStkVocation());
        fixture.echeance(dossier, avance, new BigDecimal("1200000"), LocalDateTime.of(2026, 1, 1, 0, 0));

        valider(dossier.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ligneCible").value("NOUVELLE_LIGNE_AUTOFINANCEMENT"));

        em.flush();
        em.clear();
        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId());
        assertThat(lignes).hasSize(2)
                .anySatisfy(d -> assertThat(d.getAmount()).isEqualByComparingTo("120000"));
    }

    @Test
    void valider_dossierSansEcart_409() throws Exception {
        ComFolder dossier = dossier("DOS-REAJ4", "100");
        fixture.echeance(dossier, autofinancement, new BigDecimal("1200000"), LocalDateTime.of(2026, 1, 1, 0, 0));

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        valider(dossier.getId()).andExpect(status().isConflict());
    }

    /** Dossier saisi au forfait comme ARS 2024/00286 : 1 m², prix du m² nul, 1 290 000 TTC ; le stock dit 88 m². */
    private ComFolder dossierAuForfait(String code, BigDecimal prixM2HT, BigDecimal superficie) {
        ComFolder dossier = dossier(code, "88");
        dossier.setArea(superficie);
        dossier.setUnitCostHT(prixM2HT);
        dossier.setUnitCost(prixM2HT);
        dossier.setItemPriceHT(BigDecimal.ZERO);
        dossier.setPriceBeforeDiscountHT(BigDecimal.ZERO);
        dossier.setPriceAfterDiscountHT(BigDecimal.ZERO);
        dossier.setPrice(new BigDecimal("1290000"));
        dossier.setPriceAfterDiscount(new BigDecimal("1290000"));
        return folderRepository.save(dossier);
    }

    @Test
    void dossierAuForfait_prixDuM2NulOuAbsent_exclusAvecMotif_etPostRefuseSansRienEcrire() throws Exception {
        ComFolder nul = dossierAuForfait("DOS-REAJ8", BigDecimal.ZERO, BigDecimal.ONE);
        ComDocument ligneNul = fixture.echeance(nul, autofinancement, new BigDecimal("1290000"),
                LocalDateTime.of(2026, 1, 1, 0, 0));
        ComFolder absent = dossierAuForfait("DOS-REAJ9", null, null);
        fixture.echeance(absent, autofinancement, new BigDecimal("1290000"), LocalDateTime.of(2026, 1, 1, 0, 0));

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dossierId").value(nul.getId().toString()))
                .andExpect(jsonPath("$[0].motifExclusion").value("Prix du m² absent sur le dossier : réajustement impossible"))
                .andExpect(jsonPath("$[0].nouveauPrix").doesNotExist())
                .andExpect(jsonPath("$[0].differenceSchema").doesNotExist())
                .andExpect(jsonPath("$[1].motifExclusion").value("Prix du m² absent sur le dossier : réajustement impossible"));

        valider(nul.getId()).andExpect(status().isConflict());

        em.flush();
        em.clear();
        ComFolder relu = folderRepository.findById(nul.getId()).orElseThrow();
        assertAll(
                () -> assertThat(documentRepository.findById(ligneNul.getId()).orElseThrow().getAmount())
                        .isEqualByComparingTo("1290000"),
                () -> assertThat(relu.getArea()).isEqualByComparingTo("1"),
                () -> assertThat(relu.getPriceAfterDiscount()).isEqualByComparingTo("1290000"));
    }

    @Test
    void sousArticleSansPrixDeStock_superficieChangee_exclusAvecMotif() throws Exception {
        ComFolder dossier = dossier("DOS-REAJ10", "100");
        fixture.echeance(dossier, autofinancement, new BigDecimal("1200000"), LocalDateTime.of(2026, 1, 1, 0, 0));
        StkItem cave = new StkItem();
        cave.setId(UUID.randomUUID());
        cave.setCode("CAVE-REAJ10");
        cave.setStkItemType(dossier.getStkItem().getStkItemType());
        cave.setStkItemCategory(dossier.getStkItem().getStkItemCategory());
        cave.setStkVocation(dossier.getStkVocation());
        cave.setStkKind(dossier.getStkItem().getStkKind());
        cave.setArea(new BigDecimal("12"));
        cave.setSalePrice(BigDecimal.ZERO);
        stkItemRepository.save(cave);
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(dossier);
        ligne.setStkItem(cave);
        ligne.setCode("CAVE-REAJ10");
        ligne.setIsUnderItem(true);
        ligne.setArea(new BigDecimal("10"));
        ligne.setVATRatio(new BigDecimal("0.2"));
        ligne.setPriceBeforeDiscountHT(new BigDecimal("50000"));
        folderItemRepository.save(ligne);

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motifExclusion")
                        .value("Prix de stock absent pour le sous-article CAVE-REAJ10 : réajustement impossible"));
        valider(dossier.getId()).andExpect(status().isConflict());
    }

    @Test
    void dossierSansStatut_ecartNul_nonReajuste() throws Exception {
        ComFolder dossier = dossier("DOS-REAJ6", "105");
        dossier.setStrStatus(null);
        folderRepository.save(dossier);
        fixture.echeance(dossier, autofinancement, new BigDecimal("1200000"), LocalDateTime.of(2026, 1, 1, 0, 0));

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        valider(dossier.getId()).andExpect(status().isConflict());
    }

    @Test
    void valider_dossierHorsDeLaTranche_404() throws Exception {
        ComFolder ailleurs = fixture.dossier("DOS-REAJ5", fixture.tranche("REAJ-AUTRE", true),
                StatutDossier.CONCRETISATION);

        valider(ailleurs.getId()).andExpect(status().isNotFound());
    }

    @Test
    void cleApiReferentiels_surUnCompteAdministrateur_horsPortee_403() throws Exception {
        dossier("DOS-REAJ7", "105");
        AdmUser compte = fixture.compte("N8N-REAJ", "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-reaj-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        assertThat(PorteeCleApi.REFERENTIELS_LIRE.couvre("GET", "/api/tranches/" + tranche.getId())).isTrue();
    }

    @Test
    void verifier_trancheInconnue_404() throws Exception {
        mockMvc.perform(get(REAJUSTEMENT, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void verifier_sansRoleAdministrateur_403() throws Exception {
        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()))
                .andExpect(status().isForbidden());
    }
}

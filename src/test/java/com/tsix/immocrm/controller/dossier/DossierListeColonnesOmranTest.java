package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.services.dossier.liste.LigneDossier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Colonnes de parité OMRAN de la liste des dossiers, une famille par test (FEATURE-vente-liste.md §1). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeColonnesOmranTest {

    private static final String PREMIER = "$.content[0].";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private FixtureInteraction fixtureInteraction;
    @Autowired
    private EntityManager em;

    private CfgTranche tranche;
    private String prefixe;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TR-LST-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "LSTC" + UUID.randomUUID().toString().substring(0, 6);
    }

    private ComFolder dossier() {
        return fixture.dossier(prefixe, tranche, StatutDossier.CONCRETISATION);
    }

    private void relire() {
        em.flush();
        em.clear();
    }

    @Test
    void catalogueEtDto_ontLesMemesNoms() {
        assertThat(Arrays.stream(DossierListItemDto.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactlyInAnyOrderElementsOf(ColonneDossier.nomsValides());
        assertThat(LigneDossier.nombreDeComposantes()).isEqualTo(ColonneDossier.values().length);
    }

    @Test
    void familleClient_identiteEtTelephonesFormates() throws Exception {
        ComFolder f = dossier();
        CfgTier client = f.getCfgCustomer();
        client.setFirstName("Amina");
        client.setLastName("BENALI");
        client.setMobile("612345678");
        client.setMobileIndicatif("212");
        client.setPhone("522000000");
        client.setIDNumber("BK123456");
        f.setIsOldCustomer(true);
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes",
                        "ancienClient,clientPrenom,clientNomFamille,clientMobile,clientMobileIndicatif,clientCin,"
                                + "clientMobileComplet,clientTelephone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "ancienClient").value(true))
                .andExpect(jsonPath(PREMIER + "clientPrenom").value("Amina"))
                .andExpect(jsonPath(PREMIER + "clientNomFamille").value("BENALI"))
                .andExpect(jsonPath(PREMIER + "clientMobile").value("612345678"))
                .andExpect(jsonPath(PREMIER + "clientMobileIndicatif").value("212"))
                .andExpect(jsonPath(PREMIER + "clientCin").value("BK123456"))
                .andExpect(jsonPath(PREMIER + "clientMobileComplet").value("(212) 612345678"))
                .andExpect(jsonPath(PREMIER + "clientTelephone").value("522000000"));
    }

    @Test
    void familleDates_jalonsDuDossierEtDeLaTranche() throws Exception {
        ComFolder f = dossier();
        f.setBankRecordDate(LocalDateTime.of(2025, 2, 6, 0, 0));
        f.setAgreementSavingDate(LocalDateTime.of(2025, 3, 7, 0, 0));
        f.setIsConfirmedAgreementDate(true);
        f.setContractPrintDate(LocalDateTime.of(2025, 5, 9, 0, 0));
        tranche.setOccupancyPermitsDate(LocalDateTime.of(2026, 6, 30, 0, 0));
        tranche.setDeliveryDate(LocalDateTime.of(2026, 7, 31, 0, 0));
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes",
                        "dateFicheBancaire,dateMinute,envoiNotaireConfirme,dateImpressionContrat,datePermisHabiter,"
                                + "dateRemiseClesPrevue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "dateFicheBancaire").value("2025-02-06T00:00:00"))
                .andExpect(jsonPath(PREMIER + "dateMinute").value("2025-03-07T00:00:00"))
                .andExpect(jsonPath(PREMIER + "envoiNotaireConfirme").value(true))
                .andExpect(jsonPath(PREMIER + "dateImpressionContrat").value("2025-05-09T00:00:00"))
                .andExpect(jsonPath(PREMIER + "datePermisHabiter").value("2026-06-30T00:00:00"))
                .andExpect(jsonPath(PREMIER + "dateRemiseClesPrevue").value("2026-07-31T00:00:00"));
    }

    /** Cas réel APF 2022/00193 : 39 m², 700 000 après remise, TVA 20 % → 17 948,717949 TTC et 14 957,264957 HT le m². */
    @Test
    void famillePrix_prixAuMetreCarreCommeLaVue() throws Exception {
        ComFolder f = dossier();
        f.setArea(new BigDecimal("39"));
        f.setPriceAfterDiscount(new BigDecimal("700000"));
        f.setItemPriceAfter(new BigDecimal("700000"));
        f.setItemPriceAfterHT(new BigDecimal("583333.333333"));
        f.setPriceHT(new BigDecimal("583333.333333"));
        f.setVATRatio(new BigDecimal("0.2"));
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes",
                        "superficie,tva,prixHt,prixArticleTtc,prixM2Ttc,prixVenteM2Ttc,prixVenteM2Ht,prixM2HtAvantRemise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "superficie").value(39.0))
                .andExpect(jsonPath(PREMIER + "tva").value(0.2))
                .andExpect(jsonPath(PREMIER + "prixHt").value(583333.333333))
                .andExpect(jsonPath(PREMIER + "prixArticleTtc").value(700000.0))
                .andExpect(jsonPath(PREMIER + "prixM2Ttc").value(17948.717949))
                .andExpect(jsonPath(PREMIER + "prixVenteM2Ttc").value(17948.717949))
                .andExpect(jsonPath(PREMIER + "prixVenteM2Ht").value(14957.264957))
                .andExpect(jsonPath(PREMIER + "prixM2HtAvantRemise").value(10000.0));
    }

    /** Cas réel MBR 2022/00205 dans ComFolderView : « prix de vente HT » = UnitCostHT 12 000, ItemPriceHT 876 000. */
    @Test
    void famillePrix_prixDeVenteHtCommeLaVueOmran() throws Exception {
        ComFolder f = dossier();
        f.setArea(new BigDecimal("73"));
        f.setUnitCostHT(new BigDecimal("12000"));
        f.setItemPriceHT(new BigDecimal("876000"));
        f.setUnderItemsPriceHT(BigDecimal.ZERO);
        f.setItemPriceAfterHT(new BigDecimal("750000"));
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe)
                        .param("colonnes", "prixM2Ht,prixArticleHt,prixSousArticlesHt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "prixM2Ht").value(12000.0))
                .andExpect(jsonPath(PREMIER + "prixArticleHt").value(876000.0))
                .andExpect(jsonPath(PREMIER + "prixSousArticlesHt").value(0.0));
    }

    /** La superficie des annexes (ComFolderItem sous-article) entre au dénominateur : 700 000 / (39 + 11). */
    @Test
    void famillePrix_annexesAjouteesALaSuperficie() throws Exception {
        ComFolder f = dossier();
        f.setArea(new BigDecimal("39"));
        f.setPriceAfterDiscount(new BigDecimal("700000"));
        f.setVATRatio(new BigDecimal("0.2"));
        em.persist(itemDuDossier(f, "PARK-1", "Parking 1", new BigDecimal("11"), true));
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "prixVenteM2Ttc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "prixVenteM2Ttc").value(14000.0));
    }

    @Test
    void familleDelais_joursEntreJalons_etZeroMeilleurTauxSansDecision() throws Exception {
        ComFolder f = dossier();
        f.setLoanCompanySendDate(LocalDateTime.of(2025, 1, 10, 0, 0));
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "delaiMeilleurTaux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "delaiMeilleurTaux").value(0));
    }

    @Test
    void familleActeurs_banque() throws Exception {
        ComFolder f = dossier();
        CfgTier banque = fixture.tier("BANQ");
        banque.setFirstName(null);
        banque.setLastName(null);
        banque.setSocialReason("BANQUE POPULAIRE");
        f.setCreditBank(banque);
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "banqueCredit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "banqueCredit").value("BANQUE POPULAIRE"));
    }

    /** Colonnes vides à plus de 99 % en base réelle : retirées du catalogue (FEATURE-dossiers-liste-complete.md B5).
     *  `numeroTitre` y revient (FEATURE-dossiers-colonnes-omran.md §2.1). */
    @Test
    void colonneRetireeDuCatalogue_400() throws Exception {
        mockMvc.perform(get("/api/dossiers").param("colonnes", "code,gestionnaire2Nom"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void familleCalculees_sousArticlesCadeauxModificationsCoAcquereurs() throws Exception {
        ComFolder f = dossier();
        em.persist(itemDuDossier(f, "A-01", "Appartement 1", BigDecimal.ZERO, false));
        em.persist(itemDuDossier(f, "P-07", "Parking 7", BigDecimal.ONE, true));
        CfgTier coAcquereur = fixture.tier("COACQ");
        coAcquereur.setDescription("KARIMI Youssef");
        ComFolderCustomer lien = new ComFolderCustomer();
        lien.setId(UUID.randomUUID());
        lien.setComFolder(f);
        lien.setCfgTier(coAcquereur);
        em.persist(lien);
        em.flush();
        inserer("INSERT INTO ComFolderGift (PKey, ComFolderId) VALUES (NEWID(), ?1)", f.getId());
        inserer("INSERT INTO ComFolderGift (PKey, ComFolderId) VALUES (NEWID(), ?1)", f.getId());
        inserer("INSERT INTO ComModification (PKey, Status, ComFolderId) VALUES (NEWID(), 'ENCOURS', ?1)", f.getId());
        inserer("INSERT INTO ComModification (PKey, Status, ComFolderId) VALUES (NEWID(), 'VALIDEE', ?1)", f.getId());
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes",
                        "sousArticlesCodes,sousArticlesLibelles,nombreCadeaux,nombreEtatsModification,autresClients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "sousArticlesCodes").value("A-01, P-07"))
                .andExpect(jsonPath(PREMIER + "sousArticlesLibelles").value("Appartement 1, Parking 7"))
                .andExpect(jsonPath(PREMIER + "nombreCadeaux").value(2))
                .andExpect(jsonPath(PREMIER + "nombreEtatsModification").value(1))
                .andExpect(jsonPath(PREMIER + "autresClients").value("KARIMI Youssef"));
    }

    @Test
    void familleSuivi_derniereCommunicationAlerteInstructionEtActions() throws Exception {
        AdmUser compte = fixture.compte("LST-INSTR");
        ComFolder f = dossier();
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-" + prefixe);
        p.setDocDate(LocalDateTime.now().minusMonths(2));
        em.persist(p);
        f.setComProspection(p);

        ComActionMessageCategory communication = fixtureInteraction.categorieProspection();
        ComActionMessageSubCategory resultat = new ComActionMessageSubCategory();
        resultat.setId(UUID.randomUUID());
        resultat.setCode("RAPPEL-" + prefixe);
        resultat.setDescription("A RAPPELER");
        resultat.setComActionMessageCategory(communication);
        em.persist(resultat);

        ComActionMessage ancienne = message(p, communication, LocalDateTime.now().minusDays(10));
        ancienne.setAlertMessage("Premier appel");
        ComActionMessage derniere = message(p, communication, LocalDateTime.now().minusDays(1));
        derniere.setComActionMessageSubCategory(resultat);
        derniere.setIsNotRead(true);
        derniere.setCfgEmployee(compte.getCfgEmployee());

        AdmAlertData alerte = new AdmAlertData();
        alerte.setId(UUID.randomUUID());
        alerte.setRelatedKey(derniere.getId());
        alerte.setActive(true);
        alerte.setAlertDate(LocalDateTime.of(2025, 6, 1, 10, 0));
        em.persist(alerte);

        fixture.categoriesAction();
        ComActionMessage action = message(null, em.find(ComActionMessageCategory.class,
                ActionsDossier.CATEGORIE_RESERVATION), LocalDateTime.now().minusDays(3));
        action.setComFolder(f);
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes",
                                "derniereCommunicationResultat,derniereCommunicationMessage,derniereCommunicationDate,"
                                        + "alerte,instructionNonLue,nombreActions")
                        .with(FixtureDossierVente.connecte(compte)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "derniereCommunicationResultat").value("A RAPPELER"))
                .andExpect(jsonPath(PREMIER + "derniereCommunicationMessage").value("A RAPPELER"))
                .andExpect(jsonPath(PREMIER + "derniereCommunicationDate").exists())
                .andExpect(jsonPath(PREMIER + "alerte").value("2025-06-01T10:00:00"))
                .andExpect(jsonPath(PREMIER + "instructionNonLue").value(true))
                .andExpect(jsonPath(PREMIER + "nombreActions").value(1));
    }

    /** L'instruction non lue ne s'affiche qu'au commercial qu'elle vise, comme l'icône d'OMRAN. */
    @Test
    void instructionNonLue_fausseQuandElleViseUnAutreCommercial() throws Exception {
        AdmUser autre = fixture.compte("LST-AUTRE");
        ComFolder f = dossier();
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN2-" + prefixe);
        p.setDocDate(LocalDateTime.now().minusMonths(1));
        em.persist(p);
        f.setComProspection(p);
        ComActionMessage m = message(p, fixtureInteraction.categorieProspection(), LocalDateTime.now().minusDays(1));
        m.setIsNotRead(true);
        m.setCfgEmployee(fixture.tier("EMP-VISE"));
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "instructionNonLue")
                        .with(FixtureDossierVente.connecte(autre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "instructionNonLue").value(false));
    }

    @Test
    void colonneCalculeeNonDemandee_absenteDeLaReponse() throws Exception {
        dossier();
        relire();

        mockMvc.perform(get("/api/dossiers").param("search", prefixe).param("colonnes", "code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "code").exists())
                .andExpect(jsonPath(PREMIER + "nombreCadeaux").doesNotExist())
                .andExpect(jsonPath(PREMIER + "libre").doesNotExist());
    }

    private ComActionMessage message(ComProspection p, ComActionMessageCategory categorie, LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(date);
        m.setComProspection(p);
        m.setComActionMessageCategory(categorie);
        m.setIsSystem(false);
        m.setIsUnpaid(false);
        m.setIsNotRead(false);
        em.persist(m);
        return m;
    }

    private ComFolderItem itemDuDossier(ComFolder f, String code, String libelle, BigDecimal superficie,
                                        boolean annexe) {
        ComFolderItem item = new ComFolderItem();
        item.setId(UUID.randomUUID());
        item.setComFolder(f);
        item.setCode(code);
        item.setDescription(libelle);
        item.setArea(superficie);
        item.setIsUnderItem(annexe);
        item.setIsUnderItemOrigin(false);
        return item;
    }

    private void inserer(String sql, UUID dossierId) {
        em.createNativeQuery(sql).setParameter(1, dossierId).executeUpdate();
    }
}

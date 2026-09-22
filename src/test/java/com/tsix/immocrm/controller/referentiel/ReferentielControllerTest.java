package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.com.ComProspectionProfile;
import com.tsix.immocrm.entities.com.ComProspectionProfileCategory;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReferentielControllerTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionOriginRepository originRepository;

    @Autowired
    private ComProspectionOriginSupportRepository originSupportRepository;

    @Autowired
    private CfgTierTypeRepository tierTypeRepository;

    @Autowired
    private CfgTierKindRepository tierKindRepository;

    @Autowired
    private AdmCountryRepository countryRepository;

    @Autowired
    private AdmCityRepository cityRepository;

    @Autowired
    private ComProspectionProfileCategoryRepository profileCategoryRepository;

    @Autowired
    private ComProspectionProfileRepository profileRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private CfgCompanyRepository companyRepository;

    /** Entité juridique en exploitation — c'est {@code isActive} qui la rend visible. */
    private CfgCompany newActiveCompany() {
        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("ADMOD");
        company.setDescription("ADMOD");
        company.setIsActive(true);
        return companyRepository.save(company);
    }

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StkKindRepository stkKindRepository;

    @Autowired
    private StkFloorRepository stkFloorRepository;

    @Autowired
    private AdmRoleRepository roleRepository;

    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private AdmAttachedFileTypeRepository attachedFileTypeRepository;

    @Test
    void getTypesPieceJointe_returnsCodeAndDescription() throws Exception {
        AdmAttachedFileType type = new AdmAttachedFileType();
        type.setId(UUID.randomUUID());
        type.setCode("PIECE D'IDENTITE TEST");
        type.setDescription("PIECE D'IDENTITE TEST");
        attachedFileTypeRepository.save(type);

        mockMvc.perform(get("/api/referentiels/types-piece-jointe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code==\"PIECE D'IDENTITE TEST\")].description").value("PIECE D'IDENTITE TEST"));
    }

    @Test
    void getRoles_returnsCodeAndDescription() throws Exception {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode("ROLE-TEST-1");
        role.setDescription("Rôle de test");
        roleRepository.save(role);

        mockMvc.perform(get("/api/referentiels/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='ROLE-TEST-1')].description").value("Rôle de test"));
    }

    @Test
    void getTypesReglement_exposeBanqueObligatoire() throws Exception {
        StlSettlementType cheque = new StlSettlementType();
        cheque.setId(UUID.randomUUID());
        cheque.setCode("CHEQUE-TEST");
        cheque.setDescription("CHEQUE TEST");
        cheque.setIsBankMandatory(true);
        cheque.setIsReferenceMandatory(true);
        settlementTypeRepository.save(cheque);

        StlSettlementType especes = new StlSettlementType();
        especes.setId(UUID.randomUUID());
        especes.setCode("ESPECES-TEST");
        especes.setDescription("ESPECES TEST");
        especes.setIsBankMandatory(false);
        especes.setIsReferenceMandatory(false);
        settlementTypeRepository.save(especes);

        mockMvc.perform(get("/api/referentiels/types-reglement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='CHEQUE-TEST')].banqueObligatoire").value(true))
                .andExpect(jsonPath("$[?(@.code=='ESPECES-TEST')].banqueObligatoire").value(false))
                .andExpect(jsonPath("$[?(@.code=='CHEQUE-TEST')].referenceObligatoire").value(true))
                .andExpect(jsonPath("$[?(@.code=='ESPECES-TEST')].referenceObligatoire").value(false));
    }

    @Test
    void getBanques_filtreSurTypeBan() throws Exception {
        CfgTierType typeBanque = new CfgTierType();
        typeBanque.setId(UUID.randomUUID());
        typeBanque.setCode("BAN");
        typeBanque.setDescription("BANQUE");
        tierTypeRepository.save(typeBanque);

        CfgTierType typeClient = new CfgTierType();
        typeClient.setId(UUID.randomUUID());
        typeClient.setCode("CLI-TEST-BAN");
        typeClient.setDescription("CLIENT");
        tierTypeRepository.save(typeClient);

        CfgTier banque = new CfgTier();
        banque.setId(UUID.randomUUID());
        banque.setCode("BANQUE-TEST-1");
        banque.setDescription("Banque Populaire");
        banque.setCfgTierType(typeBanque);
        tierRepository.save(banque);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLIENT-TEST-BAN");
        client.setDescription("Client non-banque");
        client.setCfgTierType(typeClient);
        tierRepository.save(client);

        mockMvc.perform(get("/api/referentiels/banques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='BANQUE-TEST-1')]").exists())
                .andExpect(jsonPath("$[?(@.code=='CLIENT-TEST-BAN')]").doesNotExist());
    }

    @Test
    void getEtapesProspection_returnsArray() throws Exception {
        mockMvc.perform(get("/api/referentiels/etapes-prospection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getOriginesProspection_exposesBooleanFlags() throws Exception {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode("ORIG-TEST-1");
        origin.setDescription("Origine de test");
        origin.setIsWithProposer(true);
        origin.setIsWithGift(false);
        origin.setIsWithSupport(true);
        originRepository.save(origin);

        mockMvc.perform(get("/api/referentiels/origines-prospection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + origin.getId() + "')].avecProposeur").value(true))
                .andExpect(jsonPath("$[?(@.id=='" + origin.getId() + "')].avecSupport").value(true));
    }

    @Test
    void getSupportsForOrigine_filtersByOrigineId() throws Exception {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode("ORIG-TEST-2");
        originRepository.save(origin);

        ComProspectionOriginSupport support = new ComProspectionOriginSupport();
        support.setId(UUID.randomUUID());
        support.setCode("SUP-TEST-1");
        support.setComProspectionOrigin(origin);
        originSupportRepository.save(support);

        ComProspectionOrigin otherOrigin = new ComProspectionOrigin();
        otherOrigin.setId(UUID.randomUUID());
        otherOrigin.setCode("ORIG-TEST-3");
        originRepository.save(otherOrigin);

        ComProspectionOriginSupport otherSupport = new ComProspectionOriginSupport();
        otherSupport.setId(UUID.randomUUID());
        otherSupport.setCode("SUP-TEST-2");
        otherSupport.setComProspectionOrigin(otherOrigin);
        originSupportRepository.save(otherSupport);

        mockMvc.perform(get("/api/referentiels/origines-prospection/{origineId}/supports", origin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + support.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + otherSupport.getId() + "')]").doesNotExist());
    }

    @Test
    void getTierKindsForType_filtersByTypeId() throws Exception {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-REF-1");
        tierTypeRepository.save(type);

        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("KIND-REF-1");
        kind.setCfgTierType(type);
        tierKindRepository.save(kind);

        CfgTierType otherType = new CfgTierType();
        otherType.setId(UUID.randomUUID());
        otherType.setCode("TYPE-REF-2");
        tierTypeRepository.save(otherType);

        CfgTierKind otherKind = new CfgTierKind();
        otherKind.setId(UUID.randomUUID());
        otherKind.setCode("KIND-REF-2");
        otherKind.setCfgTierType(otherType);
        tierKindRepository.save(otherKind);

        mockMvc.perform(get("/api/referentiels/tier-types/{typeId}/kinds", type.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + kind.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + otherKind.getId() + "')]").doesNotExist());
    }

    @Test
    void getTypesEmploye_filtersByEmpTierType() throws Exception {
        CfgTierType empType = tierTypeRepository.findByCode("EMP").orElseGet(() -> {
            CfgTierType type = new CfgTierType();
            type.setId(UUID.randomUUID());
            type.setCode("EMP");
            return tierTypeRepository.save(type);
        });

        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("KIND-EMP-REF-1");
        kind.setDescription("Commercial de test");
        kind.setCfgTierType(empType);
        tierKindRepository.save(kind);

        CfgTierType otherType = new CfgTierType();
        otherType.setId(UUID.randomUUID());
        otherType.setCode("TYPE-NON-EMP-REF-1");
        tierTypeRepository.save(otherType);

        CfgTierKind otherKind = new CfgTierKind();
        otherKind.setId(UUID.randomUUID());
        otherKind.setCode("KIND-NON-EMP-REF-1");
        otherKind.setCfgTierType(otherType);
        tierKindRepository.save(otherKind);

        mockMvc.perform(get("/api/referentiels/types-employe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + kind.getId() + "')].description").value("Commercial de test"))
                .andExpect(jsonPath("$[?(@.id=='" + otherKind.getId() + "')]").doesNotExist());
    }

    @Test
    void getPays_parsesNumericPhoneCode() throws Exception {
        AdmCountry country = new AdmCountry();
        country.setId(UUID.randomUUID());
        country.setCode("MG-TEST");
        country.setDescription("Madagascar Test");
        country.setPhoneCode("261");
        countryRepository.save(country);

        mockMvc.perform(get("/api/referentiels/pays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + country.getId() + "')].indicatifTelephone").value(261));
    }

    @Test
    void getVilles_paginatesAndFiltersByPays() throws Exception {
        AdmCountry country = new AdmCountry();
        country.setId(UUID.randomUUID());
        country.setCode("PAYS-VILLE-TEST");
        countryRepository.save(country);

        AdmCity city = new AdmCity();
        city.setId(UUID.randomUUID());
        city.setCode("VILLE-TEST-1");
        city.setDescription("Ville de test");
        city.setAdmCountry(country);
        cityRepository.save(city);

        mockMvc.perform(get("/api/referentiels/villes").param("paysId", country.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[?(@.id=='" + city.getId() + "')]").exists());
    }

    @Test
    void getProfilsProspection_groupsProfilsByCategory() throws Exception {
        ComProspectionProfileCategory category = new ComProspectionProfileCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-PROFIL-TEST");
        category.setDescription("Categorie de test");
        category.setMultiSelect(true);
        profileCategoryRepository.save(category);

        ComProspectionProfile profile = new ComProspectionProfile();
        profile.setId(UUID.randomUUID());
        profile.setCode("PROFIL-TEST-1");
        profile.setComProspectionProfileCategory(category);
        profileRepository.save(profile);

        mockMvc.perform(get("/api/referentiels/profils-prospection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + category.getId() + "')].multiSelect").value(true))
                .andExpect(jsonPath("$[?(@.id=='" + category.getId() + "')].profils[?(@.id=='"
                        + profile.getId() + "')]").exists());
    }

    @Test
    void getProjets_returnsArray() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REF-TEST");
        project.setDescription("Projet de test");
        project.setCfgCompany(newActiveCompany());
        projectRepository.save(project);

        mockMvc.perform(get("/api/referentiels/projets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + project.getId() + "')].description").value("Projet de test"));
    }

    @Test
    void getTranches_filtersByProjetId() throws Exception {
        CfgProject projetA = new CfgProject();
        projetA.setId(UUID.randomUUID());
        projetA.setCode("PRJ-REF-TR-A");
        projetA.setCfgCompany(newActiveCompany());
        projectRepository.save(projetA);

        CfgProject projetB = new CfgProject();
        projetB.setId(UUID.randomUUID());
        projetB.setCode("PRJ-REF-TR-B");
        projectRepository.save(projetB);

        CfgTranche trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-REF-A");
        trancheA.setCfgProject(projetA);
        trancheRepository.save(trancheA);

        CfgTranche trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-REF-B");
        trancheB.setCfgProject(projetB);
        trancheRepository.save(trancheB);

        mockMvc.perform(get("/api/referentiels/tranches").param("projetId", projetA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + trancheA.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + trancheB.getId() + "')]").doesNotExist());
    }

    private StkItem newArticleTestItem(String code, CfgTranche tranche, UUID comFolderId) {
        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-REF-ART-" + code);
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-REF-ART-" + code);
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-REF-ART-" + code);
        stkVocationRepository.save(vocation);

        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article de test " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        item.setComFolderId(comFolderId);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return item;
    }

    @Test
    void getArticles_defaultsToDisponible_andExcludesSousArticles() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REF-ART");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REF-ART");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem disponible = newArticleTestItem("ART-REF-DISPO", tranche, null);
        stkItemRepository.save(disponible);

        StkItem occupe = newArticleTestItem("ART-REF-OCCUPE", tranche, UUID.randomUUID());
        stkItemRepository.save(occupe);

        StkItem sousArticle = newArticleTestItem("ART-REF-SOUS", tranche, null);
        sousArticle.setParent(disponible);
        stkItemRepository.save(sousArticle);

        mockMvc.perform(get("/api/referentiels/articles").param("trancheId", tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + disponible.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + occupe.getId() + "')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id=='" + sousArticle.getId() + "')]").doesNotExist());
    }

    /**
     * Les catégories annexes (parking, terrasse, jardin) ne sont jamais un lot principal —
     * mesuré : 0 des 358 dossiers réels — mais 241 parkings RACINE existent au parc et noyaient
     * la sélection (10 des 11 disponibles de FERME BRETONE). Ils sont donc écartés du dropdown
     * Article... sauf si l'appelant demande explicitement leur catégorie : cocher PARKING doit
     * rendre les parkings, pas une liste vide.
     */
    @Test
    void getArticles_ecarteLesCategoriesAnnexes_saufDemandeExplicite() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REF-ANNEXE");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REF-ANNEXE");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem appartement = newArticleTestItem("ART-REF-ANNEXE-APP", tranche, null);
        stkItemRepository.save(appartement);

        // Un parking RACINE (sans parent), disponible — le cas réel AP-PARKING-xx d'APF.
        StkItem parking = newArticleTestItem("ART-REF-ANNEXE-PARKING", tranche, null);
        parking.getStkItemCategory().setCode("PAR");
        stkItemCategoryRepository.save(parking.getStkItemCategory());
        stkItemRepository.save(parking);

        mockMvc.perform(get("/api/referentiels/articles").param("trancheId", tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + appartement.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + parking.getId() + "')]").doesNotExist());

        // Demande explicite de la catégorie PARKING : l'exclusion tombe.
        mockMvc.perform(get("/api/referentiels/articles")
                        .param("trancheId", tranche.getId().toString())
                        .param("categorieId", parking.getStkItemCategory().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + parking.getId() + "')]").exists());
    }

    @Test
    void getArticles_unknownTranche_returns404() throws Exception {
        mockMvc.perform(get("/api/referentiels/articles").param("trancheId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getArticles_filtreParCategorieEtType_etExposeCodeEtNom() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REF-ART-FILTRE");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REF-ART-FILTRE");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        // newArticleTestItem crée une StkItemCategory/StkItemType dédiée par appel — les deux
        // articles ont donc chacun leur propre couple catégorie/type, comme en situation réelle
        // (ex. un APPARTEMENT T2 vs une VILLA T4).
        StkItem appartement = newArticleTestItem("ART-REF-FILTRE-APP", tranche, null);
        stkItemRepository.save(appartement);

        StkItem villa = newArticleTestItem("ART-REF-FILTRE-VILLA", tranche, null);
        stkItemRepository.save(villa);

        mockMvc.perform(get("/api/referentiels/articles")
                        .param("trancheId", tranche.getId().toString())
                        .param("categorieId", appartement.getStkItemCategory().getId().toString())
                        .param("typeId", appartement.getStkItemType().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + appartement.getId() + "' && @.code=='ART-REF-FILTRE-APP' "
                        + "&& @.description=='Article de test ART-REF-FILTRE-APP')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + villa.getId() + "')]").doesNotExist());
    }

    @Test
    void getSousArticles_defaultsToDisponible() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REF-SA");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REF-SA");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem main = newArticleTestItem("ART-REF-SA-MAIN", tranche, null);
        stkItemRepository.save(main);

        StkItem disponible = newArticleTestItem("ART-REF-SA-DISPO", tranche, null);
        disponible.setParent(main);
        stkItemRepository.save(disponible);

        StkItem occupe = newArticleTestItem("ART-REF-SA-OCCUPE", tranche, UUID.randomUUID());
        occupe.setParent(main);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/referentiels/sous-articles").param("articleId", main.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + disponible.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + occupe.getId() + "')]").doesNotExist());
    }

    @Test
    void getSousArticles_unknownArticle_returns404() throws Exception {
        mockMvc.perform(get("/api/referentiels/sous-articles").param("articleId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSousArticles_exposeNomEtPasSeulementLeCode() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REF-SA-NOM");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REF-SA-NOM");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem main = newArticleTestItem("ART-REF-SA-NOM-MAIN", tranche, null);
        stkItemRepository.save(main);

        StkItem annexe = newArticleTestItem("ART-REF-SA-NOM-ANNEXE", tranche, null);
        annexe.setDescription("Place de parking sous-sol n°3");
        annexe.setParent(main);
        stkItemRepository.save(annexe);

        mockMvc.perform(get("/api/referentiels/sous-articles").param("articleId", main.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + annexe.getId() + "' && @.code=='ART-REF-SA-NOM-ANNEXE' "
                        + "&& @.description=='Place de parking sous-sol n°3')]").exists());
    }

    @Test
    void getNatures_returnsStkKindEntries() throws Exception {
        StkKind kind = new StkKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("TER-REF-TEST");
        kind.setDescription("Terrain");
        stkKindRepository.save(kind);

        mockMvc.perform(get("/api/referentiels/natures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + kind.getId() + "' && @.code=='TER-REF-TEST' "
                        + "&& @.description=='Terrain')]").exists());
    }

    @Test
    void getEtages_returnsStkFloorEntries() throws Exception {
        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setCode("RDC-REF-TEST");
        floor.setDescription("Rez de chaussée");
        stkFloorRepository.save(floor);

        mockMvc.perform(get("/api/referentiels/etages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + floor.getId() + "' && @.code=='RDC-REF-TEST' "
                        + "&& @.description=='Rez de chaussée')]").exists());
    }
}

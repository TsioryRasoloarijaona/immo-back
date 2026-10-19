package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Référentiel des tests d'écriture du patrimoine (FEATURE-patrimoine-ecriture.md §11), avec les PKey de
 * production pour les lignes que le service recherche par code. Créer-si-absent : la suite tourne contre
 * {@code yesRemsTest} comme contre le schéma reconstitué en CI ; le {@code @Transactional} des tests annule tout.
 */
@Component
public class FixturePatrimoine {

    public static final UUID ENTITE_STK_ITEM = UUID.fromString("E7C73F70-7507-40D9-AA78-A2825C42B602");
    public static final UUID NATURE_BAT = UUID.fromString("BEDD4235-FBE5-41FA-B5C1-B7EE83C90DE0");
    public static final UUID VOCATION_VENTE = UUID.fromString("7E729AD8-A030-4D20-91CF-9D8E9D89C318");
    public static final UUID VOCATION_PROJET = UUID.fromString("4814C424-F23B-48BB-8E54-020FAA27AF24");

    public static final String AUTEUR = "Admin Patrimoine";

    private final EntityManager entityManager;
    private final StkKindRepository kindRepository;
    private final StkVocationRepository vocationRepository;
    private final StkItemCategoryRepository categoryRepository;
    private final StkItemTypeRepository typeRepository;
    private final StkBuildingRepository buildingRepository;
    private final StkFloorRepository floorRepository;
    private final CfgCompanyRepository companyRepository;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final AdmCountryRepository countryRepository;
    private final AdmUserRepository userRepository;
    private final ComFolderRepository folderRepository;

    @SuppressWarnings("java:S107")
    public FixturePatrimoine(EntityManager entityManager, StkKindRepository kindRepository,
                             StkVocationRepository vocationRepository, StkItemCategoryRepository categoryRepository,
                             StkItemTypeRepository typeRepository, StkBuildingRepository buildingRepository,
                             StkFloorRepository floorRepository, CfgCompanyRepository companyRepository,
                             CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository,
                             AdmCountryRepository countryRepository, AdmUserRepository userRepository,
                             ComFolderRepository folderRepository) {
        this.entityManager = entityManager;
        this.kindRepository = kindRepository;
        this.vocationRepository = vocationRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.countryRepository = countryRepository;
        this.userRepository = userRepository;
        this.folderRepository = folderRepository;
    }

    // ------------------------------------------------------------------ référentiels de production

    /** StrEntity est @Immutable côté JPA : insertion native, idempotente. */
    public void entiteStkItem() {
        entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'StkItem', N'Article')""")
                .setParameter("id", ENTITE_STK_ITEM)
                .executeUpdate();
    }

    public StkKind natureBat() {
        return kindRepository.findById(NATURE_BAT).orElseGet(() -> nature(NATURE_BAT, "BAT"));
    }

    public StkKind nature(UUID id, String code) {
        StkKind kind = new StkKind();
        kind.setId(id);
        kind.setCode(code);
        kind.setDescription(code);
        return kindRepository.save(kind);
    }

    public StkVocation vocationVente() {
        return vocationRepository.findById(VOCATION_VENTE).orElseGet(() -> vocation(VOCATION_VENTE, "VENTE"));
    }

    public StkVocation vocationProjet() {
        return vocationRepository.findById(VOCATION_PROJET).orElseGet(() -> vocation(VOCATION_PROJET, "PROJET"));
    }

    private StkVocation vocation(UUID id, String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(id);
        vocation.setCode(code);
        vocation.setDescription(code);
        return vocationRepository.save(vocation);
    }

    /** Toutes les lignes que la création d'un article recherche par code. */
    public void referentielArticle() {
        entiteStkItem();
        natureBat();
        vocationVente();
        vocationProjet();
    }

    // ------------------------------------------------------------------ référentiels de test

    public StkItemCategory categorie(String code, StkKind nature, boolean pourSousArticle) {
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription(code);
        categorie.setStkKind(nature);
        categorie.setForUnderItem(pourSousArticle);
        return categoryRepository.save(categorie);
    }

    public StkItemType type(String code, StkKind nature, boolean pourSousArticle) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(code);
        type.setStkKind(nature);
        type.setForUnderItem(pourSousArticle);
        return typeRepository.save(type);
    }

    public StkBuilding immeuble(String code) {
        StkBuilding building = new StkBuilding();
        building.setId(UUID.randomUUID());
        building.setCode(code);
        building.setDescription("Immeuble " + code);
        return buildingRepository.save(building);
    }

    public StkFloor etage(String code) {
        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setCode(code);
        floor.setDescription("Etage " + code);
        return floorRepository.save(floor);
    }

    public CfgCompany entite(String code, boolean active) {
        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode(code);
        company.setDescription("Entite " + code);
        company.setIsActive(active);
        return companyRepository.save(company);
    }

    public AdmCountry pays(String code) {
        AdmCountry country = new AdmCountry();
        country.setId(UUID.randomUUID());
        country.setCode(code);
        country.setDescription("Pays " + code);
        return countryRepository.save(country);
    }

    public AdmUser administrateur() {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode("admin.patrimoine." + UUID.randomUUID());
        user.setDescription(AUTEUR);
        user.setDisabled(false);
        return userRepository.save(user);
    }

    public CfgProject projet(String code, CfgCompany entite) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Programme " + code);
        projet.setCfgCompany(entite);
        return projectRepository.save(projet);
    }

    public CfgTranche tranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setDescription("Tranche " + code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    /** Dossier de vente réel : StkItem.ComFolderId porte une FK en CI. */
    public ComFolder dossier(String code) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocationVente());
        return folderRepository.save(folder);
    }

    // ------------------------------------------------------------------ lecture des lignes écrites

    public Tuple ligneProjet(UUID id) {
        return ligne("SELECT * FROM CfgProject WHERE PKey = :id", id);
    }

    public Tuple ligneTranche(UUID id) {
        return ligne("SELECT * FROM CfgTranche WHERE PKey = :id", id);
    }

    public Tuple ligneArticle(UUID id) {
        return ligne("SELECT * FROM StkItem WHERE PKey = :id", id);
    }

    public List<Tuple> habilitations(UUID admUserId) {
        entityManager.flush();
        return entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), CfgCompanyId) AS CfgCompanyId,
                               CONVERT(varchar(36), CfgProjectId) AS CfgProjectId,
                               CONVERT(varchar(36), CfgTrancheId) AS CfgTrancheId,
                               CONVERT(varchar(36), PrjProjectId) AS PrjProjectId
                        FROM AdmUserHabilitation WHERE AdmUserId = :id""", Tuple.class)
                .setParameter("id", admUserId)
                .getResultList();
    }

    private Tuple ligne(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return (Tuple) entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    /** Une colonne uniqueidentifier peut revenir en UUID ou en chaîne selon le pilote. */
    public static String guid(Object valeur) {
        return valeur == null ? null : valeur.toString().toUpperCase(Locale.ROOT);
    }

    public static String guid(UUID id) {
        return id.toString().toUpperCase(Locale.ROOT);
    }

    public static BigDecimal decimal(Object valeur) {
        return valeur == null ? null : new BigDecimal(valeur.toString());
    }
}

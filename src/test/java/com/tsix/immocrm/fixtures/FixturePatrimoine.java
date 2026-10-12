package com.tsix.immocrm.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Référentiel des tests d'écriture du patrimoine (FEATURE-patrimoine-ecriture.md §11). Les lignes qu'OMRAN désigne par
 * constante portent les GUID de la production, créées si absentes : la base de CI a les clés étrangères.
 */
@Component
public class FixturePatrimoine {

    public static final UUID STR_ENTITY_ARTICLE = UUID.fromString("E7C73F70-7507-40D9-AA78-A2825C42B602");
    public static final UUID NATURE_BATIMENT = UUID.fromString("BEDD4235-FBE5-41FA-B5C1-B7EE83C90DE0");
    public static final UUID VOCATION_PROJET = UUID.fromString("4814C424-F23B-48BB-8E54-020FAA27AF24");
    public static final String ROLE_ADMIN = "Administrateur";

    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    private final EntityManager em;
    private final AdmUserRepository userRepository;
    private final CfgCompanyRepository companyRepository;
    private final AdmCountryRepository countryRepository;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkKindRepository kindRepository;
    private final StkVocationRepository vocationRepository;
    private final StkBuildingRepository buildingRepository;
    private final StkFloorRepository floorRepository;
    private final StkItemCategoryRepository categoryRepository;
    private final StkItemTypeRepository typeRepository;
    private final ComFolderRepository folderRepository;

    @SuppressWarnings("java:S107") // fixture de test : un dépôt par référentiel
    public FixturePatrimoine(EntityManager em, AdmUserRepository userRepository,
                             CfgCompanyRepository companyRepository, AdmCountryRepository countryRepository,
                             CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository,
                             StkKindRepository kindRepository, StkVocationRepository vocationRepository,
                             StkBuildingRepository buildingRepository, StkFloorRepository floorRepository,
                             StkItemCategoryRepository categoryRepository, StkItemTypeRepository typeRepository,
                             ComFolderRepository folderRepository) {
        this.folderRepository = folderRepository;
        this.em = em;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.kindRepository = kindRepository;
        this.vocationRepository = vocationRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
    }

    public AdmUser utilisateur(String code) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code);
        user.setDescription("Compte " + code);
        user.setDisabled(false);
        return userRepository.save(user);
    }

    /** Vrai principal {@link AuthenticatedUser} : l'auteur et l'habilitation se résolvent depuis son admUserId. */
    public static RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        List<GrantedAuthority> autorites = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, autorites));
    }

    public static String json(Map<String, ?> corps) {
        try {
            return JSON.writeValueAsString(corps);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public CfgCompany entite(String code, boolean active) {
        CfgCompany entite = new CfgCompany();
        entite.setId(UUID.randomUUID());
        entite.setCode(code);
        entite.setDescription("Entité " + code);
        entite.setIsActive(active);
        return companyRepository.save(entite);
    }

    public AdmCountry pays(String code) {
        AdmCountry pays = new AdmCountry();
        pays.setId(UUID.randomUUID());
        pays.setCode(code);
        pays.setDescription("Pays " + code);
        return countryRepository.save(pays);
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
        tranche.setIsForSale(true);
        return trancheRepository.save(tranche);
    }

    public StkKind natureBatiment() {
        return kindRepository.findById(NATURE_BATIMENT).orElseGet(() -> nature(NATURE_BATIMENT, "BAT"));
    }

    public StkKind nature(UUID id, String code) {
        StkKind nature = new StkKind();
        nature.setId(id);
        nature.setCode(code);
        nature.setDescription("Nature " + code);
        return kindRepository.save(nature);
    }

    /** {@code StrEntity} est {@code @Immutable} côté JPA : insertion native, si absente. */
    public void strEntityArticle() {
        Number existe = (Number) em.createNativeQuery("SELECT COUNT(*) FROM StrEntity WHERE PKey = :id")
                .setParameter("id", STR_ENTITY_ARTICLE).getSingleResult();
        if (existe.intValue() == 0) {
            em.createNativeQuery("INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, 'StkItem', 'Article')")
                    .setParameter("id", STR_ENTITY_ARTICLE).executeUpdate();
        }
    }

    public StkVocation vocation(String code) {
        return vocationRepository.findFirstByCode(code).orElseGet(() -> {
            StkVocation vocation = new StkVocation();
            vocation.setId(code.equals("PROJET") ? VOCATION_PROJET : UUID.randomUUID());
            vocation.setCode(code);
            vocation.setDescription("Vocation " + code);
            return vocationRepository.save(vocation);
        });
    }

    public StkBuilding immeuble(String code, int ordre) {
        StkBuilding immeuble = new StkBuilding();
        immeuble.setId(UUID.randomUUID());
        immeuble.setCode(code);
        immeuble.setDescription("Immeuble " + code);
        immeuble.setSorting(ordre);
        return buildingRepository.save(immeuble);
    }

    public StkFloor etage(String code, int ordre) {
        StkFloor etage = new StkFloor();
        etage.setId(UUID.randomUUID());
        etage.setCode(code);
        etage.setDescription("Étage " + code);
        etage.setSorting(ordre);
        return floorRepository.save(etage);
    }

    public StkItemCategory categorie(String code, StkKind nature, boolean pourSousArticle) {
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription("Catégorie " + code);
        categorie.setStkKind(nature);
        categorie.setForUnderItem(pourSousArticle);
        return categoryRepository.save(categorie);
    }

    /** Tout ce qu'une création d'article exige, dans une tranche neuve (codes suffixés pour rester uniques). */
    public record ReferentielArticle(CfgTranche tranche, StkBuilding immeuble, StkFloor etage,
                                     StkItemCategory categorie, StkItemType type,
                                     StkItemCategory categorieSous, StkItemType typeSous, StkVocation vente) {
    }

    public ReferentielArticle referentielArticle(String suffixe) {
        strEntityArticle();
        StkKind batiment = natureBatiment();
        CfgTranche tranche = tranche("TRA-" + suffixe, projet("PRJ-" + suffixe, entite("ENT-" + suffixe, true)));
        return new ReferentielArticle(tranche, immeuble("IMM-" + suffixe, 1), etage("ETG-" + suffixe, 1),
                categorie("APP-" + suffixe, batiment, false), type("T3-" + suffixe, batiment, false),
                categorie("PAR-" + suffixe, batiment, true), type("PRIVE-" + suffixe, batiment, true),
                vocation("VENTE"));
    }

    /** Dossier de vente minimal : sert à rendre un article « occupé » ({@code ComFolderId} sans clé étrangère). */
    public ComFolder dossier(String code, StkVocation vocation) {
        ComFolder dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode(code);
        dossier.setStkVocation(vocation);
        dossier.setTotalToPay(BigDecimal.ZERO);
        return folderRepository.save(dossier);
    }

    public StkItemType type(String code, StkKind nature, boolean pourSousArticle) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription("Type " + code);
        type.setStkKind(nature);
        type.setForUnderItem(pourSousArticle);
        return typeRepository.save(type);
    }
}

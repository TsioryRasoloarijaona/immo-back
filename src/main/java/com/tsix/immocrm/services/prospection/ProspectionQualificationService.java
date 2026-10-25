package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.AjouterAutreTrancheRequest;
import com.tsix.immocrm.dto.prospection.AjouterCategorieRequest;
import com.tsix.immocrm.dto.prospection.AjouterProfilRequest;
import com.tsix.immocrm.dto.prospection.AjouterTypeBienRequest;
import com.tsix.immocrm.dto.prospection.AjouterVilleRequest;
import com.tsix.immocrm.dto.prospection.ProspectionAutreTrancheDto;
import com.tsix.immocrm.dto.prospection.ProspectionCategorieDto;
import com.tsix.immocrm.dto.prospection.ProspectionProfilDto;
import com.tsix.immocrm.dto.prospection.ProspectionTypeBienDto;
import com.tsix.immocrm.dto.prospection.ProspectionVilleDto;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionCategory;
import com.tsix.immocrm.entities.com.ComProspectionCity;
import com.tsix.immocrm.entities.com.ComProspectionProfile;
import com.tsix.immocrm.entities.com.ComProspectionProject;
import com.tsix.immocrm.entities.com.ComProspectionSelectedProfile;
import com.tsix.immocrm.entities.com.ComProspectionType;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.com.ComProspectionCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionCityRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProjectRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComProspectionSelectedProfileRepository;
import com.tsix.immocrm.repositories.com.ComProspectionTypeRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.services.perimetre.ObjetsVisibles;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Prospection multi-valued qualification sub-resources: villes, categories, types de
 * bien, profils, autres tranches. Each is a thin join table onto {@code ComProspection}.
 */
@Service
@Transactional(readOnly = true)
public class ProspectionQualificationService {

    private final ComProspectionRepository prospectionRepository;
    private final ComProspectionCityRepository cityLinkRepository;
    private final AdmCityRepository cityRepository;
    private final ComProspectionCategoryRepository categoryLinkRepository;
    private final StkItemCategoryRepository itemCategoryRepository;
    private final ComProspectionTypeRepository typeLinkRepository;
    private final StkItemTypeRepository itemTypeRepository;
    private final ComProspectionSelectedProfileRepository profileLinkRepository;
    private final ComProspectionProfileRepository profileRepository;
    private final ComProspectionProjectRepository projectLinkRepository;
    private final ObjetsVisibles objetsVisibles;
    private final Clock clock;

    public ProspectionQualificationService(ComProspectionRepository prospectionRepository,
                                            ComProspectionCityRepository cityLinkRepository,
                                            AdmCityRepository cityRepository,
                                            ComProspectionCategoryRepository categoryLinkRepository,
                                            StkItemCategoryRepository itemCategoryRepository,
                                            ComProspectionTypeRepository typeLinkRepository,
                                            StkItemTypeRepository itemTypeRepository,
                                            ComProspectionSelectedProfileRepository profileLinkRepository,
                                            ComProspectionProfileRepository profileRepository,
                                            ComProspectionProjectRepository projectLinkRepository,
                                            ObjetsVisibles objetsVisibles,
                                            Clock clock) {
        this.clock = clock;
        this.prospectionRepository = prospectionRepository;
        this.cityLinkRepository = cityLinkRepository;
        this.cityRepository = cityRepository;
        this.categoryLinkRepository = categoryLinkRepository;
        this.itemCategoryRepository = itemCategoryRepository;
        this.typeLinkRepository = typeLinkRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.profileLinkRepository = profileLinkRepository;
        this.profileRepository = profileRepository;
        this.projectLinkRepository = projectLinkRepository;
        this.objetsVisibles = objetsVisibles;
    }

    private ComProspection findProspection(UUID id) {
        return prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));
    }

    // -- Villes --

    public List<ProspectionVilleDto> villes(UUID prospectionId) {
        findProspection(prospectionId);
        return cityLinkRepository.findByComProspectionId(prospectionId).stream()
                .map(ProspectionQualificationService::toVilleDto).toList();
    }

    @Transactional
    public ProspectionVilleDto ajouterVille(UUID prospectionId, AjouterVilleRequest request) {
        ComProspection prospection = findProspection(prospectionId);
        Requis.champ(request.villeId(), "villeId est requis");
        AdmCity city = cityRepository.findById(request.villeId())
                .orElseThrow(() -> new InvalidRequestException("AdmCity introuvable: " + request.villeId()));

        ComProspectionCity link = new ComProspectionCity();
        link.setId(UUID.randomUUID());
        link.setComProspection(prospection);
        link.setAdmCity(city);
        return toVilleDto(cityLinkRepository.save(link));
    }

    @Transactional
    public void retirerVille(UUID prospectionId, UUID itemId) {
        findProspection(prospectionId);
        cityLinkRepository.deleteByIdAndComProspectionId(itemId, prospectionId);
    }

    private static ProspectionVilleDto toVilleDto(ComProspectionCity link) {
        return new ProspectionVilleDto(
                link.getId(),
                link.getAdmCity() != null ? link.getAdmCity().getId() : null,
                link.getAdmCity() != null ? link.getAdmCity().getDescription() : null
        );
    }

    // -- Categories --

    public List<ProspectionCategorieDto> categories(UUID prospectionId) {
        findProspection(prospectionId);
        return categoryLinkRepository.findByComProspectionId(prospectionId).stream()
                .map(ProspectionQualificationService::toCategorieDto).toList();
    }

    @Transactional
    public ProspectionCategorieDto ajouterCategorie(UUID prospectionId, AjouterCategorieRequest request) {
        ComProspection prospection = findProspection(prospectionId);
        Requis.champ(request.categorieId(), "categorieId est requis");
        StkItemCategory category = itemCategoryRepository.findById(request.categorieId())
                .orElseThrow(() -> new InvalidRequestException("StkItemCategory introuvable: " + request.categorieId()));

        ComProspectionCategory link = new ComProspectionCategory();
        link.setId(UUID.randomUUID());
        link.setComProspection(prospection);
        link.setStkItemCategory(category);
        return toCategorieDto(categoryLinkRepository.save(link));
    }

    @Transactional
    public void retirerCategorie(UUID prospectionId, UUID itemId) {
        findProspection(prospectionId);
        categoryLinkRepository.deleteByIdAndComProspectionId(itemId, prospectionId);
    }

    private static ProspectionCategorieDto toCategorieDto(ComProspectionCategory link) {
        return new ProspectionCategorieDto(
                link.getId(),
                link.getStkItemCategory() != null ? link.getStkItemCategory().getId() : null,
                link.getStkItemCategory() != null ? link.getStkItemCategory().getDescription() : null
        );
    }

    // -- Types de bien --

    public List<ProspectionTypeBienDto> typesBien(UUID prospectionId) {
        findProspection(prospectionId);
        return typeLinkRepository.findByComProspectionId(prospectionId).stream()
                .map(ProspectionQualificationService::toTypeBienDto).toList();
    }

    @Transactional
    public ProspectionTypeBienDto ajouterTypeBien(UUID prospectionId, AjouterTypeBienRequest request) {
        ComProspection prospection = findProspection(prospectionId);
        Requis.champ(request.typeBienId(), "typeBienId est requis");
        StkItemType type = itemTypeRepository.findById(request.typeBienId())
                .orElseThrow(() -> new InvalidRequestException("StkItemType introuvable: " + request.typeBienId()));

        ComProspectionType link = new ComProspectionType();
        link.setId(UUID.randomUUID());
        link.setComProspection(prospection);
        link.setStkItemType(type);
        return toTypeBienDto(typeLinkRepository.save(link));
    }

    @Transactional
    public void retirerTypeBien(UUID prospectionId, UUID itemId) {
        findProspection(prospectionId);
        typeLinkRepository.deleteByIdAndComProspectionId(itemId, prospectionId);
    }

    private static ProspectionTypeBienDto toTypeBienDto(ComProspectionType link) {
        return new ProspectionTypeBienDto(
                link.getId(),
                link.getStkItemType() != null ? link.getStkItemType().getId() : null,
                link.getStkItemType() != null ? link.getStkItemType().getDescription() : null
        );
    }

    // -- Profils --

    public List<ProspectionProfilDto> profils(UUID prospectionId) {
        findProspection(prospectionId);
        return profileLinkRepository.findByComProspectionIdOrderByUserCreateDateTimeDesc(prospectionId).stream()
                .map(ProspectionQualificationService::toProfilDto).toList();
    }

    @Transactional
    public ProspectionProfilDto ajouterProfil(UUID prospectionId, AjouterProfilRequest request) {
        ComProspection prospection = findProspection(prospectionId);
        Requis.champ(request.profilId(), "profilId est requis");
        ComProspectionProfile profile = profileRepository.findById(request.profilId())
                .orElseThrow(() -> new InvalidRequestException("ComProspectionProfile introuvable: " + request.profilId()));

        ComProspectionSelectedProfile link = new ComProspectionSelectedProfile();
        link.setId(UUID.randomUUID());
        link.setComProspection(prospection);
        link.setComProspectionProfile(profile);
        link.setUserCreateDateTime(LocalDateTime.now(clock));
        return toProfilDto(profileLinkRepository.save(link));
    }

    @Transactional
    public void retirerProfil(UUID prospectionId, UUID itemId) {
        findProspection(prospectionId);
        profileLinkRepository.deleteByIdAndComProspectionId(itemId, prospectionId);
    }

    private static ProspectionProfilDto toProfilDto(ComProspectionSelectedProfile link) {
        ComProspectionProfile profile = link.getComProspectionProfile();
        return new ProspectionProfilDto(
                link.getId(),
                Champs.de(profile, ComProspectionProfile::getId),
                Champs.de(profile, ComProspectionProfile::getDescription),
                profile != null && profile.getComProspectionProfileCategory() != null
                        ? profile.getComProspectionProfileCategory().getId() : null,
                profile != null && profile.getComProspectionProfileCategory() != null
                        ? profile.getComProspectionProfileCategory().getDescription() : null
        );
    }

    // -- Autres tranches --

    public List<ProspectionAutreTrancheDto> autresTranches(UUID prospectionId) {
        findProspection(prospectionId);
        return projectLinkRepository.findByComProspectionIdOrderByUserCreateDateTimeDesc(prospectionId).stream()
                .map(ProspectionQualificationService::toAutreTrancheDto).toList();
    }

    @Transactional
    public ProspectionAutreTrancheDto ajouterAutreTranche(UUID prospectionId, AjouterAutreTrancheRequest request) {
        ComProspection prospection = findProspection(prospectionId);
        Requis.champ(request.trancheId(), "trancheId est requis");
        CfgTranche tranche = objetsVisibles.tranche(request.trancheId());

        ComProspectionProject link = new ComProspectionProject();
        link.setId(UUID.randomUUID());
        link.setComProspection(prospection);
        link.setStkHierarchy(tranche);
        link.setUserCreateDateTime(LocalDateTime.now(clock));
        if (request.articleId() != null) {
            link.setStkItem(objetsVisibles.articleDeLaTranche(request.articleId(), tranche));
        }
        return toAutreTrancheDto(projectLinkRepository.save(link));
    }

    @Transactional
    public void retirerAutreTranche(UUID prospectionId, UUID itemId) {
        findProspection(prospectionId);
        projectLinkRepository.deleteByIdAndComProspectionId(itemId, prospectionId);
    }

    private static ProspectionAutreTrancheDto toAutreTrancheDto(ComProspectionProject link) {
        return new ProspectionAutreTrancheDto(
                link.getId(),
                link.getStkHierarchy() != null ? link.getStkHierarchy().getId() : null,
                link.getStkHierarchy() != null ? link.getStkHierarchy().getCode() : null,
                link.getStkHierarchy() != null ? link.getStkHierarchy().getDescription() : null,
                link.getStkItem() != null ? link.getStkItem().getId() : null,
                link.getStkItem() != null ? link.getStkItem().getCode() : null,
                link.getStkItem() != null ? link.getStkItem().getDescription() : null
        );
    }
}

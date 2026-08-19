package com.tsix.immocrm.services.referentiel;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.OrigineProspectionDto;
import com.tsix.immocrm.dto.referentiel.PaysDto;
import com.tsix.immocrm.dto.referentiel.ProfilCategorieDto;
import com.tsix.immocrm.dto.referentiel.ProfilDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.dto.referentiel.TitreDto;
import com.tsix.immocrm.dto.referentiel.VilleDto;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmFamilySituation;
import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmMatrimonialRegime;
import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.adm.AdmResidenceType;
import com.tsix.immocrm.entities.adm.AdmSalary;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.adm.AdmFamilySituationRepository;
import com.tsix.immocrm.repositories.adm.AdmIdentityDocumentTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmMatrimonialRegimeRepository;
import com.tsix.immocrm.repositories.adm.AdmNationalityRepository;
import com.tsix.immocrm.repositories.adm.AdmProfessionRepository;
import com.tsix.immocrm.repositories.adm.AdmResidenceTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmSalaryRepository;
import com.tsix.immocrm.repositories.adm.AdmSexTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmTierTitleRepository;
import com.tsix.immocrm.repositories.cfg.CfgRelationTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Backs the {@code /api/referentiels/*} lookup endpoints (GAP-02). */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final ComProspectionKindRepository prospectionKindRepository;
    private final ComProspectionOriginRepository prospectionOriginRepository;
    private final ComProspectionOriginSupportRepository prospectionOriginSupportRepository;
    private final ComProspectionClosingReasonRepository closingReasonRepository;
    private final ComActionMessageTypeRepository actionMessageTypeRepository;
    private final ComActionRepository actionRepository;
    private final StkVocationRepository vocationRepository;
    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierKindRepository tierKindRepository;
    private final AdmTierTitleRepository tierTitleRepository;
    private final AdmSexTypeRepository sexTypeRepository;
    private final AdmCountryRepository countryRepository;
    private final AdmNationalityRepository nationalityRepository;
    private final AdmFamilySituationRepository familySituationRepository;
    private final AdmResidenceTypeRepository residenceTypeRepository;
    private final AdmMatrimonialRegimeRepository matrimonialRegimeRepository;
    private final AdmIdentityDocumentTypeRepository identityDocumentTypeRepository;
    private final AdmProfessionRepository professionRepository;
    private final AdmSalaryRepository salaryRepository;
    private final StkItemCategoryRepository itemCategoryRepository;
    private final StkItemTypeRepository itemTypeRepository;
    private final CfgRelationTypeRepository relationTypeRepository;
    private final AdmCityRepository cityRepository;
    private final ComProspectionProfileCategoryRepository profileCategoryRepository;
    private final ComProspectionProfileRepository profileRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;

    public ReferentielService(ComProspectionKindRepository prospectionKindRepository,
                               ComProspectionOriginRepository prospectionOriginRepository,
                               ComProspectionOriginSupportRepository prospectionOriginSupportRepository,
                               ComProspectionClosingReasonRepository closingReasonRepository,
                               ComActionMessageTypeRepository actionMessageTypeRepository,
                               ComActionRepository actionRepository,
                               StkVocationRepository vocationRepository,
                               CfgTierTypeRepository tierTypeRepository,
                               CfgTierKindRepository tierKindRepository,
                               AdmTierTitleRepository tierTitleRepository,
                               AdmSexTypeRepository sexTypeRepository,
                               AdmCountryRepository countryRepository,
                               AdmNationalityRepository nationalityRepository,
                               AdmFamilySituationRepository familySituationRepository,
                               AdmResidenceTypeRepository residenceTypeRepository,
                               AdmMatrimonialRegimeRepository matrimonialRegimeRepository,
                               AdmIdentityDocumentTypeRepository identityDocumentTypeRepository,
                               AdmProfessionRepository professionRepository,
                               AdmSalaryRepository salaryRepository,
                               StkItemCategoryRepository itemCategoryRepository,
                               StkItemTypeRepository itemTypeRepository,
                               CfgRelationTypeRepository relationTypeRepository,
                               AdmCityRepository cityRepository,
                               ComProspectionProfileCategoryRepository profileCategoryRepository,
                               ComProspectionProfileRepository profileRepository,
                               ComPaymentMethodRepository paymentMethodRepository,
                               ComSettlementCategoryRepository settlementCategoryRepository) {
        this.prospectionKindRepository = prospectionKindRepository;
        this.prospectionOriginRepository = prospectionOriginRepository;
        this.prospectionOriginSupportRepository = prospectionOriginSupportRepository;
        this.closingReasonRepository = closingReasonRepository;
        this.actionMessageTypeRepository = actionMessageTypeRepository;
        this.actionRepository = actionRepository;
        this.vocationRepository = vocationRepository;
        this.tierTypeRepository = tierTypeRepository;
        this.tierKindRepository = tierKindRepository;
        this.tierTitleRepository = tierTitleRepository;
        this.sexTypeRepository = sexTypeRepository;
        this.countryRepository = countryRepository;
        this.nationalityRepository = nationalityRepository;
        this.familySituationRepository = familySituationRepository;
        this.residenceTypeRepository = residenceTypeRepository;
        this.matrimonialRegimeRepository = matrimonialRegimeRepository;
        this.identityDocumentTypeRepository = identityDocumentTypeRepository;
        this.professionRepository = professionRepository;
        this.salaryRepository = salaryRepository;
        this.itemCategoryRepository = itemCategoryRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.relationTypeRepository = relationTypeRepository;
        this.cityRepository = cityRepository;
        this.profileCategoryRepository = profileCategoryRepository;
        this.profileRepository = profileRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
    }

    public List<ReferentielItemDto> etapesProspection() {
        return prospectionKindRepository.findAll().stream().map(k -> new ReferentielItemDto(
                k.getId(), k.getCode(), k.getDescription(), null)).toList();
    }

    public List<OrigineProspectionDto> originesProspection() {
        return prospectionOriginRepository.findAllByOrderBySortingAsc().stream().map(o -> new OrigineProspectionDto(
                o.getId(), o.getCode(), o.getDescription(),
                o.getIsWithProposer(), o.getIsWithGift(), o.getIsWithSupport())).toList();
    }

    public List<ReferentielItemDto> supportsForOrigine(UUID origineId) {
        return prospectionOriginSupportRepository.findByComProspectionOriginId(origineId).stream()
                .map(s -> new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> raisonsCloture() {
        return closingReasonRepository.findAll().stream().map(r -> new ReferentielItemDto(
                r.getId(), r.getCode(), r.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> typesContactProspection() {
        return actionMessageTypeRepository.findAll().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> actionsCommerciales() {
        return actionRepository.findAll().stream().map(a -> new ReferentielItemDto(
                a.getId(), a.getCode(), a.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> vocations() {
        return vocationRepository.findAll().stream().map(v -> new ReferentielItemDto(
                v.getId(), v.getCode(), v.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> tierTypes() {
        return tierTypeRepository.findAllByOrderBySortingAsc().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), t.getSorting())).toList();
    }

    public List<ReferentielItemDto> tierKindsForType(UUID typeId) {
        return tierKindRepository.findByCfgTierTypeIdOrderBySortingAsc(typeId).stream().map(k -> new ReferentielItemDto(
                k.getId(), k.getCode(), k.getDescription(), k.getSorting())).toList();
    }

    public List<TitreDto> titres() {
        return tierTitleRepository.findAllByOrderBySortingAsc().stream().map(t -> new TitreDto(
                t.getId(), t.getCode(), t.getDescription(),
                t.getAdmSexType() != null ? t.getAdmSexType().getId() : null)).toList();
    }

    public List<ReferentielItemDto> sexes() {
        return sexTypeRepository.findAllByOrderBySortingAsc().stream().map(s -> new ReferentielItemDto(
                s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    public List<PaysDto> pays() {
        return countryRepository.findAllByOrderByDescriptionAsc().stream().map(ReferentielService::toPaysDto).toList();
    }

    public List<ReferentielItemDto> nationalites() {
        return nationalityRepository.findAllByOrderByDescriptionAsc().stream().map(n -> new ReferentielItemDto(
                n.getId(), n.getCode(), n.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> situationsFamiliales() {
        return familySituationRepository.findAllByOrderBySortingAsc().stream().map(s -> new ReferentielItemDto(
                s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    public List<ReferentielItemDto> typesResidence() {
        return residenceTypeRepository.findAllByOrderBySortingAsc().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), t.getSorting())).toList();
    }

    public List<ReferentielItemDto> regimesMatrimoniaux() {
        return matrimonialRegimeRepository.findAllByOrderBySortingAsc().stream().map(r -> new ReferentielItemDto(
                r.getId(), r.getCode(), r.getDescription(), r.getSorting())).toList();
    }

    public List<ReferentielItemDto> typesPieceIdentite() {
        return identityDocumentTypeRepository.findAllByOrderBySortingAsc().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), t.getSorting())).toList();
    }

    public List<ReferentielItemDto> professions() {
        return professionRepository.findAllByOrderByDescriptionAsc().stream().map(p -> new ReferentielItemDto(
                p.getId(), p.getCode(), p.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> salaires() {
        return salaryRepository.findAllByOrderBySortingAsc().stream().map(s -> new ReferentielItemDto(
                s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    public List<ReferentielItemDto> categoriesBien() {
        return itemCategoryRepository.findAll().stream().map(c -> new ReferentielItemDto(
                c.getId(), c.getCode(), c.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> typesBien() {
        return itemTypeRepository.findAll().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> typesRelationContact() {
        return relationTypeRepository.findAll().stream().map(r -> new ReferentielItemDto(
                r.getId(), r.getCode(), r.getDescription(), null)).toList();
    }

    public Page<VilleDto> villes(UUID paysId, String search, Pageable pageable) {
        Specification<AdmCity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (paysId != null) {
                predicates.add(cb.equal(root.get("admCountry").get("id"), paysId));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return cityRepository.findAll(spec, pageable).map(ReferentielService::toVilleDto);
    }

    public List<ReferentielItemDto> modesPaiement() {
        return paymentMethodRepository.findAll().stream().map(m -> new ReferentielItemDto(
                m.getId(), m.getCode(), m.getDescription(), null)).toList();
    }

    /** {@code ComSettlementCategory} — the "Catégorie" lookup for échéancier lines (AVANCE, AUTO-FINANCEMENT, FRAIS SYNDIC...). */
    public List<ReferentielItemDto> categoriesEcheance() {
        return settlementCategoryRepository.findAll().stream().map(c -> new ReferentielItemDto(
                c.getId(), c.getCode(), c.getDescription(), null)).toList();
    }

    public List<ProfilCategorieDto> profilsProspection() {
        return profileCategoryRepository.findAllByOrderBySortingAsc().stream().map(cat -> new ProfilCategorieDto(
                cat.getId(), cat.getCode(), cat.getDescription(), cat.getMultiSelect(),
                profileRepository.findByComProspectionProfileCategoryIdOrderBySortingAsc(cat.getId()).stream()
                        .map(p -> new ProfilDto(p.getId(), p.getCode(), p.getDescription()))
                        .toList()
        )).toList();
    }

    private static PaysDto toPaysDto(AdmCountry c) {
        return new PaysDto(c.getId(), c.getCode(), c.getIsoAlpha3(), c.getDescription(), parseIntOrNull(c.getPhoneCode()));
    }

    private static VilleDto toVilleDto(AdmCity c) {
        return new VilleDto(c.getId(), c.getCode(), c.getDescription(),
                c.getAdmCountry() != null ? c.getAdmCountry().getId() : null);
    }

    private static Integer parseIntOrNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
